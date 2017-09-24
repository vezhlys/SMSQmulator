package smsqmulator;
//7666
/**
 * A file for an (X)FA device. Each SMSQE file should be mapped to one of these.
 * This implements files for the NFA and SFA devices.
 * 
 * <p>In a dir, the file header contains at offset the length of the file including the length of the file header (=Types.SMSEHeaderLength)
 * 
 * @author and copyright (c) 2012 - 2017 Wolfgang Lenerz.
 * 
 * @version  
 *   1.10  don't show files whose filenames are too long.
 *   1.09  if file is a dir on sfa and file has qemuheader : set correct filelength in dirBuffer ; getExtendedInfo is for all files, not ony dirs..
 *   1.08  getLine,saveFile: return bytes read / sent in D1.L, not only D1.W, trap#3,D0=6 implemented ; setdate modified ; 
 *         max file name length is 36 chars not 34 ; call setFileDates when closing file
 *   1.07  makeDirBuffer() case QEMUHeader, "continue" converted to "break" in "if".
 *   1.06  replace getCanonicalPath by getAbsolutePath, to get names correctly via symlinks.
 *   1.05  set correct extended info as to format/type/density.
 *   1.04  don't rename to an existing file.
 *   1.03  trap#3 with d0=$49 behaves exactly like trap#3,d0=7; except that the size in D2 is long.
 *  01.02  dirs are handled differently : read the entire dir in a go.
 *         check for pending input handled correctly even for dirs.
 *         double file separators converted into single ones for the filename.
 *         default file header = QemuHeader, not SFA header.
 *  01.01  trap#3 set file positions (rel or abs) : if position to set is beyond end of file, return error eof
 *  01.00  use time adjustment from Monitor for date/time.
 *  00.10  getLine : if no LF found but count is smaller than buffer size,return eof, not buffer full.
 *  00.09  handleTrap3 d0=$47:limit max size of header read to 64 bytes
 *  00.08  getBytes : removed spurious addition to file position (when did that get there?)
 *  00.07  getBytes : cater for WDIR correctly
 *  00.06  getBytes: if Sfa file, check for "_" at end : if dir and not main dir and "_" not at end, add it.
 *  00.05  getBytes converts filename between smsqe and java if bytes gotten from Dir
 *  00.04  for SFA, a dir shows the "correct" filename (i.e. that of the underlying file) if that name and that in the header diffeer (e.g. when file scopied under the natice filesysm).
 *  00.03  corrected error returns that were bytes, not long
 *  00.02  dir sizes are shown "correctly" (i.e number of files in dir * 64).
 *  00.01  getBytes: if dir: sfa device "double entries" fixed,  
 *         getLine returns ERR_BFFL = -5 (BUFFER FULL) and not ERR_OVFL = -18 (ARITHMETIC OVERFLOW) if no CR found in bytes fetched
 *  00.00  initial version
 */
public class XfaFile 
{
    protected java.io.RandomAccessFile raFile;
    protected java.nio.channels.FileChannel inoutChannel;
    protected XfaFileheader header;
    protected boolean isDir=false;
    protected java.io.File file;
    protected int driveNumber;
    protected int totalSize;                                // total size of this partition (for dirs)
    protected int freeSize;                                 // free size thereof (for dirs)
    protected static final int allocSize=1024;
    protected java.io.File[] myFiles;                       // files in a dir
    protected String[] myFileNames;                         // filenames of files in a dir
    protected int filePosition=0;                           // true position in native file for next I/O.    
    protected String[]driveNames;                           // the names of the dirs the "drives" point to
    protected int deviceID;
    protected int headerOffset;                             // header offset for file positioning 
    protected String filename;
    protected boolean readOnly=true;
    protected int filenameChange;                           // kind of filename change, if any.;
    protected int usageName;                                // the usage naame of the device
    private boolean  setDate=true;                          // whether the date should be set when closing the file -default -> set date
    private boolean setVersion=true;                        // same for version
    protected java.util.TimeZone timeZone=java.util.TimeZone.getDefault();
    protected java.nio.ByteBuffer dirBuffer;                // a buffer with space for headers of all files
    
    
    /*********************** File open & close  *************************************/
   
    /**
     * Creates this object for any file that isn't a dir file.
     * 
     * @param aFile a <code>java.io.RandomAccessFile</code> representing the true OS's file.
     * @param inoutChannel a <code>java.nio.channels.FileChannel</code> for that file.
     * @param lockTheFile should we lock this file( granting execlusive access)?
     * @param file a <code>java.io.File</code> for the same file.
     * @param filename the name of the file. This may be put to upper/lower case (NIY).
     * @param deviceID the deviceID of the driver.
     * @param isDir should be <code>false</code>.
     * @param driveNumber the number of the drive (eg. 0 for xFA1_ etc).
     * @param names  the names of subdrs, if any.
     * @param filenameChange type of filename change (0,1,2).
     * @param usageName usage name for this device.
     */
    public XfaFile (java.io.RandomAccessFile aFile,java.nio.channels.FileChannel inoutChannel,boolean lockTheFile,java.io.File file,
                   String filename,boolean isDir,int deviceID,int driveNumber,String [] names,int filenameChange,int usageName)
    {
        this.raFile=aFile;
        this.inoutChannel=inoutChannel;
        this.file=file;
        this.driveNumber=driveNumber;
        this.driveNames=names;
        this.filename=filename.replace(java.io.File.separator+java.io.File.separator,java.io.File.separator);
       
        this.isDir=isDir;
        this.deviceID=deviceID;
        this.readOnly=!lockTheFile;
        makeFileheader(file,filename,this.inoutChannel);    // set file pointer in native file to just after the header
        this.filePosition=this.header.getOffset();          // set to file position "0" for SMSQ/E 
        if (lockTheFile)
        {
            try
            {
                inoutChannel.lock();                        // (try to) make this file exclusive to me
            }
            catch (Exception e)
            {/* NOP */ }                                    // I can't, just ignore, then.
        }
        this.filenameChange=filenameChange;
        this.usageName=usageName;
        if (this.readOnly)
        {
            this.setDate=false;
            this.setVersion=false;
        }
    }
    
