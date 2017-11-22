package smsqmulator;


/**
 * This is the device driver enabling SMSQmulator to read QXL.Win files.
 * It maintains a list of 8 drives.
 * 
 * @author and copyright (c) wolfgang Lenerz 2013-2017.
 * 
 * @version  
 * 1.04 setNames adjusted.
 * 1.04 modified open.
 * 1.03 implement closeAllFiles.
 * 1.02 setNames catches UnsupportedOperationException, if file lock can't be acquired.
 * 1.01 added writeBack, some changes for MEM device, use inifile in setNames.
 * 1.00 a rewrite of the QxlWin driver.
 * 
 */
public class WinDriver implements DeviceDriver
{
    protected WinDrive[] drives=new WinDrive[8];            // the eight drives
    protected int deviceID=Types.WINDriver;                 //'WIN0';
    protected String [] nativeDir =new String[8];           // names of the native dirs
    protected smsqmulator.cpu.MC68000Cpu cpu;               // the cpu being emulated
    protected int usage=this.deviceID;                      // usage name for device.
    public static final int QLWA=0x514c5741;                // 'QLWA'
    protected Warnings warnings;                            // differents warning config items
    
    /**
     * Length of one file header.
     */
    public final static int HEADER_LENGTH=64;               // length of one file header
    
  /**
   * Creates the driver object.
   * 
   * @param cpu the cpu for this object.
   * @param warn object with warning flags.
   */
    public WinDriver(smsqmulator.cpu.MC68000Cpu cpu,Warnings warn)
    {
        this.cpu=cpu;
        this.warnings=warn;
    }
    
    /************************************************** File handling ************************************************///
    
    /**
     * Opens a file.
     * 
     * @param devDriverLinkageBlock pointer to the device driver linkage block (a3).
     * @param channelDefinitionBlock pointer to the channel definition block (a0).
     * @param openType  which kind of open is requested? (0 - old exclusive, 1 old shared,2 new exclusive,3 new overwrite,4 open dir).
     * @param driveNumber which drive number the file is to be opened on.
     * @param filename the (SMSQE) name of the file to open.
     * @param uncased the lower cased (SMSQE) name of the file to open.
     * 
     * @return true if file opened ok.
     */
    @Override
    public boolean openFile(int devDriverLinkageBlock, int channelDefinitionBlock, int openType, int driveNumber, 
            byte[] filename,byte[]uncased) 
    {
        if (this.drives[driveNumber]==null)
        {
            this.cpu.data_regs[0]=Types.ERR_FDNF;
            return false;
        }
        return this.drives[driveNumber].openFile(devDriverLinkageBlock, channelDefinitionBlock,openType,driveNumber, filename,uncased) ;
    }
    
    /**
     * Closes the file.
     * 
     * @param driveNbr which drive number the file to be closed is on.
     * @param fileID the file number (index into array).
     * 
     * @return true if file was on this device, false if the file wasn't for this device or if the drive, whilst for this device, doesn't exist.
     * This does NOT indicate the success of the close call which is set in register D0.
     */
    @Override
    public boolean closeFile(int driveNbr,int fileID) 
    {
      if (this.drives[driveNbr]==null)
            return false;  
      this.drives[driveNbr].closeFile(fileID);
      return true;
    }
    
    @Override
    /**
     * Handles trap#3 I/O calls by calling the corresponding method on the drive.
     * 
     * @param driveNumber number of drive on which the file is supposed to be.
     * @param trapKey what kind of trap #3 is it?
     * @param channelDefinitionBlock  pointer to the SMSQE channel definition block (a0)
     * @param fileNbr the file number given by the ddd when file was opoened (A0+0x1e)
     * 
     * @return <code>false</code> if the drive does not exist, else <code>truee</code>. This says nothing about the success of the trap #3 
     * operation in itself, it only shows that the device driver recognized that this I/O operation was for a file on one of its existing drives.
     * <p>  The drive's method MUST set the D0 register to signal success (or not) of the operation to SMSQE.
     */
    public boolean trap3OK(int driveNumber, int trapKey, int channelDefinitionBlock, int fileNbr) 
    {
        if (this.drives[driveNumber]==null)
            return false;                                       // I don't see how this could happen.
        this.drives[driveNumber].trap3OK(driveNumber, trapKey, fileNbr);// let the correct drive handle this.
        return true;
    }
    
