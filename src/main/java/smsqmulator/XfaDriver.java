
package smsqmulator;
/**
 *
 * The File Access device driver template class that allows SMSQE to access native files.
 * This should be extended by other drivers.
 * It implements 8 "drives" (xxx1_ to xxx8_) which point to native dirs.
 * @see DeviceDriver DeviceDriver for more information on device drivers.
 * <p>
 * The driver opens NFAFiles,one for each SMSQE file. Each NfaFile gets a unique number (integer 0 - 0xffff) which gets put into the
 * SMSQE channel definition block (offset 0x1E).
 * For each drive, the driver maintains a HashMap ‹Integer,XfaFile› so that it can find the NfaFile with the integer.
 * <p> 
 * 
 * @author and copyright (c) 2012 -2017 Wolfgang Lenerz
 * @version 
 * 1.06 openFile as directory, use root dir if no part of the file is a dir ; use correct subdir found, if any. .
 * 1.05 openFile don't crash if directory not found.
 * 1.04 openFile better handling of names in chan defn block when opening a directory file
 * 1.03 setNames adjusted ;trying to open a non-existing file returns err.fdnf and not medium is full
 * 1.02 if file open is exclusive, file is locked in the fileOpen method here.
 * 1.01 modified open.
 * 1.00 use StringBuilder whenever appropriate
 * 00.04 closeAllFiles doesn't set the cpu to null.
 * @version 00.03 added writeBack
 * @version 00.02 let the SfaDriver readHeaderOk set the error returns in case of error.
 * @version 00.01 use a temporary header for Sfa drives    
 * @version 00.00 initial version
 */
public class XfaDriver implements DeviceDriver
{
    protected int deviceID;                                 //'NFA0';
    protected String [] nativeDir =new String[8];           // names of the native dirs
    protected smsqmulator.cpu.MC68000Cpu cpu;                   // the cpu being emulated
    private final java.util.HashMap <Integer,XfaFile>[]fileMap;  // one hashmap per drive
    protected boolean [][]fileNumber;                       // a primitive way to identify a file : if[x][y] is true, a file object at that index [y] exists, at drive [x]
    protected int usage;                                    // usage name for device.
    protected int filenameChange=0;                         //  0 = no change,  1= set to UPPER, 2 =set to lower.
    protected java.nio.ByteBuffer tempHeader;               // this should speed up Sfa drive operations
    
    /*
     *  TODO close all channels on a drive when drive name changes. 'NFA_DRIVE x,yyyyyyy'
     */
    
