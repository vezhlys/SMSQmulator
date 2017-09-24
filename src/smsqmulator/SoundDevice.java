package smsqmulator;

/**
 * A SOUND device to play sampled sound according to SMSQ/E's SSS specification.
 * 
 * SMSQmulator's implementation of the SSSS only allows 20Kz stereo. This device allows some other sampling rates.<p>
 * The sound isn"t quite played according to those specs : it's played at 22050 Hz.<p>
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
 * 
 * <p> The advantage of this device over the SmpledSound is that here I know when the sound will stop: this corresponds to the closing of
 * the SMSQ/E channel to the device. I know that after that, no more data will be coming and i can drain the sourcedataline. This gets around
 * the Java bug (see the SampledSopund" class.
 * <p>
 * The sound itself is played via an independent thread.<p>
 * This device only allow ONE channel to be open to it at a time.
 * <p>
 * The actual playback is handled by an independent thread.
 * <P>
 * This implements a simplistic buffering scheme.
 * AT each call from SMSQE (send multiple bytes), these bytes are put into an array of type byte. This array is added to an ArrayList. The
 * playback thread gets a element from this ArrayList and writes it into a sourcedataline.
 * @author and copyright (c)Wolfgang Lenerz 2014
 * @version
 *  1.01 implement iob.sbyt, posre, flush and added resampling.
 *  1.00 First build
 * 
 */
public class SoundDevice
{ 
    private javax.sound.sampled.SourceDataLine sourceDataLine; 
    private PlayThread playThread;
    private javax.sound.sampled.FloatControl volume;            // volume of sound played
    private java.util.ArrayList<byte[]> buffer;                 // buffers for all of the sound to be played.
    private int counter=0;                                      
    private boolean channelClosed=true;                         // is true if "channel" to this device is closed.
    private int deviceType;                                     // SOND1 to SOUND9
    private int av,av1;                                         // used for averages accross calls
    private double rate;
    private double adjustFreq;
   
    /**
     * Creates this object, a DataLine object and an independent thread for filling the DataLine.
     * 
     * @param volume at what volume sound is played (0...1000)
     * @param warn warning object
     * @param cpu the cpu used.
     */
    public SoundDevice(int volume,Warnings warn,smsqmulator.cpu.MC68000Cpu cpu)
    {
        javax.sound.sampled.AudioFormat audioFormat = new javax.sound.sampled.AudioFormat (22050.0F, 8,2,false,false); //8000,11025,16000,22050,44100 -- 8,16 -- 1,2 -- boolean --boolean
       // javax.sound.sampled.AudioFormat audioFormat = new javax.sound.sampled.AudioFormat (44100.0F, 8,2,false,false); //8000,11025,16000,22050,44100 -- 8,16 -- 1,2 -- boolean --boolean
        javax.sound.sampled.DataLine.Info dataLineInfo = new javax.sound.sampled.DataLine.Info (javax.sound.sampled.SourceDataLine.class, audioFormat);
        cpu.data_regs[0]=0;
        try
        {   
            this.sourceDataLine = (javax.sound.sampled.SourceDataLine) javax.sound.sampled.AudioSystem.getLine (dataLineInfo);
            this.sourceDataLine.open(audioFormat);
        } 
        catch (Exception e) 
        {
            this.channelClosed=false;                       // this will prevent any activity in this device. 
            return;
        }
        try
        {
            this.volume= (javax.sound.sampled.FloatControl) this.sourceDataLine.getControl(javax.sound.sampled.FloatControl.Type.VOLUME);
            setVolume(volume);
        }
        catch (Exception e) 
        {
            try
            {
                this.volume= (javax.sound.sampled.FloatControl) this.sourceDataLine.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
                setVolume(volume);
            }
            catch (Exception v)
            {
                if (warn.warnIfSoundProblem)
                {
                    Helper.reportError(Localization.Texts[45], Localization.Texts[72]+":\n",null,e);
                    cpu.data_regs[0]=-1;
                }
            }
        }   
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
            this.playThread = new PlayThread();
            this.playThread.start();
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
                if (this.playThread!=null)
                    this.playThread.stopNow();
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
      //  this.buffer.add(resample(buff));
        addToQueue(buff);
        cpu.addr_regs[1]=A1;
        cpu.data_regs[1]=nbrOfBytes;
        cpu.data_regs[0]=0;
    }
    
