
package smsqmulator;


/**
 * Creates an SMSQE fileheader for QEMU or SFA files on SFA devices.
 * The length of the file in the header is a length WITHOUT the length of the header.
 * 
 *  ------------ According to Daniele Terdina ------ :
 * 
QL files have a special piece of information associated with them, called the ‘QDOS file header’.
The header stores such information as the file name and whether the file is an executable program.
Q-emuLator stores part of the header at the beginning of files. The header is present
only when it is useful, ie. only if it contains non-default information.
The header has the following format:

OFFSET  LENGTH(bytes)         CONTENT
0       18                    “]!QDOS File Header“
18      1                     0 (reserved)
19      1                     total length_of_header, in 16 bit words
20      length_of_header*2-20 QDOS INFO

The first 18 bytes are there to detect whether the header is present (ID string).
The headers Q-emuLator supports can be 30 bytes or 44 bytes long (the value of the
corresponding byte at offset 19 is either 15 or 22). In the first case, there are 10 bytes with the values
present in bytes 4 to 13 of the 64 bytes QDOS header. In the second case the same piece of
information is followed by 14 bytes containing a microdrive sector header, useful for emulating
microdrive protection schemes. Additional header information (file length, name, dates) is obtained
directly from the file through the host file system.
*--------------------------------------
* 
 * However, SMSQmulator wil ALWAYS create a "special" file header on SFA devices.
 * 
 * @author and copyright (c) 2012 - 2017 Wolfgang Lenerz.
 * 
 * @version 
 * 1.03 setFileDates : setting a file date to a date before 01.01.1970 will set date to 01.01.1970 ; show correct date in dir listing.
 * 1.02 setFileDates implemented.
 * 1.01 use StringBuilder whenever appropriate.
 * 1.00 initial version.
 */
public class QemuFileheader implements XfaFileheader
{
    private java.nio.ByteBuffer header=java.nio.ByteBuffer.allocate(Types.SFAHeaderLength);
    private smsqmulator.cpu.MC68000Cpu cpu;
    private int headerLength=0;                             // offset in file from true beginning of file to where SMSQ/E thinks the beginning of hte file is.
    private java.nio.ByteBuffer addInfo=null;               // buffer for possible extra info in the file
    private boolean dateChanged=false;
    
    
    /**
     * Creates the object either from an existing <code>java.io.File</code>, or a newly created one.
     * 
     * @param f the file.
     * @param filename the filename.
     * @param inoutChannel a channel for reading the existing header.
     * @throws java.io.IOException from java io operations. 
     */
    public QemuFileheader (java.io.File f,String filename,java.nio.channels.FileChannel inoutChannel) throws java.io.IOException
    {
        long size=0;
        if (inoutChannel!=null)
            size=inoutChannel.size();                       // get the size of that file - this will be 0 if file newly created
        if (size!=0)                                        // the file already has a size, so it exists, so it should have a valid header
        {
            inoutChannel.read(this.header);                 // read the header from the file.
            for (int i=0;i<18;i++)
            {
                if (this.header.get(i)!=Types.QEMU[i])      // check whether this is a valid Qemu file
                {
                    if (this.header.getInt(0)!=Types.SFADriver)// this is no Qemu file, is it an ex SFA file?!
                        throw new java.io.IOException();    // no, not even that, leave with error.    
                    // if we get here, this is an old SFA header
                    for (int k=4;k<Types.SFAHeaderLength;k+=4)
                    {
                        this.header.putInt(i-4,this.header.getInt(i)); //copy header down 4 bites, which makes it a normal SMSQ/E header
                        this.headerLength=Types.SFAHeaderLength; // offset from native beginning of file to SMSQ/E "beginning" of file
                    }
                    return;                                 // we're done
                }   
            }
            // if we get here, the file is an existing QEMU file.
            this.headerLength=this.header.get(19)*2;        // length in bytes of header in file
            inoutChannel.position(this.headerLength);       // position channel there now (= beginning of Qdos file)
            if (this.headerLength>30)                       // get extra info if present
            { 
                addInfo=java.nio.ByteBuffer.allocate(14);
                for (int i=30; i<44;i++)
                {
                    addInfo.put(i-30,this.header.get(i));
                }
            }
            for (int i=4;i<14;i++)
            {
                this.header.put(i,header.get(i+16));        // get QDOS header info from QEMU header        
            }
            this.header.putInt(+0,Types.SFADriver);         // set SFA marker
            int l=(int) f.length();                             // length will be truncated if need be
            this.header.putInt(0,l-this.headerLength);      // set SMSQ/E length of file in header - THIS IS THE LENGTH WITHOUT THE SMSQE header and WITHOUT the SFA/QEMU header = true length of file
            l=(int)(f.lastModified()/1000)+Monitor.TIME_OFFSET; // file date ** magic offset;                    
            this.header.putInt(0x34,l);                     // update date                
            this.header.putInt(0x3C,l);                     // backup date (unused)               
            this.header.putInt(0x38,0);                     // make sure this is reset
        }
        
        else                                                // the file doesn't yet have a header, create it : create a fileheader!
        {                
            if (f.isDirectory())
            {
                 throw new java.io.IOException(); 
            }
            else
            {
                this.header.putInt(0,0);                    // length of file
                int l= (int)((System.currentTimeMillis()/1000)+Monitor.TIME_OFFSET); // ** magic offset  file date = now                   
                this.header.putInt(0x34,l);                 // update date                       
                this.header.putShort(0x38,(short)0);                       
                this.header.putShort(0x3a,(short)0);        // should be fileID
                this.header.putInt(0x3c,l);                 // backup date = creation date
                this.headerLength=30;                       // we ALWAYS create a QEMU header.
                inoutChannel.position (this.headerLength);  
            }
        }
        this.header.limit(this.header.capacity());
        this.header.position(0);
        setSMSQEFilename(filename);                         // set name in heade
    }
    