    /**
     * Creates the object with a cpu.
     * 
     * @param cpu the cpu the object refers to for I/O operations and more.
     */
    public XfaDriver(smsqmulator.cpu.MC68000Cpu cpu)
    {
        this.cpu=cpu;
        for (int i=0;i<this.nativeDir.length;i++)
        {
            this.nativeDir[i]="";                           // initialise drive names to empty strings
        }
        this.fileMap = new java.util.HashMap[8];            // ok, ok warning  unchecked conversion but do I REALLY need a collection here?
        for (int i=0;i<8;i++)
        {
            this.fileMap[i]=new java.util.HashMap <Integer,XfaFile>(500);
        }
        this.fileNumber=new boolean [8][65536];             // a simple map for knowing which integer can be used for the next file open
        if (this.deviceID==Types.SFADriver)
            this.tempHeader=java.nio.ByteBuffer.allocate(Types.SFAHeaderLength);// keep a buffer for SFA io operations
    }
    
    
    /**
     * Opens a file.
     * 
     * @param devDriverLinkageBlock pointer to the device driver linkage block (a3).
     * @param channelDefinitionBlock pointer to the channel defintiion block (a0).
     * @param openType  which kind of open is requested? (0 - old exclusive, 1 old shared,2 new exclusive,3 new overwrite,4 open dir).
     * @param driveNumber which drivenumver the file is to be opened on.
     * @param fname the (SMSQE) name of the file to open.
     * @param uncased the lower cased (SMSQE) name of the file to open.
     * 
     * @return true if open OK, false if not.
     * @see DeviceDriver DeviceDriver for more information on file open operations.
     */
    @Override
    public boolean openFile(int devDriverLinkageBlock, int channelDefinitionBlock,int openType,int driveNumber,byte[]fname,byte[]uncased) 
    {
        this.cpu.data_regs[0]=Types.ERR_ITNF;               // signal not found YET
        if (this.nativeDir[driveNumber].isEmpty())          // no dir set for this drive
        {
            return false;                                   // file was for this device (but wasn't found)
        }
       
        // first, convert from QL filename to java filename
        String filename=this.cpu.readSmsqeString(channelDefinitionBlock+0x32);// filename now as java name with accented chars
        if (filename==null)
            return false;
        if (filename.toLowerCase().equals("*d2d"))
        {
            this.cpu.data_regs[0]=Types.ERR_NIMP;
            return false;
        }
        switch (this.filenameChange)
        {
            case 0:
            default:
                break;
            case 1 :
                filename=filename.toUpperCase();
                break;
            case 2 :
                filename=filename.toLowerCase();
                break;
        }
        java.io.File file=new java.io.File(this.nativeDir[driveNumber]+convertFilename(this.nativeDir[driveNumber],filename));
        switch (openType)
        {
            case 0:                                         // old exclusive - open an existing file for read/write access
                if (!file.exists())                         // such a file must already exist, else error not found
                    return false;                            
                if (!file.canWrite() && !file.isDirectory()) // I must be able to read/write to this file unless this is a DIR
                {
                    this.cpu.data_regs[0]=Types.ERR_FDIU;// signal file in use
                    return false;  
                }
                if (this.deviceID==Types.SFADriver)         // if this is for an SFA file, make sure that file IS an SFA file.
                {
                    if (SfaDriver.readHeaderOK(file, this.cpu,this.tempHeader)!=1) 
                    {
                        return false;                        // the error returns have been set by the readHeaderOk routine
                    }   
                }
                fileOpen(driveNumber,file,filename,channelDefinitionBlock,file.isDirectory(), true);
                break;
                
            case 1:                                         // old shared - open an existing file for read only access 
                if (!file.canRead())
                    return false;
                if (this.deviceID==Types.SFADriver)         // make sure, if this is for an SFA file, that file IS an SFA file.
                {
                    if (SfaDriver.readHeaderOK(file, this.cpu,this.tempHeader)==0) 
                    {
                        return false;  
                    }   
                }
                fileOpen(driveNumber,file,filename,channelDefinitionBlock,file.isDirectory(), false);
                break;
                
            case 2:                                         // new exclusive - open a not yet existing file for read/write access
                if (file.exists() || file.isDirectory())          
                {
                    this.cpu.data_regs[0]=Types.ERR_FEX;// signal file already exists
                    return false;  
                }                 
                fileOpen(driveNumber,file,filename,channelDefinitionBlock,false, true);
                break;
                
            case 3:                                         // open overwrite - delete old file if it exists
                if (file.isDirectory())                     // shurely shome mishtake
                {
                    this.cpu.data_regs[0]=Types.ERR_FDIU;// signal file is in use
                    return false;  
                }
                if (file.exists())
                {
                    if (this.deviceID==Types.SFADriver)     // make sure, if this is for an SFA file, that file IS an SFA file.
                    {
                        if (SfaDriver.readHeaderOK(file, this.cpu,this.tempHeader)!=1) 
                        {
                            return false;  
                        }   
                    }
                    boolean success= file.delete();         // try to delete file if it already exists
                    if (! success)
                    {
                        this.cpu.data_regs[0]=Types.ERR_FDIU;// signal file is in use
                        return false;  
                    }
                }
                fileOpen(driveNumber,file,filename,channelDefinitionBlock,false, true);
                break;
                
            case 4:                                         // open dir - this is special - if any name on the path isn't a subdir, use the earlier dir, SMSQE does the filetrings
 /*
                String oldName=this.nativeDir[driveNumber];
                String newName=this.nativeDir[driveNumber];
                String []subnames=filename.split("_");
                int i=0;
                file=new java.io.File(newName);
                while (file.exists() && file.isDirectory())
                {
                    oldName=newName;
                    if (i>=subnames.length)
                        break;
                    newName+=java.io.File.separator+subnames[i++]; 
                    file=new java.io.File(newName);
                }
                file=new java.io.File(oldName);             // if the file we're trying to open is not a dir, use the last dir we could
                if (!file.isDirectory())
                    return false;                            //  if file isn't a dir return not found
                fileOpen(driveNumber,file,oldName,channelDefinitionBlock,true, true);
                break;
            */
                if (filename.endsWith("_"))
                  filename=filename.substring(0, filename.length()-1);
                String oldName=this.nativeDir[driveNumber];
                StringBuilder newName= new StringBuilder (250);
                newName.append(oldName).append(java.io.File.separator).append(filename);
                file=new java.io.File(newName.toString());
                
                // first try to open this file as is
                if ((file.exists() && file.isDirectory()))
                {
                      fileOpen(driveNumber,file,filename,channelDefinitionBlock,true, true);
                      return true;
                }
                
                // if that fails, try to compose the filename from the possible subdirs
                String []subnames=filename.split("_");
                newName.setLength(0);
                newName.append(oldName);
                for (int i=0;i<subnames.length;i++)
                {
                    newName.append(subnames[i]).append(java.io.File.separator);
                }
                file=new java.io.File(newName.toString());
                if ((file.exists() && file.isDirectory()))
                {
                      fileOpen(driveNumber,file,filename,channelDefinitionBlock,true, true);
                      return true;
                }
                // if all else fails, try to build up filename gradually
//                if (subnames.length==1)
  //                  return false;                               // but couldn't
                newName.setLength(0);
                newName.append(oldName);
                int current = newName.length();
                int nml = composeSubdirName(subnames,0,newName);
                if (nml==current)                               // I couldn(t open this at all
                {
                    newName.setLength(0);
                    newName.append(oldName);                    // so use the root dir (!) 
                }
                else                                            // "else" added in 1.07
                    newName.setLength(nml);                     // added in 1.07
                file=new java.io.File(newName.toString());
                if (!file.isDirectory())
                    return false;
                if (nml==current)
                    oldName="";                                 // use root dir
                else
                    oldName=newName.substring(oldName.length(), nml-1).replace(java.io.File.separator, "_");
                
                if (!filename.equals(oldName))
                {
                    cpu.writeSmsqeString(channelDefinitionBlock+0x32,oldName,-1);
                }
                fileOpen(driveNumber,file,oldName,channelDefinitionBlock,true, true);
           
                return true;
                
            case 255:                                       // delete this file
                deleteFile(driveNumber,fname,file);
                break;
            
        }
        return true;                                        // the file was for this device
    }
    
