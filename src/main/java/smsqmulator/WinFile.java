
package smsqmulator;

/**
 * This is the class representing one file on a QXL.WIN device.
 * 
 * Each file maintains a ByteBuffer containing the entire contents of the file.
 * The limit of this buffer must always be set to its capacity on entry/exit to any routine.
 * The position should correspond to the current file position (+ header).
 * <p>
 * There are 4 flags:
 * <ul>
 *      <li> fileChanged : if true, something has changed in the file. It should be written out when closed/flushed.</li>
 *      <li> dirChanged  : if true, something has changed in the directory containing this file. It should be written back to the drive.</li>
 *      <li> mapChanged  : the drive FAT was changed (e.g. a new cluster allocated to the file). Most changes will be written back during the file close routine.</li>
 *      <li> setDate     : if true, the file date must be set to the current date when the file is closed.</li>
 * </ul>
 * <p>
 * A file object is created whenever a file is "opened". The file position is set to point just after the header.
 * The file object is called upon when:
 * <ul>
 *      <li> any i/o operation is to be made.</li>
 *      <li> the file is to be closed again.</li>
 * </ul>
 * It is NOT called when the file is deleted.
 * <p>
 * The file MAY also be a directory that is opened via a normal open only call.
 * If filePosition=fileSize the we're at EOF.
 * 
 * Each file has a "clusterchain", an arraylist with the number of the cluster for a part of the file.
 * <p>
 * @author and copyright (c) wolfgang lenerz 2013-2017
 * @version  
 *  1.07 when getting the length of the roor dir, get the length set in root sector - the fileheader length.
 *  1.06 readbytes, getLine, sendMultipleBytes: return bytes read / sent in D1.L, not only D1.W, trap#3,D0=6 implemented.
 *  1.05 small optimizations, files aren't passed chan defn blk,setFileHeader call uses the true length of the file, not the one passed in the header block 
 *  1.04 readBytes catches EOF error when nothing is read from channel
 *  1.03 don't set date when making directory
 *  1.02 when converting file into dir get correct filesize after all corresponding files are moved into the new dir,
 *       read files headers : handle subdirs like files.
 *  1.01 dir must be saved even when file was flushed.
 *  1.00 initial version
 */

public class WinFile 
{
    protected int filePosition=WinDriver.HEADER_LENGTH;     // this is the JAVA file position in the  buffer = smsqe position + fileheader(now points after the header of the file).
    protected int fileSize;                                 // file size INCLUDING the header.
    protected java.nio.ByteBuffer buffer;                   // buffer containing the content of the file.
    protected int index;                                    // index into the directory where file is in  = position, in bytes, in the dir.
                                                            // unless the is the root directory, this will always be<>0 (1st file is after file header)
    protected WinDir dir;                                   // the dir containing the file, null if this is the root dir.
    protected WinDrive drive;                               // the drive this dir is on
    protected boolean readOnly;                             // = true if file is read only
    protected boolean isDir=false;                          // true if this file is a directory
    protected java.util.ArrayList<Integer> clusterchain;    // the clusterchain for this file
    protected boolean fileChanged=false;                    // flag : true is something in the file has changed
    protected boolean dirChanged=false;                     // true if something in the dir has changed (e.g. the size of the file changed)
    protected boolean mapChanged=false;                     // is true if the drive FAT has changed, e.g. a cluster was added to the file, or file truncated
    protected boolean setDate=true;                         // must we set the date of the file when closing it? - normally, yes
   // protected String fxname;
    
