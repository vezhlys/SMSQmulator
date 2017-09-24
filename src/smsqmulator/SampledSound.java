package smsqmulator;

/**
 * An object to make some sampled sound according to SMSQ/E's SSSS specification.
 * 
 * This creates a buffer (a 100K byte array). SMSQ/E's SSSS queue is emptied into this here buffer and then fed to a SourceDataLine
 * for playback via an independent <code> PlayThread</code> thread. This thread is created once and, when it has nothing to do (no sound is to be played)
 * it will just go to sleep. A new thread is NOT created every time a sound is to be played.<p>
 * 
 * The only soundfile allowed are 20KHz ".UB" files.
 * 
 * The general contract with the SMSQ/E's ssss is that whenever a sampled is to be sent to the ssss, smsq gets here via the corresponding
 * trap (playsound). Then the independent job is started which copies the data from the SMSQE buffer to the buffer set up here.
 * <p>
 * There is a bug in the java SourceDataLine : if one repeatedly sends it small sound samples, the sound is repeated indefintely (in total or in part)
 * until the dataline is closed/stopped. To try to get around this, a special flag may be set that tells this, a new vector is introduced into the SSSS in SMSQ/E, that 
 * will cause the PlayThread to empty and STOP the SoundDataLine once the queue is empty.
 * <p>
 * @author and copyright (c)Wolfgang Lenerz 2012-2016
 * @version
 *  1.04    resampling if 22.05 Khz is chosen, thanks to Marcel Kilgus for the algorithm.
 *  1.03    minor modification in queueIsEmpty
 *  1.02    better handling of killsound, faster killsound by limiting the sample size in getFromQueue, frequency in object creation.
 *  1.01    Volume control tries volume then master gain. Popup warnign is made configurable.
 *  1.00    setVolume implemented
 *  0.01    implemented queue is empty, close sound, query sound : try to get around Java bug of repeating sounds.
 *  0.00    first build
 * 
 */
public class SampledSound 
{ 
    private byte[] audioData=new byte[10];
    private javax.sound.sampled.SourceDataLine sourceDataLine=null;
    private PlayThread playThread;
    private static final int MAX_BUFF_LEN=100*1024;
    private int base;                                           // base of SSSS memory structure    
    private final int qstart=0x10;                              // some indexes into the memory structure : start of queue
    private final int qin=qstart+4;                             // ptr to location for next byte to put into queue
    private final int qout=qin+4;                               // ptr to location to get next byte from
    private final int qend=qout+4;                              // ptr to end of queue
    private final int qbase=qend+4;                             // ptr to base of queue   
    private int start;                                          // start address of queue in my memory block adjusted for word access
    private int last;                                           // same for end address of queue
    private int qoutPtr;                                        // location to get next byte from adjusted for word access
    private final byte[] myBuffer=new byte[SampledSound.MAX_BUFF_LEN];
    private short[] memory;                                     // this is the cpu's memory, for direct access to it - this should only be read here, except for the queue out pointer.
    private int nextOut;                                        // where to get the next data from in SMSQ/E's queue
    private boolean stopSound=false;                            // Signals that once queue is empty sound should be killed
    private javax.sound.sampled.FloatControl volume;            // volume of sound played
    private volatile boolean isAsleep=false;
    private boolean reSample=false;
    private double rate;
    private double adjustFreq;
    