    /**
     * Gradually build up the name.
     * 
     * @param subdirs
     * @param index
     * @param sb      the initial dir/device name, must be a valid dir and contain file separator at end
     * @return 
     */
    private int composeSubdirName(String[]subdirs,int index,StringBuilder sb)
    {
        // gradually build up the name
        java.io.File f;         
        int current = sb.length();                              // length of last name that was a correct directory fiiename
        for (int i = index;i<subdirs.length;i++)
        {
            int currlen=sb.length();
            sb.append(subdirs[i]).append(java.io.File.separator);// try next subdir
            f=new java.io.File(sb.toString());
            if (f.exists() && f.isDirectory())
            {
                return composeSubdirName(subdirs,i+1,sb);
            }
            else
            {
                if (currlen!=current)
                {
                    sb.setLength(currlen-1);                        // remove the file separator
                    sb.append("_");
                }
                else
                    sb.setLength(current);
                sb.append(subdirs[i]).append(java.io.File.separator) ; // and replace it wirh "_", and try again  f=new java.io.File(sb.toString());
                f=new java.io.File(sb.toString());
                if (f.exists() && f.isDirectory())
                {
                    return composeSubdirName(subdirs,i+1,sb);
                }
            }
        }
        return current;
    }
    

    /**
     * Closes the file. For an explanation of the parameters, see the DeviceDriver interface.
     * 
     * @param driveNumber the drive on which the file to close lies.
     * @param fileID ID (numbrer) of the file to close.
     * 
     * @return true if file was for this device, else false.
     * 
     * @see DeviceDriver For an explanation of the parameters, see the DeviceDriver class.
     */
    @Override
    public boolean closeFile(int driveNumber,int fileID) 
    {                        // this is not a file on for this device at all
        XfaFile xfaFile=this.fileMap[driveNumber].get(fileID);
        if (xfaFile==null)
        {
            this.cpu.data_regs[0]=Types.ERR_ICHN;           // this channel wasn't open????
            return true;
        }
        else
        {
            xfaFile.close(this.cpu);
            this.fileMap[driveNumber].remove(fileID);
            this.fileNumber[driveNumber][fileID]=false;
            return true;
        }
    }

   /**
     * Formats a medium. This will fail for sfa and nfa devices.
     * 
     * @param formatName the name to give to the formatted drive.
     * 
     * @return <code>true</code> if deviceID corresponded to this device, <code>false</code> if not.
     * <p>  This method MUST set the D0 register to signal success (or not) of the operation to SMSQE.
     */
    @Override
    public boolean formatMedium(String formatName,inifile.IniFile inifile)
    {
        this.cpu.data_regs[0]= Types.ERR_NIMP;              // signal format not implemented
        return true;  
    }
    