    private synchronized void addToQueue(byte[] buff)
    {
        this.buffer.add(buff);
    }
    private byte[] resample(byte[]buff)
    {
        int l=buff.length;
        java.util.ArrayList<Byte>ar=new java.util.ArrayList<>(l+l/9);
        for (int i=0;i<l-1;i+=2)
        {
            ar.add(buff[i]);
            ar.add(buff[i+1]);
            if (this.adjustFreq<1.0)
                this.adjustFreq +=this.rate;
            else
            {
                ar.add(buff[i]);
                ar.add(buff[i+1]);
                this.adjustFreq-=1.0;
            }
        }
        l=ar.size();
        byte  []f=new byte[l];
        for (int i=0;i<l;i++)
        {
            f[i]=ar.get(i);
        }
        return f;
    }
    
    private synchronized byte[] getFromQueue()
    {
        if (this.counter!=0)
            this.buffer.set(counter-1, null);                   // mem can be garbage collected
        if (this.counter!=this.buffer.size())
            return this.buffer.get(counter++);
        return null;
    }
    
    /**
     * Sets the sound volume.
     * 
     * @param percentage the volume, from 0 (no sound) to 100 (loudest).
     */
    public void setVolume(int percentage)
    {
        if (this.volume==null)
            return;
        float maxv=this.volume.getMaximum();
        float minv=this.volume.getMinimum();
        float diff=maxv-minv;
        if (percentage<0)
            percentage=0;
        if (percentage>100)
            percentage=100;                                 // percentage must be in this limit
        if (percentage==0)
        {
            diff=minv;          
        }
        else
        {
            diff=diff*((float)percentage/100.0f) + minv;
        }
        try
        {
            this.volume.setValue(diff);
        }
        catch (Exception e)
        {
            //NOP//
        }
    }
  
    
    /**
     * Queries the current volume or -1 if line no longer active.
     * NB contrary to documentation, this doesn't work.
     * 
     * @return the volume.
     */
    public int queryVolume()
    {
        if (this.sourceDataLine!=null && this.sourceDataLine.isRunning())
        {
            float vol=this.sourceDataLine.getLevel();
            if (vol<-1)
                return 1;
            else
                return (int) (vol*25.0);
        }
        else
            return -1;
    }
    
    
    /**
     * The independent thread that fills the DataLine.
     * 
     * This is a continuous loop which stops when:
     *   - no data is returned from the getFromQueue call 
     * AND
     *      - the channelClosed falg is set to true.
     */
    class PlayThread extends Thread
    {
        private volatile boolean stopNow=false;
        
        @Override
        @SuppressWarnings("SleepWhileInLoop")
        public void run()
        {
            byte[] buffer;
            sourceDataLine.start();                             //
            
            while(!this.stopNow)                                         // playthread is one continuous loop
            {
                try
                {                        
                    buffer=getFromQueue();                      // get some more data
                    if (buffer == null)                         // nothing gotten
                    {
                       if (channelClosed)                       // is it because this is the end?
                       {
                           sourceDataLine.drain();              // yes, wait till all sound is played
                           this.stopNow=true;                   // job is done
                       }
                       else                                     // noting got, but channel not yet closed
                           Thread.sleep(50);                    // so wait a little bit.
                    }
                    else
                    {
                        sourceDataLine.write(buffer,0,buffer.length);// got something, write it.
                    }
                }
                catch (InterruptedException e) 
                { /*nop*/ }
            }
            
            sourceDataLine.stop();                         // stop soundline
            sourceDataLine.flush();                        // flush it 
        }
        public void stopNow()                                   // signal that sound should be killed now
        {
            this.stopNow=true;
        }
    }
}
