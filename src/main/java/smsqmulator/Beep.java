package smsqmulator;

/**
 * This implements a simple BEEP interface.
 * It produces the BEEP via a SourceDataLine.
 * BEEP duration,pitch1,pitch2,interval,step
 * <p>
 * ---- The rest of the QL beep parameters are ignored !
 * The sound is played via an independent thread.
 * 
 * @author and copyright (C) Wolfgang Lenerz 2012-2017.
 * @version
 *  v. 1.05 improved parameter handling, should produce sounds closer to the original.
 *  v. 1.04 kill sound before playing new one.
 *  v. 1.03 PlayThread sleeps for 1/1000th of a second before draining, helps openjdk (dunno why),
 *          for this sound : 0a0a 0000 0aaa 0115 6400 e40c 0100 0000 0100
 *  v. 1.02 Volume control tries volume then master gain.
 *  v. 1.01 provides for volume setting.
 */
public class Beep
{
    private static final boolean BIG_ENDIAN=true;           // true
    private boolean canPlay=true;
    private javax.sound.sampled.SourceDataLine sdl;         // used to play the sound
    private static final float SAMPLE_RATE = 44100;         // CD quality
    private static final int channels=1;                    // 1 channel
    private static final int bits=8;                        // 8 bits
    private static final boolean SIGNED=false;              // unsigned bytes
    private static final double RAD = 2.0 * Math.PI;        // radian factor
    private double vol=1.0;                                 // sound volume
    private int beepStateInSysvars=0;                       // how the SMSQ/E sees beeping as active or nor
    private javax.sound.sampled.FloatControl volume;        // volume control
    
    
    /**
     * Creates the object, setting up a sourcedataline.
     * 
     * @param percentage the volumne in percent, from 100 = loudest to 0 = no beep.
     */
    public Beep(int percentage)
    {
        try
        {
            javax.sound.sampled.AudioFormat af=new javax.sound.sampled.AudioFormat(SAMPLE_RATE,bits,channels,SIGNED,BIG_ENDIAN);
            this.sdl=(javax.sound.sampled.SourceDataLine) javax.sound.sampled.AudioSystem.getLine(new javax.sound.sampled.DataLine.Info(javax.sound.sampled.SourceDataLine.class,af));
            this.sdl.open(af);
        }
        catch (Exception e)
        {                                                   // i couldn't get a valid source date line
            this.canPlay=false;
            return;
        }
        try
        {
            this.volume= (javax.sound.sampled.FloatControl) this.sdl.getControl(javax.sound.sampled.FloatControl.Type.VOLUME);
            setVolume(percentage);                          // try to get a normal volume control
        }
        catch (Exception e) 
        {                                                   // couldn't
            try
            {
                this.volume= (javax.sound.sampled.FloatControl) this.sdl.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
                setVolume(percentage);                      // if no volume control try to get a master gain control
            }
            catch (Exception v)
            {
                /*nop*/                                     // too bad, you don't have a volume control, then!
            }
        }
    }
    
    /**
     * Creates a simple beep object, with volume set at 100 %.
     */
    public Beep()
    {
        this (100);
    }
    