    /**
     * Deletes a file.
     * @param fname the name of the file to delete..
     * @param driveNumber number of drive on which to delete the file.
     * @param file the file to delete.
     * 
     * @return <code>true</code> if the file deleted.
     */
    @Override
    public boolean deleteFile(int driveNumber,byte[] fname,java.io.File file) 
    {
        String filename=new String(fname);
        if (fileIsOpen(driveNumber,filename))
        {
            this.cpu.data_regs[0]=Types.ERR_FDIU;               // signal file in use
        }
        else
        {
            if (file.delete())
                this.cpu.data_regs[0]=0;// signal DELETE ok
            else
            {
                if (file.exists())
                    this.cpu.data_regs[0]=Types.ERR_FDIU;       // signal file in use
                else
                    this.cpu.data_regs[0]=0;                    // signal DELETE ok : it doesn't even exist
            }
        }
        return true;
    } 
    
    /**
     * Checks whether a file is already open.
     * 
     * @param driveNumber on what drive the file is.
     * @param filename Name of the file too check.
     * 
     * @return <code>true</code> if file is open, <code>false</code> if not.
     */
    private boolean fileIsOpen(int driveNumber,String filename)
    {
        java.util.Collection c = this.fileMap[driveNumber].values();
        java.util.Iterator itr = c.iterator();
        while(itr.hasNext())
        {
            String t = ((XfaFile)itr.next()).getFilename();
            if (t.equals(filename))
                return true;       
        }
        return false;
    }
   
    
    /**
     * Finds the first free file number that can be used.
     * @param drive the drive for which to find a file number.
     * @return the file number (0...65535) or -1 if problem.
     */
    private int getFreeNumber(int drive)
    {
        for (int i=0;i<this.fileNumber[drive].length;i++)
        {
            if (!this.fileNumber[drive][i])
                return i;
        }
        return -1;
    }
    
    /**
     * Sets the names for the drives
     * 
     * @param names an 8 elements String array with the names.
     * @param inifile the initialization object.
     * @param suppressWarnings = true if warnings about absent etc. devices should be suppressed (e.g. in case of reset)s
     * @param forceRemoval true if files/drives should removed before being remounted, IGNORED here!
     * 
     * @return <code>true</code> if deviceDriver  name is for my device, <code>false</code> if not.
     */
    @Override
    public boolean setNames(String[]names,inifile.IniFile inifile,boolean forceRemoval, boolean suppressWarnings)
    { 
        if (names.length!=8)      // this is not for this device at all, or array wrong
            return false;                       
        for (int i=0;i<8;i++)
        {
            if (names[i]!=null)
                this.nativeDir[i]=names[i];
        }                                 
        return true;
    }
    
    
    /**
     * Sets the native dir this drive points to.
     * 
     * @param driveNumber number of drive to set dir for, starting at 0.
     * @param dirname the name of the dir. NO CHECK IS MADE THAT THIS IS A VALID DIR!!!!!.
     */
    public void setDirname(int driveNumber,String dirname)
    {
        if (driveNumber<0 || driveNumber>7 || dirname==null)
            return;
        this.nativeDir[driveNumber]=dirname;
    }
    