    /**
     * Creates the object.
     * 
     * @param drive the WinDrive the file is on.
     * @param dir the directory where this file is in.
     * @param index index into the buffer of the dir containing this file (if file is not the root dir) (this index point to the file header for this file).
     * @param readOnly set to true of this file is opened for read only access.
     * @param buf the buffer to be used by this file.
     * @param cchain the clusterchain for this object.
     */
    public WinFile(WinDrive drive,WinDir dir, int index,boolean readOnly,java.nio.ByteBuffer buf,java.util.ArrayList<Integer> cchain)
    {
        this.drive=drive;
        this.readOnly=readOnly;
        this.index=index;
        this.dir=dir;
        this.buffer=buf;
        this.buffer.position(this.filePosition);
        this.clusterchain=cchain;
        if (this.dir!=null)
            this.fileSize=this.dir.getFileLength(this.index);
        else
            this.fileSize=this.drive.getIntFromFAT(WinDrive.QWA_RLEN);
        // special check whether this file is a dir even if opened normally. If it is a dir, the file is read only.
        if (!readOnly)
        {
            if (this.dir==null) 
            {
                this.readOnly=true;                         // if this file has no parent directory, it is the main directory  -> read only!
            }
            else 
            {
                this.readOnly=this.dir.fileIsDir(this.index);// check that this file isn't a dir - it it is, read only.
            }
        }
        if (this.index==0)                                  // this is only 0 if we're opening the main directory as a normal file
        {
            this.isDir=true;
            this.readOnly=true;
        }
       
         // useful for debugging (uncomment function in helper, too)
        /*
        if(this.dir!=null)
        {
            fxname=Helper.readSmsqeString(this.dir.getDirBuffer(), this.index+WinDir.HDR_NAME);
        }*/
    }
    
    /**
     * Do not call this way of opening for real.
     */
    public WinFile()
    {
        this.drive=null;
    }
    
    /**
     * Closes this file : writes the file to disk if need be.
     * 
     * Also possibly causes the dir and the drive to write themselves out to disk.
     */
    public void close()
    {
        if (!this.readOnly)                                 // if file is read only, nothing of the file should be saved to dsik
        {
            if (this.fileChanged)                           // something in the file has changed, write out the current cluster
            {
                writeFile(0,0);
            }
        }
        if (this.dir!=null)
            this.dir.closeFile(this.index, this.dirChanged,this.setDate);  // close this file in dir: set size & dates, write dir back to file
        if (this.mapChanged)                                // if something in the FAT has changed, write out the FAT.
            this.drive.flush();
        this.buffer=null;
    }
    
