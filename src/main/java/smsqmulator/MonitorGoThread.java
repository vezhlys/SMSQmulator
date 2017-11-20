
package smsqmulator;

/**
 * The independent thread that actually handles the emulation on a "g","gb" or "x" command.
 * 
 * The MonitorGoThread may observe breakpoints. When a breakpoint is hit, execution of the 68K program stops.<p>
 * The MonitorGoThread may log instructions. Logging is done to an internal data structure called a DebugList. Instructions executed are logged there
 * but are only written to the log file when execution of the program stops. The DebugList is a fifo queue and only keeps a certain
 * number of instructions (set up when this object is created).<p>
 * This thread may also watch a memory location and stop excution when the content of that memory location changes (long word only).<p>
 * This thread may also stop at a certain condition (eg register==value).
 * <p>
 * In "<code>gofast</code>" mode, no memory is watched, no breakpoints are checked etc - the program just executes as fast as it can.
 * 
 * @author and copyright (c) 2012 - 2015 Wolfgang Lenerz
 * @version 
 *  1.02 DebugList uses array.
 *  1.01 set TrapDispatcher for CPU even if slow mode.
 */
 public class MonitorGoThread extends Thread //javax.swing.SwingWorker <Void,Void>
 {
    private final smsqmulator.cpu.MC68000Cpu cpu;
    private final Monitor monitor;
    private QL50HzInterrupt ih =null;
    private final TrapDispatcher trapDispatcher;            // TrapDispatcher used to dispatch traps to drivers & callback traps to java.
    private int iterations;                                 // how many instructions are to be executed?
    private int deduct=1;                                   // number deducted from the number of instructions to be executed.
    private boolean stopNow;                                // as soon as this is <code>true</code>, the thread terminates (e.g. at a breakpoint).
    private boolean logInstructions=false;                  // true if instructions should be logged
    private DebugList dbl=null;                             // the list containing the debug info for logging
    private String instructionsListFilename="/home/wolf/debugSmsqmulator.txt";  // the file into which the list containing the debug info for logging will be dumped
    private boolean excludeSuper=false;                     // when logging, exclude all instructions executed in supervisor mode
    private final StringBuilder sBuilder;                         // used for building logging info
    private final java.util.ArrayList<Integer> breakpoints;       // the array with the breakpoints.
    private boolean watchBreakpoints=false;
    private int provbkp = -1;                               // provisional breakpoint used with traceover
    private boolean goFast=false;                           // switches go fast mode on or off
    private int conditionDataReg=-1;                        
    private int conditionAddressReg=-1;
    private int conditionValue;                             // the address & data ragisters, and the value, for a conditonal break.
    private boolean conditionIsContent;                             // the address & data ragisters, and the value, for a conditonal break.
    private int conditionAddress=-1;
    private boolean signalDeath=true;
    private int upperLimit=0;                               // when logging instructions, instructions beyond this limit will NOT be logged, set to 0 to log everything (useful to exclude OS code in high mem).
    private int oldval=0;                                   // old value of memory to be watched
    private boolean ckmem=false;                            // = true if memory is to be watched
    private int memWatched=0;                               // what mem address is to be watched
    private boolean oldSupervisor=false;
    
    /**
     * Creates the monitor go thread.
     * 
     * @param nbrOfInstructions how many instructions are to be executed, or 0 if continuous execution.
     * @param cpu for which copu the instructions are to be executed.
     * @param breakpoints an arrayList of Integers with memory addresses where breakpoints are set.
     * @param monitor the motor launching this goThread.
     * @param filename the name of the file where instructions are logged to when logging is switched on.
     * @param watchBreakpoints <code>true</code> if breakpoints should be observed.
     * @param logInstructions <code>true</code> if instrcutions should be logged.
     * @param ih the interrupt handler for a 50 Hz interrupt. This is started by the go thread.
     * @param goFast if this is <code>true</code>, no breakpoints will be wtached, no memory will be watched etc...
     * @param trapDispatcher the TrapDispatcher used to dispatch traps to drivers and callback traps to java.
     * @param upperLimit when logging instructions, instructions beyond this limit will NOT be logged (useful to exclude OS code in high mem).
     * @param checkmem set to <code>true</code> if memory is to be watched
     * @param memWatched what memory address is to be watched
     * @param provbreak provisional breakpoint used with traceover
     * @param excludeSuper if <code>true</code> : when logging, exclude all instructions executed in supervisor mode
     */
    public MonitorGoThread (int nbrOfInstructions, smsqmulator.cpu.MC68000Cpu cpu, java.util.ArrayList<Integer> breakpoints,Monitor monitor,String filename,
                            boolean watchBreakpoints,boolean logInstructions,QL50HzInterrupt ih,boolean goFast,TrapDispatcher trapDispatcher,
                            int upperLimit, boolean checkmem,int memWatched,int provbreak,boolean excludeSuper)
    {
        if (nbrOfInstructions!=0)
        {
            this.iterations=nbrOfInstructions;
            this.deduct=1;
        }
        else
        {
            this.iterations=1;
            this.deduct=0;
        }
        this.cpu=cpu;
        this.breakpoints=breakpoints;
        this.monitor=monitor;
        if (filename!=null && !filename.isEmpty())
            this.instructionsListFilename=filename;             // name of file to (possibly) save instructions to.
        this.dbl=new DebugList(50020);
        this.logInstructions=logInstructions;
        this.watchBreakpoints=watchBreakpoints;
        this.ih=ih;
        this.goFast=goFast;
        this.trapDispatcher= trapDispatcher;
        this.sBuilder=new StringBuilder(400);
        this.upperLimit=upperLimit;
        this.ckmem=checkmem;
        this.memWatched=memWatched;
        this.provbkp=provbreak;
        this.excludeSuper=excludeSuper;
    }
   
    @Override
    /**
     * Does the actual work : execute cpu instructions in a loop.
     */
    public void run()
    {
        boolean addressFlag=false;
        int time;
        this.dbl.clear();
        this.oldval=this.cpu.readMemoryLong(this.memWatched);
        this.cpu.setTrapDispatcher(this.trapDispatcher);
        if (this.ih!=null)
        { 
            this.ih.startInterrruptHandler();               // this stops interrupt threads before starting new ones
        }
        String message="";
        if (this.goFast)                                    // just go as fast as possible
        {
         //   try
           // {
            this.cpu.executeContinuous();                   // goes into continuous loop in the cpu object, doesn't come back here (unless problem)
            this.stopNow=true;         
         /*   }
            catch(Exception e)
            {
                e.printStackTrace();
                this.stopNow=true;
            }*/
        }
        while(this.iterations>0 && !this.stopNow)
        {
            try
            {
                if (this.logInstructions)                   // are instructions to be logged to "logfile"?
                {
                    if (!(this.excludeSuper && cpu.isSupervisorMode()))
                    {
                        if (this.upperLimit!=0)
                        {
                            int addr = this.cpu.pc_reg*2;
                            if (addr<this.upperLimit)
                                saveInstruction();
                        }
                        else
                            saveInstruction();
                    }
                }
                    
                if (this.cpu.pc_reg*2==this.conditionAddress)
                    addressFlag=true;
                time = this.cpu.execute();                  // execute 1 instruction
                
                if (time<0)                                 // SMSQE signaled something special                        
                {
                    if (time==-10)
                    {                                       // "Hard" breakpoint
                        this.stopNow=true;
                        message="Hard breakpoint";
                        break;
                    }
                    else if (time==-20)                     // hit an illegal istruction!
                    {
                        this.stopNow=true;
                        message="ILLEGAL instruction!";
                        break;
                    }
                    else if (time==-30)                     // use whenever I want to save instructions
                    {
                        saveInstruction();
                        this.dbl.saveToFile("/home/wolf/smsqe/DEBUG.txt");
                        this.cpu.stopNow=0;
                    }
                    else
                    {
                        this.trapDispatcher.dispatchTrap(-time,this.cpu);// it was some kind of TRAP instruction, or another request to come back to Java
                    }
                }
                int addr = this.cpu.pc_reg*2;
                this.cpu.stopNow=0;
                
                if (this.upperLimit!=0)                     // only count iterations if PC is below some arbitrary limit
                {                                           // (mans that when G is done, interrupts & trap are NOT counted)
                    if (addr<this.upperLimit)
                         this.iterations-=this.deduct;
                }
                else
                    this.iterations-=this.deduct;
                
                    
                if (this.ckmem)                             // are we surveiling a memory change?
                {
                    if((this.cpu.readMemoryLong(this.memWatched)!=oldval) || (addr>0x4900 && addr<0x12000))
                    {
                        this.stopNow=true;
                        oldval=this.cpu.readMemoryLong(this.memWatched);
                        message="Memory changed!";
                    }
                }
                
                if (this.provbkp!=-1)                       // check if there is a provisional breakpoint to be honored
                {
                    if (addr==this.provbkp)
                    {
                        this.stopNow=true;
                        message="Provisional Breakpoint hhit";
                        break;
                    }
                }
                
                if (this.watchBreakpoints && this.breakpoints!=null)// any other breakpoint to be honored?
                {
                    if(breakpoints.contains(addr))          // is this on a breakpoint?
                    {
                        this.stopNow=true;
                        message="Permanent breakpoint hit";
                        break;
                    }
                }
                if (checkConditon(addressFlag))                        // check for condition
                {
                    this.stopNow=true;
                    message="Condition met";
                }
            }
            catch(Exception e)
            {
          //      e.printStackTrace();
                this.stopNow=true;
            }
           
        }
        done(message);
    }
    /**
     * This is called when the emulation stops.
     * 
     * @param message the message ti display.
     */
    public void done(String message)
    {
        if (this.ih!=null)
            this.ih.stopInterruptHandler();                      // stop the interrupt handler
            
        this.provbkp=-1;                                    // no more provisional breakpoint
        if (this.signalDeath)
        {
            this.monitor.showInfo(true,null);               // log death of this thread
            this.monitor.goThreadStopped(message);
        }
        if (this.logInstructions)
            this.dbl.saveToFile(this.instructionsListFilename);
    }

    /**
     * Stops the thread.
     */
    public synchronized void stopThread()
    {
        this.stopNow=true;                                  // stops thread in slow mode
        this.interrupt();                                   // stops thread in fast mode
    }
    
    /** 
     * Switches logging state on/off.
     * @param newLogState if true, logging is switched on again
     */
    public synchronized void switchLogging(boolean newLogState)
    {
        this.logInstructions=newLogState;
        if (!newLogState)                                   // logging was switched off, save instructions
        {
            this.dbl.saveToFile(this.instructionsListFilename);
        }
        else
            this.dbl.clear();                              // logging switched on, clear list
    }
    
    /**
     * Do we log calls to the OS or only user state code?
     * 
     * @param superState if true, only log calls in user mode.
     */
    public synchronized void switchSuper(boolean superState)
    {
        this.excludeSuper=superState;
    }
    
    /**
     * "Saves" an instruction into the DebugList.
     */
    private void saveInstruction()
    {
        this.sBuilder.setLength(0);
        ListElement le=new ListElement();
        int address = cpu.pc_reg*2;
        le.memaddress=(String.format("%08x ",address));

        if(address < 0)
        {
            sBuilder.append(String.format("%08x   ????", address));
        }
        else
        {
            int opcode =  this.cpu.readMemoryWord(address);// opcode at current address
            smsqmulator.cpu.Instruction i = this.cpu.getInstructionFor(opcode);
            smsqmulator.cpu.DisassembledInstruction di = i.disassemble(address, opcode,this.cpu);
            boolean showBytes=false;
            if(showBytes)
            {
                di.formatInstruction(this.sBuilder);
            }
            else
            {
                di.shortFormat(this.sBuilder);
            }

            this.sBuilder.append(String.format("  %08x ", cpu.readMemoryLong(address)));
        }
        le.instruction=this.sBuilder.toString();
        
        int a7=this.cpu.addr_regs[7] & 0xfffffffe;// just in case...
        this.sBuilder.setLength(0);
        this.sBuilder.append(String.format("A7: %08x  ",a7));
        for (int i=0;i<4;i++)
        {
            this.sBuilder.append(String.format("    %08x  ",cpu.readMemoryLong(a7+(i*4))));
        }
        le.memcontent=this.sBuilder.toString();
        
        this.sBuilder.setLength(0);
        for (int i=0;i<8;i++)
        {
            this.sBuilder.append(String.format("D"+i+": %08x  ",this.cpu.data_regs[i]));
        }
        le.regContentsData=this.sBuilder.toString();
        
        this.sBuilder.setLength(0);
        for (int i=0;i<8;i++)
        {
            this.sBuilder.append(String.format("A"+i+": %08x  ",this.cpu.addr_regs[i]));
        }
        le.regContentsAddr=this.sBuilder.toString();
        
        if (this.cpu.isSupervisorMode()!=this.oldSupervisor)
        {
            this.oldSupervisor=!this.oldSupervisor;
            le.supervisor="New Supervisor state = "+this.oldSupervisor;
        }
        else
        {
            le.supervisor="";
        }
        this.dbl.add(le);
    }
    
    private boolean checkConditon(boolean addressFlag)
    {
        if (this.conditionIsContent)                        // the condition is for the content of an address pointed to by add reg
        {
            if (this.conditionAddressReg==-1)               // no addr reg?
                return false;
            if (!addressFlag)                               // we're not at the right address
                return false;
            int res=this.cpu.readMemoryLong(this.cpu.addr_regs[this.conditionAddressReg]);// get value
            return res==this.conditionValue;
        }
        
        if (this.conditionAddressReg!=-1)
        {
            return this.cpu.addr_regs[this.conditionAddressReg]==this.conditionValue;
        }
        if (this.conditionDataReg!=-1)  
        {
            if (this.cpu.data_regs[this.conditionDataReg]==this.conditionValue)
                return true;
        }
        return false;
    }
    
    /**
     * Sets the condition for a "g" command
     * @param dataReg the datareg this condition applies to, or -1 if condition is for address reg.
     * @param addrReg the addrreg this condition applies to, or -1 if condition is for data reg.
     * @param condValue the value to be met.
     * @param isContent possible content at...
     * @param address ...this address
     */
    public synchronized void setCondition (int dataReg,int addrReg,int condValue,boolean isContent,int address)
    {
        this.conditionValue=condValue;
        this.conditionDataReg=dataReg;
        this.conditionAddressReg=addrReg;
        this.conditionIsContent=isContent;
        this.conditionAddress=address;
    }
    
    /**
     * Sets whether the death of the thread should be loggined inthe MonitorGui log wdw.
     * 
     * @param b <code>true</code> if death should be logged.
     */
    public synchronized void signalDeath(boolean b)
    {
        this.signalDeath=b;
    }
    
    /**
     * Sets whether memory check should be switched on or off.
     * 
     * @param b true if memory check is the switched on.
     */
    public synchronized void setckmem(boolean b)
    {
        this.ckmem=b;
    }
    
    /**
     * Sets the mmemory to be watched.
     * 
     * @param a the mmemory to be watched : if content of memory location is changed the emulation breaks (in monitor mode).
     */
    public synchronized void setmemWatched(int a)
    {
        this.oldval=this.cpu.readMemoryLong(a);
        this.memWatched=a;
    }
     
    /**
     * Sets the upper limit of memory when logging.
     * 
     * @param address the upper limit of memory when logging any instruction executed above this will not be logged.
     */
    public synchronized void setUpperLimit(int address)
    {
        this.upperLimit=address;
    }
    /*---------------------------- classes for debugging --------------------------------------*/
   
    /**
     * Element of debuglist
     */
    private class ListElement
    {
        public ListElement next=null;
        public String instruction="";
        public String memaddress="";
        public String memcontent="";
        public String regContentsData="";
        public String regContentsAddr="";
        public String supervisor;
    }
    
    /**
     * A round robin list : elements are added at end, if list size reached, the first element is discarded
     */
    private class DebugList
    {
        private int last;
        private final int maxsize;
        private final ListElement[] elmts;
        
        public DebugList(int maxsize)
        {
            this.maxsize=maxsize;
            this.elmts=new ListElement[maxsize];
        }
        
        public void clear()
        {
            this.last=-1;
        }
        
        public void add(ListElement el)
        {
            
            if (this.last!=-1 && el.memaddress.equals(this.elmts[this.last].memaddress))
                return;                                 // sometimes an interrupt  may cause an instruction to be treated twice  
            this.last++;
            if (this.last>=this.maxsize)
                this.last=0;   
            this.elmts[this.last]=el;
        }
        /**
         * Save list to file
         * @param filepath 
         */
        public void saveToFile(String filepath)
        {
            java.io.PrintWriter out=null;
            java.io.PrintWriter out2=null;
            ListElement el;
            int index=0;
            try 
            {
                out = new java.io.PrintWriter(new java.io.FileWriter(filepath)); 
                out2 = new java.io.PrintWriter(new java.io.FileWriter(filepath+"2"));
                int ptr=this.last+1;
                if (ptr>=this.maxsize)
                    ptr=0;
                while (ptr!=this.last)
                {
                    index=printElement(out,out2,this.elmts[ptr],index);
                    ptr++;
                    if (ptr>=this.maxsize)
                        ptr=0;
                }
                printElement(out,out2,this.elmts[this.last],index);
                out.close();
                out2.close();
            }
            catch (Exception e)
            {
               if (out!=null)
                {
                    try
                    {
                        out.close();   
                        if (out2!=null)
                            out2.close();                      
                    }
                    catch (Exception whatever)
                    { /*nop*/ }
                }
            }
        }
        
        /**
         * Prints an element.
         * 
         * @param out
         * @param out2
         * @param el
         * @param index
         * 
         * @return a code indicating how many spaces should be prepended before line, to make indentation.
         */
        private int  printElement(java.io.PrintWriter out,java.io.PrintWriter out2,ListElement el,int index)
        {
            if (el==null)
                return index;
            String pad="                                                                                                                                         "+
                    "  ";  
            String temp;
            if (index>0 && index<pad.length()-1)
                temp=pad.substring(0,index);
            else
                temp="";
            out.println(temp+"                                            "+el.regContentsData); // contents of regs before the instructions
            out.println(temp+"                                            "+el.regContentsAddr);
            out.println(temp+"                                            "+el.memcontent);
            out2.println(temp+el.instruction);
            out.println(temp+el.instruction);
            if (!el.supervisor.isEmpty())
            {
                out.println("  SUPERVISOR CHANGE " +el.supervisor);
                out2.println("  SUPERVISOR CHANGE " +el.supervisor);
            }
            if ((el.instruction.contains("bsr"))|| (el.instruction.contains("jsr")))
                index+=2;
            if ((el.instruction.contains("rts"))|| (el.instruction.contains("rte")))
                index-=2;
            if (index<0)
                index=0;
            return index;
        }
    }
}
