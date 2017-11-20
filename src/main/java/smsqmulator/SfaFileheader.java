
package smsqmulator;

/**
 * Creates an SMSQE fileheader for SFA files.
 * 
 * @author and copyright (c) 2012 - 2017 Wolfgang Lenerz.
 * @version
 * 1.04 setFileDates implemented (does nothing).
 * 1.03 use StringBuilder whenever appropriate
 * 01.02 File header read never returns more than 64 bytes.
 * 01.01 set  bogus file length if directory.
 * @version 01.00 use time adjustment from Monitor.
 * @version 00.02 when creating fileheader, set the name to the name this file is actually used under.
 * @version 00.01 read filer header, never read more than 64 bytes.
 * @version 00.00 initial version.
 */
public class SfaFileheader implements XfaFileheader
{
    private final java.nio.ByteBuffer header=java.nio.ByteBuffer.allocate(Types.SFAHeaderLength);
    private final static int hOffset=4;                     // use this to access all data in the header
    
    /**
     * Creates the object either from an existing <code>java.io.File</code>, or a newly created one.
     * 
     * @param f the file.
     * @param filename the filename.
     * @param inoutChannel a channel for reading the existing header.
     * @param lengthIfDir length if file if it is a directory.
     * 
     * @throws java.io.IOException from  java io operations. 
     */
    public SfaFileheader (java.io.File f,String filename,java.nio.channels.FileChannel inoutChannel,int lengthIfDir) throws java.io.IOException
    {
        long size=0;
        if (inoutChannel!=null)
            size=inoutChannel.size();                       // get the size of that file - this will be 0 if file newly created
        if (size!=0)                                        // the file already has a size, so it exists, so it should have a valid header
        {
            if (size<Types.SFAHeaderLength)
                throw new java.io.IOException();            // ????
            inoutChannel.read(this.header);                 // read the header.
            if (this.header.getInt(0)!=Types.SFADriver)     // check whether this is a valid SFA file
                 throw new java.io.IOException();           // this is no file for an SFAdriver!
        }
        else                                                // the file doesn't yet have a header, create it 
        {                       
            this.header.putInt(0,Types.SFADriver);          // my flag
            if (f.isDirectory())
            {
                this.header.putInt(SfaFileheader.hOffset,lengthIfDir);// length of file (header doesn't count);
                this.header.putShort(SfaFileheader.hOffset+4,(short)0x00ff); // file type if dir
            }
            else
            {
                this.header.putInt(SfaFileheader.hOffset,0);// length of file
                int l= (int)((System.currentTimeMillis()/1000)+Monitor.TIME_OFFSET); // ** magic offset  file date = now                   
                this.header.putInt(SfaFileheader.hOffset+0x34,l);// update date                       
                this.header.putShort(SfaFileheader.hOffset+0x38,(short)0);                       
                this.header.putShort(SfaFileheader.hOffset+0x3a,(short)0); // should be fileID
                this.header.putInt(SfaFileheader.hOffset+0x3c,l);// backup date = creation date
            }
        }
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
            cpu.writeMemoryByte(position+i, this.header.get(SfaFileheader.hOffset+i));
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
            this.header.put(SfaFileheader.hOffset+i, (byte)(cpu.readMemoryByte(position+i)&0xff));
        }
        return 14;
    }
    
    /**
     * Actually writes the header to the file.
     * @param inoutChannel The channel to write to.
     */
    @Override
    public boolean flushHeader(java.nio.channels.FileChannel inoutChannel,boolean setDate)
    {
        if (inoutChannel==null)
             return false;
        try
        {   
            int l=(int) inoutChannel.size();
            if (l!=0)
                l-=Types.SFAHeaderLength;
            this.header.putInt(4,l);
            if (setDate)
            {
                l= (int)((System.currentTimeMillis()/1000)+Monitor.TIME_OFFSET); // ** magic offset  file date = now                   
                this.header.putInt(SfaFileheader.hOffset+0x34,l);// update date            
            }
            long pos =inoutChannel.position();
            inoutChannel.position(0);
            this.header.position(0);
            inoutChannel.write(this.header);
            this.header.position(0);
            inoutChannel.position(pos);
        }
        catch (Exception e)
        {
            /* nop*/                                        // file was a dir, or a read only file, can't flush header
        }
        return false;
    }
    
    /**
     * Gets the header offset.
     * @see XfaFileheader#getOffset() 
     * @return the offset as int.
     */
    @Override
    public int getOffset()
    {
        return Types.SFAHeaderLength;
    }

    /**
     * Gets the (SMSQE) filename from the header.
     * @return the filename as a Java <code>String</code>.
     */
    @Override
    public String getSMSQEFilename()
    {
        int l=  this.header.getShort(SfaFileheader.hOffset+0x0e);   
        StringBuilder nm=new StringBuilder (l);                      
        for (int i=0;i<l;i++)
            nm.append((char)this.header.get(SfaFileheader.hOffset+0x10+i));   // set filename
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
        this.header.putShort(SfaFileheader.hOffset+0x0e,(short) l);                         
        for (int i=0;i<l;i++)
            this.header.put(SfaFileheader.hOffset+0x10+i,(byte)filename.charAt(i));   // set filename
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
        return this.header.getInt(SfaFileheader.hOffset+0x34+4*whatDate);
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
        this.header.putInt(SfaFileheader.hOffset+0x34+4*whatDate,dateToSet);
    }
    
     /**
     * Gets the version of the file from the header.
     * 
     * @return the file version
     */
    @Override
    public int getVersion()
    {
        return this.header.getShort(SfaFileheader.hOffset+WinDir.HDR_VERS)&0xffff;
    }
    
    /**
     * Sets the version in the header.
     * 
     * @param version the date to set as an SMSQE date (date and time in seconds as of 1.1.1961)..
     *  
     * Contrary to the normal documentation, this will NOT set the backup date for a read only file!!!!!!!!!!
     */
    @Override
    public void setVersion(int version)
    {
        this.header.putShort(SfaFileheader.hOffset+WinDir.HDR_VERS,(short)(version&0xffff));
    }
    
     @Override
    public void setFileDates(java.io.File f)
    {}
}
