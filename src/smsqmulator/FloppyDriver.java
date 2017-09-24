package smsqmulator;

/**
 * This is the driver for reading and writing QL style floppy disk images. 
 * Each floppy disk image will be one drive.
 * This can only deal with QL format floppy disks.
 * 
 * Notes:<p>
 * QL Floppy disks are FAT based. A FAT entry is 3 bytes long: the first 12 bits hold the file number, the last 12 bits the block number within the file.
 * So, to find all blocks belonging to a file : get all FAT entries with the number of the file and order them according to the block number.<p>
 * The first entry in every FAT is that of the FAT itself. The FAT itself is always file number $F80 in the FAT.<p>
 * Special file numbers in the FAT are:
 *  - $000 - the directory
 *  - $F80 - the FAT
 *  - $FCF - cluster (block) is marked for deletion
 *  - $FDF - cluster (block) is free
 *  - $FDx - cluster (block) is free (belonged to a file and was deleted) 
 *  - $FEF - cluster (block) is bad
 *  - $FFF - cluster (block) doesn't exist
 * 
 * The <b>root directory</b> is always file zero. <p>
 * 
 * NB the FAT is also called the "map".
 * 
 * Most of the nitty-gritty of the floppy interface is handled by SMSQ/E itself, this driver mainly exposes routines to 
 * read/write a sector, and to handle formatting a disk.
 * 
 * @author and copyright (c) 2014 Wolfgang Lenerz
 * @version 
 * 1.07 setCpu added
 * 1.06 correctly get floppy number parameter for setDrive.
 * 1.05 use StringBuilder in setNames
 * 1.04 getDrive correctly sets the address to write the String into SMSQE.
 * 1.03 checks for read only image files and warns of them.
 * 1.02 checks whether drive has changed, closes raChannel when filename has changed, better handling of setting native filenames.
 * 1.01 formats 1.44 MB disks, too
 * 1.00 initial version - only formats 720 K disks.
 */
public class FloppyDriver
{
    private final java.io.RandomAccessFile[] raFile;            // every access to this drive goes through this file...
    private final java.nio.channels.FileChannel [] ioChannel;   // ... and its associated channel
    private final String [] nativeFile;                         // names of the native dirs
    private final int [] flpType;                               // type of floppy
    private smsqmulator.cpu.MC68000Cpu cpu;                     // the cpu being emulated
    public static final int QL5A=0x514c3541;                    // 'QL5A' - 720 KiB floppies
    public static final int QL5B=0x514c3542;                    // 'QL5B' - 1.4 MiB floppies
    private static final int s720K=720*1024;                    // size of a 720 K image file
    private final int usage=Types.FLPDriver;                          // usage name for device.
    private final boolean[] driveStatusChanged={true,true,true,true,true,true,true,true};
    private final inifile.IniFile inifile;
    private final boolean[]readOnly={false,false,false,false,false,false,false,false};