    /**
     * "Reads" the file header into the SMSQE buffer.
     * 
     * @param cpu the cpu with the memory.
     * @param position where in that memory the header should be written.
     * @param bufflen how many bytes to write. 
     * 
     * @return nbr of bytes read
     * 
     * @see smsqmulator.XfaFileheader#readFileheader(smsqmulator.cpu.MC68000Cpu, int, int) 
     */
    @Override
    public int readFileheader(smsqmulator.cpu.MC68000Cpu cpu, int position,int bufflen)
    {
        if (bufflen>Types.SMSQEHeaderLength)
            bufflen=Types.SMSQEHeaderLength;                             
        for (int i=0;i<bufflen;i++)
        {
            cpu.writeMemoryByte(position+i, this.header.get(i));
        }
        return bufflen;
    }
    
    
    /**
     * Writes the first 14 bytes of the fileheader from SMSQE to the SFA header.
     * 
     * @param cpu the cpu with the memory from where to read the header.
     * @param position the mem position in smsqe's memory map.
     * 
     * @return always 14 = nbr of bytes written.
     * 
     * @see smsqmulator.XfaFileheader#writeFileHeader(smsqmulator.cpu.MC68000Cpu, int) 
     */
    @Override
    public int writeFileHeader(smsqmulator.cpu.MC68000Cpu cpu, int position)
    {
        for (int i=0;i<14;i++)
        {
            this.header.put(i, (byte)(cpu.readMemoryByte(position+i)&0xff));
        }
        return 14;
    }
    
    /**
     * Actually writes the header to the file.
     * 
     * !!!!
     * This is always done, contrary to what Qemulator does.
     * !!!!
     * 
     * @param inoutChannel The channel to write to.
     */
    @Override
    public boolean flushHeader(java.nio.channels.FileChannel inoutChannel,boolean setDate)
    {
        if (inoutChannel==null)
            return false;
        try
        {   
            long fpos=inoutChannel.position();              // current position in file
         
            inoutChannel.position(0);
            Types.QEMUBUF.position(0);
            inoutChannel.write(Types.QEMUBUF);              // write marker (18 bytes)
            byte[]p=new byte[2];
            p[1]=(byte)(this.headerLength/2);
            java.nio.ByteBuffer p2 = java.nio.ByteBuffer.wrap(p);
            inoutChannel.write(p2);                         // write header length (2 bytes)
            this.header.position(4);
            this.header.limit(14);
            inoutChannel.write(this.header);                // write 10 header bytes now
            if (this.headerLength>30)
            {
                inoutChannel.write(this.addInfo);           // write 14 special header bytes now
            }
            this.header.position(0);
            this.header.limit(this.header.capacity());
            inoutChannel.position(fpos);
        }
        catch (Exception e)
        {
            /* nop*/                                        // file was a dir, or a read only file, can't flush header
        }
        return this.dateChanged;
    }
    
    
    /**
     * Gets the header offset -offset from beginning of native file to beginning of QL file
     * @see XfaFileheader#getOffset() 
     * @return the offset as int.
     */
    @Override
    public int getOffset()
    {
        return this.headerLength;
    }

    /**
     * Gets the (SMSQE) filename from the header.
     * @return the filename as a Java <code>String</code>.
     */
    @Override
    public String getSMSQEFilename()
    {
        int l=  this.header.getShort(0x0e);        
        StringBuilder nm=new StringBuilder (l);                 
        for (int i=0;i<l;i++)
            nm.append((char)this.header.get(0x10+i));   // set filename
        return nm.toString();
    }
    
    /**
     * Sets the (SMSQE) filename in the header.
     * @param filename the name to set.
     */
    @Override
    public void setSMSQEFilename(String filename)
    {  
        int l=filename.length()&0x000000ff;
        if (l>34)
            l=34;                                        // max file name length
        this.header.putShort(0x0e,(short) l);                         
        for (int i=0;i<l;i++)
            this.header.put(0x10+i,(byte)filename.charAt(i));   // set filename
    }
    
    /**
     * Gets the date of the file from the header.
     * 
     * @param whatDate what date do we want : 0 update date 2 : backup date
     * 
     * @return the date of the file in smsqe format (date and time in seconds as of 1.1.1961.
     */
    @Override
    public int getDate(int whatDate)
    {
        return this.header.getInt(0x34+4*whatDate);
    }
    
    /**
     * Sets the date in the header.
     * 
     * @param whatDate what date do we want : 0 update date 2 : backup date
     * @param dateToSet the date to set as an SMSQE date (date and time in seconds as of 1.1.1961)..
     *  
     * Contrary to the normal documentation, this will NOT set the backup date for a read only file!!!!!!!!!!
     */
    @Override
    public void setDate(int whatDate,int dateToSet)
    {
        this.header.putInt(0x34+4*whatDate,dateToSet);
        if (whatDate==0)
        {
            this.dateChanged=true;
        }
  
    }
    
     /**
     * Gets the version of the file from the header.
     * 
     * @return the file version
     */
    @Override
    public int getVersion()
    {
        return this.header.getShort(WinDir.HDR_VERS)&0xffff;
    }
    
    /**
     * Sets the version in the header.
     * 
     * @param version the version (a word) to set.
     */
    @Override
    public void setVersion(int version)
    {
        this.header.putShort(WinDir.HDR_VERS,(short)(version&0xffff));
    }
    
    
    @Override
    public void setFileDates(java.io.File f)
    {
        long l=(long)(this.header.getInt(0x34)-Monitor.TIME_OFFSET);
        if (l<0)
            l=0;
        else
            l*=1000;
        f.setLastModified(l);
        
    }
}