    /**
     * This dispatches the file I/O routines (=SMSQE TRAP#3 routines).
     *
     * NB : case 0x4a : rename file is done by the drive object directly, not by the file.
     * Same for 45 (info about medium) and 4f (extended info)
     * In all cases, the timeout (in D3) is totally ignored - all operations will always take place in what the cpu thinks is no time.
     * 
     * @param trapKey what kind of trap#3 are we dealing with?
     * @param cpu the CPU.
     */
    public void handleTrap3(int trapKey,smsqmulator.cpu.MC68000Cpu cpu)
    {
        switch (trapKey)
        {
            case 0:                                         // check for pending input:...
                if (this.fileSize>this.filePosition)  
                    cpu.data_regs[0]=0;                     // ...there is some
                else
                    cpu.data_regs[0]=Types.ERR_EOF;         //... there is none 
                break;                                      
                
            case 0x01:                                      // get one byte...
                if (this.filePosition>=this.fileSize)       // 
                    cpu.data_regs[0]=Types.ERR_EOF;         //... there is none 
                else
                {
                    cpu.data_regs[1]=(cpu.data_regs[1]&0xffffff00)+ (this.buffer.get()&0xff);// set byte (increases buffer position)
                    this.filePosition++;                    // increase fileposition
                    cpu.data_regs[0]=0; 
                }
                break;
      
            case 0x02:                                      // get a line ending in LF from the file
                getLine(cpu);
                break;
         
            case 0x03:                                      // read multiple bytes from the file into memory
                readBytes(cpu,cpu.data_regs[2]&0xffff);
                break;
                
            case 0x05:                                      // send one byte to channel
                if (this.readOnly)
                {
                    cpu.data_regs[0]=Types.ERR_RDO;
                }
                else   
                {
                   sendByte(cpu);
                }
                break;
                
            case 0x06:                                      // send multiple untranslated bytes to the file
            case 0x07:                                      // send multiple bytes to the file
                if (this.readOnly)
                {
                    cpu.data_regs[0]=Types.ERR_RDO;
                }
                else   
                {
                    sendMultipleBytes(cpu,cpu.data_regs[2]&0xffff);  
                }
                break;
                              
           
            case 0x40:                                      // check whether there still is a pending I/O for this file
                cpu.data_regs[0]=0;                         // there never is
                break;  
                
            case 0x41:                                      // flush all buffers for this file. I take this to mean that I should also write the FAT & the dir entry
                cpu.data_regs[0]=0;                         // preset no error
                if (this.fileChanged)                       
                {
                    try
                    {
                        this.drive.writeFile(this.buffer, this.clusterchain);
                    }
                    catch (Exception e)
                    {
                        cpu.data_regs[0]=Types.ERR_ICHN;    // signal that something went wrong
                    }
                    this.fileChanged=false;
                    if (this.dirChanged)
                    {
                        this.dir.writeFile(0,0);
                     //   this.dirChanged=false;            // do not set this to false, else file might not have correct date when closed
                    }
                    if (this.mapChanged)
                    {
                        this.drive.flush();
                        this.mapChanged=false;
                    }
                }
                break;  
                 
            case 0x42:                                      // set file position absolute
                this.filePosition=WinDriver.HEADER_LENGTH;  // so pretend it's at start of file and fall through
            case 0x43:                                      // set file position relative     
                this.filePosition+=cpu.data_regs[1];
                cpu.data_regs[0]=0;                         // preset all OK
                if (this.filePosition>this.fileSize)        // would we go beyond end of file? 
                {
                    this.filePosition=this.fileSize;
                    cpu.data_regs[0]=Types.ERR_EOF;         // yes, so set to end of file & return this error.
                }
                if (this.filePosition<WinDriver.HEADER_LENGTH)
                    this.filePosition=WinDriver.HEADER_LENGTH;  // negative position is set to "0" without error
                cpu.data_regs[1]=this.filePosition-WinDriver.HEADER_LENGTH;// file position for SMSQ/E (no header)
                this.buffer.position(this.filePosition);    // keep those in sync
                break;
                
            //case 0x45: already done by driver
                
            case 0x46:                                      // set file header of this file (this is done in the directory)
                if (this.readOnly)
                    cpu.data_regs[0]=Types.ERR_RDO;
                else   
                {
                    this.dir.setFileHeader(this.index,cpu,this.fileSize); // the file header is set in the directory, which should also set the return error flag
                    this.dirChanged=true;                   // signal that dir has changed
                }
                break;
                        
            case 0x47:                                      // read file header of this file from the directory to memory
                int A1=cpu.addr_regs[1];
                int temp=cpu.data_regs[2]&0xffff;           // number of bytes to read
                if (temp>WinDriver.HEADER_LENGTH)
                    temp=WinDriver.HEADER_LENGTH;           // avoid Cueshell bug : never read more that 64 bytes
                if (this.index!=0)                          // this not the root direcotry
                {
                    if (temp>4)
                    {
                        cpu.readFromBuffer(A1+4,temp-4,this.dir.getDirBuffer(),this.index+4);// read bytes of header
                    }
                    cpu.writeMemoryLong(A1, this.fileSize-WinDriver.HEADER_LENGTH);//make length minus header
                }
                else
                {
                    for (int i=A1;i<A1+temp;i+=2)
                    {
                        cpu.writeMemoryWord(i, 0);
                    }
                    cpu.writeMemoryLong(A1,this.drive.getIntFromFAT(WinDrive.QWA_RLEN)-WinDriver.HEADER_LENGTH);
                    cpu.writeMemoryWord(A1+4,0x00ff);
                }
                cpu.addr_regs[1]+=temp;
                cpu.data_regs[1]=(cpu.data_regs[1]&0xffff0000)|temp;// nbr of bytes read
                cpu.data_regs[0]=0;                         // show all OK
                break;
                
            case 0x48:                                      // load entire file from disk into mem
                this.filePosition=WinDriver.HEADER_LENGTH;
                this.buffer.position(this.filePosition);
                readBytes(cpu,this.fileSize-WinDriver.HEADER_LENGTH);
                this.filePosition=this.buffer.position();
                break;
                
            case 0x49:                                      // save entire file from mem to disk
                if (this.readOnly)
                {
                    cpu.data_regs[0]=Types.ERR_RDO;         // not possible if read only
                }
                else
                {
                    sendMultipleBytes(cpu,cpu.data_regs[2]);
                }
                break;
                
            //case 0x4a : rename file is done by the drive object directly
                    
            case 0x4b:                                      // truncate file to current position.
                if (this.readOnly)
                {
                    cpu.data_regs[0]=Types.ERR_RDO;         // not possible if read only
                }
                else
                {
                    this.clusterchain=this.drive.truncateFile(this.fileSize,this.filePosition,this.clusterchain);
                    this.fileSize=this.filePosition;        // truncate file now
                    this.dir.setInHeader(this.index,this.fileSize);// set new filesize in header in dir
                    this.dirChanged=true;
                    this.mapChanged=true;
                    this.fileChanged=true;                  // since this was given to a new clusterchain, must save file when closed
                    cpu.data_regs[0]=0;                     // show all OK
                }
                break;    
                
            case 0x4c:                                      // set/get file date
                cpu.data_regs[0]=0;                         // preset no error
                if (this.dir==null || this.dir.fileIsDir(this.index))
                    return;                                 // can't do that for the main dir, or any dir at all
                int whatDate=(cpu.data_regs[2]&0xff)*4+0x34+this.index;//what date to get/set
                switch  (cpu.data_regs[1])
                {
                    case -1:                                // read date
                        cpu.data_regs[1]=this.dir.getFileDate(whatDate);
                        break;
                        
                    case 0:                                 // set date to current date
                        cpu.data_regs[1]=(int)((System.currentTimeMillis()/1000)+Monitor.TIME_OFFSET); // ** magic offset  file date = now  
                        // no break here, fall through
                    default:                                 // set date to date in D1
                        if (this.readOnly && whatDate!=WinDir.HDR_BKUP)
                             cpu.data_regs[0]=Types.ERR_RDO;
                        else
                        {
                            this.dir.setFileDate(whatDate,cpu.data_regs[1]);
                            this.dirChanged=true;
                            this.setDate=false;
                        }
                        break;
                }
                break;
                
            case 0x4d:                                      // make directory
                makeDirectory(cpu);
                break;
                
            case 0x4e:                                      // set/get file version
                cpu.data_regs[0]=0;                         // preset no error
                if (this.dir==null || this.dir.fileIsDir(this.index))
                    return;                                 // can't do that for the main dir, or any dir at all
                if (cpu.data_regs[1]==-1 || cpu.data_regs[1]==0)       
                {
                    cpu.data_regs[1]=this.dir.getFileVersion(this.index);
                }
                else if ( cpu.data_regs[1]>0)  
                {
                    if (this.readOnly)
                             cpu.data_regs[0]=Types.ERR_RDO;
                    else
                    {
                        this.dir.setFileVersion(this.index, cpu.data_regs[1]);
                        this.dirChanged=true;
                    }
                }
                break;
                
            //case 0x4f caught by the driver
                
            default:                                        // catch any other trap calls
                cpu.data_regs[0]=Types.ERR_NIMP;            // what is that? whatever it is, it is not implemented for this device
                break;
        }   
    }
    
