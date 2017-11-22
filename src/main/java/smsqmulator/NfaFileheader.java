package smsqmulator;

/**
 * Simulates an SMSQE fileheader for files on an NFA device.
 * @author and copyright (c) 2012-2017 Wolfgang Lenerz.
 * @version  
 * 1.08 setAttrs implemented, try to set some native file attributes in header.
 * 1.07 setFileDates : setting a file date to a date before 01.01.1970 will set date to 01.01.1970.
 * 1.06 setFileDates implemented, sets the update date, never sets the backup date.
 * 1.05 offset no longer a variable.
 * 1.04 use StringBuilder whenever appropriate
 * 1.03 correct length of filename used in getSMSQEFilename.
 * 1.02 File header read never returns more than 64 bytes.
 * 1.01 set  bogus file length if directory.
 * 1.00 use time adjustment from Monitor (Monitor.TIME_OFFSET).
 * 
 */
public class NfaFileheader implements XfaFileheader
{
    private final byte[] header=new byte[Types.SMSQEHeaderLength];
    private int updateDate=0;
    private boolean dateChanged=false;
    
    /**
     * Creates the object for an existing java.io.File.
     * 
     * @param f the existing file
     * @param filename the smsqe filename for this file.
     * @param lengthIfDir length of the file if it is a directory.

     */
    public NfaFileheader (java.io.File f,String filename,int lengthIfDir)
    {
        int l=filename.length()&0x000000ff;
        if (l>34)
            l=34;
        this.header[0xf]=(byte)(l);
        for (int i=0;i<l;i++)
        {
            this.header[0x10+i]=(byte)filename.charAt(i);
        }
        if (f.isDirectory())
        {
            putIntW(4,0x00ff);
            putIntL(0,lengthIfDir);
        }
        else
        {
            l=(int) f.length();                             // length will be truncated if need be
            putIntL(0,l);                                   // set length of file in header
            l=(int)(f.lastModified()/1000)+Monitor.TIME_OFFSET;   // file date ** magic offset;                    
            putIntL(0x34,l);      
        }  
        //now try to set some attributes
        this.header[4]=(byte)setAttrs(f);
    }
    
    /**
     * Set a long word in the header.
     * 
     * @param position where to set it.
     * @param l the long word (= java int) to set.
     */
    private void putIntL(int position,int l)
    { 
        this.header[position]=(byte) (l>>>24);                     
        this.header[position+1]=(byte) (l>>>16);
        this.header[position+2]=(byte) (l>>>8);
        this.header[position+3]=(byte) (l);    
    } 
    
    /**
     * Set a word in the header.
     * 
     * @param position where to set it.
     * @param l the word to set.
     */
    private void putIntW(int position,int l)
    { 
        this.header[position]=(byte) (l>>>8);
        this.header[position+1]=(byte) (l);
    }
    
    /**
     * Gets a long word from the header.
     * 
     * @param position where in the header to get the long word from.
     * 
     * @return the long word.
     */
    private int getLong(int position)
    {
        int n=0;
        for (int i=0;i<3;i++)
        {
            n |= (this.header[position+i] &0xff);
            n<<=8;
        }
        return n | (this.header[position+3]&0xff);
    }
    
    /**
     * "Reads" the file header into the SMSQE buffer.
     * @param cpu the cpu with the memory.
     * @param position where in that buffer to put it.
     * @param bufflen (length of that buffer)= how many bytes to get (not more!).
     * @return the number of bytes read.
     */
    @Override
    public int readFileheader(smsqmulator.cpu.MC68000Cpu cpu, int position,int bufflen)
    {
        if (bufflen>Types.SMSQEHeaderLength)
            bufflen=Types.SMSQEHeaderLength;
        for (int i=0;i<bufflen;i++)
        {
            cpu.writeMemoryByte(position+i, this.header[i]);
        }
        return bufflen;
    }
    