    /**
     * Creates the object.
     * 
     * @param cpu the cpu used.
     * @param inifile the .ini file with initial option values.
     */
    public FloppyDriver(smsqmulator.cpu.MC68000Cpu cpu,inifile.IniFile inifile)
    {
        this.cpu=cpu;
        this.raFile= new java.io.RandomAccessFile[8];
        this.ioChannel= new java.nio.channels.FileChannel [8];
        this.nativeFile=new String[8];
        this.flpType=new int [8];
        this.inifile=inifile;
    }
    
    
    /**
     * Sets the names of the native files to be used for each drive of the device.
     * Checks that a file is a valid QL floppy image file.
     * <p> In keeping with standard SMSQE practice, it is presumed throughout that each device may have 8 drives.
     *
     * @param names an 8 element String array with names for the drives.
     * @param doPopup = true if a popup should be popped up tellling about errors
     * @param driveToSet if this is -1, set all drives, else set only the drive with that number
     * 
     * @return <code>true</code> if names were set, else <code>false</code>.
     *  algorithm is as follows:
        
         - Check whether a filename already exists. This will not be the case if this is called the very first time
         - if a filename already exists, check whether that file is open (i.e. there is a valid channel to it)
         - if a new file should NOT be opened 
                if an old file exists, close the old file, 
                erase the name in the native files array
         - if a new file should be opened and can be opened : substitue new for old
         - if a new file should be opened but cannot be opened
             - if an old file exists, close it (should I leave it open)open
     */
    public boolean setNames(String[]names,boolean doPopup, int driveToSet)
    {
        if (this.inifile.getTrueOrFalse("DISABLE-FLP-DEVICE"))
            return false;                                       // no floppies allowed, but don't popup anything
        StringBuilder errStr=new StringBuilder();               // possible error strings
   
        boolean filenameExists,fileExists;                      // used to check whether this drive already points to a valid image
        for (int i=0;i<8;i++)
        {
            this.driveStatusChanged[i]=true;
            filenameExists=(this.nativeFile[i]!=null && (!this.nativeFile[i].isEmpty()));
            if (filenameExists)
            {
                fileExists=this.raFile[i]!=null;
            }
            else
                fileExists=false;
            
            if (names[i]==null || names[i].isEmpty())           // there should not be any more file
            {
                if (fileExists)
                    raFileClose(i);                             // close file
                continue;                                       // name will be set to "" after end of loop
            }
            
            if (fileExists && names[i].equals(this.nativeFile[i]))
                continue;                                       // file exists and is the same as the one we're trying to set : do nothing
            this.readOnly[i]=false;
            OpenFileCheck ofc=new OpenFileCheck();
            ofc.checkDrive(names[i]);                           // try to open this file now
            boolean checkOK=ofc.errStr==null;
            if (!checkOK && ofc.errStr.contains(Localization.Texts[123]))
            {
                checkOK=true;
                this.readOnly[i]=true;
                if(inifile.getTrueOrFalse("WARN-ON-FLPDRIVE-READ-ONLY"))
                {
                    errStr.append(ofc.errStr).append("\n)");
                }
            }
            
            if (checkOK)                              
            {                                                   // file could validly be opened
                if (this.raFile[i]!=null)                       // this drive already assigned?
                    raFileClose(i);                             // yes, close old file  
                this.raFile[i]=ofc.raFile;                      // and set the channels
                this.ioChannel[i]=ofc.ioChannel;
                this.flpType[i]=ofc.flpType;
            }
            else                                                // new file could not validly be opened
            {
                if (filenameExists)                             // an old file existed?
                {
                    if (fileExists)                             // this drive already assigned?
                        raFileClose(i);                         // yes, close old file  
                }
                errStr.append(ofc.errStr).append("\n");
            }
        }
        System.arraycopy(names, 0,this.nativeFile, 0,8);
        if (errStr.length()!=0 && doPopup && inifile.getTrueOrFalse("WARN-ON-NONEXISTING-FLPDRIVE"))
        {
            Helper.reportError(Localization.Texts[45], Localization.Texts[81]+"\n"+errStr.toString(), null);
            return false;
        }
        return true;
    }
        
    /**
     * Reads a sector.
;       (a7)   sector to read
;       d2 cp   number of sectors to read
;       d7 c  p drive ID / number
;       a1 c  p address to read into
;       a3 c  p linkage block
;       a4 c  p drive definition
;
;       status return 0 or ERR.MCHK
;    */
    public void readSector()
    {
        int sector=this.cpu.readMemoryLong(this.cpu.addr_regs[7]);
        int drive = (this.cpu.data_regs[7]&0xffff)-1;
        if (drive<0 || drive > 7 || this.raFile[drive]==null)
        {
            this.cpu.data_regs[0]=Types.ERR_MCHK;
            return;
        }
        long position=calculatePosition (sector,drive);
        try
        {
            this.ioChannel[drive].position(position);
            this.cpu.readFromFile(this.cpu.addr_regs[1], 512, this.ioChannel[drive]);
            this.cpu.data_regs[0]=0;
            this.cpu.data_regs[2]=1;
        }
        catch (Exception e)
        {
            this.cpu.data_regs[0]=Types.ERR_MCHK;
        }
        this.cpu.data_regs[0]=0;
    }
    
