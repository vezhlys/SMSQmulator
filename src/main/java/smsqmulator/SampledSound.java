package smsqmulator;


/**
 * An object to make some sampled sound according to SMSQ/E's SSSS specification.
 * That takes 20 kHz sound, stereo, one byte for each channel.
 * <p>
 * Bytes are sent to this by "chunks". Each chunk represents a certain number of bytes corresponding to samples
 * that are added to the SSSS. There is a minimum of 2 bytes per chunk (one left, one right) if a single 
 * sample is added, and a maximum of (SMSQE SSSS buffer size) bytes if multiple bytes are added to the SSSS.<p>
 * 
 * Internally, each chunk is repreented here by an array of bytes. This object creates a very primitive 
 * FIFO buffering. This is a achived via a simple ArrayList of byte[]. If a sample is added to the SSSS, the
 * byte array corresponding to that chunk is added to the end of the ArrayList, and upon playback the first 
 * element of the ArrayList is played and removed from the list. 
 * 
 * (There is an inefficiency in this since each time element 0 is removed f0orm the ArrayList, all other elements
 * are shuffled down. I'm not sure, however, whether this really merits that I implement a real FIFO queue).
 * <p>
 * Playback is via an independent <code> PlayThread</code> thread. This thread is created once and, when it
 * has nothing to do (no sound is to be played) it will just go to sleep. A new thread is NOT created every 
 * time a sound is to be played. The thread feeds data to a SourceDataLine set up here.
 * <p>
 * The general contract with the SMSQE's SSSS is that whenever a sample is sent to it, SMSQE gets here via the 
 * corresponding trap (playsound) and the corresponding chunk is taken from SMSQE's memory and added to the arrayList.
 * Then the independent thread is started or woken, which copies each chunk from this ArrayList to the SourceDataline
 * buffer set up here.
 * <p>
 * There is a bug in the java SourceDataLine : if one repeatedly sends it small sound samples, the sound is repeated indefintely (in total or in part)
 * until the dataline is closed/stopped. To try to get around this, a special flag may be set that tells that the sound 
 * should be stopped if the SSSS is empty, a new vector is introduced into the SSSS in SMSQ/E, that 
 * will cause the PlayThread to empty and STOP the SoundDataLine once the queue is empty.
 * <p>
 * This also handles resampling. The only sound format allowed for the SSSS is 20kHz stereo sound. Some SourceDataLines
 * cannot handle that format (especially under OpenJDK). Hi=owever,a pparently all will handle 22.1 kHz (halve the CD 
 * rate), I reasmple the sound in that case.
 * 
 * <p>
 * @author and copyright (c)Wolfgang Lenerz 2012-2017
 * @version
 *  1.05    interface SMSQE <-> this objet totally revamped, uses a primitiv buffering system..
 *  1.04    resampling if 22.05 Khz is chosen, thanks to Marcel Kilgus for the algorithm.
 *  1.03    minor modification in queueIsEmpty
 *  1.02    better handling of killsound, faster killsound by limiting the sample size in getFromQueue, frequency in object creation.
 *  1.01    Volume control tries volume then master gain. Popup warnign is made configurable.
 *  1.00    setVolume implemented
 *  0.01    implemented queue is empty, close sound, query sound : try to get around Java bug of repeating sounds.
 *  0.00    first build
 */
public class SampledSound 
{ 
    private javax.sound.sampled.SourceDataLine sourceDataLine;  // this produces the sound
    private PlayThread playThread;                              // the thread feeding the data to the SourcedataLine
    private final java.util.ArrayList<byte[]> queue = new java.util.ArrayList<byte[]>();// primitive FIFO queue
    private boolean stopSound=false;                            // Signals that once queue is empty sound should be killed
    private javax.sound.sampled.FloatControl volume;            // volume of sound played
    private volatile boolean isAsleep=false;                    // is true if playThread is asleep
    private boolean reSample=false;                             // is true of sond needs to be resampled from
    private final static double RATE=(double)((double)(2050)/(double)22050); // resampling rate
    private double adjustFreq;                                  // shows when to add a sample
    private int start;                                          // start of SMSQE SSSS buffer 
    private int end;                                            // end of SMSQE SSSS buffer 
    