    /*************************************** The individual file i/o routines in no particular order *********************************/
     /**
     * Reads a certain number of bytes from the file into memory.
     * 
     * @param cpu the cpu used : A1 points to where the bytes must be read to in the cpu's memory.
     * @param bytesToRead number of bytes to read.
     */
    private void readBytes(smsqmulator.cpu.MC68000Cpu cpu,int bytesToBeRead)
    {
        int bytesToRead=bytesToBeRead;
        if (bytesToRead==0)
        {
            cpu.data_regs[1]=0;
            cpu.data_regs[0]=0;                             // there is nothing to read, so just leave
            return;
        }
        
        if (this.filePosition>=this.fileSize& bytesToRead!=0)               // we're trying to read from beyond the file end:
        {
            cpu.data_regs[0]=Types.ERR_EOF;                 // no can do
            return;
        }
        if (this.filePosition+bytesToRead > this.fileSize)  // reading would exceed EOF : get as many bytes as possible.
            bytesToRead=this.fileSize-this.filePosition;
        
        int bytesRead=cpu.readFromBuffer(cpu.addr_regs[1], bytesToRead, this.buffer, this.filePosition);// nbr of bytes read
        if (bytesRead==-1)
        {
            cpu.data_regs[1]&=0xffff0000;                       // no bytes gotten
            cpu.data_regs[0]=Types.ERR_EOF;                     // show that we tried to read more than the file has to give (end of file reached)
            return;
        }
        this.filePosition+=bytesRead;
        this.buffer.position(this.filePosition);                // new file position
        cpu.addr_regs[1]+=bytesRead;                            // updated buffer pointer for SMSQE
      //  cpu.data_regs[1]=(cpu.data_regs[1]&0xffff0000)|bytesRead;// necessary for iob.fmul, not for iof.load
        cpu.data_regs[1]=bytesRead;                             // necessary for iob.fmul, not for iof.load
        if (bytesRead==bytesToBeRead)
        {
            cpu.data_regs[0]=0;                   // all went well
        }
        else
        {
            cpu.data_regs[0]=Types.ERR_EOF;       // show that we tried to read more than the file has to give (end of file reached)
        }
    }
    