    @Override
    /**
     * Deletes a file, by mimicking an open call with openType 255.
     * 
     * @return false
     */
    public boolean deleteFile(int driveNumber, byte[] filename, java.io.File file) 
    { 
    //    if (this.drives[driveNumber]==null)
            return false;
      //  this.drives[driveNumber].openFile(this.deviceID, 0,0,255,driveNumber, filename) ;
        //return true;
    }
    
   /**
     * Formats a medium.
     * 
     * @param formatName the name to give to the formatted drive.
     * @param inifile the ".ini" file with initialized values.
     * 
     * @return <code>true</code> if deviceID corresponded to this device, <code>false</code> if not.
     * <p>  This method MUST set the D0 register to signal success (or not) of the operation to SMSQE.
     * 
     */
    @Override
    public boolean formatMedium(String formatName,inifile.IniFile inifile)
    {
        this.cpu.data_regs[0]=Types.ERR_IPAR;               // preset bad parameter
        if (formatName.charAt(4)!=Types.UNDERSCORE)         // format must be WINx_Number or WINx_Number_Name
            return true;  
        int driveNbr=(cpu.data_regs[1]&0xffff)-1;           // get drive number
        int formatSize;
        int temp=formatName.indexOf(Types.UNDERSCORE, 5);   // is there a name and not only a number?
        if (temp==-1)
            temp=formatName.length();
        try
        {
            formatSize=Integer.parseInt(formatName.substring(5,temp));// get format size in MiB
        }
        catch (Exception e)
        {
            return true;
        }
        if (driveNbr<0 || driveNbr>7)
            return true;                                    // drives go from 0 to 7;
        if (this.nativeDir[driveNbr]==null || this.nativeDir[driveNbr].isEmpty() || formatSize>2000 || formatSize<1)
            return true;                                    // there is no name for the device, or file is to be bigger than 2000 MiB
        java.io.File f=new java.io.File(this.nativeDir[driveNbr]);
        if (f.exists())
        {
            this.cpu.data_regs[0]=Types.ERR_FEX;            // file already exists
            return true;
        }
        formatSize*=1024*2;                                 // size is now in 512 byte sectors (!!!!)
        java.io.RandomAccessFile raFile=null;               // a random access file...
        java.nio.channels.FileChannel ioChannel;            // ... and its associated channel
        java.nio.ByteBuffer firstSector;                    // a buffer to write to the channel
        try
        {
            raFile=new java.io.RandomAccessFile(this.nativeDir[driveNbr],"rw");// read/write access to this file
            ioChannel = raFile.getChannel();
            firstSector=java.nio.ByteBuffer.allocate(512);
            int i;
            // now look how many clusters we need for the drive and what size the clusters can be.
            // cluster numbers are word sized, so we can have a maximum of 65536 clusters.
            // to make things easier, I'l assume that a cluster always holds a power of 2 number of clusters.
            boolean foundit=false;
            temp=0;
            for (i=4;i<65;i*=2)
            {
                temp=formatSize/i;                          // nbr of clusters at this clustersize
                if (temp<65536)
                {
                    foundit=true;
                    break;
                }
            }
            if (!foundit)
                throw new java.io.IOException();            // this **should** never happen since I limited the size of the file to be created to 2000MiB
            
            formatSize=temp*i;                              // possibly adjusted size, to number of clusters * clustersize
            firstSector.putShort(WinDrive.QWA_NGRP,(short)(temp));// nbr of clusters necessary to cover the entire drive
            int maxSects=temp;
            firstSector.putShort(WinDrive.QWA_SCTG,(short)(i));// nbr of sectors per cluster
            // here : temp = nbr of clusters, i = nbr of 512 byte sectors per cluster
             // calculate the nbr of clusters needed for the FAT : each 512 byte sector can hold the pointer to 256 clusters, so each cluster can hold the pointer to i*256 clusters
            int fatClusters=temp/(i*256);                   // basic nbr of clusters needed for the fat
            if ((fatClusters*i*256)-32<temp)                // the first sector cannot hold 512 clusters, since the disk header takes 64 bytes out of it
                fatClusters++;
            for (i=0;i<(512-64)/2;i++)
                firstSector.putShort(WinDrive.QWA_GMAP +i*2,(short)(i+1));//put in pointers to next clusters

            firstSector.putShort(WinDrive.QWA_FGRP,(short)(temp-fatClusters-1));// nbr of free clusters
            firstSector.putInt(WinDrive.QWA_GMAP +(fatClusters-1)*2,0);// pointer to last cluster for FAT AND ALSO of root dir!
            firstSector.putShort(WinDrive.QWA_ROOT,(short) fatClusters);// pointer to first cluster of root dir  
            firstSector.putInt(WinDrive.QWA_RLEN,WinDriver.HEADER_LENGTH);  //  root dir length - this is the header for the root dir itself, contains just rubbish
            firstSector.putShort(WinDrive.QWA_FREE,(short) (fatClusters+1));// pointer to first free cluster
            temp>>>=8;
            temp++;
            firstSector.putShort(WinDrive.QWA_SCTM,(short)(temp)); // sectors per map - this seems to be right, but I don't know why!
            firstSector.putInt(0,WinDriver.QLWA);        // Qxl.win drive ID
            temp=formatName.indexOf(Types.UNDERSCORE, 5);// is there a name and not only a number?
            if (temp==-1)
            {                                               // no there isn't, set drive name (e.g "win1")
                firstSector.putShort(4,(short) 4);          // length of drive name
                temp=this.usage+driveNbr+1;                 // name of device
                firstSector.putInt(6,temp);                 // into space
                temp=4;
            }
            else
            {                                               // there is a name
                temp++;                                     // point after the underscore
                int A1=this.cpu.addr_regs[1]+temp+2;        // point to start of format name
                temp=formatName.length()-temp;              // name length
                if (temp>20)
                    temp=20;
                if (temp<0)
                    temp=0;                                 // must be between 0 and 20
                for (i=0;i<temp;i++)
                    firstSector.put(6+i,(byte)(this.cpu.readMemoryByte(A1++)&0xff));
                firstSector.putShort(4,(short) temp);       // length of drive name
            }
            //now pad name with spaces, temp holds name length
            byte fill=0x20;
            for (i=6+temp;i<WinDrive.QWA_SPR0;i++)
                firstSector.put(i,fill);                    // fill the remaining space with spaces
            temp=(int)(System.currentTimeMillis()/1000);
            firstSector.putInt(WinDrive.QWA_UCHK,temp);     // update check
            firstSector.putShort(WinDrive.QWA_NMAP,(short)(1));// nbr of FATs 
            ioChannel.write(firstSector);                   // write header + FAT
            firstSector.position(0);
            temp=0xe0;
            int j;
            fatClusters=fatClusters*(firstSector.getShort(WinDrive.QWA_SCTG)&0xffff)-1;// nbr of sectors for FAT
            for (i=1;i<fatClusters;i++)
            {
                for (j=0;j<512;j+=2)                        // fill the sector with pointer to next clusters
                {
                    temp++;
                    if (temp>=maxSects)                     // HUH?????
                        firstSector.putShort(j,(short)0);
                    else
                        firstSector.putShort(j,(short)(temp&0xffff));
                }
                ioChannel.position(i*512);
                ioChannel.write(firstSector);               
                firstSector.position(0);
            }
            firstSector=java.nio.ByteBuffer.allocate(512);//  empty (filled with 0)
            ioChannel.position((formatSize-1)*512);
            ioChannel.write(firstSector);                   // fill the entire "disk" with 0s
            raFile.close();
            this.drives[driveNbr]=new WinDrive(this.cpu,this.nativeDir[driveNbr],this,driveNbr+0x31,this.warnings,inifile);
            this.cpu.data_regs[2]=formatSize;               // nbr of good sectors
            this.cpu.data_regs[1]=formatSize;               // total nbr of sectors
            this.cpu.data_regs[0]=0;
        }
        catch (java.io.FileNotFoundException e)
        {
            if (raFile!=null)
            {
                try
                {
                    raFile.close();
                }
                catch (Exception ex)
                {/*nop*/}
            }
            javax.swing.JOptionPane.showMessageDialog(null, Localization.Texts[74]+f.getAbsolutePath()+Localization.Texts[91]+
                     "\n"+Localization.Texts[92], Localization.Texts[30],javax.swing.JOptionPane.ERROR_MESSAGE);
            
        }
        catch (Exception e)
        {   
            if (raFile!=null)
            {
                try
                {
                    raFile.close();
                }
                catch (Exception ex)
                {/*nop*/}
            }
        }
        return true;  
    }
  
