
package smsqmulator;

/**
 * The interface with required routines a fileheader object must have.
 * 
 * @author and copyright (C) wolfgang Lenerz 2012-2017
 */
public interface XfaFileheader 
{     
    /**
     * Reads the file header into the CPU's memory.
     * 
     * @param cpu the cpu with the memory.
     * @param position where in that memory to put it.
     * @param bufflen length of that buffer.
     * 
     * @return number of bytes read.
     */
    public int readFileheader(smsqmulator.cpu.MC68000Cpu cpu,int position,int bufflen);
    
    /**
     * Writes the first 14 bytes of the fileheader from SMSQE to the file.
     * 
     * @param cpu the cpu with the memory.
     * @param position start address of file header in that buffer.
     * 
     * @return number of bytes written.
     */
    public int writeFileHeader(smsqmulator.cpu.MC68000Cpu cpu, int position);
    
    /**
     * This gets the offset from file position 0, as SMSQE thinks it is and the real fileposition 0 in the native filesystem.
     * <p>For example, the SFA driver uses <code>Types.QFAHeaderLength</code> (=68 at the time of writing) bytes at the beginning of the file for a header.
     * 
     * @return the offset.
     */
    public int getOffset();
            
    
    /**
     * Writes the header to the native file.
     * 
     * @param inoutChannel the file channel to write to.
     * @param setDate = true if the date should be set to that of the channel's closing.
     * 
     * @return true if the file dates should be set after closing the file
     */
    public boolean flushHeader(java.nio.channels.FileChannel inoutChannel,boolean setDate);
    
    
    /**
     * Gets the (SMSQE) filename from the header.
     * 
     * @return the filename.
     */
    public String getSMSQEFilename();
    
    
    /**
     * Sets the (SMSQE) filename in the header.
     * 
     * @param name the new name to set in the header.
     */
    public void setSMSQEFilename(String name);
    
    
    /**
     * Gets the date of the file from the header.
     * 
     * @param whatDate what date do we want : 0 update date 2 : backup date
     * 
     * @return the date of the file in smsqe format (date and time in seconds as of 1.1.1961).
     */
    public int getDate(int whatDate);
    
    /**
     * Sets the date in the header.
     * 
     * @param whatDate what date do we want : 0 update date 2 : backup date
     * @param dateToSet the date to set as an SMSQE date (date and time in seconds as of 1.1.1961).
     */
    public void setDate(int whatDate,int dateToSet);
      

  
     /**
     * Gets the version of the file from the header.
     * 
     * @return the file version
     */
    public int getVersion();
    
    /**
     * Sets the version in the header.
     * 
     * @param version the dversio to set 
     */
    public void setVersion(int version);
    
    /**
     * Sets dates of files, if necessary.
     * @param f the file in question
     */
    public void setFileDates(java.io.File f);
}
