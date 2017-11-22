package smsqmulator;

/**
 * A SOUND device to play sampled sound according to SMSQ/E's SSS specification.
 * 
 * SMSQmulator's implementation of the SSSS only allows 20Kz stereo. This device allows some other sampling rates.<p>
 * There are 9 "devices", SOUND1 to SOUND9:
 * <ul>
 *  <li>1 - 20 khz mono (default)</li>
 *  <li>2 - 20 khz stereo</li>
 *  <li>3 - 10 khz mono, with averaging</li>
 *  <li>4 - 10 khz stereo, with averaging</li>
 *  <li>5 - 40 khz mono, with averaging</li>
 *  <li>6 - 40 khz stereo, with averaging</li>
 *  <li>7 - 40 khz mono, every second byte is skipped</li>
 *  <li>8 - 40 khz stereo, every second byte pair is skipped</li>
 *  <li>9 - 40 Khz mono send alternate bytes left/right (crazy stuff)</li>
 * </ul>
 *  "SOUND" defaults to SOUND1.
 * <p>
 * The sound itself is played via an independent thread.<p>
 * This device only allow ONE channel to be open to it at a time.
 * <p>
 * The actual playback is handled by an independent thread from the SampledSound device
 * <P>
 * @author and copyright (c)Wolfgang Lenerz 2014-2017
 * @version
 *  1.03 revamped, use SampledSound device.
 *  1.02 add resampling for 22.5 Khz dataline.
 *  1.01 implement iob.sbyt, posre, flush and added (deice) resampling.
 *  1.00 First build
 * 
 */
public class SoundDevice
{ 
    private java.util.ArrayList<byte[]> buffer;                 // buffers for all of the sound to be played.
    private int counter=0;                                      
    private boolean channelClosed=true;                         // is true if "channel" to this device is closed.
    private int deviceType;                                     // SUOND1 to SOUND9
    private int av,av1;                                         // used for averages accross calls
    private final SampledSound sam;
   
    /**
     * Creates this object, a DataLine object and an independent thread for filling the DataLine.
     * 
     * @param sam used for actual playback.
     */
    public SoundDevice(SampledSound sam)
    {
        this.sam=sam;
    }
    
    /**
     * A new channel was opened to this device, start thread, but only if no channel is still open to this device.
     * 
     * @param cpu the cpu used.
     */
    public void  openChannel(smsqmulator.cpu.MC68000Cpu cpu)
    {
        if (!this.channelClosed)
            cpu.data_regs[0]=Types.ERR_FDIU;                    // only one channel can be open to this at one time
        else
        {
            this.deviceType = cpu.data_regs[2]&0xff;            // 1...9
            this.buffer=new java.util.ArrayList<byte[]>();
            this.counter=0;                                     // where to get the next element from the arraylist
            this.av1=128;
            this.av=128;
            this.channelClosed=false;
            cpu.data_regs[0]=0;                                 // signal open went OK
        }
    }
    
    /**
     * Called when the sound channel is closed.
     */
    public void closeChannel()
    {
        this.channelClosed=true;
    }
   
