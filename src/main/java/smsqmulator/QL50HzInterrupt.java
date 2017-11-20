package smsqmulator;

/**
 * This simulates a 50 Hz external frame interrupt, and generates screen redraws.
 * Two independent threads are created:
 * <ul>
 *   <li> The first one sleeps for 20 milliseconds and then signals an interrupt to the cpu,</li>
 *   <li> The second thread is responsible for drawing the screen.</li>
 * </ul>
 * There are two threads because the 50 Hz interrupt is supposed to be as precise as possible, and the screen redraw
 * may take too much time.
 * 
 * @see smsqmulator.ExternalInterruptHandler
 * 
 * @version 
 
 * 1.06 Update interval is selectable via the setScreenUpdateInterval. Interval variable added to screen thread.
 * 1.05 revert to earlier thread behaviour, just sleep for nominal 1/50th of a second (exit changes of 1.01),
 * 1.04 check whether screen is "dirty" is made here, not in the screen paintComponent routine.
 * 1.03 the screenThread no longer sets the pointer pos (handled by TrapDispatcher since v. 1.13). 
 *      Try to make sure the screen object is no longer referenced when screen is changed.
 * 1.02 the ScreenThread also sets pointer position when this is changed from within SMSQE.
 * 1.01 better way of calculating the sleep time.
 * 1.01 no longer set time each time this is called.
 * 1.00 use adjusted time.
 * 
 * author and copyright (c) Wolfgang Lenerz 2012 -2016
 */
public class QL50HzInterrupt implements ExternalInterruptHandler
{
    private InterruptThread iThread=null;
    private ScreenThread sThread=null;
    private smsqmulator.cpu.MC68000Cpu cpu;
    private Screen screen;
    /**
     * Creates the object.
     * 
     * @param timeAddress where to put the time info.
     * @param cpu the cpu to which this interrupt is attached.
     * @param screen the screen used by the screenupdater thread.
     */
    public QL50HzInterrupt (int timeAddress,smsqmulator.cpu.MC68000Cpu cpu,Screen screen)
    {
        this.cpu=cpu;
        this.screen=screen;
        try
        {
            this.cpu.registerInterruptHandler(this);
        }
        catch (Exception e)
        {
            /*NOP*/
        }
    }
    
    /**
     * Handles the interrupt, no instruction is being executed whilst this goes on.
     * This should be called from the CPU execute thread.
     * However, since there is only one interrupt, this isn't called at all, the CPU handles the interrupt directly.
     * 
     * was:
     * Port $18021 on the emulated machine is set to 8, to signal the external interrupt.
     * An exception is raised, which changes the CPU's PC.
     * 
     * @param cpu the CPU for which an interrupt was generated.
     * 
     * @see smsqmulator.ExternalInterruptHandler#handleExternalInterrupt(smsqmulator.cpu.MC68000Cpu cpu) 
     */
    @Override
    public void handleExternalInterrupt(smsqmulator.cpu.MC68000Cpu cpu)
    {
     //  cpu.writeMemoryByte(0x18021, 8);                    // show SMSQE that there was an external interrupt - this is safe since this is called from the CPU execution thread.
     //  cpu.raiseInterrupt(2);                              // generate the exception
    }

    /**
     * Starts the interrupt handler and screen redraw threads.
     */
    public void startInterrruptHandler()
    {
        if (this.iThread!=null)
            this.iThread.stopit();                          // let the interrupt thread die if there is any running (there shouldn't be)
        this.iThread=null;
        this.iThread=new InterruptThread(this.cpu);         // set up new interrupt thread    
      
        if (this.sThread!=null)
            this.sThread.stopit();
        this.sThread=null;
        this.sThread=new ScreenThread(this.screen);
        
        this.iThread.start();
        this.sThread.start();
    }
    
    /**
     * Stops the interrupt handler and screen redraw threads.
     */
    public void stopInterruptHandler()
    {
        if (this.iThread!=null)
            this.iThread.stopit();
        this.iThread=null;
        if (this.sThread!=null)
        {
            this.sThread.setScreen(null);                   // just to make sure that this will be garbage collected OK.
            this.sThread.stopit();
        }
        this.sThread=null;
    }
     