    /**
     * Gets a line ending in LF (=0x0a) from the file into the memory buffer pointed to by A1. 
     * If the line ends in CR LF,the CR (=0x0d) is stripped off.
     * If the buffer into which the line must be got is too small, the buffer still is filled with chars until its end.
     * 
     * @param cpu my cpu - A1 contains the buffer where to put the bytes, D2.w the length of the buffer.
     */
    private void getLine(smsqmulator.cpu.MC68000Cpu cpu)
    {
        if (this.filePosition>=this.fileSize)
        {
            cpu.data_regs[0]=Types.ERR_EOF;                 // I can't read anything : am at the end of the file
            return;  
        }                                                   
        this.buffer.position(this.filePosition);            // this is where I read from
        
        int A1 = cpu.addr_regs[1];                          // where to read to - THIS MAY BE AN ODD ADDRESS!!!!!
        int bufflen=cpu.data_regs[2]&0xffff;                // length of buffer to read into
        if (this.filePosition+bufflen>=this.fileSize)
        {
            bufflen=this.fileSize-this.filePosition;        // nbr of bytes to get
        }
        boolean foundit=false;
        byte res;
        int oldA1=A1;
        for (;A1<oldA1+bufflen;A1++)                        // used to be (;A1<A1+bufflen;A1++) but apparently A1+bufflen get re-evaluated each time in the loop (HUH????)
        {
            res=this.buffer.get();                          // get byte from my buffer
            cpu.writeMemoryByte(A1, res);                   // write it into memory
            if (res==10)                                    // check for LF
            {
                A1++;                                       // point after LF byte
                foundit=true;
                break;
            }
        }
        
        if (!foundit)                                       // no LF found - buffer must be too small or there is no LF until eof!
        {
            if (bufflen<cpu.data_regs[2])                   // there is no LF before the end of the file
            {
                cpu.data_regs[0]=Types.ERR_EOF; 
            }
            else
            {
                cpu.data_regs[0]=Types.ERR_BFFL;            // no LF found in bytes I got, signal buffer full
            }
        }
        else
        {
            bufflen=A1-oldA1;// this is the nbr of chars we got until an LF
            if (bufflen<=0)
            {
                cpu.data_regs[0]=Types.ERR_EOF;             // ??????????????? WTF?
                return;
            }
            else
            {                                               // check for CR before LF and strip it
                if (bufflen>1 && this.buffer.get(this.buffer.position()-2)==0x0d)
                {
                    cpu.writeMemoryByte(A1-2,0x0a);         // convert CR into LF
                    A1--;                                   
                    bufflen--;
                }
                cpu.data_regs[0]=0; 
            }
        }
        cpu.addr_regs[1]=A1;
   //     cpu.data_regs[1]=(cpu.data_regs[1]&0xffff0000)| bufflen;// nbr of bytes read in lower word
        cpu.data_regs[1]=bufflen;                               // nbr of bytes read
        this.filePosition=this.buffer.position();               // new file position
    }
    