    /**
     * Writes the fileheader from SMSQE to the NFA - except that it doesn't...
     * @param cpu the cpu with the memory.
     * @param position at what position to write to.
     */
    @Override
    public int writeFileHeader(smsqmulator.cpu.MC68000Cpu cpu, int position)
    {
        return 14;
    }
     /**
     * Gets the header offset.
     * 
     * @see XfaFileheader#getOffset() 
     * @return the offset as int, always 0.
     */
    @Override
    public int getOffset()
    {
        return 0;
    }
    
    /**
     * Normally writes the header to disk, but here we don't flush the header at all!
     * @param inoutChannel  ignored.
     * #param setDate ignored.
     * 
     * @return true if the file dates should be set after closing the file
     */
    @Override
    public boolean flushHeader(java.nio.channels.FileChannel inoutChannel,boolean setDate)
    {
        return this.dateChanged;
    }
    
    /**
     * Gets the (SMSQE) filename from the header.
     * @return the (SMSQE) filename as a Java <code>String</code>.
     */
    @Override
    public String getSMSQEFilename()
    {
        int l=  (int) this.header[0x0f];   
        StringBuilder nm=new StringBuilder (l);                      
        for (int i=0;i<l;i++)
            nm.append((char)this.header[0x10+i]);   // get filename
        return nm.toString();
    }
    
    /**
     * Sets the (SMSQE) filename in the header - but does nothing here.
     * 
     * @param name ignored.
     */
    @Override
    public void setSMSQEFilename(String name)
    {}
    
     /**
     * Gets the date of the file from the header.
     * 
     * @param whatDate what date do we want : 0 update date 2 : backup date this is ignored here, both are always the same
     * 
     * @return the date of the file in smsqe format (date and time in seconds as of 1.1.1961, or always 0 if backup date.
     */
    @Override
    public int getDate(int whatDate)
    {
        return whatDate==0?getLong(0x34):0;
    }
    
    /**
     * Sets the date in the header.
     * This only work for the update date.
     * 
     * @param whatDate what date do we want to set : 0 = update date  2 = backup date
     * @param dateToSet the date to set as an SMSQE date (date and time in seconds as of 1.1.1961)..
     */
    @Override
    public void setDate(int whatDate,int dateToSet)
    {
        if (whatDate==0)
        {
            this.updateDate=dateToSet;
            this.dateChanged=true;
        }
    }
    
    
   /**
     * Gets the version of the file from the header.
     * 
     * @return ALWAYS 0
     */
    @Override
    public int getVersion()
    {
        return 0;
    }
    
    
    /**
     * Sets the version in the header.
     * 
     * @param version the version to set
     */
    @Override
    public void setVersion(int version)
    {}
    
    /**
     * Sets the length of the file in the header.
     * 
     * @param length the length to set
     */
    public void setLength(int length)
    {
        putIntL(0,length);
    }
    
    @Override
    public void setFileDates(java.io.File f)
    {
        long l=(long)(this.updateDate-Monitor.TIME_OFFSET); if (l<0)
            l=0;
        else
            l*=1000;
        f.setLastModified(l);
    }
    
    /**
     * Try to set some attributes
     * @param attrs
     * @param f
     * @return 
     * 
     * 
     * ADVSHR
     * A archive
     * D directory
     * V
     * S system
     * H hidden
     * R read only
     */
    public static final int setAttrs (java.io.File f)
    { 
        int attrs=0;
        try
        {
            java.nio.file.attribute.DosFileAttributes attr =java.nio.file.Files.readAttributes(f.toPath(), java.nio.file.attribute.DosFileAttributes.class);
            if (attr.isArchive())
                attrs+=32;
            if (attr.isSystem())
                attrs+=4;
            if (attr.isReadOnly())
                attrs+=1;
            if (attr.isHidden())
                attrs+=2;
            if (attr.isDirectory())
                attrs+=16;
        } 
        catch (UnsupportedOperationException  | java.io.IOException x) 
        {
            // NOP
        }
        return attrs;
    }
    
}