    /**
     * Writes a sector.
     *  (a7)    sector to write
     *  d2 cp   number of sectors to write
     *  d7 c  p drive ID / number
     *  a1 c  p address to write from
     *  a3 c  p linkage block
     *  a4 c  p drive definition
     *  status return 0 or ERR.MCHK
     */
    public void writeSector()
    {
        int sector=this.cpu.readMemoryLong(this.cpu.addr_regs[7]);
        int drive = (this.cpu.data_regs[7]&0xffff)-1;
        if (this.readOnly[drive])
        {
            this.cpu.data_regs[0]=Types.ERR_RDO;
            return;   
        }
        if (drive<0 || drive > 7 || this.raFile[drive]==null)
        {
            this.cpu.data_regs[0]=Types.ERR_MCHK;
            return;
        }
        long position=calculatePosition (sector,drive);
        try
        {
            this.ioChannel[drive].position(position);
            this.cpu.writeToFile(this.cpu.addr_regs[1], 512, this.ioChannel[drive]);
     //       this.ioChannel[drive].force(true);
            this.cpu.data_regs[0]=0;
            this.cpu.data_regs[2]=1;
        }
        catch (Exception e)
        {
            this.cpu.data_regs[0]=Types.ERR_MCHK;
        }
    }
    
    /**
     * This routine formats a medium.
;
;       d0 cr   format type / error code
;       d1 cr   format dependent flag or zero / good sectors
;       d2  r   total sectors
;       d7 c  p drive ID / number
;       a3 c  p linkage block
;       a4 c  p drive definition
     */
    public void formatDrive()
    {
        int type =this.cpu.data_regs[2]&0xff;                   // type of format 0 = SD 1 = DD, 2=HD, 3=ED olny 1 & 2 allowed, defaults to 1
        int drive = (this.cpu.data_regs[7]&0xffff)-1;
        if (this.nativeFile[drive]==null || this.nativeFile[drive].isEmpty())
        {
            this.cpu.data_regs[0]=Types.ERR_FMTF;
            return;                                             // no native file exists for that!
        }
        if (this.raFile[drive]==null)                           // no channel already opened?
        {
            java.io.File file=new java.io.File(this.nativeFile[drive]);   // the drive were talking about
            try
            {
                if (file.exists())
                {
                    this.cpu.data_regs[0]=Types.ERR_FMTF;
                    return;            // there is something seriously wrong here, this file should have been opened when SMSQmuator started, better not delete it,or lese couldn't create it.
                }
                this.raFile[drive] = new java.io.RandomAccessFile(this.nativeFile[drive],"rw");// read/write access to this file
                this.ioChannel[drive] = this.raFile[drive].getChannel();// and the associated file channel
            }
            catch (Exception e)
            {
                this.cpu.data_regs[0]=Types.ERR_FMTF;
                    return;
            }
        }
        // as of here, file exists and channel is open to it.
        try
        {
            this.ioChannel[drive].truncate(0);
            this.ioChannel[drive].position(0);                  // set filesize to 0 & position at beginning
            java.nio.ByteBuffer buf;
            if (type!=2)
            {
                buf =java.nio.ByteBuffer.allocate(FloppyDriver.s720K);             
                this.flpType[drive]=FloppyDriver.QL5A;              // format 720 K floppies
                type=9;
            }
            else
            {
                buf =java.nio.ByteBuffer.allocate(FloppyDriver.s720K*2);             
                this.flpType[drive]=FloppyDriver.QL5B;              // format 1.44 M floppies
                type=18;
            }
            for (int i=0;i<buf.capacity();i+=4)
                buf.putInt(i,0x30303030);                       // empty QL floppy sectors are filled with ascii "0"
            this.ioChannel[drive].write(buf);                   // so fill them
            this.ioChannel[drive].force(true);
            this.cpu.data_regs[1]=this.cpu.data_regs[2]=type;
            this.cpu.data_regs[0]=0;
        }
        catch (Exception e)
        {
            this.cpu.data_regs[0]=Types.ERR_FMTF;   
        }
    }
    
