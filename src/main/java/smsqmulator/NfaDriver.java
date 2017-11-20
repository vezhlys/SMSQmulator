package smsqmulator;

/**
 *
 * The Native File Access device driver class that allows SMSQE to access native files.
 * It implements 8 "drives" (NFA1_ to NFA8_) which point to native dirs.
 * @see DeviceDriver DeviceDriver for more information on device drivers.
 * @see XfaDriver of which this is an extension.
 * 
 * <p>
 * The driver opens NFAFiles,one for each SMSQE file. Each Nfafile gets a unique number (integer 0 - 0xffff) which gets put into the
 * SMSQE channel definition block (offset 0x1E).
 * For each drive, the driver maintains a hashmap ‹Integer,NfaFile› so that it can find the Nfafile with the integer.
 * 
 * @author and copyright (c) 2012 Wolfgang Lenerz
 * @version v. 0.00 initial version
 */ 
public class NfaDriver extends XfaDriver
{
    /**
     * Creates this object.
     * 
     * @param cpu the smsqmulator.cpu.MC68000Cpu used.
     */
    public NfaDriver(smsqmulator.cpu.MC68000Cpu cpu)
    {                      
        super(cpu);
        this.deviceID=Types.NFADriver;                           //'NFA0';
        this.usage=this.deviceID;
    }
}