    /**
     * Sets the sound volume.
     * 
     * @param percentage the volume, from 0 (no sound) to 100 (loudest). Anything exceeding the limit will be set to the limit.
     */
    public final void setVolume(int percentage)
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
        {/*NOP*/}                                           // ignore error
    }
    
    /**
     * Kills the sound.
     * 
     * @param cpu cpu used. The SMSQ/E beeping system variable will be set to 0.
     */
    public void killSound(smsqmulator.cpu.MC68000Cpu cpu)
    {
        if (this.canPlay)
        {
            this.sdl.flush();
            this.sdl.stop();
            cpu.writeMemoryByte(this.beepStateInSysvars,0);
        }
    }
  
    /**
     * Makes a frequency in Hz out of the QL type pitch.
     * The frequency is approx.  11447/(10.6+pitch) hz.
     * 
     * @param pitch the QL pitch.
     * 
     * @return the pitch in Hz.
     */
    private int makePitch(int pitch)
    {
        double p1=(double) (pitch-1);
        double p2=10.6+p1;
        return ((int)((double)11447 / p2))&0xffff;
    }
    
    /**
     * Makes the duration in milliseconds.
     * 
     * @param duration the QL type duration, supposedly in units of 72 microseconds (not milliseconds).
     * 
     * @return the duration as milliseconds.
     */
    private int makeDuration(int duration)
    {
        if (duration==0)
            return duration;
        duration/=25;
        return duration==0?1:duration;
    }
    
    /**
     * Plays the sound.
     * The sound to be played is in the list starting at (A3).
     * Any BEEP already playing is killed first.
     * 
     * @param cpu cpu used.
     */
    public void play(smsqmulator.cpu.MC68000Cpu cpu)
    {
        if (!this.canPlay)
            return;
        killSound(cpu);
        if (this.volume.getValue() == this.volume.getMinimum())
            return;                                         // don't bother playing sound if vol = 0.
        
        int interval=-1,stepInPitch=-1;
        int duration,hiPitch,lowPitch,currentPitch;
        int bbits=4;                                        // %0100 for second parameter
        int A3=cpu.addr_regs[3]+1;                          
        int nbrOfParams=cpu.readMemoryByte(A3++)&0xff;      // nbr of parameters, QL progs will normally use 8, SMSQ/E beep
                                                            // uses 10 (!)
        this.beepStateInSysvars=cpu.addr_regs[6]+0x96;
        int nbrBits = cpu.readMemoryLong(A3);               // bitfield containing whether a param is to be taken into account(and how much of it)
        A3+=4;        
        hiPitch = cpu.readMemoryByte(A3++)&0xff;            // pitch 1 from 0 (high tone) to 255 (low tone)
        currentPitch=hiPitch;
        if ((nbrBits & bbits)==0)  
            lowPitch=cpu.readMemoryByte(A3)&0xff;           // possibly get second pitch
        else
            lowPitch=-1;
        A3++;
        bbits<<=2;                                          // point next param
        if ((nbrBits&bbits)==0)
        {
            interval=cpu.readMemoryByte(A3++)&0xff;         // interval between steps ...
            interval+=((cpu.readMemoryByte(A3++)&0xff)<<8); // ... is little endian
            interval=makeDuration (interval);               // make into duration in milliseconds
        }
        else
            A3+=2;
        
        bbits<<=4;                                          // duration is always there, no need to check for it
        duration=cpu.readMemoryByte(A3++)&0xff;
        duration+=((cpu.readMemoryByte(A3++)&0xff)<<8);     // duration is little endian
        if (duration==0)
        {
            killSound(cpu);
            return;
        }
        duration=makeDuration(duration);                    // duration is now in milliseconds
        if ((nbrBits&bbits)==0)                             // any step?
        {
            stepInPitch=(cpu.readMemoryByte(A3++));         // step is always positive for me (!)
            if (nbrOfParams==8)                             // QDOS type sound
                stepInPitch=(stepInPitch>>>4);
            stepInPitch&=0x0f;
            if((stepInPitch&8)!=0)
                stepInPitch|=0xfffffff0;                    // make step negative
        }
        
        duration =  (int)(Beep.SAMPLE_RATE * duration / 1000); // nbr of bytes needed for sound of this duration at this sample rate
        if (duration % 2 == 1)
            duration ++;
        byte []soundArray = new byte[duration];
        if (stepInPitch!=0 && interval>0 && lowPitch>0)      // if any of these is unset, only play pitch 1
        {
            interval=(int)Beep.SAMPLE_RATE * interval / 1000;// nbr of bytes per interval
        }
        else
        {
            lowPitch=hiPitch;
            interval=duration;
        }
        
        if (hiPitch<lowPitch)
        {
            bbits=hiPitch;                                      // bbits is no longer needed, use as temp
            hiPitch=lowPitch;
            lowPitch=bbits;
        }
        
        int i=0,counter=0;
        double angle;
        double m=Beep.RAD / Beep.SAMPLE_RATE;
        double mVol=127.0*vol;
        int pitch =makePitch(currentPitch);
        while (i<duration)                              // now fill in array with the bytes making up the sound
        {
            angle = i * pitch * m;
            soundArray[i] = (byte) (Math.sin(angle) * mVol);
            if (++counter==interval)                    // next interval reached
            {
                currentPitch+=stepInPitch;              // change pitch at each interval
                if (currentPitch>hiPitch || currentPitch<lowPitch)
                {
                    stepInPitch*=-1;
                    currentPitch+=stepInPitch*2;
                }

                pitch=makePitch(currentPitch);
                counter=0;
            }
            i++;
        }
        cpu.writeMemoryByte(this.beepStateInSysvars,1);
        PlayThread p=new PlayThread(soundArray,cpu);
        p.start();
    }
    
    /**
     * This is the Thread that actually plays the sound.
     */
    private class PlayThread extends Thread
    {
        private final byte[] soundArray;
        private final smsqmulator.cpu.MC68000Cpu cpu;
        
        /**
         * Creates the thread.
         * 
         * @param soundArray contains the sound data to be played.
         * @param cpu 
         */
        public PlayThread (byte[]soundArray,smsqmulator.cpu.MC68000Cpu cpu)
        {
            this.soundArray=soundArray;
            this.cpu=cpu;
        }
        
        @Override
        public void run()
        {
            try
            {                        
                sdl.start();
                sdl.write(this.soundArray, 0, this.soundArray.length);
                sleep(1);                                   // for some reason, this is necessary for openJdk.
                sdl.drain();
                killSound(this.cpu);
            }
            catch (Exception e) 
            {/*nop*/  }                                    // I don't care about an exception here,at worst the sound won't play.
        }
    }
}