    /**
     * Calculates the position, in the image file, of a sector.
     * 
     * @param sector  (given as track | side|sector).
     * @param drive which drive does this concern?
     * 
     * @return the position in the file.
     */
    private long calculatePosition (int sector,int drive)
    {
        // sector MSW = Track ; LSW = Side|Sector. In th eimage file, the sectors are contiguous ad follows:
        // image file = track   0 - side 0 -sectors 0-8 then
        //                      0        1 -sectors 0-8 then 
        //                      1        0 -sectors 0-8 etc
        int track=(sector>>>16);
        int side=(sector>>8)&0xff;
        sector&=0xff;
        if (this.flpType[drive]==QL5A)
           return  (long) track*18*512+(side*9*512)+(sector-1)*512;
        else
           return  (long) track*36*512+(side*18*512)+(sector-1)*512;
    }
    
    /**
     * Checks whether medium has changed.
     */
    public void checkDriveStatus()
    {
        int drive = (this.cpu.data_regs[7]&0xffff)-1;
        if (this.driveStatusChanged[drive])
        {
            int A4 = this.cpu.addr_regs[4]+0x24;
            this.cpu.writeMemoryByte(A4, 1);                    // medium has changed
            this.driveStatusChanged[drive]=false;
        }
    }
    /**
     * Checks whether medium is write protected.
     */
    public void checkWriteProtect()
    {
        int drive = (this.cpu.data_regs[7]&0xff)-1;
        if (drive<0 || drive > 7 || this.raFile[drive]==null)
        {
            this.cpu.data_regs[0]=Types.ERR_MCHK;               // drive doesn't exist
            this.cpu.reg_sr|=8;                                 // set neg flag
            return;
        }
        if (this.readOnly[drive])                               // drive is write protected
        {
            this.cpu.reg_sr&=0xfffb;                            // unset Z flag
        }
        else                                                    // drive not write protected
            this.cpu.reg_sr|=4;                                 // set Z flag
            
        this.cpu.data_regs[0]=0;
    }
    
    /**
     * Sets the native file name for a drive (called from SMSQ/E)
     */
    public void setDrive()
    {
        int addr=this.cpu.addr_regs[6]+this.cpu.addr_regs[1];
        int driveNbr= this.cpu.data_regs[6]&0xffff-1;// driver number is on ari stack
        String fname=this.cpu.readSmsqeString(addr);
        if (driveNbr<0 ||driveNbr>8)
        {
            this.cpu.data_regs[0]=Types.ERR_IPAR;               // drive nbr only between 1 and 8
            return;                         // PREMATURE EXIT
        }
        if (fname==null || fname.isEmpty() || this.inifile.getTrueOrFalse("DISABLE-FLP-DEVICE"))
        {
            this.cpu.data_regs[0]=Types.ERR_ITNF;               // can't find this drive
            return;                         // PREMATURE EXIT
        }  
        String []names=new String[8];
        names[driveNbr]=fname;
        boolean ok = setNames(names,false,driveNbr);
        if (!ok)
            this.cpu.data_regs[0]=Types.ERR_ITNF; 
        else
            this.cpu.data_regs[0]=0; 
    }
    
     /**
     * gets the native filename back to SMSQ/E
     */
    public void getDrive()
    {
        int addr=this.cpu.addr_regs[6]+this.cpu.addr_regs[1];
        int driveNbr=this.cpu.readMemoryWord(addr)-1;// driver number is on ari stack
        if (driveNbr<0 ||driveNbr>8 || this.nativeFile[driveNbr]== null || this.nativeFile[driveNbr].isEmpty() || this.inifile.getTrueOrFalse("DISABLE-FLP-DEVICE"))
        {
            this.cpu.data_regs[0]=Types.ERR_IPAR;
            return;                         // PREMATURE EXIT
        }
        String name=this.nativeFile[driveNbr];
        addr=(addr-2-name.length())&0xfffffffe;// this is where we write
        this.cpu.addr_regs[1]=addr-this.cpu.addr_regs[6];//a1 is relative to A6
        this.cpu.writeSmsqeString(addr, name,256);
        this.cpu.data_regs[0]=0;
    }
    