    /**
     * Opens a file. The preconditions for opening a file according to the type of open must have been prechecked
     * and this method must only be called if file opening is allowed.
     * 
     * @param driveNumber number of drive.
     * @param file the file to open
     * @param filename (SMSQE) name of file.
     * @param channelDefinitionBlock pointer to the chan def block in cpu memory space.
     * @param isDir true if file is to be a dir.
     * @param exclusiveAccess if this file is to have exclusive (i.e r/w) access.
     * 
     * @return true if file opened, false if not.
     */
    private boolean fileOpen(int driveNumber,java.io.File file,String filename,int channelDefinitionBlock,boolean isDir,boolean exclusiveAccess)
    { 
        XfaFile xfaFile;
        java.io.RandomAccessFile raFile=null;
        boolean doLock=false;
        try
        {
            if (isDir)
            {
                xfaFile=new XfaFile(file,filename,true,driveNumber,this.nativeDir,this.deviceID,this.filenameChange,this.usage);// open file as dir
            }
            else
            {
                if (exclusiveAccess)
                {
                    raFile = new java.io.RandomAccessFile(file, "rw");  // open new 
                    doLock=true;
                }
                else
                {
                    raFile = new java.io.RandomAccessFile(file, "r");   // open old shared file
                }
                java.nio.channels.FileChannel  inoutChannel = raFile.getChannel();
                if (doLock)
                {
                    java.nio.channels.FileLock fl=null;
                    try
                    {
                        fl= inoutChannel.tryLock();
                    }
                    catch (Exception e)
                    {/*NOP*/}                                    // I can't, just ignrore, then.
                    if (fl==null)
                    {
                        this.cpu.data_regs[0]=Types.ERR_FDIU;// signal drive full
                        this.cpu.reg_sr&=~4;
                        return false;
                    }
                }
                xfaFile=new XfaFile(raFile,inoutChannel,doLock,file,filename,isDir,this.deviceID,driveNumber,this.nativeDir,this.filenameChange,this.usage);
            }
            int fileNbr=getFreeNumber(driveNumber);         // try to find a space for the file.
            if (fileNbr==-1)
            {
                this.cpu.data_regs[0]=Types.ERR_DRFL;// signal drive full
                return false;
            }
            this.fileNumber[driveNumber][fileNbr]=true;
            this.fileMap[driveNumber].put(fileNbr,xfaFile);
            this.cpu.writeMemoryWord(channelDefinitionBlock+0x1e, fileNbr);
            this.cpu.writeMemoryWord(channelDefinitionBlock+0x5e, fileNbr);
            this.cpu.data_regs[0]=0;                            // open went OK
        }
        catch (java.io.FileNotFoundException e)
        {
            this.cpu.data_regs[0]=Types.ERR_FDNF;                   // signal not found(!)
            return false;
            
        }
        catch (Exception e)
        {
            try
            {
                if (raFile!=null)
                    raFile.close();
            }
            catch (Exception whatever)
            {/*nop*/}
            this.cpu.data_regs[0]=Types.ERR_DRFL;// signal drive full (!)
            return false;
        }
        return true;
    }

    /**
     * This checks whether a file is from this driver and if so returns it (if it exists). 
     * 
     * <p> For params see info at :
     * @see smsqmulator.DeviceDriver#trap3OK(int driveNumber,int trapKey,int channelDefinitionBlock,int fileNbr)
     */
    @Override
    public boolean trap3OK(int driveNumber,int trapKey,int channelDefinitionBlock,int fileNbr)
    { 
        XfaFile nfaFile=this.fileMap[driveNumber].get(fileNbr);
        if (nfaFile==null)
        {
            this.cpu.data_regs[0]=Types.ERR_FDNF;                   //*** was word, not long
        }
        else
            nfaFile.handleTrap3(trapKey,channelDefinitionBlock,this.cpu,this.nativeDir[driveNumber]);
        return true;
    }
    
    /**
     * This sets the usage name of the device, eg. "NFA_USE WIN"
     * 
     * @param usage the usage name as an int
     */
    @Override
    public void setUsage (int usage)
    {
        this.usage=usage;
    }
    
    /**
     * This is an attempt to handle smsqe filenames intelligently.
     * The smsqe "_" separator may be part of a simple filename, but also of a directory.
     * 
     * @param mainDir the name of the main dir for this drive.
     * @param  originalName the original filename.
     * 
     * @return the converted filename as used by java.
     */
    public static final String convertFilename(String mainDir,String originalName)
    {
        if (originalName==null || originalName.isEmpty())
            return originalName;
        String []subnames=originalName.split("_");          // all individual subnames
        if (subnames.length<2)
            return originalName.replace("_", java.io.File.separator); // there is no subdir divider in name, or else last char
        String currentName=mainDir+java.io.File.separator;  // this where we start
        int current=0;
        java.io.File file;
        while (current<subnames.length)
        {
            currentName+=java.io.File.separator+subnames[current];
            file=new java.io.File(currentName);
            if (!file.exists() || !file.isDirectory())      // this file doesn't exist, so it can't be a subdir
            {
                return makeName(subnames,current);
            }
            current++;
        }
        return makeName(subnames,subnames.length-1);
    }
    /**
     * This is an attempt to handle smsqe filenames intelligently.
     * The smsqe "_" separator may be part of a simple filename, but also of a directory.
     * 
     * @param mainDir the name of the main dir for this drive.
     * @param  originalName the original filename.
     * 
     * @return the converted filename as used by java.
     */
    public static final String convertFilename2(String mainDir,String originalName)
    {
        if (originalName==null || originalName.isEmpty())
            return originalName;
        String []subnames=originalName.split("_");          // all individual subnames
        if (subnames.length<2)
            return originalName.replace("_", java.io.File.separator); // there is no subdir divider in name, or else last char
        String currentName=mainDir+java.io.File.separator;  // this where we start
        int current=0;
        java.io.File file;
        while (current<subnames.length)
        {
            currentName+=java.io.File.separator+subnames[current];
            file=new java.io.File(currentName);
            if (!file.exists() || !file.isDirectory())      // this file doesn't exist, so it can't be a subdir
            {
                return makeName(subnames,current);
            }
            current++;
        }
        return makeName(subnames,subnames.length-1);
    }
    