    /**
     * Writes one byte to the file.
     * (The byte is in D1.B).
     * 
     * @param cpu 
     */
    private void sendByte(smsqmulator.cpu.MC68000Cpu cpu)
    {
        if (this.filePosition>=this.buffer.capacity())      // is there enough space to add one byte?
        {
            this.buffer.position(0);                        // no, increase space
            this.buffer.limit(this.fileSize);               // prepare for increase capacity
            java.nio.ByteBuffer buf = this.drive.increaseCapacity(this.buffer,1,this.clusterchain);
            if (buf==null)
            {
                cpu.data_regs[0]=Types.ERR_DRFL;
                return;
            }
            this.buffer=buf;
            this.mapChanged=true;
        }   
        this.buffer.position(this.filePosition);
        this.buffer.put((byte)(cpu.data_regs[1]&0xff));
        this.filePosition++;                                    
        if (this.filePosition>this.fileSize)
        {
            this.fileSize++;               
            this.dir.setInHeader(this.index, this.fileSize);// new size of file in dir
            this.dirChanged=true;                           // dir should be saved when file closes                 
        }
        this.fileChanged=true;                              // file should be saved when closed
        cpu.data_regs[0]=0;
    }
    
    /**
     * Writes multiple bytes to the file.
     * 
     * @param cpu the CPU with address reg 1 pointing to where to take the bytes from.
     * @param bytesToWrite nbr of bytes to write.
     */
    private void sendMultipleBytes(smsqmulator.cpu.MC68000Cpu cpu, int bytesToWrite)
    {
        if (bytesToWrite==0)
        {
            cpu.data_regs[1]&=0xffff0000;
            cpu.data_regs[0]=0;
            return;
        }
        
        // check whether we have enough space in the buffer  
        int spaceLeft=this.buffer.capacity()-this.filePosition; // how much space I have left in the buffer to write as of here
        if (spaceLeft<bytesToWrite)
        {
            this.buffer.position(0);                            // prepare for increaseCapacity call.
            this.buffer.limit(this.fileSize);
            java.nio.ByteBuffer buf = this.drive.increaseCapacity(this.buffer,bytesToWrite-spaceLeft,this.clusterchain);
            if (buf==null)
            {
                cpu.data_regs[0]=Types.ERR_DRFL;
                return;
            }
            this.buffer=null;
            this.buffer=buf;
            this.mapChanged=true;
        }   
        this.buffer.position(this.filePosition);            // start to write as of here
      
        /* special case if only one byte is sent - but the extra complication isn't worth it, no noticeable speed increase
        if (bytesToWrite==1)
        {
            this.buffer.put((byte)(cpu.readMemoryByte(cpu.addr_regs[1]++)));
            this.filePosition++;                                    
            if (this.filePosition>this.fileSize)
            {
                this.fileSize++;               
                this.dir.setInHeader(this.index, this.fileSize);// new size of file in dir
                this.dirChanged=true;                           // dir should be saved when file closes                 
            }
            this.fileChanged=true;                              // file should be saved when closed
        
            cpu.data_regs[1]&=0xffff0000;                       // eliminate lower word of D1
            cpu.data_regs[1]|= 1;                               // put nbr of bytes sent 
            cpu.data_regs[0]=0;
            return;
        } 
        */
        bytesToWrite= cpu.writeToBuffer(this.buffer,cpu.addr_regs[1],bytesToWrite);
        if (bytesToWrite == -1)
        {
            cpu.data_regs[0]=Types.ERR_DRFL;
            return;
        }
        this.filePosition+=bytesToWrite;
        if (this.filePosition>this.fileSize)                
            this.fileSize=this.filePosition;
        this.buffer.position(this.filePosition);
        cpu.addr_regs[1]+=bytesToWrite;
    //    cpu.data_regs[1]&=0xffff0000;                       // eliminate lower word of D1
      //  cpu.data_regs[1]|= (bytesToWrite&0xffff);           // put nbr of bytes there
        cpu.data_regs[1]= bytesToWrite;                         // put nbr of bytes there
        cpu.data_regs[0]=0;
        this.dir.setInHeader(index, this.fileSize);             // new size of file in dir
        this.dirChanged=true;                                   // dir should be written out
        this.fileChanged=true;
    }
        