    /************************** Getting/setting the names of the native files containing the drives *************************************/

    /**
     * Sets the names for the native files containing the drives and creates the drive objects.
     * 
     * @param names an 8 element string array containing the native names of the container files for the drives.
     * @param inifile the file with initialisation data.
     * @param forceRemove = true if devices should be removed then remounted.
     * @param suppressWarnings = true if warnings about absent etc. devices should be suppressed (e.g. in case of reset).
     * 
     * @return <code>true</code> if operation successful.
     */
    @Override
    public boolean setNames(String[] names,inifile.IniFile inifile,boolean forceRemove, boolean suppressWarnings) 
    {
        if (names.length==8)
        { 
            if (inifile.getTrueOrFalse("DISABLE-WIN-DEVICE"))
                return true;
            StringBuilder errStr=new StringBuilder();            
            for (int i=0;i<8;i++)
            {
                // first of all, unset all drives that aren't equal to the one being set now
                if (this.nativeDir[i]!=null && !this.nativeDir[i].isEmpty())
                {   
                    if (forceRemove ||names[i]==null || names[i].isEmpty())// if new name is null, unset drive
                    {
                        if(this.drives[i]!=null)
                        {
                            this.drives[i].unuse();
                            this.drives[i]=null;
                        }
                    }
                    else if(!this.nativeDir[i].toLowerCase().equals(names[i].toLowerCase()))//if new names != old name, unset drive
                    {
                        if(this.drives[i]!=null)
                        {
                            this.drives[i].unuse();
                            this.drives[i]=null;
                        }
                    }
                }
            }
            this.nativeDir=names.clone();                   // keep a local copy of the names
            
            for (int i=0;i<8;i++)
            {
                try
                {
                    if (this.nativeDir[i]!=null && !this.nativeDir[i].isEmpty())
                    {
                        if (this.drives[i]!=null)           // if the drive already exists and the file is the same, don't do anything...
                        {
                            if(!this.nativeDir[i].equals(this.drives[i].getDrivename()))
                            {
                                this.drives[i].unuse();
                                this.drives[i]=new WinDrive(this.cpu,this.nativeDir[i],this,i+0x31,this.warnings,inifile);
                            }
                        }
                        else
                        {
                            this.drives[i]=new WinDrive(this.cpu,this.nativeDir[i],this,i+0x31,this.warnings,inifile);// drive doesn't exist yet
                        }
                        if (this.drives[i].isReadOnly() && this.warnings.warnIfQXLDriveIsReadOnly)
                        {
                            errStr.append(this.nativeDir[i]).append("\n");
                            errStr.append(Localization.Texts[105]).append("\n");// possibly warn if drive is read only
                        }
                    }
                }
                catch (java.io.FileNotFoundException e)     // the native file wasn't found at all
                {
                    if (this.warnings.warnIfNonexistingQXLDrive)
                    {
                        errStr.append(this.nativeDir[i]).append("\n");
                        errStr.append(Localization.Texts[64]).append("\n");
                    }
                    this.drives[i]=null;
                }
                catch (java.net.ProtocolException e)        // the main dir couldn't be read
                {
                    errStr.append(this.nativeDir[i]).append("\n");
                    errStr.append(e.getMessage()).append("\n");
                    this.drives[i]=null;
                }
                catch (ArrayIndexOutOfBoundsException e)    // file had seriously wrong clusters for FAT
                {
                    errStr.append(this.nativeDir[i]).append("\n");
                    errStr.append(Localization.Texts[65]).append("\n");
                    this.drives[i]=null;
                }
                catch (DoNothingException e)                // file had non compliant clusters and user didn't want to continue using it
                {
                    this.drives[i]=null;
                }
               
                catch (IncorrectFiletypeException e)        // file is NOT a valid qxl.win file
                {
                    errStr.append(this.nativeDir[i]).append("\n");
                    errStr.append(Localization.Texts[111]).append("\n");
                    this.drives[i]=null;
                }
                catch (UnsupportedOperationException e)
                {
                    errStr.append(this.nativeDir[i]).append("\n");
                    errStr.append(Localization.Texts[126]).append("\n");
                }
                catch (Exception e)                         // any other exception
                {
                    errStr.append(this.nativeDir[i]);
                    if (e.getLocalizedMessage()!=null)
                        errStr.append(e.getLocalizedMessage()).append("\n");
                    else
                        errStr.append(e.toString()).append("\n");
                    this.drives[i]=null;
                }
            }
            if (errStr.length()!=0 && !suppressWarnings)
            {
                StringBuilder errs=new StringBuilder();
                Helper.reportError(Localization.Texts[45], Localization.Texts[49]+"\n"+errStr.toString(), null);
            }
            return true;
        }
        else
            return false;
    }