    /**
     * This stops the interrupt handler, and removes any reference to the cpu.
     */
    @Override
    public void removeHandler()
    {
        stopInterruptHandler();
        this.cpu=null;
    }
    
    /**
     * Sets a (new) screen object.
     * @param screen the Screen to set.
     */
    public void setScreen (Screen screen)
    {
        this.screen=screen;
        if (this.sThread!=null)
            this.sThread.setScreen(screen);
    } 
    
    /**
     * Sets a (new) cpu object.
     * This stops the interrupt threads
     * @param ncpu the new Cpu to set.
     */
    public void setCpu(smsqmulator.cpu.MC68000Cpu ncpu)
    {
        stopInterruptHandler();
        this.cpu=ncpu;
        try
        {
            this.cpu.registerInterruptHandler(this);
        }
        catch (Exception e)
        {
            /*NOP*/
        }
    }
    
    /**
     * Sets the interval between screen updates.
     * 
     * @param tim the interval between screen updates in milliseconds
     */
    public void setScreenUpdateInterval(int tim)
    {
        if (this.sThread!=null)
            this.sThread.setScreenUpdateInterval(tim);
    }
    
    
    /**
     * This is the independent thread doing the "interrupting". 
     * 
     * This class generates the interrupt by (NOMINALLY) just waking up very 20 ms and telling the cpu that an interrupt was generated.
     */
    private class InterruptThread extends Thread
    {
        private volatile boolean stopNow=false;                 // will be true if this thread must stop
        private final smsqmulator.cpu.MC68000Cpu cpu;           // the cpu to notify of the interrupt.
        private static final int NOMINAL_CLOCK_TICK=20;         // 1 tick every 20 ms = 50 Hz timer.
        
        /**
         * Creates the object.
         * 
         * @param cpu what cpu are we interrupting?
         */
        public InterruptThread(smsqmulator.cpu.MC68000Cpu cpu)
        {
            this.cpu=cpu;
        }
        
        /**
         * The actual method that does the interrupting.
         * It's a continuous loop, until stopIt is set to true - then the loop ends and this method finishes, ending the thread.
         */
        @Override
        public void run()
        {
            try
            {
                InterruptThread.sleep (1000);               // at first, sleep for a second, give the emulation time to start running
                while (!this.stopNow)                       // do this until thread is stopped
                {
                    InterruptThread.sleep(InterruptThread.NOMINAL_CLOCK_TICK);
                    if(!this.stopNow)
                    {
                        this.cpu.generateInterrupt();           // tell the cpu that interrupt was generated
                    }
                }
            }
            catch(Exception e)
            {/*nop*/}
        }
        
        /**
         * Stops this thread.
         */
        public void stopit()
        {
            this.stopNow=true;
        }
    } 
    
    /**
     * This is the independent thread doing the screen redrawing.
     * It's a continuous loop, until stopIt is set to true - then the loop ends and this method finishes, ending the thread.
     */
    private class ScreenThread extends Thread
    {
        private volatile boolean stopNow=false;             // will be true if htis thread must stop
        private Screen screen;                              // the screen to be redrawn
        private volatile long interval=40;
        
        /**
         * Creates the object.
         * 
         * @param screen the screen to be redrawn.
         */
        public ScreenThread(Screen screen)
        {
            this.screen=screen;
        }
        
        @Override
        public void run()
        {
            try
            {
                ScreenThread.sleep (1000);                  // at first, sleep for a second, give the emulation time to start running
                while (!this.stopNow)
                {
                    ScreenThread.sleep(this.interval);
                    if (this.screen!=null && this.screen.isDirty)
                    {
                        this.screen.repaint();
                    }
                }
            }                                               
            catch(Exception e)
            {/*nop*/}
        }
        
        /**
         * Stops this thread next time it wakes up.
         */
        public void stopit()
        {
            this.stopNow=true;
        }       
        
        /**
         * Sets the screen this object is to repaint.
         * 
         * @param screen the screen to repaint.
         */
        public void setScreen (Screen screen)
        {
           this.screen=screen; 
        }
        
        /**
         * Sets the interval between screen updates.
         * 
         * @param tim the interval between screen updates in milliseconds.
         */
        public void setScreenUpdateInterval(int tim)
        {
            this.interval=(long) tim;
        }
    }
}