    private void raFileClose(int nbr)
    {
        try
        {
            this.raFile[nbr].close();
        }
        catch (Exception e)
        { /*nop*/ }
        this.raFile[nbr]=null;
        this.ioChannel[nbr]=null;
    }
    
    /**
     * Gets the names of the native files used for each drive of the device.
     * <p> In keeping with standard SMSQE practice, it is presumed throughout that each device may have 8 drives.
     *
     * @param deviceID an int containing the name of the device for which the names are to be returned (eg 'NFA0', i.e. 0x0x4e464130).
     * 
     * @return An 8 elements string array with the names, or <code>null</code> if the device ID wasn't mine. 
     * Individual elements may be <code>null</code> or empty if the corresponding drive isn't assigned.
     */
    public String[] getNames(int deviceID) 
    {
        return this.nativeFile.clone();
    }
 
    /**
     * Sets the cpu used by the device driver.
     * 
     * @param cpu the cpu to be set.
     */
    public void setxCpu (smsqmulator.cpu.MC68000Cpu cpu)
    {
        this.cpu=cpu;
    }
    
    /**
     * This class is used to try to open a native file as a valid QL Image.
     */
    private class OpenFileCheck
    {
        private java.io.RandomAccessFile raFile;
        private java.nio.channels.FileChannel ioChannel;
        private String errStr;
        private int flpType;
        
        /**
         * Checks whether a native file seems to be a valid QL disk image file.
         * 
         * At the end of this operation, this.errStr will either be null, or contain an error string
         * If errStr is not null, raFile and ioChannel must have been validly filled in.
         * 
         */
        public void checkDrive(String name)
        {
            java.io.File device=new java.io.File(name);         // the drive were talking about
            if (!device.exists())      
            {
                 this.errStr = name +" "+ Localization.Texts[64];
                 return;                                        // oops, can't even be found
            }
            try
            {
                this.raFile=new java.io.RandomAccessFile(device,"rw");// get read/write access to this file
            }
            catch (Exception e)
            {
                try
                {
                    this.raFile=new java.io.RandomAccessFile(device,"r");// get read only access to this file
                    this.errStr = name +" "+ Localization.Texts[123];// signal that file is read only
                }
                catch (Exception ex)
                {
                    this.errStr = name +" "+ Localization.Texts[64];// oops consider file not found
                    return;     
                }
            }
            
            this.ioChannel = this.raFile.getChannel();          // the associated file channel
            long fsize;
            java.nio.ByteBuffer driveMap=java.nio.ByteBuffer.allocate(512);
            try
            {
                fsize =  this.ioChannel.size();                 // must be 720 or 1440 KiB 
                this.ioChannel.read(driveMap);                  // read the first sector (512 bytes) of the disk
                int type=driveMap.getInt(0);
                boolean ok=false;                               // preset error
                switch (type)
                {
                    case QL5A:
                        ok=(fsize==s720K);
                        break;

                    case QL5B:
                        ok= (fsize==s720K*2);
                        break;
                }
                if (!ok)
                {
                    this.errStr = name +" "+ Localization.Texts[86]; // oops consider file not found
                }
                else
                    this.flpType=type;
            }
            catch (Exception e)
            {
                 this.errStr = name +" "+ Localization.Texts[86];    // oops consider file not found
            }
        }
    }
    /**
     * Sets the cpu for this object.
     * @param cpu2 the cpu to set.
     */
    public void setCPU (smsqmulator.cpu.MC68000Cpu cpu2)
    {
        this.cpu=cpu2;
    }
}
    