    @Override
    /**
     * Gets the names of the native files used for each drive of the device.
     * <p> In keeping with standard SMSQE practice, it is presumed throughout that each device may have 8 drives.
     * 
     * @return An 8 elements string array with the names, or <code>null</code> if the device ID wasn't mine. 
     * Individual elements may be <code>null</code> or empty if the corresponding drive isn't assigned.
     */
    public String[] getNames() 
    {
        return this.nativeDir.clone();
    }
    
    /**
     * Gets the names of the native file used for one drive of the device.
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
    
    /********************************************* Getter / setters ********************************************/

    /**
     * This sets the usage name of the device, eg. "WIN_USE QXL"
     * 
     * @param usage the usage name as an int
     */
    @Override
    public void setUsage (int usage)
    {
        this.usage=usage;
    }
    
    /**
     * This gets the usage name of the device.
     * @return the usage name as an int, or 0 if this wasn't for this device
     */
    @Override
    public int getUsage()
    {
        return this.usage;
    }
   
    /**
     * Gets this device's deviceID.
     * 
     * @return this device's deviceID.
     */
    @Override
    public int getDeviceID() 
    {
        return this.deviceID;
    }

     /**
     * Sets the cpu to be used.
     * 
     * @param cpu the cpu to use.
     */
    @Override
    public void setCpu(smsqmulator.cpu.MC68000Cpu cpu) 
    {
        this.cpu=cpu;
        for (WinDrive drive :this.drives)
        {
            if (drive!=null)
                drive.setCpu(cpu);
        }
    }

    /*************************************** Misc routines ***********************************************/
    
    /**
     * Changes the filename case. Not necessary for this driver.
     * 
     * @param change unused here
     */
    @Override
    public void setFilenameChange(int change) 
    {
    }

    @Override 
    /**
     * Closes all files opened by all drives of this device driver.
     */
    public void closeAllFiles() 
    { 
        for (int i=0;i<8;i++)
        {
            if (this.drives[i]!=null)
                this.drives[i].closeAllFiles();
        }
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
