
package smsqmulator;

/**
 * This is the special access file ("*d2d) on a qxl.win drive.
 * 
 * All file positions are actually multiple of 512 bytes (setting pos#1 will set the file position to 512 etc).
 * 
 *  @author and copyright (c) wolfgang Lenerz 2013.
 *  @version 
 *  1.01 correct handling of sector size return if read and buffer size = 2.
 */
public class WinSpecialFile extends WinFile
{
    /**
     * Creates the object.
     * 
     * @param drive on what drive the special file is to be opened.
     */
    public WinSpecialFile(WinDrive drive)
    {
        this.filePosition=0;                                    // preset position to 0
        this.buffer=java.nio.ByteBuffer.allocate(512);
        this.drive=drive;
    }
    
    /**
     * Closes the special file : the drive is notified of this.
     */
    @Override
    public void close()
    {
        this.drive.specialFileClosed();
    }
    
    /**
     * This dispatches the file I/O routines (=SMSQE TRAP#3 routines).
     * This special file only allows few operations.
     * 
     * @param trapKey what kind of trap#3 are we dealing with?
     * @param cpu the CPU.
     */
    @Override
    public void handleTrap3(int trapKey,smsqmulator.cpu.MC68000Cpu cpu)
    {
        int temp=cpu.data_regs[2]&0xffff;
        int A1=cpu.addr_regs[1];
        switch (trapKey)
        {
            case 0x03:                                      // iob.fmul get a number of bytes from the file
                if (temp<512)
                {
                    if (temp==2)                            // this must return the sector size in (a1)
                    {
                        cpu.data_regs[1]=2;
                        cpu.writeMemoryShort(A1,(short)512);
                        cpu.addr_regs[1]+=2;
                        cpu.data_regs[0]=0;
                    }
                    else   
                        cpu.data_regs[0]=Types.ERR_NIMP;    // we must ask for 2, 512 or 2+512 bytes
                    break;
                }
                long fpos=this.filePosition*512;
                if (this.drive.readBytes(512, this.buffer, fpos)!=512)// read bytes from disk to buffer
                    return;                                 /// ooops!            
                if (temp==514)
                {
                    cpu.writeMemoryShort(A1,(short)512);
                    A1+=2;
                }
                cpu.readFromBuffer(A1, 512, this.buffer,0);//Read bytes from a buffer and write them into memory.
                cpu.addr_regs[1]=A1+512;
                cpu.data_regs[1]=512;
                cpu.data_regs[0]=0;
                break;
                
            case 0x07:                                      // send multiple bytes from mem to disk : iob.smul
                if (temp!=512 & temp!=514)
                { 
                    if (temp==2)
                    {
                        cpu.data_regs[0]=0;                 // this call is ignored plain and simple
                    }
                    else   
                        cpu.data_regs[0]=Types.ERR_NIMP;    // we must ask for 2, 512 or 2+512 bytes
                    break;
                }
                fpos=this.filePosition*512;
                if (temp==514)
                    cpu.addr_regs[1]+=2;                    // ignore length word
                int tt=cpu.data_regs[2];                    // keep old length
                cpu.data_regs[2]=512;                       // we want 512 bytes
                cpu.writeToBuffer(this.buffer,0);           // read from memory to clusterbuffer
                if (this.drive.writeBytes(512, this.buffer, fpos,0)!=512)// write bytes in buffer to drive
                    return;                                 
                cpu.addr_regs[1]+=512;
                cpu.data_regs[2]=tt;
                cpu.data_regs[1]=temp;
                cpu.data_regs[0]=0;
                break;
                
            case 0x42:                                      // set file position absolute
                this.filePosition=0;
            case 0x43:                                      // set file position relative
                this.filePosition+=cpu.data_regs[1];
                this.filePosition=this.drive.checkSector(this.filePosition);
                cpu.data_regs[0]=0;
                cpu.data_regs[1]=this.filePosition;
                break;
                 
            case 0x40:                                      // check pending I/O
                cpu.data_regs[0]=0;                         // always succeeds but don't do anything *************
                break;  
          
            default:
                cpu.data_regs[0]=Types.ERR_NIMP;            // all other traps : file is read only
                break;
        }
    }   
}