    /***
     * Handle the IO calls. Only IOB.SBYT and IOB.SMUL are allowed. 
     * @param cpu the cpu used.
     */
    public void doIO(smsqmulator.cpu.MC68000Cpu cpu)
    {
        if(this.channelClosed)
        {
            cpu.data_regs[0]= Types.ERR_ICHN;
            return;
        }
                
        switch (cpu.data_regs[4])
        {
            case 5:                                             // iob.sbyt, send one byte
                byte[]b=new byte[1];
                b[0]=(byte)cpu.data_regs[1];
                this.buffer.add(b);
                break;
                
            case 7:                                             // iob.smul - multiple bytes sent
                addBytes(cpu);
                break;
                
            case 65:                                            // flush = stop playing
            //    if (this.playThread!=null)
              //      this.playThread.stopNow();
                this.sam.killSound(cpu);
                break;
                
            case 67:                                            // get position = get sample size
                if (cpu.data_regs[1]!=0) 
                {
                    cpu.data_regs[0]= Types.ERR_ICHN;
                    return;
                }
                cpu.data_regs[1]=102400;                        // pretend I can handle that many bytes
                break;
                
            default:
                cpu.data_regs[0]=Types.ERR_NIMP; 
        }
    }
    /**
     * Called to add bytes to the queue.
     * 
     * on first call, D3 = 0, D2 = nbr of bytes
     * on subsequent calls : D3 = -1; D1 = nbr of bytes
     * A1 points to the data, updated on return
     * D1 contains nbr of bytes on return
     * 
     * @param cpu the cpu pointing to the data.
     */
    public void addBytes(smsqmulator.cpu.MC68000Cpu cpu)
    {
        int nbrOfBytes;
        if (cpu.data_regs[3]==0)
            nbrOfBytes=cpu.data_regs[2];
        else
            nbrOfBytes=cpu.data_regs[1];
        
        int A1=cpu.addr_regs[1];                                // where the data lies
        byte[] buff;
        byte t,t1;
        int b,b1;
        switch (this.deviceType)
        {
            case 1:                                             // 20 khz mono, just double it for stereo
            default:
                buff=new byte[nbrOfBytes*2];                    // double for stereo
                for (int i=0;i<nbrOfBytes*2;i+=2)
                {
                    t=(byte)(cpu.readMemoryByte(A1++));         // get a sound byte
                    buff[i]=t;                                  // send to left channel
                    buff[i+1]=t;                                // and to right
                }
                break;
                
            case 2:                                             // 20 khz stereo, just copy it 
            case 9:                                             // 40 Khz mono send alternate bytes left/right (crazy stuff)
                buff=new byte[nbrOfBytes];
                for (int i=0;i<nbrOfBytes;i++)
                {
                    buff[i]=(byte)(cpu.readMemoryByte(A1++));
                }
                break;
                
            case 3:                                             // 10 khz mono, with averaging
                buff=new byte[nbrOfBytes*4];                    // double for mono, double again for 10 Khz
                for (int i=0;i<nbrOfBytes;i++)
                {
                    b= cpu.readMemoryByte(A1++);                // get a byte
                    av=(b+this.av)/2;                                // average it (keep average for enxt iteration)
                    t=(byte)(this.av);               
                    buff[i*4]=t;
                    buff[i*4+1]=t;
                    buff[i*4+2]=t;
                    buff[i*4+3]=t;
                }
                break;
                
             case 4:                                            // 10 khz stereo, with averaging
                buff=new byte[nbrOfBytes*2];                    // double for 10 Khz
                for (int i=0;i<nbrOfBytes*2;i+=4)
                {
                    b= cpu.readMemoryByte(A1++);                // get a byte, left
                    this.av=(b+this.av)/2;                               // average it
                    t=(byte)(this.av);  
                    b= cpu.readMemoryByte(A1++);                // get a byte, right channel
                    this.av1=(b+this.av1)/2;                              // average it
                    t1=(byte)(this.av1);
                    buff[i]=t;                                  // left
                    buff[i+1]=t1;                               // right
                    buff[i+2]=t;                                // and again, for doubleing sample rate
                    buff[i+3]=t1;
                }
                break;
                 
             case 5:                                            // 40 khz mono, with averaging
                buff=new byte[nbrOfBytes];                      // double for mono, but half for 40 Khz
                for (int i=0;i<nbrOfBytes;i+=2)
                {
                    b= cpu.readMemoryByte(A1++);                // get a byte
                    this.av=(b+cpu.readMemoryByte(A1++))>>>1;        // average it with next byte 
                    t=(byte)(this.av);  
                    buff[i]=t;                                  // left
                    buff[i+1]=t;                                // right
                }
                break;     
                 
             case 6:                                            // 40 khz stereo, with averaging
                int k=nbrOfBytes/2;
                if ((k&1)==1)
                     k++;
                buff=new byte[k];                               //  half for 40 Khz
                
                for (int i=0;i<k;i+=2)
                {
                    b= cpu.readMemoryByte(A1++);                // get a byte, left
                    b1=cpu.readMemoryByte(A1++);
                    this.av=(b+cpu.readMemoryByte(A1++))/2;     // average it with next byte
                    this.av1=(b1+cpu.readMemoryByte(A1++))/2;   // average it with next byte
                    buff[i]=(byte)(this.av);                    // left
                    buff[i+1]=(byte)(this.av1);                 // right
                }
                break;
                 
            case 7:                                             // 40 khz mono, every second byte is skipped
                buff=new byte[nbrOfBytes];                      // half for 40 Khz but double for mono
                for (int i=0;i<nbrOfBytes;i+=2)
                {
                    t=(byte) (cpu.readMemoryByte(A1));          // get a byte, mono
                    A1+=2;                                      // skip next byte    
                    buff[i]=t;                                  // left
                    buff[i+1]=t;                                // right
                }
                break;
                
            case 8:                                             // 40 khz stereo, every second byte pair is skipped
                k=nbrOfBytes/2;
                if ((k&1)==1)
                     k++;
                buff=new byte[k];                               //  half for 40 Khz
                for (int i=0;i<k;i+=2)
                {
                    buff[i]=(byte) (cpu.readMemoryByte(A1++));  // left
                    buff[i+1]=(byte) (cpu.readMemoryByte(A1));  // right
                    A1+=3;                                      // point to next byte
                }
                break;
        }
        sam.addChunk(buff);
        cpu.addr_regs[1]=A1;
        cpu.data_regs[1]=nbrOfBytes;
        cpu.data_regs[0]=0;
    }
}