    /**
     * Converts this (newly created) file into a directory.
     * This necessarily happens to a new created file. If the file wasn't newly created (i.e. 0 size) then I refuse to make it into a dir.
     * 
     * @param cpu 
     */
    private void makeDirectory(smsqmulator.cpu.MC68000Cpu cpu)
    {
        cpu.data_regs[0]=Types.ERR_RDO;                     // pretend this is read only      
       
        if (this.fileSize!=WinDriver.HEADER_LENGTH || this.dir==null || this.dir.fileIsDir(this.index) || this.clusterchain.size()!=1)  // the file MUST be empty for it to be converted into a dir.
        {                                                   // it must not be the main dir and not be a dir already             
             cpu.data_regs[0]=Types.ERR_IPAR;               //  
             return;
        }
        
        if (this.dir.makeDirectory(this.index, this.clusterchain, this.buffer))// make me into a directory within the direcory that contains me
        {
            this.fileSize=this.dir.getFileLength(this.index);
            //this.fileSize=WinDriver.HEADER_LENGTH;         
            this.filePosition=this.fileSize;
            this.readOnly=true;                                 // I was successfully converted into a dir, now avoid any more I/O actions in here
            cpu.data_regs[0]=0;
            this.setDate=false;
        }
    }
    
    /**
     * Writes all or part of this file back to the disk.
     * 
     * @param start where to start writing from (index into the buffer).
     * @param nbr  how many bytes to write.
     * Note: if both start and nbr are 0, the entire file is written.
     */
    public void writeFile(int start,int nbr)
    {
        try
        {
            if (start==0 && nbr==0)
            {
                this.drive.writeFile(this.buffer, this.clusterchain); // write the file to the disk
            }
            else
            {
                this.drive.writePartOfFile(this.buffer, this.clusterchain,start,nbr); // write the file to the disk
            }
        }
        catch (Exception e)
        {
            /*nop*/
        }
    }
    
    /**
     * Sets whether this file is a dir or not.
     * 
     * @param dirstat is true if this file is to be a dir.
     */
    public void setDirStatus (boolean dirstat)
    {
        this.isDir=dirstat;
    }
   
    
    /**
     * Gets the directory this file is in.
     * 
     * @return the directory this file is in, may be null if this is the main dir.
     */
    public WinDir getDir()
    {
        return this.dir;
    }
    
    /**
     * Sets the directory this file is in.
     * 
     * @param newDir the new directory this file is in.
     */
    public void setDir(WinDir newDir)
    {
        this.dir=newDir;
    }
    
    /**
     * Gets the file index.
     * 
     * @return the index , i.e. where in its directory this file lies.
     */
    public int getIndex()
    {
        return this.index;
    }
    
    /**
     * Sets the file index.
     * 
     * @param newIndex the new index into the directory.
     */
    public void setIndex(int newIndex)
    {
        this.index=newIndex;
    }
    
    /**
     * Show that the directory and the FAT must have changed.
     */
    public void setDirAndFatChanged()
    {
        this.dirChanged=true;
        this.mapChanged=true;
    }
}
    