    /**
     * Creates this object, a DataLine object and an independent thread for filling the DataLine.
     * 
     * @param cpu the cpu.
     * @param volume the volume the sound is to have : 0 -100.
     * @param warn a flag, if <code> true</code>, warn if sound problems may arise in the future.
     * @param frequency either "22.05" or "20" (any other value will be set to 22.05) : frequency in KHz.
     */
    public SampledSound(smsqmulator.cpu.MC68000Cpu cpu,int volume,Warnings warn,String frequency)
    {
        javax.sound.sampled.AudioFormat audioFormat;
        if (frequency!=null && frequency.equals("20"))
            audioFormat = new javax.sound.sampled.AudioFormat (20000.0F, 8,2,false,false); //8000,11025,16000,22050,44100 -- 8,16 -- 1,2 -- boolean --boolean
        else
        {
            this.reSample=true;
            audioFormat = new javax.sound.sampled.AudioFormat (22050.0F, 8,2,false,false); //8000,11025,16000,22050,44100 -- 8,16 -- 1,2 -- boolean --boolean
        }
        javax.sound.sampled.DataLine.Info dataLineInfo = new javax.sound.sampled.DataLine.Info (javax.sound.sampled.SourceDataLine.class, audioFormat);
        try
        {   
            this.sourceDataLine = (javax.sound.sampled.SourceDataLine) javax.sound.sampled.AudioSystem.getLine (dataLineInfo);
            this.sourceDataLine.open(audioFormat);              // open dataline
            this.playThread = new PlayThread();                 // now create the thread 
            this.playThread.setName("SampledSound play thread");
            this.playThread.setDaemon(true);
            this.playThread.start();
        } 
        catch (javax.sound.sampled.LineUnavailableException e) 
        {
            if (warn.warnIfSoundProblem)
            {
                Helper.reportError(Localization.Texts[45], Localization.Texts[72]+":\n",null,e);
                cpu.data_regs[0]=-1;
            }
            this.sourceDataLine =null;
            return;
        }
        try                                                     // try to set volume via volume control
        {
            this.volume= (javax.sound.sampled.FloatControl) this.sourceDataLine.getControl(javax.sound.sampled.FloatControl.Type.VOLUME);
            setVolume(volume);
        }
        catch (Exception e) 
        {
            try                                                 //..0 if that deosn't work try to set volume via mster gain
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
     * Fill in the pointers to the SSSS buffer.
     * Called during SMSQ/E initialization of the SSSS.
     * 
     * @param cpu the smsqmulator.cpu.MC68000Cpu used.
     */
    public void fillPointers(smsqmulator.cpu.MC68000Cpu cpu)
    {
        if (this.sourceDataLine==null)
            return;
        this.sourceDataLine.flush();
        this.start=cpu.addr_regs[2];                            // start of bytes
        this.end=cpu.addr_regs[1];                              // end of bytes
        cpu.data_regs[0]=0;                                     // all OK
    }
    
    /**
     * Gets the next chunk to be played.
     * 
     * This is called from the PlayThread.
     * 
     * @return the chunk to tbe played or null if there is none.
     */
    private byte[] getFromQueue()
    {
        if (this.queue.isEmpty())  
            return null;
        byte []temp;
        synchronized(this.queue)
        { 
            temp= this.queue.get(0);                            // get the first chunk (FIFO)
            this.queue.remove(0);                               // remove it from "queue"
        }
        return temp;
    }
    
    /**
     * Checks whether the chunk queue currently seems to be empty.
     * 
     * @return <code>true</code> if current sound queue seems to be empty.
     */
    private boolean queueIsEmpty()
    {
       return this.queue.isEmpty();
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
     * This is called from the emulation thread.
     * 
     * @param cpu the smsqmulator.cpu.MC68000Cpu used.
     */
    public void killSound(smsqmulator.cpu.MC68000Cpu cpu)
    {
        if (this.playThread!=null)
            this.playThread.stopNow();                          // stop playing
        synchronized(this.queue)
        {
            this.queue.clear();                                 // and empty queue
        }
        cpu.data_regs[0]=0;                                     // signal no error
    }
     
    /**
     * This adds a chunk from the SSSS and plays it. It wakes up the PlayThread, if need be (by interrupting it).
     * A1 points to the end of the queue.
     * 
     * This is called from the emulation thread.
     * 
     * @param cpu the cpu used.
     */
    public void playSample(smsqmulator.cpu.MC68000Cpu cpu)
    {
        cpu.data_regs[0]=0;
        if (this.sourceDataLine!=null)
        {
            if (this.volume.getValue() == this.volume.getMinimum())
                return;                                         // no sound is being hearde anyway
            if (this.playThread!=null)
            {
                if (this.isAsleep)
                    this.playThread.interrupt();
                byte[]buff=resample(cpu);
                if (buff!=null)
                {
                    synchronized(this.queue)
                    {
                        this.queue.add(buff);
                    }
                }
            }
        }
    }
    
    /**
     * Add a chunk.
     * 
     * @param buff the chunk to add.
     */
    public void addChunk(byte[]buff)
    {
        buff=resample(buff);
        if (buff!=null)
        {
            synchronized(this.queue)
            {
                this.queue.add(buff);
            }
        }
        if (this.playThread!=null)
            if (this.isAsleep)
                this.playThread.interrupt();

    }
    
    /**
     * Resample an exisiting chunk.
     * 
     * @param buff the chunk to resample.
     * 
     * @return the resampled chunk.
     */
    private byte[] resample(byte[]buff)
    {
        if (!this.reSample)
            return buff;
        int l=buff.length;
        java.util.ArrayList<Byte>ar=new java.util.ArrayList<>(l+l/9);
        for (int i=0;i<l-1;i+=2)
        {
            ar.add(buff[i]);
            ar.add(buff[i+1]);
            if (this.adjustFreq<1.0)
                this.adjustFreq +=this.RATE;
            else
            {
                ar.add(buff[i]);
                ar.add(buff[i+1]);
                this.adjustFreq-=1.0;
            }
        }
        return convertArrayList(ar);
    }
    
    /**
     * Converts an ArrayList of byte into an array of byte.
     * 
     * @param ar
     * 
     * @return 
     */
    private byte[] convertArrayList (java.util.ArrayList<Byte>ar)
    {
        if (ar==null)
            return null;
        int l=ar.size();
        if (l==0)
            return null;
        byte  []f=new byte[l];
        for (int i=0;i<l;i++)
        {
            f[i]=ar.get(i);
        }
        return f;
    }
    /**
     * This gets a chunk from the SSSS and possibly resamples it.
     * 
     * @param cpu the cpu used. A1 points to the end of the queue.
     * 
     * @return a new chunk, or null.
     */
    private byte[] resample(smsqmulator.cpu.MC68000Cpu cpu)
    {
        byte[] buff;
        short[]mem=cpu.getMemory();                             // cpu memory array
        int a1=cpu.addr_regs[1];                                // end of sample(s)
        int count=a1-this.start;                                // number of bytes in sample(s)
        if (count==0)
            return null;
        if (!this.reSample)
        {                                                       // no resamplng, split words into bytes
            int index=0;
            buff=new byte[count];                               
            for (int i=this.start/2;i<a1/2;i++)
            {
                short sh=mem[i];
                buff[index]=(byte)(sh>>>8);
                index++;
                buff[index]=(byte)sh;
                index++;
            }
            
            return buff;
        }
        else
        {
            java.util.ArrayList<Byte>ar=new java.util.ArrayList<>(count+count/9);// try to guess approximate size of resulting reasmped bytes
            for (int i=this.start/2;i<a1/2;i++)
            {
                short sh=mem[i];
                ar.add((byte)(sh>>>8));
                ar.add((byte)sh);
                if (this.adjustFreq<1.0)
                    this.adjustFreq +=SampledSound.RATE;
                else
                {
                    ar.add((byte)(sh>>>8));
                    ar.add((byte)sh);
                    this.adjustFreq-=1.0;
                }
            }
            buff=convertArrayList(ar);                          // convert Arraylist into bytes
        }
        return buff;
    }
    
    /**
     * Queries the current volume.
     * NB contrary to documentation, this doesn't work.
     * 
     * @return the volume or -1 if line no longer active.
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
    
    public boolean isStillPlaying(smsqmulator.cpu.MC68000Cpu cpu)
    {
        return !this.queue.isEmpty();
    }
    
    /**
     * The independent thread that fills the DataLine.
     * Most of the time this thread will just be sleeping.
     */
    class PlayThread extends Thread
    {                                                           
        private final int sleepPeriod = 2;                      // nbr of milliseconds to sleep if no sound got
        private final int waitPeriod=7000/sleepPeriod;          // nbr of loops till we reach 7 seconds
        private boolean stopNow=false;
        @Override
        @SuppressWarnings({"UnusedAssignment", "SleepWhileInLoop"})
        public void run()
        {
            int count=0;
            byte []bytes;
            while(true)                                         // playthread is one continuous loop
            {
                try
                {               
                    if (this.stopNow)                           // killsound wished, introduced in 1.02
                    {
                        sourceDataLine.flush();
                        sourceDataLine.stop();
                        this.stopNow=false; 
                        isAsleep=true;                          // signal that this thread is asleep now
                        
                        Thread.sleep(2000000000);               // sleep for a LOOONG time...
                        isAsleep=false;                         // ... until it is interrupted (normally this will divert to the catch block!)
                        sourceDataLine.start();                 // and then restart the dataline
                    }
                    if ((bytes=getFromQueue())!=null)           // (try to) get chunk from queue
                    {   
                        if (!this.stopNow)                      // got one, possibly play them
                            sourceDataLine.write(bytes,0,bytes.length);// write chunk into source dataline, blocks until done
                        count=0;  
                    }
                    else                                        // no chunk got, queue is empty
                    {
                        if (querySoundClose())                  // should I close the sound at end?
                        {
                            count=this.waitPeriod;              // yes, so prepare for it
                            closeSound(false);
                            sourceDataLine.drain();             // make sure this played to the end
                        }
                        count++;                                // one more wait 
                        if (count>=this.waitPeriod)             // have I waited long enough?   
                        {
                            count=0;
                            if (queueIsEmpty())                 // yes, one last check
                            {
                                isAsleep=true;
                                sourceDataLine.stop();          // after x seconds of silence, stop line
                                sourceDataLine.flush();
                                
                                Thread.sleep(2000000000);       // sleep for a LOOONG time
                                isAsleep=false;
                                sourceDataLine.start();         // and then restart the dataline
                            } 
                        }
                        else
                            Thread.sleep(this.sleepPeriod);     // I haven't waited long enough yet 
                    }
                }
                catch (InterruptedException e)                  // called when thread was asleep and sound is to be played
                {
                    isAsleep=false;                             // I'm no longer asleep
                    sourceDataLine.start();
                }
                catch (Exception e) 
                { 
                   //nop                                        // what could that be?
                }
            }
        }
        public void stopNow()                                   // signal that sound should be killed now
        {
            this.stopNow=true;
        }
    }
}