    /**
     * Create a directory "file" for read only access.
     * 
     * @param file the file to use.
     * @param filename its filename.
     * @param isDir should be true.
     * @param names the names of the dirs the "drives" point to.
     * @param driveNumber the number of he drive on which this file is (starting at 0).
     * @param deviceID the deviceID of the driver.
     * @param filenameChange type of filename change (0,1,2).
     * @param usageName usage name for this device.
     */
    public XfaFile (java.io.File file,String filename,boolean isDir,int driveNumber,String [] names,int deviceID,int filenameChange,int usageName)
    {
        this.file=file;
        this.isDir=isDir;
        this.driveNumber=driveNumber;
        this.driveNames=names;
        this.filename=filename.replaceAll( java.io.File.separator+"{2,}",java.io.File.separator);
        this.filenameChange=filenameChange;
        this.usageName=usageName;
        this.deviceID=deviceID;
        if (file.isDirectory())
        {
            this.myFiles=file.listFiles();
            this.myFileNames=file.list();
            this.freeSize=(int)(file.getUsableSpace()/XfaFile.allocSize);
            this.totalSize=(int)(file.getTotalSpace()/XfaFile.allocSize);
            makeDirBuffer();
        }
        makeFileheader(file,this.filename,null);
    }
    
    /**
     * This makes headers for Nfa or Sfa files.
     * @param deviceID which device : NFA or SFA?.
     */
    private void makeFileheader(java.io.File file,String filename,java.nio.channels.FileChannel inoutChannel)
    {
        try
        {
            switch (this.deviceID)
            {
                case Types.NFADriver:                                // 'NFA0';
                    if (this.isDir)
                    {
                        this.header = new NfaFileheader(file,makeFilename(),this.dirBuffer.capacity());
                    }
                    else
                    {
                        this.header = new NfaFileheader(file,filename,0);
                    }
                    break;
                    
                case Types.SFADriver:                                // 'SFA0'  
                    if (this.isDir)
                    {
                        this.header = new SfaFileheader(file,filename,inoutChannel,this.dirBuffer.capacity());
                    }
                    else
                    {
                        try
                        {
                            this.header = new  QemuFileheader(file,makeFilename(),inoutChannel);// try a QEMU header
                        }
                        catch (Exception e)
                        {
                            inoutChannel.position(0);
                            this.header = new SfaFileheader(file,filename,inoutChannel,0);// if Qemu header doesn't work, try SFA
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown device: "+this.deviceID);
         //           return;                                         // huh?
            }
            // as of here,we presume that we have a valid header  
            if (!this.isDir)
            {
                this.headerOffset=this.header.getOffset();
                if (!this.readOnly)
                    this.header.flushHeader(inoutChannel,this.setDate);          //
            }
        }
        catch (Exception e)      
        { /*NOP*/}
    }
           
    
   /**
     * Close this file : release file locks, if any and flush headers.
     * The SMSQE close routine for ths driver handles releasing the cdb lock etc
     * @param cpu the CPU object used for executing the program.
     */
    public void close(smsqmulator.cpu.MC68000Cpu cpu)
    {
        boolean doDateChange=false;
        if (!this.readOnly && this.header!=null)
            doDateChange = this.header.flushHeader(this.inoutChannel,this.setDate);
        try
        {  
            if (this.inoutChannel!=null && this.inoutChannel.isOpen())
            {
                this.inoutChannel.force(true);
                this.inoutChannel.close();
            }
            if (this.raFile!=null)
                this.raFile.close();
        }
        catch (Exception e)
        { /*NOP*/}
        
        if (doDateChange)
        {
            this.header.setFileDates(this.file);
        }
        cpu.data_regs[0]=0;
        this.header=null;       
    } 

    /****************************** File I/O ****************************************/
    
    /**
     * This dispatches the file I/O routines (=SMSQE TRAP#3 routines).
     * 
     * @param trapKey what kind of trap#3 are we dealing with?
     * @param channelDefinitionBlock = A0
     * @param cpu the CPU.
     * @param nativeDir the name of hte native dir this file belongs to (necessary for renaming).
     */
    public void handleTrap3(int trapKey,int channelDefinitionBlock,smsqmulator.cpu.MC68000Cpu cpu,String nativeDir)
    {
        int temp;
        switch (trapKey)
        {
            case 0:                                         // test pending input
                cpu.data_regs[0]=0;                         // pretend there is some
                if (!this.isDir)
                {
                    try
                    {
                        if (this.inoutChannel.position()>= this.raFile.length())
                            cpu.data_regs[0]=Types.ERR_EOF;
                    }
                    catch (Exception e)
                    {
                        cpu.data_regs[0]=Types.ERR_EOF;
                    }
                }
                else
                {
                    if (this.filePosition>=this.dirBuffer.capacity())
                        cpu.data_regs[0]=Types.ERR_EOF;
                }
                break;                                     
                
            case 0x01:                                      // get one byte from the file
                readByte(cpu);
                break;
                
            case 0x02:                                      // get a line 
                getLine(cpu);
                break;
                
            case 0x03:                                      // get a number of bytes from the file
                getBytes(cpu);                
                break;
                
            case 0x04:  
                cpu.data_regs[0]=Types.ERR_EOF;   // this doesn't make sense here
                break; 
                
            case 0x05:                                      // send one byte to channel
                if (this.isDir || this.readOnly)            // but you can't if this is a directory
                    cpu.data_regs[0]=Types.ERR_RDO;             
                else
                {
                    writeByte(cpu);
                }
                break;
                
            case 0x06:
            case 0x07:                                      // send multiple bytes
                if (this.isDir || this.readOnly)            // but you can't if this is a directory
                    cpu.data_regs[0]=Types.ERR_RDO;               
                else
                {
                    saveFile(cpu,false);
                }
                break;
                
            case 0x40:                                      // check pending I/O
            case 0x41:                                      // flush all buffers
                cpu.data_regs[0]=0;               // they always succeed but don't really do anything *************
                break;  
                
            case 0x42:                                      // set file position absolute
                setFilePosition(cpu,cpu.data_regs[1],true);
                break;
                
            case 0x43:                                      // set file position relative
                setFilePosition(cpu,cpu.data_regs[1],false);
                break;
                
            case 0x45:                                      // get (FAKE) info about medium
                int A1 = cpu.addr_regs[1];
                if (this.deviceID==Types.NFADriver)
                    cpu.writeSmsqeString(A1, "NFA DRIVER",false,-1);
                else
                    cpu.writeSmsqeString(A1, "SFA DRIVER",false,-1);
                cpu.addr_regs[1]=A1+10;
                cpu.data_regs[1]=0x01001000;      // This is totally fake info!
                cpu.data_regs[0]=0;               // success.
                break;
                
            case 0x46:                                      // set file header (mem->file jeader)
                if (this.isDir || this.readOnly)            // but you can't if this is a directory
                    cpu.data_regs[0]=Types.ERR_RDO;             
                else
                {
                    temp=this.header.writeFileHeader(cpu,cpu.addr_regs[1]);
                    if (temp!=0)                                // all OK
                    {
                        cpu.data_regs[0]=0;
                        cpu.data_regs[1]=(cpu.data_regs[1]&0xffff0000)|temp;        // D1 = number of bytes written
                        cpu.addr_regs[1]= cpu.addr_regs[1]+temp;
                    }
                    else
                    {
                       cpu.data_regs[0]=Types.ERR_ICHN;// ??? signal channel not open (?)
                    }
                }
                break;
                
            case 0x47:                                      // read file header
                A1=cpu.addr_regs[1];
                temp =this.header.readFileheader(cpu,A1,cpu.data_regs[2]&0xffff);
                if (temp!=0)
                {
                    cpu.data_regs[0]=0;
                    cpu.data_regs[1]=(cpu.data_regs[1]&0xffff0000)|temp;
                    cpu.addr_regs[1]=A1+temp;
                }
                else
                    cpu.data_regs[0]=Types.ERR_ICHN;        // ??? signal channel not open (?)
                break;
                
            case 0x48:                                      // load file into mem from disk
                loadFile(cpu,true,true);
                break;
                
            case 0x49:                                      // save entire file from mem to disk
                if (this.isDir || this.readOnly)            // but you can't if this is a directory or a read only file
                    cpu.data_regs[0]=Types.ERR_RDO;             
                else
                    saveFile(cpu,true);                     // (must reposition pointer & length is longword)
                break; 
                
            case 0x4a:                                      // rename file
                renameFile(cpu,nativeDir);
                break;
                
            case 0x4b:                                      // truncate file
                if (this.isDir || this.readOnly)            // but you can't if this is a directory or a read only file
                    cpu.data_regs[0]=Types.ERR_RDO;     
                else
                {
                    try
                    {
                        this.inoutChannel.truncate(this.inoutChannel.position());
                        cpu.data_regs[0]=0;             
                    }
                    catch (Exception e)
                    {
                        cpu.data_regs[0]=Types.ERR_ORNG;                                 
                    }
                }
                break;
                
            case 0x4c:                                      // set/get file dates
                setOrReadDate(cpu);
                break;
                
            case 0x4d:                                      // make directory
                if (this.isDir || this.readOnly)            // but you can't if this is a directory or a read only file
                    cpu.data_regs[0]=Types.ERR_RDO;             
                else
                    makeDirectory(cpu);
                break; 
                
            case 0x4e:                                      // get / set version - always set to 0 **********************
               getOrSetVersion(cpu);
                break;
                
            case 0x4f:                                      // get extended info into (a1)
                getExtendedInfo(cpu);
                break;
        }
    }
    
    /**
     * Get Extended Info.
     * 
     * @param cpu 
     */
    private void getExtendedInfo(smsqmulator.cpu.MC68000Cpu cpu)
    {
        int A1=cpu.addr_regs[1];                            // pointer to block where extended info will be written
        if (this.deviceID==Types.NFADriver)
        {
            cpu.writeSmsqeString(A1, "NFA DRIVE "+(this.driveNumber+1),-1);       // change the loop below if length of this changes !!!!
            cpu.writeSmsqeString(A1+0x16, "NFA",-1);
        }
        else
        {
            cpu.writeSmsqeString(A1, "SFA DRIVE "+(this.driveNumber+1),-1);       // change the loop below if length of this changes !!!!
            cpu.writeSmsqeString(A1+0x16, "SFA",-1);
        }
        for (int i=14;i<22;i+=2)                             // set all remaining to 0
            cpu.writeMemoryShort(A1+i, (short)0);
        cpu.writeMemoryWord(A1+0x1c, (this.driveNumber+1)<<8);  // drive number + medium is read/write
        cpu.writeMemoryWord(A1+0x1e, XfaFile.allocSize);    // fake allocation unit size
        cpu.writeMemoryLong(A1+0x20, this.totalSize);       // total size
        cpu.writeMemoryLong(A1+0x24, this.freeSize);        // free size
        cpu.writeMemoryLong(A1+0x28, Types.SMSQEHeaderLength); // file header length
        cpu.writeMemoryWord(A1+0x2c, 0x0202);               // format
        cpu.writeMemoryWord(A1+0x2e, 0x0102);               // density/medium type
        cpu.writeMemoryLong(A1+0x30, 0x00ffffff);
        cpu.writeMemoryLong(A1+0x34, 0xffffffff);  
        cpu.writeMemoryLong(A1+0x38, 0xffffffff);  
        cpu.writeMemoryLong(A1+0x3c, 0xffffffff);     
        cpu.data_regs[0]=0;
    }
    
    /**
     * Sets or reads the file version.
     * 
     * @param cpu 
     */
    private void getOrSetVersion(smsqmulator.cpu.MC68000Cpu cpu)
    {
        if (this.header==null)
            return;                                         // PREMATURE EXIT
        cpu.data_regs[0]=0;                                 // preset no error
        int D1 = cpu.data_regs[1];                // switch what to do =-1 read version = 0: keep current version, other = version to set
        switch (D1)
        {
            case -1:                                        // read version
                cpu.data_regs[1]=this.header.getVersion();
                break;
            case 0:                                         // keep current version
                this.setVersion=false;                      // so don't change file version when closing
                break;
            default:                                        // set version in D1
                if (this.isDir || this.readOnly)            // but you can't if this is a directory or read only file
                    cpu.data_regs[0]=Types.ERR_RDO;  
                else
                {   
                    if (this.deviceID==Types.SFADriver)  // NFA doesn't handle version seeting, don't even try
                        this.header.setVersion(D1);
                    this.setVersion=false;
                }
                break;
        }
    }
    
   /**
     * Sets or reads the file date.
     * 
     * @param cpu 
     */
    private void setOrReadDate(smsqmulator.cpu.MC68000Cpu cpu)
    {
        if (this.header==null)
            return;                                         // PREMATURE EXIT
        cpu.data_regs[0]=0;                                 // presume call went OK
        int D1 = cpu.data_regs[1];                          // switch what to do =-1 read date = 0: set date to current date other = date to set
                                                            // D2 =0 update date = 2 backup date
        switch (D1)
        {
            case -1:                                        // read date
                cpu.data_regs[1]=this.header.getDate(cpu.data_regs[2]);
                break;
            case 0:                                         // set current date
                D1=(int)((System.currentTimeMillis()/1000)+Monitor.TIME_OFFSET);
                // NO break here!
            default:                                        // set date in D1
                if (this.isDir || this.readOnly)            // but you can't if this is a directory or read only file
                    cpu.data_regs[0]=Types.ERR_RDO;  
                else 
                {
                    this.header.setDate(cpu.data_regs[2]&0xff,D1);
                    this.setDate=false;
                }
                break;
        }
    }
    
    /**
     * Sets the file position, either absolute or relatve to current position.
     * The position given by SMSQE doesn't take the header offset into account : add it if absolute.
     * @param cpu
     * @param position the position to set.
     * @param absolute if <code>true</code>, the position to set is absolute.
     */
    private void setFilePosition(smsqmulator.cpu.MC68000Cpu cpu,int position,boolean absolute)
    {
        cpu.data_regs[0]=0;                                 // preset no error
        if (this.isDir)
        {
            if (!absolute)
                this.filePosition+=position;
            else
                this.filePosition=position; 
            if (this.filePosition<0)
            {
                this.filePosition=0;           
            }
            if (this.filePosition>=this.dirBuffer.capacity())
            {
                this.filePosition=this.dirBuffer.capacity();
                cpu.data_regs[0]=Types.ERR_EOF;
            }
            cpu.data_regs[1]=this.filePosition;
        }
        else
        {
            if (!absolute)
            {
                this.filePosition+=position;
            }
            else
            {
                this.filePosition=position+this.headerOffset; 
            }
            if (this.filePosition<0)
            {
                this.filePosition=this.headerOffset;            // NO NEGATIVE FILEPOSITIONS
            }
            cpu.data_regs[1]=this.filePosition-this.headerOffset; // show new file position
            try
            {
                if (this.filePosition>(int) this.inoutChannel.size())
                {
                    this.filePosition=(int) this.inoutChannel.size();
                    if (this.isDir)
                    {
                        cpu.data_regs[1]=this.filePosition;      
                    }
                    else
                    {
                        cpu.data_regs[1]=this.filePosition-this.headerOffset; // show new file position        
                    }
                    cpu.data_regs[0]=Types.ERR_EOF;
                }
                if (this.inoutChannel!=null)
                    this.inoutChannel.position(this.filePosition);
            }
            catch (Exception e)
            {
                cpu.data_regs[0]=Types.ERR_EOF;             // pretend EOF if problem with file positioning
            } 
        }
    }
    
    /**
     * This (reads multiple bytes from the file) / (loads the entire file).
     * On entry D2 = length of file, D3 = timeout (ignored), A1 = buffer.
     * Depending  on longword, D2 is either a long word (load entire file) or a word (get bytes);
     * @param cpu the cpu being emulated (to get registers from)
     * @param resetPosition do we need to reset the file position to "0"?
     * @param longword if true,D2 is a long word (->load entire file trap) else a word (-> get bytes trap);
     * @return true if operation was successful, else false.
     */
    private boolean loadFile(smsqmulator.cpu.MC68000Cpu cpu,boolean resetPosition,boolean longword)                                
    {
        try
        {
            int A1 = cpu.addr_regs[1];                          // where to read to
            int length=cpu.data_regs[2];                        // number of bytes to read.
            if (!longword)
                length &=0x0000ffff;                            // if word sized, get rid of upper word
            if (resetPosition)
                this.inoutChannel.position(this.headerOffset);  // start reading at beginning of file (evt. after header)
            else
                this.inoutChannel.position(this.filePosition);  // start reading where we're supposed to
            
            int bytesRead=cpu.readFromFile(A1,length, this.inoutChannel);// read length bytes
            this.filePosition=(int) this.inoutChannel.position();// make sure we've got the correct file IO position
            if (bytesRead==-1)                                  // if we get this, the file was EOF before the read
            {
                cpu.data_regs[0]=Types.ERR_EOF;                 // so set the error  
                return false;
            }
            else
                cpu.data_regs[0]=0;                             // else show no error
         
            A1+=bytesRead;
            cpu.addr_regs[1]=A1;                                // updated buffer
            cpu.data_regs[1]=bytesRead;                         // necessary for iob.fmul, not for iof.load
            return true;
        }
        catch (Exception e)
        {
            cpu.data_regs[0]=Types.ERR_DRFL;                    // pretend that drive is full if error
            try
            {
                this.filePosition=(int) this.inoutChannel.position();// make sure we've got the correct file IO position
            }
            catch (Exception n)
            {
                this.filePosition=this.headerOffset;            // this seems really f**cked up, try to reset file position to file start.
            }
            return false;
        }
    }
    
    /**
     * Gets a number of bytes from the file.
     * @param cpu 
     */
    private void getBytes(smsqmulator.cpu.MC68000Cpu cpu)
    {
        if (this.isDir)                                   
        {
            cpu.data_regs[0]=0;                             // preset no error
            int D2=cpu.data_regs[2]&0xffff;                 // nbr of bytes to get
            int A1=cpu.addr_regs[1];                        // points to buffer in cpu mem
            if (this.filePosition+D2 > this.dirBuffer.capacity())
            {
                D2 = this.dirBuffer.capacity()-this.filePosition;
                cpu.data_regs[0]=Types.ERR_EOF;             // we would go beyond the end of the file
            }
            this.dirBuffer.position(0);
            this.dirBuffer.limit(this.dirBuffer.capacity());
            for (int i=0;i<D2;i++,A1++,this.filePosition++)
            {
                cpu.writeMemoryByte(A1, this.dirBuffer.get(this.filePosition));
            }
            cpu.data_regs[1]=D2;
            cpu.addr_regs[1]=A1;
        }
        else
        {
            loadFile(cpu,false,false);
        }
    }
    
    
    /**
     * Gets a line ending in LF (0x0a) and strips off any possible CR (0x0d) immediately preceeding it!!!!!
     * Load as many bytes as possible into the buffer, then check for $0a.
     * @param cpu 
     */
    private void getLine(smsqmulator.cpu.MC68000Cpu cpu)
    {
        int A1 = cpu.addr_regs[1];                // where to read to -start of buffer, keep
        int mypos=this.filePosition;                        // this is the current file position.
        if (!loadFile(cpu,false,false))                     // load as much as we can get - this changes the fileposition
            return;                                         // but we couldn't get anything sensible
        int count=this.filePosition-mypos;                  // nbr of bytes got
        int end=A1+count;                                   // end of buffer
        count=A1;
        boolean foundit=false;
        while (A1<end)                                      
        {
            if (cpu.readMemoryByte(A1++)==0x0a)             // find an LF, A1 points after it
            {
                foundit=true;
                break;
            }
        }
        
        if (!foundit)                                       // no LF found - buffer must be too smallor there isno LF!
        {
            if (this.filePosition-mypos<cpu.data_regs[2])              // there is no LF
                cpu.data_regs[0]=Types.ERR_EOF; 
            else
                cpu.data_regs[0]=Types.ERR_BFFL; 
        }   
        else
        {
            count=A1-count;
            if (count<=0)
                cpu.data_regs[0]=Types.ERR_EOF;    // ???????????????
            end=0;                                          // check for CR, to eliminate it
            if (cpu.readMemoryByte(A1-2)==0x0d && count>1)
            {
                cpu.writeMemoryByte(A1-2,0x0a);
                A1--;
                count--;
                end=1;
            }
            cpu.addr_regs[1]=A1;
          //  cpu.data_regs[1]=(cpu.data_regs[1]&0xffff0000)|(count&0xffff);
            cpu.data_regs[1]=count;
            this.filePosition=mypos+count+end;
            try
            {
                this.inoutChannel.position(this.filePosition);
            }
            catch (Exception e)
            {/*nop*/}
        }
    }
    
    /**
     * Saves the entire file, or some bytes only, to disk.
     * On entry D2.L = length of file or nbr of bytes, D3 = timeout (ignored), A1 = buffer.
     * @param cpu
     * @param longword is D2 a word or a long word?.
     */
    private void saveFile(smsqmulator.cpu.MC68000Cpu cpu,boolean longword)                                 // saves this entire file
    {
        if (this.isDir)
        {
            cpu.data_regs[0]=Types.ERR_NIMP;
            return;                                         // dirs are read only
        }
        try
        {
            int A1 = cpu.addr_regs[1];
            int length;
            if (longword)
            {
                length=cpu.data_regs[2];
            }
            else
            {
                length=cpu.data_regs[2]&0xffff;
            }
                
            if (length!=0)
            {
                this.inoutChannel.position(this.filePosition);
                int bytesWritten=cpu.writeToFile(A1,length, this.inoutChannel);
                this.filePosition=(int) this.inoutChannel.position();
                cpu.addr_regs[1]= A1+bytesWritten;
             //   if (!longword)
               //     cpu.data_regs[1]=(cpu.data_regs[1]&0xffff0000)|(bytesWritten&0xffff); 
                cpu.data_regs[1]=bytesWritten;
            }
            else
            {
                cpu.data_regs[1]=0;
            }
            cpu.data_regs[0]=0;
        }
        catch (Exception e)
        {
            cpu.data_regs[0]=Types.ERR_DRFL;     // pretend that drive is full if write error
        }
    } 
    
   
    /**
     * Writes a byte to the file.
     * @param cpu 
     */
    private void writeByte(smsqmulator.cpu.MC68000Cpu cpu)
    { 
        if (this.isDir)
        {
            cpu.data_regs[0]=Types.ERR_NIMP;
            return;                                         // dirs are always read only
        }
        try
        {
            this.raFile.writeByte(cpu.data_regs[1]&0xff);
            this.filePosition++;
            cpu.data_regs[0]=0;
        }
        catch (Exception e)
        {
            cpu.data_regs[0]=Types.ERR_ICHN;
        }
    }
     /**
     * Reads a byte from the file.
     * @param cpu 
     */
    private void readByte(smsqmulator.cpu.MC68000Cpu cpu)
    {
        try
        {
            cpu.data_regs[1]=this.raFile.readByte()& 0xff;
            this.filePosition++;
            cpu.data_regs[0]=0;
        }
        catch (java.io.EOFException e)
        {
            cpu.data_regs[0]=Types.ERR_EOF;
        }
        catch (Exception e)
        {
            cpu.data_regs[0]=Types.ERR_ICHN;
        }
    }
    
      /**
     * Checks whether this file is a dir.
     * @return <code>true</code> if this file is a dir, <code>false</code> if not.
     */
    public boolean isIsDir() {
        return isDir;
    }
    
    /**
     * Signals that this file should be treated as a dir.
     * 
     */
    public void setAsDir()                                  // this file is the dir itself
    {
        this.isDir=true;
    }
    
    /**
     * Flush the file header(write to disk).
     */
    public void flushHeader()
    {
        if (this.header!=null)
            this.header.flushHeader(this.inoutChannel,this.setDate);
    }
    
    /**
     * Makes a (sub)directory. Convert from SMSQE names to NF system names and make all intermediary dirs.
     * <p> We know that the file corresponding to the subdir already exists. However, when making a dir, I want to make a true
     *     subdir in the native file system.
     * <p> To make things more secure, I first check that I can make a "true" subdir, before starting to make it.
     * <P> Moreover, I should move all files which SMSQE thinks belong to this subdir into this subdir.
     * <p> Remember that on some filesystems, to create a subdir4 so that : <device>/subdir1/subdir2/subdir3/subdir4
     *     you have to create the subdir1, subdir2, subdir3 subdirectories FIRST (if they don't exist) before you can create subdir4.
     */
    private void makeDirectory(smsqmulator.cpu.MC68000Cpu cpu)
    {
        boolean success=false;
        String initDir=this.driveNames[this.driveNumber];   // the initial dir, corresponding to the SMSQE <device_> 
        try
        {
            if (this.raFile!=null)
                this.raFile.close();                        // close the file that was initially opened
            
            if (this.file!=null)
                success=this.file.delete();                 // and delete it (!)
        }
        catch (Exception whatever)
        {
        /*NOP*/
        }
        if (!success)
        {
             cpu.data_regs[0]=Types.ERR_FEX;      // I can't convert this into a directory!
             return;
        }
        String []subnames=this.header.getSMSQEFilename().split("_");// get all subnames
        java.io.File nFile,tempFile,destFile;
        java.util.ArrayList <java.io.File> filesList=new java.util.ArrayList<>();// list with files I have created (empty at first)
        String fname=initDir;
        for (int i=0;i<subnames.length;i++)                 // first of all check whether a file like that exists along the path
        {
            fname+=subnames[i]+java.io.File.separator;         // new subdir name
            nFile=new java.io.File(fname);
            if (nFile.exists() && !nFile.isDirectory())     // this file exists but is NOT a directory
            {
                unravelFiles(filesList,cpu);                // delete all files created
                return;                                     // premature exit
            }
            
            if (!nFile.exists())                            // the file doesn't exist, so try to create it
            {
                try
                {
                    success= nFile.mkdir();                 // try to make the subdir
                    if (!success)
                    { 
                        unravelFiles(filesList,cpu);        // delete all files created
                        return;                             // premature exit
                    }
                }
                catch (Exception e)                         // what exactly am I catching here, anyway?
                { 
                    unravelFiles(filesList,cpu);            // delete all files created
                    return;                                 // premature exit
                }
            }
            // if we get here, the file exists and is a subdir, so just go around again
        }
        // If we get here, the subdir was successfully created. Now move all files into the subdir if they should be in it.
        // e.g. if a I have a file sfa1_progs_myprog and create a "true" subdir "sfa1_progs" which is translated to <device>/progs/ 
        // then I must move the file into <device>/progs/ and strip the "progs_" part off the name
        String []fileNames;                                 // will ontain the names of all files in a dir.
        fname=initDir;
        String subname;
        int l;
        for (int i=0;i<subnames.length;i++)                 // first of all check whether a file like that exists along the path
        {
            nFile=new java.io.File(fname);                  // this is the dir cotaining the next subdir
            if (!nFile.isDirectory())                       // it **should** be a dir
            {                                               // but it isn't?
                 cpu.data_regs[0]=Types.ERR_FDIU; // huh? how can that be? Don't even try to unravel the whole thing anymore
                 return;                                
            }
            fileNames=nFile.list();                         // this gets all filenames (NOT ABSOLUTE PATHS) in this subdir into an array. Now check whether they should go into the subdir
            subname=subnames[i]+"_";                        // this is the name I'm looking for
            l=subname.length();
            for (String temp:fileNames)
            {
                if (temp.startsWith(subname))
                {
                    tempFile=new java.io.File(nFile.getAbsolutePath()+java.io.File.separator+temp);// the "old" file
                    temp=temp.substring(l);
                    destFile=new java.io.File(nFile.getAbsolutePath()+java.io.File.separator+subnames[i]+java.io.File.separator+temp);
                    tempFile.renameTo(destFile);            // try to move file into subdir
                }
            }
           fname+=subnames[i]+java.io.File.separator;       // new subdir name
        }
        cpu.data_regs[0]=0;                       // all went well
    }
    /**
     * Deletes all files created when trying to make a subdir.
     * <p> To do this, I must start at the END of the list.
     * @param filesList a list with freshly created subdirs. These do NOT contain any files yet (except for possible subdirs newly created).
     */
    private void unravelFiles(java.util.ArrayList <java.io.File> filesList,smsqmulator.cpu.MC68000Cpu cpu)
    {
        java.io.File nFile;
        for (int i = filesList.size()-1;i>-1;i--)
        { 
            nFile=filesList.get(i);
            nFile.delete();
        }
        cpu.data_regs[0]=Types.ERR_FDIU;  // signal file is in use!
    }
    
    /**
     * Get the name of the file.
     * @return the name of the file.
     */
    public String getFilename()
    {
        return this.filename;
    }
    
    private void renameFile(smsqmulator.cpu.MC68000Cpu cpu,String nativeDir)
    {       
        String s=cpu.readSmsqeString(cpu.addr_regs[1]);// new name
        cpu.data_regs[0]=Types.ERR_ITNF;         // preset error
        if (s== null || s.length()<5 || s.length()>39)
        {
            return;                                         // premature exit with error
        }
        int device=smsqmulator.Helper.convertUsageName(s.substring(0,3));
        if (device!=this.deviceID && device!=this.usageName)
        {
            return;                                         // premature exit with error
        }
        try
        {
            device=Integer.parseInt(s.substring(3,4)) -1;
            if (device!=this.driveNumber)
                throw new IllegalArgumentException();
        }
        catch(Exception e)                                  // either because 4th char wasn't a number, or because it wasn't this drive
        {
            return;                                         // premature exit with error
        }
        // we have now established that the new name concerns a file on this drive.    
        s=s.substring(5);                                   // strip name of device part
        String unChanged=s;                                 // unchanged name w/o device part
        
        switch (this.filenameChange)
        {
            case 0:
            default:
                break;
            case 1 :
                s=s.toUpperCase();
                break;
            case 2 :
                s=s.toLowerCase();
                break;
        }
        s=XfaDriver.convertFilename(nativeDir, s);
        /* Some subssytems don't like to rename open files*/
        boolean openRa=this.raFile!=null;
        if (openRa)
        {
            try
            {
                this.raFile.close();                        // close file & channel
            }
            catch (Exception e)
            {
                return;                                     // return with error itnf
            }
        }
        java.io.File f=new java.io.File(nativeDir+s);
        if (f.exists())
        { 
            cpu.data_regs[0]=Types.ERR_FEX;                // file already exists, can't rename to it          
            return;
        }
        if (!this.file.renameTo(f))
        { 
            cpu.data_regs[0]=Types.ERR_FDIU;                // if we get an error here, it's most likely because the file is in use.            
            return;
        }
        this.file=f;
        // if this is an Sfa file,we must set the fileheader & flush it
        if (this.deviceID==Types.SFADriver)
        {
            this.header.setSMSQEFilename(unChanged);
            try
            {
                this.raFile = new java.io.RandomAccessFile(file, "rw");  // open new 
                this.inoutChannel = this.raFile.getChannel(); 
                this.header.flushHeader(this.inoutChannel,this.setDate);
                this.raFile.close();                        // close this again as the true file might be read only.
            } 
            catch (Exception e)
            {
                return;                                     // return with error itnf if pb This wouls be probmlematic, since th filename was already changed
            }
        }
        if (openRa)
        {
            try
            {
                if (!this.readOnly)
                {
                    this.raFile = new java.io.RandomAccessFile(file, "rw");  // open new 
                }
                else
                {
                    this.raFile = new java.io.RandomAccessFile(file, "r");   // open old shared file
                }
                this.inoutChannel = this.raFile.getChannel(); 
                if (!this.readOnly)
                    this.inoutChannel.lock();
            }
            catch (Exception e)
            {
                return;                                     // return with error itnf
            }
        }
        cpu.data_regs[0]=0;                       // show all ok
    }
    
    /**
     * Makes a buffer containing smsqe file headers for all files in the native dir.
     * EXclude files the name of which would be too long.
     */
    private void makeDirBuffer()
    {
        this.dirBuffer=java.nio.ByteBuffer.allocate(Types.SMSQEHeaderLength*this.myFiles.length);
        int index = 0;                                      // where we are in the buffer, in Types.SMSQEHeaderLength steps
        java.io.File f;
        java.nio.channels.FileChannel ioChannel;
        java.io.RandomAccessFile ra;
        String s;
        int temp;
        byte res;
        int qemu=0;
        for (int i=0;i<this.myFiles.length;i++)
        {
            f=this.myFiles[i];                          // file to treat 
            s=this.myFileNames[i];                      // name of that file
 //               if (s.contains("TODO"))
   //         f=f;
 
            switch (this.filenameChange)                // convert it
            {
                case 0:
                default:
                    break;
                case 1 :
                    s=s.toUpperCase();
                    break;
                case 2 :
                    s=s.toLowerCase();
                    break;
            }
            if (!s.equals(this.myFileNames[i]))             // converted name correspods to my case choice?...
            {
                continue;                                   // no, don't show this file in dir
            }
            if ((this.deviceID==Types.SFADriver) && (!f.isDirectory()))// treat SFA files which aren't dirs
            {
                this.dirBuffer.position(index);             // position at start of entry for this file
                this.dirBuffer.limit(index+4);              // prepare to read 4 bytes
                try 
                {
                    ra= new java.io.RandomAccessFile(f, "r");
                    ioChannel =ra.getChannel();
                    ioChannel.read(this.dirBuffer);      // get 4 bytes of the file (=the header SFA watermark if this is a sfa file)
                    temp=this.dirBuffer.getInt(index);
                    switch (temp)
                    {
                        case Types.SFADriver:               // it is an SFA file
                            this.dirBuffer.position(index);
                            this.dirBuffer.limit(index+Types.SMSQEHeaderLength);// prepare to read 64 bytes (the header)
                            ioChannel.read(this.dirBuffer);       // get 64 bytes of the file (=the header if this is a sfa file)
                            ioChannel.close();  
                            index+=Types.SMSQEHeaderLength;
                            break;
                        case Types.QEMUHeader:              // it is (perhaps) a Qemulator file
                          //  this.dirBuffer.position(index);
                            this.dirBuffer.limit(index+20); // read 16 more bytes to get at offset 20 in file
                            ioChannel.read(this.dirBuffer);
                            for (int k=4;k<18;k++)
                            {
                                if (this.dirBuffer.get(index+k)!=Types.QEMU[k])
                                {
                                    ioChannel.close();
                                    ra.close(); 
                                    this.dirBuffer.limit(this.dirBuffer.capacity());// this isn't a Qemulator file
                                    break;                  // WAS continue.
                                }
                            }
                            qemu=this.dirBuffer.get(index+19)*2;  // length of header         
                            this.dirBuffer.position(index+4);
                            this.dirBuffer.limit(index+14);
                            ioChannel.read(this.dirBuffer);// get 10 bytes of the file (bytes 4 to 13 of the Qdos header if this is a Qemulator file
                            ioChannel.close();
                            ra.close(); 
                            this.dirBuffer.putInt(index,(int)f.length()-qemu);
                            this.dirBuffer.limit(this.dirBuffer.capacity());
                            break;
                            
                        default:                            // it is neither
                            ioChannel.close();
                            ra.close(); 
                            this.dirBuffer.limit(this.dirBuffer.capacity()); 
                            break;
                    }
                }
                catch (Exception e)
                { 
                    // NOP //
                }     
                if (qemu==0)
                    continue;                               // don't go any further now, we're finished with this iteration of the loop
            }
            // if we get here, the file is either a directory or an NFA file or a QEMUl file (qemu!=0)  
            try                                             // now check filename length
            {
                s=f.getAbsolutePath();                      // under SMSQ/E the (sub)dir name is part of the filename, but NOT the device name
                temp=this.driveNames[this.driveNumber].length();// length of "device" name
                if (temp>=s.length())
                    s=this.driveNames[this.driveNumber]+"_**UNKNOWN**";
                s=s.substring(temp).replace(java.io.File.separator,"_");
            }
            catch (Exception e)
            {
                s=this.driveNames[this.driveNumber]+"_**UNKNOWN**";
            }
            temp=s.length();
            if (temp>36)                                        // skip any files with filenames that are too long
            {
                if ((this.deviceID==Types.SFADriver) && (!f.isDirectory())) // for those files, soething might have been written in the header
                {
                    for (int k=index;k<index+Types.SMSQEHeaderLength;k+=4)
                    {
                        this.dirBuffer.putInt(k,0);
                    }
                }
                continue;
            }
            this.dirBuffer.putShort(index+0x0e, (short)temp);
            for (int k=0;k<temp;k++)
            {
             //   res=(byte)Helper.convertToSMSQE(s.substring(k,k+1)); // convert char if need be
           //     this.dirBuffer.put(index+16+k,res);
              //  res=Helper.convertToSMSQE(s.charAt(k)); // convert char if need be
                this.dirBuffer.put(index+16+k,Helper.convertToSMSQE(s.charAt(k)));
            }
            
            if (f.isDirectory())    
            {
            }
            else if (qemu==0)
            {
                this.dirBuffer.putShort(index+4, (short)0); 
            }
            
            if (f.isDirectory())
            {
                this.dirBuffer.putShort(index+4, (short)0x00ff); 
                String []files=f.list();
                if (files!=null)
                    temp=(files.length+1)* Types.SMSQEHeaderLength;
                else
                    temp=0;
            }
            else
            {  
                if (qemu==0)
                   this.dirBuffer.putShort(index+4, (short)0);
                temp=(int)(f.length()) + Types.SMSQEHeaderLength;// length of the file in the directory comprises the header length
                if (this.deviceID==Types.SFADriver)          // pretend file is shorter than it actually is if SFA : eliminate the SFA header!
                {
                    temp-=qemu;
                    if (temp<0)                             // huh?????
                        temp=0;
                }
            }
            this.dirBuffer.putInt(index, temp);  
            if (!f.isDirectory())
            {
                long ttime=f.lastModified();                    // date/time of that file
                ttime=ttime+this.timeZone.getOffset(ttime);     // adjusted for DST and offset from UTC
                temp=(int)(ttime/1000)+Types.DATE_OFFSET;       // ** magic adjusted for SMSQE
                this.dirBuffer.putInt(index+0x34, temp);
                this.dirBuffer.putInt(index+0x3c, temp); 
            }
            index+=Types.SMSQEHeaderLength;                 //point next entry in dir
            qemu=0;
        }
    }
    /**
     * Adds the name of directories to a filename.
     * 
     * @return the new filename.
     */
    private String makeFilename()
    {
        String s;
        try
        {
            s=this.file.getAbsolutePath();                     // under SMSQ/E the (sub)dir name is part of the filename, but NOT the device name
            int temp=this.driveNames[this.driveNumber].length();// length of "device" name
            if (this.driveNames[this.driveNumber].endsWith(java.io.File.separator))
                temp--;                                    
            if (temp>s.length())
                s=this.driveNames[this.driveNumber]+"_**UNKNOWN**";
            if (temp==s.length())
                s="";
            else
                s=s.substring(temp).replace(java.io.File.separator,"_");
        }
        catch (Exception e)
        {
            s=this.driveNames[this.driveNumber]+"_**UNKNOWN**";
        }
        if (s.startsWith("_") && s.length()>1)
            s=s.substring(1);
        return s;
    }
}
