package smsqmulator;

/**
 *
 * The SMSQE File Access device driver class that allows SMSQE access to SMSQE type files (with header) on a native drive.
 * It implements 8 "drives" (SFA1_ to SFA8_) which point to native dirs.
 * <p>The driver maitains header files for SMSQE files, which are saved with the files.
 * <p>
 * The driver opens XFAFiles,one for each SMSQE file. Each XfaFile gets a unique number (integer 0 - 0xffff) which gets put into the
 * SMSQE channel definition block (offset 0x1E).
 * For each drive, the driver maintains a HashMap ‹Integer,NfaFile› so that it can find the NfaFile with the integer.
 * <p>
 * @see DeviceDriver DeviceDriver for more information on device drivers.
 * @see XfaDriver of which this is an extension.
 * 
 * @author and copyright (c) 2012 Wolfgang Lenerz
 * @version v. 0.01 implemented  date setting and getting.
 * @version v. 0.00 initial version
 */ 
public class SfaDriver extends XfaDriver
{
    /**
     * Creates the device driver.
     * 
     * @param cpu the cpu used.
     */
    public SfaDriver(smsqmulator.cpu.MC68000Cpu cpu)
    {                      
        super(cpu);
        this.deviceID=Types.SFADriver;                       //'SFA0';
        this.usage=this.deviceID;
    }
    
    /**
     * Tries to get a header for an SFA file and possibly copy it somewhere to memory.
     * 
     * @param f the file in question.
     * @param cpu the smsqmulator.cpu.MC68000Cpu used.
     * @param tempHeader a ByteBuffer to read the header to from the file. If this is <code>null</code> or of insufficent size, one will be created on the fly.
     * 
     * @return an <code>int</code> which can have the following values:
     * <ul>
     *      <li> 0 = this file is not an SFA file</li>
     *      <li> 1 = this file is an SFA file</li>
     *      <li>-1 = this file is a directory</li>
     * </ul>
     */
    public static int readHeaderOK(java.io.File f, smsqmulator.cpu.MC68000Cpu cpu,java.nio.ByteBuffer tempHeader)
    {
        if (f.isDirectory())
            return -1;                                      // file is a directory
        java.nio.channels.FileChannel inoutChannel=null;
        try 
        {
            inoutChannel = new java.io.RandomAccessFile(f, "r").getChannel();
            if (tempHeader==null || tempHeader.capacity()<Types.SFAHeaderLength)
                tempHeader=java.nio.ByteBuffer.allocate(Types.SFAHeaderLength);
            else
            {
                tempHeader.position(0);
                tempHeader.limit(Types.SFAHeaderLength);
            }
            inoutChannel.read(tempHeader);                  // get the header into a temporary space
            inoutChannel.close();                           // close the channel
            if (tempHeader.getInt(0)==Types.SFADriver)      // is this an SFA file?...
            {                                               // ... yes
                return 1;                                   // everything is ok
            }
            else if (tempHeader.getInt(0)==Types.QEMUHeader) 
            {
                for (int i=4;i<18;i++)
                {
                    if (tempHeader.get(i)!=Types.QEMU[i])      // check whether this is a valid Qemu file
                    {
                        return 0;        // this is no Qemu file!
                    }
                }
                return 1;
            }
            else
                return 0;                                   // this is not an SFA file
        }
        catch (java.io.FileNotFoundException e)
        { 
            if (inoutChannel!=null)
            {
                try
                {
                    inoutChannel.close();                   
                }
                catch (Exception whatever)
                { /*nop*/ }                                 // OK  this is better handled in Java 7.
            }
            cpu.data_regs[0]=Types.ERR_FDNF;                // file wasn't found
            return 0;                                       // couldn't even read this file, so it's not an SFA file
        }      
        catch (java.io.IOException e)
        { 
            if (inoutChannel!=null)
            {
                try
                {
                    inoutChannel.close();                   // this also closes Channel
                }
                catch (Exception whatever)
                { /*nop*/ }                                 // OK  this is better handled in Java 7.
            }
            cpu.data_regs[0]=Types.ERR_FDIU;               // file was in use
            return 0;                                       // no I/O to this file -> problem
        }                                      
    }
    
}