    /**
     * This makes a filename with native file separators replacing the "_" where appropriate, ie where the "_" denotes a subdir and not simply a part of the filename.
     * 
     * @param subnames the array to convert : this contains all parts of the original filename that were separated by a "_".
     * @param current pointer to first element of the array (name part) that isn't part of a subdir.
     * 
     * @return the complete filename.
     */
    public static final String makeName (String []subnames,int current)
    {
        StringBuilder result=new StringBuilder();
        for (int i=0;i<current;i++)
        {
            result.append(subnames[i]).append(java.io.File.separator);
        }
        for (int i=current;i<subnames.length;i++)
        {
            result.append(subnames[i]).append("_");
        }
        if(result.charAt(result.length()-1)=='_')
            result.setLength(result.length()-1);
        return result.toString();
    }
    
    /**
     * This sets how a filename case should be changed (or not).
     * 
     * @param change how a filename case should be changed (or not):
     * <ul>
     *      <li>0 = no change</li>
     *      <li>1 = all to upper case</li>
     *      <li>2 = all to lower case</li>
     * </ul>
     */
    @Override
    public void setFilenameChange(int change)
    {
        if (change>2 || change<0)
            return;
        this.filenameChange=change;
    }
    
    /**
     * Gets the device ID for this device, eg. SFA0.
     * 
     * @return the device ID as int.
     */
    @Override
    public int getDeviceID()
    {
        return this.deviceID;
    }
    
    /**
     * Gets the names of the native directories used for each drive of the device.
     * <p> In keeping with standard SMSQE practice, it is presumed throughout that each device may have 8 drives.
     *
     * @return A string array with the names, or <code>null</code> if the device ID wasn't mine.
     */
    @Override
    public String[] getNames()
    {
        return this.nativeDir.clone();
    }
    
    /**
     * Closes all files on all drives on this device.
     */
    @Override
    public void closeAllFiles()
    {
        XfaFile file;
        for (int drive=0;drive<8;drive++)
        {
            int count=this.fileNumber[drive].length;        // nbr of elements in sub array
            for (int files=0;files<count;files++)
            {
                if (this.fileNumber[drive][files])          // any files open?
                {
                    file=this.fileMap[drive].get(files);    // yes, get it from map      
                    file.close(this.cpu);                   // close file  
                    this.fileMap[drive].remove(files);      // remove from map
                    this.fileNumber[drive][files]=false;    // show no file open
                }
            }
        }
       // this.cpu=null;                                      // remove reference to object
    }
    
    /**
     * Sets the cpu used by the device driver.
     */
    @Override
    public void setCpu (smsqmulator.cpu.MC68000Cpu cpu)
    {
        this.cpu=cpu;
    }
    
    /**
     * Gets the name of the native directory used for one drive of the device, or the current usage name of the device.
     * <p> In keeping with standard SMSQE practice, it is presumed throughout that each device may have 8 drives.
     *
     * @param drive the drive (from 1 to 8) for which the name is to be obtained, or 0 if the current usage name is to be returned.
     * 
     * @return A string array with the name, or <code>null</code> if the device ID wasn't mine, or "" if the device ID was mine but the drive didn't exist..
     */
    @Override
    public String getName(int drive)
    {
        drive--;
        if ((this.nativeDir[drive]!=null) &&(!this.nativeDir[drive].isEmpty()))
            return this.nativeDir[drive];
        return "";
    }
    
     /**
     * This gets the usage name of the device.
     * 
     * @return the usage name as an int, or 0 if this wasn't for this device
     */
    @Override
    public int getUsage()
    {
        return usage;
    }
   
    
    /**
     * Writes the drive back to a native file
     * 
     * @param driveNbr the drive number (1...8)
     * 
     */
    @Override
    public void writeBack (int driveNbr)
    {
        this.cpu.data_regs[0]=Types.ERR_NIMP;
    }
}