    /**
     * Creates this object, a DataLine object and an independent thread for filling the DataLine.
     * 
     * @param cpu the cpu.
     * @param DMAAccess : the cpu's memory, for direct access to it.
     * @param volume the volume the sound is to have : 0 -100.
     * @param warn a flag, if <code> true</code>, warn if sound problems may arise in the future.
     * @param frequency either "22.05" or "20" (any other value will be set to 22.05) : frequency in KHz.
     */
    public SampledSound(short []DMAAccess,smsqmulator.cpu.MC68000Cpu cpu,int volume,Warnings warn,String frequency)
    {
        javax.sound.sampled.AudioFormat audioFormat;
        if (frequency!=null && frequency.equals("20"))
            audioFormat = new javax.sound.sampled.AudioFormat (20000.0F, 8,2,false,false); //8000,11025,16000,22050,44100 -- 8,16 -- 1,2 -- boolean --boolean
        else
        {
            this.reSample=true;
            this.rate=(double)((double)(2050)/(double)22050);
            audioFormat = new javax.sound.sampled.AudioFormat (22050.0F, 8,2,false,false); //8000,11025,16000,22050,44100 -- 8,16 -- 1,2 -- boolean --boolean
        }
        this.memory=DMAAccess;
        cpu.data_regs[0]=0;
        javax.sound.sampled.DataLine.Info dataLineInfo = new javax.sound.sampled.DataLine.Info (javax.sound.sampled.SourceDataLine.class, audioFormat);
        try
        {   
            this.sourceDataLine = (javax.sound.sampled.SourceDataLine) javax.sound.sampled.AudioSystem.getLine (dataLineInfo);
            this.sourceDataLine.open(audioFormat);
            this.playThread = new PlayThread();
            this.playThread.start();
        } 
        catch (Exception e) 
        {
            if (warn.warnIfSoundProblem)
            {
                Helper.reportError(Localization.Texts[45], Localization.Texts[72]+":\n",null,e);
                cpu.data_regs[0]=-1;
            }
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
     * Fill in the pointers to the data area for this sampled sound.
     * Called during SMSQ/E initialization of the SSSS.
     * A0 points to memory for queue.
     * 
     * @param cpu the smsqmulator.cpu.MC68000Cpu used.
     */
    public void fillPointers(smsqmulator.cpu.MC68000Cpu cpu)
    {
        if (this.sourceDataLine==null)
            return;
        if (this.sourceDataLine!=null)
            this.sourceDataLine.flush();
        this.memory=cpu.getMemory();
        this.base=cpu.addr_regs[0]-8;                       // base address of queue memory block
        this.last=cpu.readMemoryLong(this.qend+this.base) /2; // end address of queue, adjusted for word sized access  
        this.start=(this.qbase+this.base)/2;                // start address of queue in my memory block adjusted for word access
        this.nextOut=this.start;                            // at the beginning, next byte out is at start of queue
        this.qoutPtr=(this.base+this.qout)/2;               // where our out pointer lies
        cpu.data_regs[0]=0;                                 // all OK
    }
    
    /**
     * Sets the memory used by the cpu.
     * 
     * @param memory the memory (array of short) to set.
     */
    public synchronized void setMemory(short [] memory)
    {
        this.base=0;
        this.memory=memory;
    } 
  
    /**
     * Copies the sound data from the SMSQE queue into my (empty!) buffer.
     * 
     * @return the number of bytes got.
     */
    private int getFromQueue()
    {
   /*      FOR TESTING POURPOSES ONLY
        if (this.base==0 || this.memory==null)              //huh? these are not initialized, can't do anything!
            return 0;
        int m = alternateGet();
        if (m>-1)
            return m;
        
     */   
        int first=((this.memory[(this.qin+this.base)/2]<<16)+(this.memory[(this.qin+this.base+2)/2]&0xffff))/2; // this is where next element to insert goes= end of my output queue
        // there is a small RISK here : it is conceivable that the "first" address is corrupted if SMSQE modifies the queue-in pointer whilst the
        // playthread thread tries to get it. Hence the next test:
        if (first > this.last || first <this.start)
            return 0;
        if (this.nextOut==first)
            return 0;                                       // can't get anything, queue is empty
        int content=0;
    //    while (this.nextOut!=first &&  content< SampledSound.MAX_BUFF_LEN)
        while (this.nextOut!=first &&  content< 10000)
        {
            this.myBuffer[content++]=(byte)((this.memory[this.nextOut]>>>8));// get data for left channel
            this.myBuffer[content++]=(byte)(this.memory[this.nextOut++]&0xff);// get data for right channel
            if (this.nextOut==this.last)
                this.nextOut=this.start;                    // restart at the beginning
            if (this.reSample)                              // possibly resample
            {
                 if (this.adjustFreq<1.0)
                     this.adjustFreq +=this.rate;
                 else
                 {
                      this.myBuffer[content]= this.myBuffer[content-2];
                      content++;  
                      this.myBuffer[content]= this.myBuffer[content-2];
                      content++;
                      this.adjustFreq-=1.0;
                 }
            }
        }
        this.memory[this.qoutPtr]=(short)((this.nextOut*2)>>>16);// set upper word of address
        this.memory[this.qoutPtr+1]=(short)((this.nextOut*2)&0xffff);// set lower word of address
        
        return content;   
    }
    
    /** Alternative, unused, keep for testing purposes only -*
    private int alternateGet()
    {
        int base=0xd000/2;
        int count = this.memory[base]&0xffff;
        base++;
        for (int i=0;i<count;i++)
        {
            int m = this.memory[base+i]&0xffff;
            m=~m;
            this.myBuffer[i*2]=(byte)((m>>>8));// get data for left channel
            this.myBuffer[i*2+1]=(byte)(m&0xff);// get data for right channel
        }
        if (count!=0)
            count=count;
        this.memory[--base]=0;
        return count*2;
    }
    
    /**
     * Checks whether the sound queue currently seems to be empty.
     * 
     * @return <code>true</code> if current sound queue seems to be empty.
     */
    private boolean queueIsEmpty()
    {
        if (this.base==0 || this.memory==null)              //huh? these are not initialized, can't do anything!
            return true;
        int first=((this.memory[(this.qin+this.base)/2]<<16)+(this.memory[(this.qin+this.base+2)/2]&0xffff))/2; // this is where next element to insert goes= end of my output queu
        // there is a small RISK here : it is conceivable that the "first" address is corrupted if SMSQE modifies the queue in pointer whilst the
        // playthread thread tries to get it. Hence the next test:
        if (first > this.last || first <this.start)
            return true;
        return this.nextOut==first;
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
     * Tries to kill the currently played sound.
     * 
     * @param cpu the smsqmulator.cpu.MC68000Cpu used.
     */
    public void killSound(smsqmulator.cpu.MC68000Cpu cpu)
    {
     //   if (this.sourceDataLine!=null)
       //     this.sourceDataLine.flush();
        if (this.playThread!=null)
            this.playThread.stopNow();
        cpu.data_regs[0]=0;
    }
     
    /**
     * Wakes up the Sound job, if need be.
     * This is done by interrupting it.
     * 
     * @param cpu the cpu used.
     */
    public void playSound(smsqmulator.cpu.MC68000Cpu cpu)
    {
        if (this.sourceDataLine!=null)
        {
            closeSound(false);
            if (this.volume.getValue() == this.volume.getMinimum())
                return;
            if (this.playThread!=null)
                if (this.isAsleep)
                    this.playThread.interrupt();
        }
        cpu.data_regs[0]=0;
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
     * Signals that once queue is empty sound should be killed:
     * This tries to get around a java bug.
     * 
     * @param close set to <code>true</code> if the sound should be killed if queue empty.
     */
    public synchronized void closeSound(boolean close)
    {
        this.stopSound=close;
    }
    
    /**
     * Checks whether sound is set to be killed when queue empty.
     * 
     * @return <code>true</code> if the sound should be killed if queue empty.
     */
    public synchronized boolean querySoundClose()
    {
        return this.stopSound;
    }
    
    /**
     * The independent thread that fills the DataLine.
     * Most of the time this thread will just be sleeping.
     */
    class PlayThread extends Thread
    {
        private final int sleepPeriod = 2;                       // nbr of milliseconds to sleep if no sound got
        private final int waitPeriod=7000/sleepPeriod;            // nbr of loops till we reach 7 seconds
        private boolean stopNow=false;
        @Override
        @SuppressWarnings({"UnusedAssignment", "SleepWhileInLoop"})
        public void run()
        {
            int num,count=0;
            while(true)                                         // playthread is one continuous loop
            {
                try
                {               
                    if (this.stopNow)                           // killsound wished, introduced in 1.02
                    {
                        sourceDataLine.flush();
                        sourceDataLine.stop();
                        this.stopNow=false; 
                        isAsleep=true;
                        nextOut=((memory[(qin+base)/2]<<16)+(memory[(qin+base+2)/2]&0xffff))/2; // this is where next element to insert goes= end of my output queue
        
                        Thread.sleep(2000000000);               // sleep for a LOOONG time
                        isAsleep=false;                         
                        sourceDataLine.start();                 // and then restart the dataline
                    }
                    if ((num=getFromQueue())!=0)            // (try to) get bytes from queue
                    {   
                        if (!this.stopNow)
                            num=sourceDataLine.write(myBuffer,0,num);// write content of my buffer into source dataline
                        count=0;  
                    }
                    else 
                    {
                        if (querySoundClose())
                        {
                            count=this.waitPeriod;
                            closeSound(false);
                            sourceDataLine.drain();
                        }
                        count++;
                        if (count>=this.waitPeriod)
                        {
                            count=0;
                            if (queueIsEmpty())             // one last check
                            {
                                isAsleep=true;
                                sourceDataLine.stop();      // after x seconds of silence, stop line
                                sourceDataLine.flush();
                                
                                Thread.sleep(2000000000);   // sleep for a LOOONG time
                                isAsleep=false;
                                sourceDataLine.start();     // and then restart the dataline
                            } 
                        }
                        else
                            Thread.sleep(this.sleepPeriod);
                    }
                }
                catch (InterruptedException e)                  // called when sound is to be played
                {
                    isAsleep=false;
                    sourceDataLine.start();
                }
                catch (Exception e) 
                { 
                   //nop 
                }
            }
        }
        public void stopNow()                                   // signal that sound should be killed now
        {
            this.stopNow=true;
        }
    }
}
