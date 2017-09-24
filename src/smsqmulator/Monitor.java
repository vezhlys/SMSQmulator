package smsqmulator;

/**
 * The "monitor" object for the smsqe emulator, controls / starts / stops the emulation thread.
 * This is a central piece of SMSQmulator. It has a double function :
 * 
 * 1 - it serves as a monitor/debugger for M68K machine code instructions.
 * 2 - It is this monitor that launches the thread executing the instructions and also serves as a go-between between different objects.
 * 
 * <p> This executes commands that are given to it. Somme commands are:
 * <ul>
 * <li>t = trace one instruction (also enter)
 *  <li>j : jump over next instruction (don't execute it).
 *  <li>g [number_of_Instructions] : execute the program, optionally for [number_of_Instructions]
 *  <li>b [address_of_breakpoint] show (no parameter) or set/unset (one parameter) a breakpoint.
 *  <li>d [address] : display memory, if no parameter is given start at current PC address
 *  <li>di [address] : disassemble instructions, if no parameter is given start at current PC address.
 * </ul>
 * and many more, see the showHelp() method.
 * <p>
 * Many of the methods of this object are just go-betweens between the <code>Monitor</code> object and other objects, such as the <code>MC68000Cpu</code>object.
 * This means that some methods are called from the emulation thread, wheres others are called from the Swing Event Dispatch thread.
 *
 * The monitor outo=puts to two windows (instruction and data windows) and has an input wdw, see the MonitorPanel object.
 * @see smsqmulator.Monitor#showHelp() 
 * @author and copyright (c) Wolfgang Lenerz 2012-2016. Very loosely based on Tony Headford's work, see his licence below.
 * @version 
 * 1.18
 * 1.17 setCopyScreen amended to suit jva_qlscremu ; setNamesForDrives; forceRemoval parameter to force "unmount" of 
 *      exiting drives and remount.
 * 1.16 setCopyScreen implemented, changeMemSize: provide for alternate CPU, reset cpu for floppy if cpu changed in changeMemSize.
 * 1.15 changeMemSize also called when new screen made, reset no longer changes screen.
 * 1.14 setScreenUpdateInterval implemented
 * 1.13 better char display in monitor itself
 * 1.12 use StringBuilder whenever appropriate
 * 1.11 call TrapDispatcher.reset() when reset.
 * 1.10 "g"o commmnd sets cpu.stopNow to 0 before executing.
 * 1.09 handle floppy disks separately ; device drivers are only loaded if they aren't disabled.
 * 1.08 the monitor no longer starts the emulation when loading the rom file, each of the guis does ;
 *      setLogger added (may set the loggers after creation).
 * 1.07 provides for beep volume setting.
 * 1.06 only input mouse and keys if rom loaded ok.
 * 1.05 pass screen position to TrapDispatcher when wdw is moved/resized. needs new SMSQE.
 * 1.04 changeMemSize makes a new interrupt server (when did that get lost?).
 * 1.03 caps lock status is written directly into system variables (!).
 * 1.02 Use timezone time adjustment and user configured additional time adjustment.
 * 1.01 "Wake" monitorGo thread if key typed.
 * 1.00 Throttle value is passed to this object, passed on to TrapDispatcher.
 * 0.10
 */
public class Monitor 
{
    protected smsqmulator.cpu.MC68000Cpu cpu;                   // the cpu we're monitoring
    private boolean watchBreakpoints;                           // flag whether we observe breakpoints
    private boolean logInstructions;                            // and whether we log the instructions traced (will make everything very slow)
    private javax.swing.JTextArea regLogger;                    // where we display
    private javax.swing.JTextArea dataLogger;               
    private static final String commandNames="tgjbddikgbrhqrasxcbcdi1di2d1d2mwc1c2drdr1dr2sculslck0ck1wmspewrdp";// what commands are recognized by the monotir
    private StringBuilder sbuffer;                              // avoid too much string creation
    private MonitorGoThread goThread=null;                      // the thread that does the actual emulation
    private static final int ERRORCODE=-123456789;              // an error code I use
    private java.util.ArrayList<Integer> breakpoints=new java.util.ArrayList<>();
    private String debugFilename;                               // where to put the logged istructions
    private QL50HzInterrupt ih =null;                           // the interrupt server I create
    private TrapDispatcher trapDispatcher;                      // where to dispatch calls to Java from the monitorGoThread.
    private int diPanelNbr=1;
    private javax.swing.JTextArea diTextArea;                   // the instruction window (to the right)
    private int dPanelNbr=2;
    private javax.swing.JTextArea dTextArea;                    // the data window (to the left)
    private String lastCommand="";
    private MonitorGui gui;
    private javax.swing.JTextField inputWdw;                    // the input wdw, where you enter commands
    private int conditionDataReg=-1;                            // these ...
    private int conditionAddressReg=-1;
    private int conditionAddress=-1;
    private int conditionValue;
    private boolean conditionIsContent;                         // ... determine when emulation stops at a condition
    private int upperLimit=0;
    private boolean checkmem=false;                             // do we check memory?
    private int memoryToBeWatched=0;
    private boolean fastMode;                                   // does cpu go "fast" or under the debugger?
    private boolean excludeSuper=false;
    private SampledSound sam;
    private SoundDevice sound;
    private FloppyDriver floppy;
    private final inifile.IniFile inifile;                      // file with default/configured values
    public static int TIME_OFFSET;                              // individual time offset.
       
    /**
     * Creates the object
     * 
     * @param cpu the cpu we're monitoring.
     * @param watchBreakpoints flag whether we observe breakpoints (<code>true</code> = yes).
     * @param logInstructions whether we log the instructions traced (will make everything very slow).
     * @param sam the sampled sound system object .
     * @param regLogger where we display registers.
     * @param dataLogger another display, for data.
     * @param fastmode  whether we're in fast mode (<code>true</code>) or not.
     * @param warnings object with warning flags.
     * @param throt throttle value to be passed on to TrapDispatcher.
     * @param tOffset time offset.
     * @param gui  the MonitorGui.
     * @param beepVolume how loud is the beep.
     * @param snd device for SSSS (Smsqe Sampled Sound System).
     * @param inifile the ".ini" file with initial and current values.
     */
    public Monitor (smsqmulator.cpu.MC68000Cpu cpu, boolean watchBreakpoints,boolean logInstructions,SampledSound sam,
                    javax.swing.JTextArea regLogger,javax.swing.JTextArea dataLogger,boolean fastmode,
                    Warnings warnings,int throt,int tOffset,MonitorGui gui,int beepVolume,SoundDevice snd,inifile.IniFile inifile)    
    {
        this.cpu=cpu;
        this.watchBreakpoints=watchBreakpoints;
        this.logInstructions=logInstructions;
        this.regLogger=regLogger;
        this.dataLogger=dataLogger;
        this.sbuffer = new StringBuilder(240);
        this.sam=sam;
        this.gui=gui;
        this.sound=snd;
        this.inifile=inifile;
        this.diTextArea=getPanel(this.diPanelNbr);
        this.dTextArea=getPanel(this.dPanelNbr);
        this.fastMode=fastmode;
        
        this.ih=new QL50HzInterrupt(0,this.cpu,this.cpu.screen); // interrupt server (registers to cpu automatically)
        this.trapDispatcher=new TrapDispatcher(this.sam,throt,gui,beepVolume,snd,inifile,new IPHandler());
        if (!this.inifile.getTrueOrFalse("DISABLE-WIN-DEVICE"))
            this.trapDispatcher.register(new WinDriver(cpu,warnings));// WIN driver is the first to be initialised
        if (!this.inifile.getTrueOrFalse("DISABLE-MEM-DEVICE"))
            this.trapDispatcher.register(new MemDriver(cpu,warnings));
        if (!this.inifile.getTrueOrFalse("DISABLE-NFA-DEVICE"))
            this.trapDispatcher.register(new NfaDriver(cpu));
        if (!this.inifile.getTrueOrFalse("DISABLE-SFA-DEVICE"))
            this.trapDispatcher.register(new SfaDriver(cpu));
        if (!this.inifile.getTrueOrFalse("DISABLE-FLP-DEVICE"))
        {
            this.floppy = new FloppyDriver (this.cpu,inifile);
            this.trapDispatcher.setFloppy(this.floppy);
        }
  /*  
        SWinDriver pw=new SWinDriver(this.cpu,false);
        this.trapDispatcher.setSwin(pw);
    */    
        try
        {
            this.debugFilename=(System.getProperty( "user.home" )+java.io.File.separator+"SMSQmulatorDebug.txt");// log file
        }
        catch (Exception e)
        {/*nop*/}                                               // if I can't get the debug log file, then that's not the end of the world                   
        
        java.util.TimeZone tz = java.util.TimeZone.getDefault();
        Monitor.TIME_OFFSET = (int) (tz.getOffset(System.currentTimeMillis())/1000) + Types.DATE_OFFSET+tOffset;
        if (this.regLogger==null)                               // make sure this exists
            this.regLogger=this.dataLogger;
        if (this.regLogger==null)
        {
            this.regLogger=new javax.swing.JTextArea();         // if it still doesn't create both
            this.dataLogger=this.regLogger;
        }
    }

    /*-------------------------------------------------------------- Monitor commands themselves ---------------------------------------------------------*/
    
    /**
     * Executes a command.
     * <p> See the help for what commands are recognized by the monitor.
     * 
     * @param command a command line : command [options]:
     * <p>Commands are single or double letters, possibly followed by options.
     * <p>Some commands are (there are more,see the help method):
     * <ul>
     * <li>   h - Help: show help.
     * <li>   g - Go (start monitoring) &lt;start_address (default current PC)&gt; &lt;number_of_instructions to go (default unlimited)&gt;.
     * <li>   gb- Go to a provisional breakpoint. One parameter: the breakpoint.
     * <li>   r - Registers : show content of registers.
     * <li>   a - check whether the go thread is still running.
     * <li>   di- Disassemble instructions &lt;start_address (default current PC)&gt; &lt;number_of_instructions (default : 8)&gt;.
     * <li>   j - Jump over current instruction (do not execute, advance PC).
     * <li>   b - Breakpoint. No parameters = show breakpoints , parameter= set breakpoint at that addresss, or unset it if it already is set at that address.
     * <li>   t - Trace (execute) one instruction at the current PC.
     * <li>   d - display memory content &lt;start_address (default current PC)&gt; &lt;number_of_bytes (default : 16)&gt;.
     * <li>   k - kill current Go thread if it exists.
     * <li>   s - set a register to a value: s &lt;reg&gt; &lt;value&gt; or s &lt;reg&gt; &lt;value&gt;.
     * <li>   bc - clear all breakpoints.
     * <li>   qr- quickly execute the following instruction and set a breakpoint to the one after that. Useful for JSR/BSR/Trap.
     * </ul>
     * <p> Options are separated from the command and each other by single spaces.
     * <p> Numbers can be given in decimal or hex format. The default is hex, decimals must be prefixed by "&amp;".
     * <p> An address can also be given by register number enclosed in parenthesis, eg (a7) or (d0) and also (pc).
     * <p> The address will then be that of the content of the register, so, if a7= $100, 'di (a7)' will disassemble instructions as of address $100.
     * <p> An address can also be calculated with + or - , e.g. *-10 or (a0)+$100.
     * @see smsqmulator.Monitor#showHelp() 
     */
    public void executeMonitorCommand (String command)
    {
        if (command==null)
            return;                                         // no nonsense, please
        command=command.toLowerCase().trim();                 
        if (command.isEmpty())
            command=this.lastCommand;                       // simple enter = do last command again
        if (command.isEmpty())
            return;                                         // if no last command, just do nothing
        int t=command.indexOf("  ");                        // any double spaces?
        while (t!=-1)
        {
            command=command.replace("  "," ");              // make sure there are no double spaces at all
            t=command.indexOf("  ");
        }
        String [] commands=command.split(" ");
        if (commands.length==0)
            return;                                         // how could that be?
        int commandIndex=Monitor.commandNames.indexOf(commands[0]);
        if (commandIndex==-1 && command.startsWith("s"))
            commandIndex=15;
        switch (commandIndex)
        {
            case 0:                                         // "t"race
                int time = cpu.execute();
                cpu.stopNow=0;
                showInfo(true,this.diTextArea);
                this.lastCommand=command;
                break;
            case 1:                                         // "g"o
                this.fastMode=false;
                cpu.stopNow=0;
                goCommand(commands,-1);
                this.lastCommand="";
                break;
            case 2:                                         // "j"ump over the next command
                int address=this.cpu.pc_reg*2;
                int opcode =  this.cpu.readMemoryWord(address);// opcode at current address
                smsqmulator.cpu.Instruction i = this.cpu.getInstructionFor(opcode);
                smsqmulator.cpu.DisassembledInstruction di = i.disassemble(address, opcode,this.cpu);
                address+=di.size();                         // total size of instrauction at that address
                this.cpu.pc_reg=address/2;                  // set PC to that address
                showInfo(true,this.diTextArea);             // show new state
                this.lastCommand="";
                break;
            case 3:                                         // "b"reakpoint (show, set or delete)
                setBreakpoints(commands);
                this.lastCommand="";
                break;
            case 4:                                         // "d"isplay mem
                dispMem(commands);
                this.lastCommand=command;
                break;
            case 5:                                         // "di"nstructions
                diCommand(commands);
                this.lastCommand=command;
                break;
            case 7:                                         // "k"ill go thread
                waitForGoThreadToDie();                     // if there is a thread, tell it to stop
                this.lastCommand="";
                break;
            case 8:                                         // "GB" - go till breakpoint (one option : the breakpoint)
                if (commands.length>1)
                {
                    address=parseForInteger(commands[1]);
                    if (address!=Monitor.ERRORCODE)
                    {
                        this.fastMode=false;
                        goCommand(null,address);
                    }
                }
                this.lastCommand="";
                break;
            case 10:                                        // "R" : show registers
            case 38:                                        // "DR1": set wdw
                this.diTextArea=this.dataLogger;
            case 36:                                        // "DR"
                showInfo(true,this.diTextArea);
                this.lastCommand="";
                break;
            case 11:                                        // "H"elp
                showHelp();
                this.lastCommand="";
                break;
            case 12:                                        // "qr" trace over.
                traceOver();
                this.lastCommand="t";                       // and pretend last command was a trace
                break;
            case 14:                                        // "A" -status of gothread
                if (this.goThread!=null)
                    this.regLogger.append("Go thread still running...!\n");
                else
                    this.regLogger.append("Go thread is stopped.\n");
                this.lastCommand="";
                break;
            case 15:                                        // "s" set a register with a value
                setRegister (commands);
                this.lastCommand="";
                break;
            case 16:                                        // X just execute the emulation as fast as possible
                this.fastMode=true;
                goCommand(commands,-1);
                this.lastCommand="";
                break;
            case 17:                                        // "c"
                this.dataLogger.setText("");
                this.regLogger.setText("");
                break;
            case 18:                                        // "BC" clear breakpoints
                this.breakpoints.clear();
                this.lastCommand="";
                this.regLogger.setText("All breakpoints cleared");
                break;
            case 20:
            case 23:                                        // di1 or di2
                dixCommand(commands);
                this.lastCommand=command;
                break;
            case 26:
            case 28:                                        // d1 or d2
                dispXMem(commands);
                this.lastCommand=command;
                break;
            case 30:                                        // "mw"
		setMem(commands);
                break;
            case 32:                                        // "c1"
                this.dataLogger.setText("");
                break;
            case 34:                                        // "c2"
                this.regLogger.setText("");
                break;
            case 41:                                        // "dr2"
                showInfo(true,this.regLogger);
                this.lastCommand="";
                break;
            case 44:                                        // "SC" set condition
                parseCondition (commands);
                break;
            case 46:                                        // "UL" set upper limit
                setUpperLimit(commands);
                break;
            case 48:                                        //"SL" switch logging
                this.logInstructions=!this.logInstructions;
                this.regLogger.setText("Logging is now "+ (this.logInstructions? "ON" : "OFF")+"\n");
                if (this.goThread!=null)
                    this.goThread.switchLogging(this.logInstructions);
                break;
            case 50:                                        // ck0 = stop checking memory
                this.checkmem=false;
                if (this.goThread!=null)
                    this.goThread.setckmem(false);
                break;
            case 53:        
                this.checkmem=true;                         // ck1 start checking memory
                if (this.goThread!=null)
                    this.goThread.setckmem(true);
                break;
            case 56:                                        // "wm" watch memory
                setMemToBeWatched(commands);
                break;
            case 58:                                        // "sp" special - does nothing any more, see bits of code after end of object
           //     special();
                break;
            case 60:
                excludeRom();
                break;                                      // e - exclude rom calls
            case 61:
              //  writeFile();
                break;                                      // wr write NIY
            case 63:                                        // dp display last 10 pc values
                this.regLogger.setText("");
                for (int k=0;k<10;k++)
                {
                    this.regLogger.append(String.format("%08x", this.cpu.pcs[k]*2).toUpperCase()+"\n");
                }
        }
    }
    
    private void setMemToBeWatched(String []options)
    {
        if (options.length!=2)
        {
            showError ("wm - wrong ","wm <address>",options);
            return;
        } 
        int address=parseForInteger(options[1]);
        if (address==Monitor.ERRORCODE)
        {
            showError ("wm - wrong ","wm <address>",options);
            return;
        }
        this.memoryToBeWatched=address; 
        if (this.goThread!=null)
            this.goThread.setmemWatched(address);
    }
    
    private void setUpperLimit(String []options)
    {
        if (options.length!=2)
        {
            this.regLogger.setText("Upper limit = "+String.format("%08x",this.upperLimit)+"\n");
            return;
        } 
        int address=parseForInteger(options[1]);
        if (address==Monitor.ERRORCODE)
        {
            showError ("ul - wrong ","ul <address>",options);
            return;
        }
        this.upperLimit=address;
        if (this.goThread!=null)
            this.goThread.setUpperLimit(address);
    }
    
    private void excludeRom()
    {
        int address=this.cpu.readMemoryLong(0x28020);
        this.upperLimit=address;
        if (this.goThread!=null)
        {
            this.goThread.setUpperLimit(address);
            this.goThread.switchSuper(true);
        }
        this.regLogger.setText("Exclude ROM : upper limit set to "+String.format("%08x",this.upperLimit)+"\n");
        this.excludeSuper=true;
    }
    
    private void setMem(String []options)
    {
        if (options.length!=3)
        {
            showError ("mw - wrong number of instructions","mw <address> <number>",options);
            return;
        }
        int address=parseForInteger(options[1]);
        int mem=parseForInteger(options[2]);
        if (address==Monitor.ERRORCODE || mem ==Monitor.ERRORCODE)
        {
            showError ("mw - wrong number of instructions","mw <address> <number>",options);
            return;
        }
        
        this.cpu.writeMemoryWord(address, mem&0x0000ffff);
        showInfo(true,this.diTextArea);
    }
    
    /**
     * QR = Traces over the instruction until the next instruction.
     * <p> If the instruction is a jump, this will trace until the rts.
     */
    private void traceOver()
    {
        int address=this.cpu.pc_reg*2;
        smsqmulator.cpu.DisassembledInstruction di = getDI(address);// get instruction (and its size)
        int stop=address+di.size();
        while (address!=stop)
        {
            int ret=this.cpu.execute();
            if (ret<-1)
                 this.trapDispatcher.dispatchTrap(ret,this.cpu);
            address=this.cpu.pc_reg*2;
        }
        showInfo(true,this.diTextArea);
    }
    
    /**
     * Handles the "go" command : execute all instructions until either breakpoint reached or the optional nbr of instructions is reached.
     * <p>The Go command is handled by a separate thread and can thus be stopped with the "k" commmand.
     * 
     * @param options Options[1] optionally contains the number of instructions to execute.
     * @param provbkp a provisional break for the "GB" command.
     */
    public void goCommand(String []options,int provbkp)
    {
        int nbrInst=0;
        if (options!= null && options.length>1)             // any options?
        {
            nbrInst=parseForInteger(options[1]);            // yes, try to get the number of instructions
            if (nbrInst==Monitor.ERRORCODE)
            {
                showError ("g - wrong number of instructions","g <nbr of instructions>",options);
                return;
            }
        }
        if (this.goThread!=null)                            // don't execute the Go thread twice!
        {
            this.regLogger.setText("Error : Already or still running (go command? - may be killed with k)");
        }
        else
        {
            this.goThread=new MonitorGoThread(nbrInst,this.cpu,this.breakpoints,this,this.debugFilename,this.watchBreakpoints, 
                    this.logInstructions,this.ih,this.fastMode,this.trapDispatcher,this.upperLimit,this.checkmem,this.memoryToBeWatched,provbkp,this.excludeSuper);
            setCondition();
            this.goThread.start();
            setFocusToEmulScreen();
        }
    }
    
    /**
     * di1 or di2 : Disassembles instructions in the text area given by the command.
     * @param options    Options[1] optionally contains the address where to start disassembling,
     * <p>                 Options[2] optionally contains the number of instructions to disassemble (default=16).
     */
    private void dixCommand(String []options)
    {
        int panelNbr=1;
        try
        {
            panelNbr=Integer.parseInt((options[0].substring(2)));// get panel nbr
        }
        catch (Exception e)
        { /*NOP*/ }                                         // keep default 1
        this.diTextArea=getPanel(panelNbr);                 // set textarea to the one wished
        diCommand(options);        
    }

    /**
     * Disassembles Instructions.
     * @param options   Options[1] optionally contains the address where to start disassembling,
     * <p>                 Options[2] optionally contains the number of instructions to disassemble (default=16).
     */
    private void diCommand(String []options)
    {
        int nbrInst=16;
        int address=this.cpu.pc_reg*2;
        if (options.length>1)
        {
            address=parseForInteger(options[1]);        // try to get the start address
            if (address==Monitor.ERRORCODE || address<0)
            {
                showError ("di - wrong start address","di <start_address> [<nbr of bytes>]",options);
                return;
            }
        }
        if (options.length>2)
        {
            nbrInst=parseForInteger(options[2]);        // try to get the number of instructions
            if (nbrInst==Monitor.ERRORCODE)
            {
                showError ("di - wrong number of bytes","di <start_address> [<nbr of bytes>]",options);
                return;
            }
        }
        //this.diTextArea.setText("");
        StringBuilder sb = new StringBuilder(80);
        int memsize=this.cpu.readableSize();
        for (int i=0;i<nbrInst && address<memsize;i++)
        {
            sb.delete(0, 80);
            smsqmulator.cpu.DisassembledInstruction di = getDI (address);
            di.shortFormat(sb);//di.formatInstruction(buffer);
            this.diTextArea.append(sb.toString().toUpperCase()+"\n");
            address += di.size();
        }
    }
    
    /**
     * d1 or d2 : Displays memory the text area given by the command.
     * @param options Options[1] optionally contains the address where to start showing mem
     *                Options[2] optionally contains the number of bytes to show.
     */
    private void dispXMem(String []options)
    { 
        int panelNbr=1;
        try
        {
            panelNbr=Integer.parseInt((options[0].substring(1)));// get panel nbr
        }
        catch (Exception e)
        { /*NOP*/ }                                         // keep default 1
        this.dTextArea=getPanel(panelNbr);                 // set textarea to the one wished
        dispMem(options);  
    }

    /**
     * Displays memory.
     * @param options Options[1] optionally contains the address where to start showing mem
     *                Options[2] optionally contains the number of bytes to show.
     */
    private void dispMem(String []options)
    {
        int nbrBytes=16 *16;                                // 16 ROWS @ 16 BYTES        
        int address=this.cpu.pc_reg*2;
        if (options.length>1)
        {
            address=parseForInteger(options[1]);            // try to get the start address
            if (address==Monitor.ERRORCODE || address<0)
            {
                showError ("d - wrong start address","d <start_address> [<nbr of bytes>]",options);
                return;
            }
        }
               
        if (options.length>2)
        {
            nbrBytes=parseForInteger(options[2]);        // try to get the number of bytes
            if (nbrBytes==Monitor.ERRORCODE)
            {
                showError ("d - wrong number of bytes","d <start_address> [<nbr of bytes>]",options);
                return;
            }
        }
        
        
        StringBuilder sb = new StringBuilder(80);
        int end=address+nbrBytes;
        
        StringBuilder p = new StringBuilder(80);
        int b;
        char c;
        while (address<end)
        {
            sb.delete(0,80);
            p.setLength(0);
            p.append("  ");
            sb.append(String.format("%08x", address).toUpperCase()).append(" ");
            for (int i=0;i<8 && address<end; i++)
            {
                if (i==4)
                    sb.append(" ");
                b = cpu.readMemoryByte(address++);
                c=(b<32 || b>126)?'.':(char)b;
                sb.append(String.format("%02x", b).toUpperCase()); 
                p.append(c);
                b = cpu.readMemoryByte(address++);
                c=(b<32 || b>126)?'.':(char)b;
                sb.append(String.format("%02x", b).toUpperCase()); 
                sb.append(" "); 
                p.append(c);
               
            }
            this.dTextArea.append (sb.toString()+p+"\n");
        }
    }
    
    /**
     * Sets a register to a value.
     * @param commands 
     */
    private void setRegister (String [] commands)
    {
        if (commands.length<2)
        {
            showError ("setRegister","s<register> <content> or s <register> <content>",commands);
            return;
        }
        String reg,value;
        if (commands[0].length()!=1)                        // is it "sxx" or "s xx"?
        {
            reg=commands[0];
            if (reg.length()!=3)                        
            {
                showError ("setRegister","s<register> <content> or s <register> <content>",commands);
                return;
            }
            reg=reg.substring(1);
            value=commands[1];
        }
        else
        {
            if (commands.length<3)
            {
                showError ("setRegister","s<register> <content> or s <register> <content>",commands);
                return;
            }
            reg=commands[1];
            value=commands[2];
        }
        // here now reg=the register, value = the value;
        int val=parseForInteger(value);
        if (val==Monitor.ERRORCODE || reg.length()!=2)
        {
            showError ("setRegister","s<register> <content> or s <register> <content>",commands);
            return;
        }
        int register="01234567".indexOf(reg.substring(1));
        if (register==-1 && (!reg.equals("pc")))
        {
            showError ("setRegister","s<register> <content> or s <register> <content>",commands);
            return;
        }
        int type="adp".indexOf(reg.substring(0,1));
        switch (type)
        {
            case 0:
                this.cpu.addr_regs[register]= val;
                showInfo(true,this.diTextArea);
                break;
            case 1:
                this.cpu.data_regs[register]= val;
                showInfo(true,this.diTextArea);
                break;
            case 2:
                this.cpu.pc_reg=val/2;
                break;
            default:
                showError ("setRegister","s<register> <content> or s <register> <content>",commands);
                break;
        }
    }
      
    /**
     * Sets/unsets a breakpoint.
     * @param options 
     */
    private void setBreakpoints(String []options)
    {
        if (this.breakpoints==null)
            this.breakpoints=new java.util.ArrayList<>();
        if (options.length==1)
        {
            this.regLogger.setText("Breakpoints:");
            for (int i: this.breakpoints)
                this.regLogger.append(String.format("%08x (%d)",i,i).toUpperCase()+"\n");
        }
        else
        {
            int address=parseForInteger(options[1]);
            if (address==Monitor.ERRORCODE)
            {
                showError("breakpoint","b [< address>]",options);
                return;
            }
           
            if (this.breakpoints.contains(address))
            {
                this.breakpoints.remove((Integer)address);
                this.regLogger.append(String.format("Breakpoint removed at $%08x\n",address).toUpperCase());
            }
            else
            {
                this.breakpoints.add(address);
                this.regLogger.append(String.format("Breakpoint set at %08x\n",address).toUpperCase());
            }
        }
    }


    /**
     * Parses the condition : sc <condition>. SC alone resets the condition.
     * condtiion is regt =value,
     * eg a6=x..
     * d6=x..
     * OR!!! c4=x...
     * 
     * OR x=(address) sets the address for the condition
     * 
     * where c4 then denotes that this is the content of address reg 4
     */
    private void parseCondition(String []options)
    {
        if (options.length==1)                              // "sc" alone
        { 
            this.conditionDataReg=-1;
            this.conditionAddressReg=-1;
            this.conditionValue=0;  
            this.conditionAddress=-1;
            this.conditionIsContent=false;
            setCondition();
            return;
        } 
        if (options.length!=2)                              
        {
            showError("set condition","sc <condition>",options);
            return;
        }
        String cond=options[1];                             // the coondition
        if (cond==null || cond.isEmpty())                   // must't be empty
        {
            showError("set condition","sc <condition>",options);
            return;
        }
        String []condvals=cond.split("=");
        if (condvals==null || condvals.length!=2)           // condition format is reg=value
        {
            showError("set condition","sc <condition>",options);
            return;
        }
        String condreg=condvals[0].toLowerCase().trim();    // get reg  
        int value=parseForInteger(condvals[1].trim());      // get value
        if (condreg.length()!=2 || value==Monitor.ERRORCODE)
        {
            showError("set condition","sc <condition>",options);
            return;
        }
        int regtype="adcx".indexOf(condreg.substring(0,1));      // which type of reg is it?
        if (regtype<0)
        {
            showError("set condition","sc <condition>",options);
            return;
        }
        int temp=parseForInteger(condreg.substring(1));     // reg number
        if (temp==Monitor.ERRORCODE || temp>7 || temp<0)    // must be between 0 & 7 
        {
            showError("set condition","sc <condition>",options);
            return;
        }
        if (regtype==0)                                     // condition is for address reg
        {
            this.conditionDataReg=-1;
            this.conditionAddressReg=temp;
            this.conditionValue=value;
            this.conditionIsContent=false;
            setCondition();
        }
        else if (regtype==1)
        {
            this.conditionDataReg=temp;
            this.conditionAddressReg=-1;
            this.conditionValue=value;
            this.conditionIsContent=false;
            setCondition();
        }
        else if (regtype==2)
        {
            this.conditionDataReg=-1;
            this.conditionAddressReg=temp;
            this.conditionValue=value;
            this.conditionIsContent=true;
            setCondition();
        }
        
        else if (regtype==3)
        {
            this.conditionAddress=value;
            setCondition();
        }
    }
    
    /**
     * Sets a condition (reg=value).
     */
    private void setCondition()
    {
        if (this.goThread!=null)
        {
            this.goThread.setCondition (this.conditionDataReg,this.conditionAddressReg,this.conditionValue,
                    this.conditionIsContent,this.conditionAddress);
        }
    }
    
    /**
     * Sets the log windows.
     * 
     * @param reglogger where to show the register contents.
     * @param datalogger where to show data, e.g. memory content.
     */
    public void setLoggers (javax.swing.JTextArea reglogger,javax.swing.JTextArea datalogger)
    {
        this.regLogger=reglogger;
        this.dataLogger=datalogger;
        this.diTextArea=getPanel(this.diPanelNbr);
        this.dTextArea=getPanel(this.dPanelNbr);
    }               

    /**
     * Shows the help.
     */
    private void showHelp()
    {
        this.dataLogger.setText("");
        this.dataLogger.append("Commands are single or double letters, possibly followed by options.\n");
        this.dataLogger.append("Commands are:\n");
        this.dataLogger.append("  h - Help: show help.\n");
        this.dataLogger.append("  g - Go (start monitoring) <start_address (default current PC)> <number_of_instructions to go (default unlimited)>.\n");
        this.dataLogger.append("  gb- Go to a provisional breakpoint. One paramter: the breakpoint.\n");
        this.dataLogger.append("  r and dr - display registers : show content of all registers (no parameter).\n");
        this.dataLogger.append("  dr1 and dr2 - display registers in window 1 or 2\n");
        this.dataLogger.append("  a - check whether the go thread is still running.\n");
        this.dataLogger.append("  di- Disasssemble instructions <start_address (default current PC)> <number_of_instructions (default : 8)>.\n");
        this.dataLogger.append("      This can also be 'di1' or 'di2' to choose the display area where the result will be shown\n");
        this.dataLogger.append("  j - Jump over current instruction (do not execute, advance PC to next instruction).\n");
        this.dataLogger.append("  b - Breakpoint. No parameters = show breakpoints , parameter= set breakpoint at that addresss, or unset it, if it already is set at that address.\n");
        this.dataLogger.append("  t - Trace (execute) one instruction at the current PC.\n");
        this.dataLogger.append("  d - display memory content <start_address (default current PC)> <number_of_bytes (default : 16)>.\n");
        this.dataLogger.append("      This can also be 'd1' or 'd2' to choose the display area where the result will be shown\n");
        this.dataLogger.append("  k - kill current Go thread if it exists.\n");
        this.dataLogger.append("  s - set a register to a value: s<reg> <value> or s <reg> <value>.\n");
        this.dataLogger.append("  bc - clear all breakpoints.\n");
        this.dataLogger.append("  qr- quickly execute the following instruction & set a breakpoint to the one after that. Useful for JSR/BSR/Trap.\n");
        this.dataLogger.append("      This does NOT stop at any normal breakpoints.\n");
        this.dataLogger.append("  mw- change a word in memory : mw <address> <new word content>.\n");
        this.dataLogger.append("  ck0, ck1 swicth watching memory on (ck1) or off (ck2). When on, and watched memory != 0 any change in the long value in that memory location will trigger a breakpoint.\n");
        this.dataLogger.append("  wm - watched memory : wm <address> Sets the memory location that will be watched..\n");
        this.dataLogger.append("  sc- set a condition : sc <condition> <address> sets the condition at that address. a condition is <register>=<value> WITHOUT spaces in between, eg a0=28000\n");
        this.dataLogger.append("  ul- set upper limit : when logging instructions, if the pc is beyond this limit, instructions will not be logged.\n");
        this.dataLogger.append("  e  - exclude ROM code from logging (switch on/off).\n");
        this.dataLogger.append("  sl - switch logging instructions from yes to no to yes etc When switched on and then off, the logged isntrcutions are .\n");
        this.dataLogger.append("       written into a log file, by default this is (user home)+SMSQmulatorDebug.txt.\n");
        this.dataLogger.append("  wf - write file : write the logging file immediately.\n");
        this.dataLogger.append("  dp - display the previous 10 PCs - uncomment the relevant section in the CPU.execute method!!!!\n");
        this.dataLogger.append("  wf - write file : write the logging file immediately.\n");
        this.dataLogger.append("  ck1 & ck0 start/stop checking memory.\n");
        this.dataLogger.append("  sp - special, could be anything.\n");
        this.dataLogger.append("  sp - special, could be anything.\n");
        this.dataLogger.append("A simple ENTER will repeat the last t, d or di command.\n");
        this.dataLogger.append("Options are separated from the command and each other by single spaces.\n");
        this.dataLogger.append("Numbers can be given in decimal or hex format. The default is hex, decimals must be prefixed by '&'.\n");
        this.dataLogger.append("The star '*' can be used for an address, this then is the current PC \n");
        this.dataLogger.append("An address can also be given by register number enclosed in parenthesis, eg (a7) or (d0) and also (pc):\n");
        this.dataLogger.append("The address will then be that of the content of the register, so, if a7= $100, 'di (a7)' will disassemble instructions as off address $100.\n");
        this.dataLogger.append("An address can also be calculated with + or - , e.g. *-10 or (a0)+$100.\n");
        this.dataLogger.append("An address can also be calculated with a displacement eg 10(a0) and combined regs eg. 10(a3,a6). Both regs will be long.\n");
        
    }


/*---------------------------------------------------  Monitor commands subroutines -------------------------------------------------------*/
    /**
     * Shows that an error occurred in a command.
     * 
     * @param command the command with the error.
     * @param correct the correct way to use the command.
     * @param options the command line that was used instead.
     */
    private void showError(String command, String correct,String []options)
    {
        StringBuilder p=new StringBuilder();
        for (String m:options)
            p.append(m).append(" ");
        this.regLogger.setText("Error in instruction -"+command+" : "+p.toString()+"\n");
        this.regLogger.append("Usage : "+correct+"\n");
    }
    
    /**
     * Parses a string for an integer.
     * 
     * @param s the string to parse as integer. This may be a hex integer or a decimal (it is then prefixed by "&", e.g. &0123),
     *        or the char "*" which then means the current PC address of the CPU,
     *        or a data or address reg in parentheses (eg :" (a7) " and also (a6,a7)).  
     *        or any of the above and a dispalcement eg *+45 or even (a7)+(a6);
     * 
     * @return the result. This will be -123456789 if there was any problem with the string.
     */
    private int parseForInteger(String s)
    {
        int result=Monitor.ERRORCODE;                       // signal error
        if (s==null || s.isEmpty())
            return result;                                  // null or empty string  - oops
        s=s.trim(); 
        if (s.isEmpty())
            return result;                                  // string was only spaces - oops
        
        String additionString;
        int addedValue=0,temp;
        int t=s.indexOf("(");                               // any index or regs?
        if (t>0)
        {
            addedValue=parseForInteger(s.substring(t));
            if (addedValue==Monitor.ERRORCODE)
                return addedValue;
            s=s.substring(0,t);
        }
        
        boolean minus=false;
        t=s.indexOf("-");                                   // any minus sign in string?...
        if (t==-1)
            t=s.indexOf("+");                               // ...no, any plus sign?
        else
            minus=true;                                     // ....yes, there was a minus sign
        if (t!=-1)                                          // was there a + or - sign?
        {
            if (t+1>=s.length())                            // plus or minus sign at end? error
                return result;                      
            additionString=s.substring(t+1);                // the string, stripped of the sign
            temp=parseForInteger(additionString);     // get as address (or offset)
            if (temp==Monitor.ERRORCODE)
                return temp;      
            if (minus)
                temp*=-1;
            addedValue+=temp;
            s=s.substring(0,t);
            if (s.isEmpty())
                return addedValue;
        }
            
        if (s.equals("*"))
            return this.cpu.pc_reg*2+addedValue;
        if(s.startsWith("("))                               // is it something like "(a7)"?
        {
            if (!(s.endsWith(")")))                         // string not enclosed in ()?
                return result;
            t=s.indexOf(",");                               // composed registers eg. (a6,a3)?       
            if (t==-1)                          
            {                                               // no, single reg
                if(s.length()!=4)
                    return result;                          // oops, something wrong
                String a=s.substring(1,2);                  // the register, either "a" or "d" or "p"
                if (!("adp".contains(a)))                   // but it isn't - oops
                    return result;
                try
                {
                    if (a.equals("p"))
                        return this.cpu.pc_reg*2+addedValue; // reg was (pc)
                    result=Integer.parseInt(s.substring(2,3));// get the register number
                    if (result<0 || result>7)
                        return Monitor.ERRORCODE;           // regs go from 0 to 7 only
                    if (a.equals("a"))
                        return this.cpu.addr_regs[result]+addedValue;// get content of reg
                    else
                        return this.cpu.data_regs[result]+addedValue;

                }
                catch(Exception e)
                {
                    return Monitor.ERRORCODE;               // register number isn't
                }
            }
            else                                            // composed regs eg (a6,a1) - they will always be taken as long
            {   
                if ((t!=3) || s.length()!=7)
                    return result;                          // something wrong
                t=parseForInteger("("+s.substring(1,3)+")");// get value of first reg
                if (t==result)                              // oops
                    return result;
                result=parseForInteger("("+s.substring(4,6)+")");// second regget its value
                if (result==Monitor.ERRORCODE)
                    return result;                          // oops
                return t+result+addedValue;                 // return combined values
            }
        }
        int radix=16;                                       // signals hex number as default
        if (s.startsWith("$"))
        {
            if (s.length()==1)                              // string contains only a "$"
                return result;
            s=s.substring(1);
            radix=16;
        }
        else if (s.startsWith("&"))
        {
            if (s.length()==1)                              // string contains only a "&"
                return result;
            s=s.substring(1);
            radix=10;
        }
        try
        {
            boolean isNeg=false;
            if (s.length()>8 && radix==16)
                return result;                              // hex values only accept 8 chars
            if (s.length()==8 && radix ==16)
            {
                String m="0"+s.substring(0,1);              // get first nibble of 32 bit number
                try
                {
                    temp=Integer.parseInt(m,radix);
                }
                catch (Exception e)
                {
                    return result;
                }
                if (temp>7)
                {
                    isNeg=true;
                    temp-=8;
                    s=temp+s.substring(1);
                }
            }
            result=Integer.parseInt(s,radix);
            if (isNeg)
                result |=0x80000000;
        }
        catch (Exception e)
        {
            return Monitor.ERRORCODE;
        }
        return result+addedValue;
    }
    
    /**
     * Shows the content of the regs and the next instruction.
     * 
     * @param clearArea should the text area be cleared before info is shown?
     * @param textArea where to show the info.
     */
    public void showInfo(boolean clearArea,javax.swing.JTextArea textArea)
    {
        if (textArea==null)
            textArea=this.diTextArea;
        if(clearArea)
            textArea.setText("");
        textArea.append(String.format("D0:%08x  D1:%08x  D2:%08x  D3:%08x  PC:%08x\n",
                        this.cpu.data_regs[0], this.cpu.data_regs[1], this.cpu.data_regs[2], this.cpu.data_regs[3],this.cpu.pc_reg*2).toUpperCase());
        textArea.append(String.format("D4:%08x  D5:%08x  D6:%08x  D7:%08x  SR:%04x %s\n",
                        this.cpu.data_regs[4], this.cpu.data_regs[5], this.cpu.data_regs[6], this.cpu.data_regs[7],this.cpu.reg_sr, showFlags()).toUpperCase());
        textArea.append(String.format("A0:%08x  A1:%08x  A2:%08x  A3:%08x  USP:%08x\n",
                        this.cpu.addr_regs[0], this.cpu.addr_regs[1], this.cpu.addr_regs[2], this.cpu.addr_regs[3],this.cpu.reg_usp).toUpperCase());
        textArea.append(String.format("A4:%08x  A5:%08x  A6:%08x  A7:%08x  SSP:%08x\n\n",
                        this.cpu.addr_regs[4], this.cpu.addr_regs[5], this.cpu.addr_regs[6], this.cpu.addr_regs[7],this.cpu.reg_ssp).toUpperCase());
        sbuffer.delete(0, sbuffer.length());
        int address = this.cpu.pc_reg*2;
        if(address < 0 || address >= this.cpu.readableSize())
        {
            sbuffer.append(String.format("%08x   ????", address));
        }
        else
        {
            int opcode = this.cpu.readMemoryWord(address);
            smsqmulator.cpu.Instruction i = cpu.getInstructionFor(opcode);
            smsqmulator.cpu.DisassembledInstruction di = i.disassemble(address, opcode,this.cpu);
            di.shortFormat(sbuffer);
        }

        textArea.append(String.format("==> %s\n\n", sbuffer.toString().toUpperCase()));
    }

    /** 
     * Shows the CPU flag status.
     * @return a String with the flag status.
     */
    private String showFlags()
    {
        String result="";
        result+=(this.cpu.isSupervisorMode() ? "S " : "  ");
        result+=((this.cpu.reg_sr&16)!=0) ? "X" : "-";
        result+=((this.cpu.reg_sr&8)!=0) ? "N" : "-";
        result+=((this.cpu.reg_sr&4)!=0) ? "Z" : "-";
        result+=((this.cpu.reg_sr&2)!=0) ? "V" : "-";
        return result+(((this.cpu.reg_sr&1)!=0) ? "C" : "-");
    }
     
    /**
     * Gets a disassembled instruction.
     * @param address where to get the instruction from.
     * @return  the String with the disassembmed instruction.
     */
    private smsqmulator.cpu.DisassembledInstruction getDI(int address)
    {
        int opcode = this.cpu.readMemoryWord(address);
        smsqmulator.cpu.Instruction i = cpu.getInstructionFor(opcode);
        return i.disassemble(address, opcode,this.cpu);
    }            
    
     /**
     * Get the text area to display info/commands/results:
     * @param nbr the text area number either 1 (right) or 2 (left) - default=1.
     * @return the JTextArea chosen
     */
    private javax.swing.JTextArea getPanel(int nbr)
    {
        switch (nbr)
        {
            case 1:
            default:
                return this.dataLogger;
                //break;
            case 2:
                return this.regLogger;
                //break;
        }
    }
 

/*-------------------------------------------------------- Communication with the goThread & interrupt threads--------------------------------------------------*/
    /**
     * This is called from the goThread when the goThreead is done.
     * 
     * @param message a String to print out.
     */
    public synchronized void goThreadStopped(String message)
    {
        showInfo(false,this.diTextArea);
        setFocusToInputWindow();
        this.regLogger.setText(message+"\n");
        this.goThread=null;
    }

    /**
     * Checks whether the goThread still exists.
     * 
     * @return <code>true</code> if GoThread no longer exists.
     */
    public synchronized boolean checkThread()
    {
        return this.goThread==null;
    }
  
    /**
     * Shows that a hard break (instruction $4b00) was encountered in the code.
     */
    public void showHardbreak()
    {
        this.regLogger.append("HARD BREAK ENCOUNTERED.\n");
    }

    /**
     * Suspends execution of the GoThread.
     * 
     * This is called from the Event Dispatch Thread, via the MonitorGui object when the window is minimized.
     * Note the use of <code>Thread.suspend</code> which is discouraged. However, adding a check for a "suspend yourself" flag within the goThread,
     * which is the recommended method, would further slow the emulation down. Here there is no danger of the goThread and the
     * event dispatch thread blocking each other.
     */
    public void suspendExecution()
    {
        if (this.goThread!=null)
        {
            try
            {
                this.goThread.suspend();
            }
            catch (Exception e)
            {
                
            }
        }
    } 
    
    /**
     * Resumes execution of the GoThread.
     * 
     * This is called from the Event Dispatch Thread, via the MonitorGui object when the window is minimized.
     * Note the use of <code>Thread.resume</code> which is discouraged. However, adding a check for a "suspend yourself" flag within the goThread,
     * which is the recommended method, would further slow the emulation down. Here there is no danger of the goThread and the
     * event dispatch thread blocking each other.
     */
    public void resumeExecution()
    {
        if (this.goThread!=null)
        {
            try
            {
                this.goThread.resume();
            }
            catch (Exception e)
            { /*NOP*/ }
        }
    }
    
    /**
     * Sets the interval between screen updates.
     * 
     * @param tim the interval between screen updates in milliseconds
     */
    public void setScreenUpdateInterval(int tim)
    {
        if (this.ih!=null)
        {
            this.ih.setScreenUpdateInterval(tim);
            this.gui.setScreenUpdateInterval(tim);
        }
    }
    
    /*-------------------------------------------------- Emulation --------------------------------------------------------*/
     
     /**
     * Sets the focus to the panel containing the screen of the emulated machine
     */
    public void setFocusToEmulScreen()
    {
        if (this.gui!=null)
        {
            this.gui.setFocus();
        }
    }

    /**
     * Sets the textfield containing my input field.
     * @param p the textfield containing my input field.
     */
    public void setInputWindow(javax.swing.JTextField p)
    {
        this.inputWdw=p;
    }

     /**
     * Sets the focus to the panel containing the screen of the emulated machine
     */
    public void setFocusToInputWindow()
    {
        if (this.inputWdw!=null)
        {
            this.inputWdw.requestFocus();
            this.inputWdw.requestFocusInWindow();
        }
    }
   
    /**
     * NASTY NASTY NASTY way of setting the Caps lock status.
     * 
     * @param isSet  =<code> true</code> if caps lock status is to be set.
     */
    public void setCapsLockStatus(boolean isSet)
    {
        int t= isSet?0xff00:0;
        this.cpu.writeMemoryWord(0x28088, t);
    }
    /*--------------------------------- Fallthrough methods : they more or less just call the cpu object or other objects --------------------------------------------*/

    /**
     * Input a key typed into the Java - SMSQE linkage block area.
     * <p> This should then be picked up by the SMSQE keyboard read routine.
     *  *** This is called from the swing EDT***.
     * @param key the "key" that was typed.
     */
    public void inputKey(int key)
    {
        if (!this.cpu.isRomLoadedOk())
            return;
        this.cpu.writeMemoryLong(this.cpu.getLinkageBlock()+Types.LINKAGE_KBD, key);
        this.trapDispatcher.resetCounter();
    }

    
    /**
     * Inputs the mouse position into the SMSQE linkage area.
     * 
     *  *** This is called from the swing EDT***.
     * 
     * @param msmvtx  x movement of the mouse (how much did mouse move).
     * @param msmvty  y movement of the mouse.
     * @param msx   mouse x pos.
     * @param msy   mouse y pos.
     */
    public void inputMouse(int msmvtx,int msmvty,int msx,int msy)
    {
        if (!this.cpu.isRomLoadedOk())
            return;
        msx=(msx<<16)+msy;                                  // mouse pos as long word
        msmvtx=(msmvtx<<16) | (msmvty&0xffff);              // mouse relatvie position
        this.cpu.writeMemoryLong(this.cpu.getLinkageBlock()+Types.LINKAGE_MOUSEPOS,msx);
        this.cpu.writeMemoryLong(this.cpu.getLinkageBlock()+Types.LINKAGE_MOUSEREL,msmvtx);
        this.trapDispatcher.resetCounter();
    }
    
    /**
     * Gets the mouse button pressed into the emulation.
     *  *** This is called from the swing EDT***.
     * 
     * @param btn  the button (1 = left,2=right, 4 = middle button
     */
    public void inputMouseButton(int btn)
    {
        if (!this.cpu.isRomLoadedOk())
            return;
        this.cpu.writeMemoryWord(this.cpu.getLinkageBlock()+Types.LINKAGE_MOUSEBTN,btn);
        this.trapDispatcher.resetCounter();
    }
      
    
    /**
     * Passes on the new absolute screen coordinates of the screen object (normally called at startup and after every window move operation).
     * 
     * The coordinates are just passed on to the TrapDispatcher object.
     * 
     * @param x the new absolute x screen coordinates of the screen object.
     * @param y the new absolute y screen coordinates of the screen object.
     */
    public void setScreenCoordinates(int x,int y)
    {
        if (this.trapDispatcher!=null)
            this.trapDispatcher.setScreenCoordinates(x, y);
    }

    /**
     * Sets the names for the drives of a device, i.e. the directories the drives will point to.
     * 
     *  *** This is called from the swing EDT***.
     * 
     * @param deviceDriver the deviceID
     * @param names an 8 element String array with the names for 8 drives (from 0 to 7).
     * @param forceRemoval force "unmount" of exiting drives and remount.
     */
    public void setNamesForDrives (int deviceDriver,String[]names,boolean forceRemoval)
    {
        this.trapDispatcher.setNamesForDrives(deviceDriver,names,forceRemoval);
    }
    
     /**
     * Sets the usage names for a device.
     * 
     * @param deviceDriver the deviceID.
     * @param usage the usage name.
     */
    public void setUsageForDrive (int deviceDriver,String usage)
    {
        this.trapDispatcher.setUsage(deviceDriver,Helper.convertUsageName(usage));
    }
    
    /**
     * Sets whether the case of names of files should be changed.
     * 
     * @param deviceDriver the deviceID.
     * @param change 0 = unchanged, 1=all upper case, 2=all lower case.
     */
    public void setFilenameChange (int deviceDriver,int change)
    {
        this.trapDispatcher.setFilenameChange(deviceDriver,change);
    }   
    
    /**
     * Sets the names of the native files to be used for floppy images.
     * 
     * @param names 8 element String array with the names of the native files to be used for floppy images.
     */
    public void setFloppyNames(String[]names)
    {
        if (this.floppy!=null)
            floppy.setNames(names,true,-1);
    }
    
    /**
     * A pass through method to the screen object, to copy from the original QL memory area to the screen.
     * 
     * @param origins possible origin where to copy to, currently ignored
     * @param mode which mode the screen is supposed to be in 4 or 8. 0 switches it off
     */
    public void setCopyScreen(int mode,int origins)
    {
       this.cpu.setCopyScreen(mode,origins);
    }
    
    /**
     * Resets the emulator : restarts it, possibly with a new screen size.
     * 
     */
    public void reset()
    {
        if (!waitForGoThreadToDie())
            return;
        this.ih.stopInterruptHandler();
        this.regLogger.setText("");
        this.dataLogger.setText("");
        this.lastCommand="";
        if (this.cpu.isRomLoadedOk())
        {
            this.cpu.setupSMSQE(true);              // yes, just reset the cpu & SMSQ/E
            if (this.trapDispatcher!=null)
            {
                this.trapDispatcher.reset();
            }
            goCommand(null,-1);                         // this also starts interrupt & screen redraw threads
        }
    }
    
    /**
     * Gets the CPU this object is monitoring.
     * 
     * @return the CPU this object is monitoring.
     */
    public smsqmulator.cpu.MC68000Cpu getCPU()
    {
        return this.cpu;
    }
    
    /**
     * Gets the screen used by the cpu.
     * 
     * @return the screen used by the cpu this monitor is attached to.
     */
    public Screen getScreen()
    {
        return this.cpu.getScreen();
    }
    
    /**
     * Gets the size of memory used by the cpu.
     * 
     * @return the size of memory used by the cpu.
     */
    public int getMemSize()
    {
        return this.cpu.getMemory().length;
    }
    
    /**
     * Loads a rom image.
     * 
     * @param f the file to load it from.
     * 
     * @return  <code>true</code> of loaded OK.
     */
    public boolean loadRom(java.io.File f)
    { 
        return loadRom(f.getAbsolutePath(),null);
    }
    
    /**
     * Loads a rom image by calling the appropriate routine of the CPU object.
     * 
     * @param filename the file to load it from.
     * @param fileURL alternatively, the URL of the file to load it from.
     * 
     * @return  <code>true</code> if loaded OK.
     */
    public boolean loadRom(String filename,java.net.URL fileURL)
    {   
        if (filename!=null && !filename.isEmpty())
        {
            if (!waitForGoThreadToDie())
                return false;                               // couldn't kill go thread, do nothing.
            return this.cpu.loadRomImage(filename,fileURL);
        }
        Helper.reportError(Localization.Texts[45], Localization.Texts[32]+" ("+filename+")", null);
        return false;
    }

    /**
     * Sets a keyrow bit for the indicated row and col.
     * 
     * @param row row number.
     * @param col column number.
     */
    public void setKeyrow(int row,int col)
    {
        this.cpu.setKeyrow(row,col);
        this.trapDispatcher.resetCounter();
    }
 
    /**
     * Removes a keyrow bit for the indicated row and col.
     * 
     * @param row row number.
     * @param col column number.
     */
    public void removeKeyrow(int row,int col)
    {
        this.cpu.removeKeyrow(row,col);
        this.trapDispatcher.resetCounter();
    }
    
    /**
     * Change the memory size. 
     * Involves making a new CPU.
     * Should only work when used as an application, not an applet.
     * 
     * @param size the memory size in bytes.
     * @param screen the screen to be used or null if current one.
     * @param allowEmulation = true if QL Screen emulation is switched on.
     */
    public void changeMemSize(int size,Screen screen,boolean allowEmulation)
    {
        if (!waitForGoThreadToDie())            
            return;
        this.ih.stopInterruptHandler();                     // no more interrupts, please
        if (screen == null)
            screen=this.cpu.getScreen();                 // get the screen
        screen.clearScreen();
        if (size ==-1)
            size=cpu.getLinkageBlock();
        String s=this.cpu.getRomFile();
        this.dataLogger.setText("");
        this.regLogger.setText("");
        this.trapDispatcher.closeAllFiles();
        this.cpu=null;
        this.trapDispatcher.setCpu(null);
        this.ih.setCpu(null);
        this.sam.setMemory(null);
        if (this.floppy!=null)
            this.floppy.setCPU(null);
        if (allowEmulation)
            this.cpu = new smsqmulator.cpu.CPUforScreenEmulation(size,screen,this.inifile,350000);
        else
            this.cpu = new smsqmulator.cpu.MC68000Cpu(size,screen,this.inifile,350000);
        if (this.cpu.loadRomImage(s,null))
        {
            this.ih.setCpu(this.cpu);
            this.ih.setScreen(screen);
            this.trapDispatcher.setCpu(this.cpu);
            this.sam.setMemory(this.cpu.getMemory());
            if (this.floppy!=null)
                this.floppy.setCPU(this.cpu);
            goCommand(null,0);
        }
    }
    
    /**
     * Gets the TrapDispatcher.
     * @return the TrapDispatcher.
     */
    public TrapDispatcher getTrapDispatcher()
    {
        return this.trapDispatcher;
    }
    /**
     * This kills the emulation thread.
     * 
     * @return true.
     */
    private boolean waitForGoThreadToDie()
    {
        if (this.goThread!=null)
            this.goThread.stop();                       // kill the thread brutally whatever the consequences
        this.goThread=null;
        return true;
    }
    
    /**
     * Go into fast or slow mode
     * 
     * @param fm if true, goes into fast mode, else slow mode.
     */
    public void setFastMode(boolean fm)
    {
        if (this.fastMode!=fm)
        {
            this.fastMode=fm;
            if (waitForGoThreadToDie())
                goCommand(null,-1);
        }
    }
    /**
     * Sets the "throttle" value in the TrapDispatcher object.
     * 
     * @param throt throttle value to set.
     */
    public void setThrottle(int throt)
    {
        if (this.trapDispatcher!=null)
            this.trapDispatcher.setThrottle (throt);
    }
     
    /**
     * Sets the sound volume via the TrapDispatcher object.
     * 
     * @param vol volume foom 0 (no sound) to 100 (loudest).
     */
    public void setSoundVolume(int vol)
    {
        if (this.trapDispatcher!=null)
            this.trapDispatcher.setSoundVolume(vol);
    }
    
    /**
     * Stops the TrapDispatcher throttle for a short time.
     */
    public void stopThrottle()
    {
        if (this.trapDispatcher!=null)
            this.trapDispatcher.setThrottleStatus(false);
    }
    
    /**
     * Sets the time offet value.
     * 
     * @param to the time offset value.
     */
     public void setTimeOffset(int to)
     {
         Monitor.TIME_OFFSET=Types.DATE_OFFSET+to;
     }
}
  /**
     * Well....
     * 
     * @param names 
     */
/*
    public void test(String[]names)
    {
    //    this.trapDispatcher.test(names);
    }
    */
    /**
     * This is like the test procedure, what it does varies from time to time.
     * Could be deleted in final versions.
     *//*
    private void special()
    {
        int cur=cpu.readMemoryLong(0x28004); //point to first c h  entry
        while (cur!=0)
        {
           
            int newt=cpu.readMemoryLong(cur);               // ptr to next entry
            if (newt<0)
            {
                this.regLogger.setText("Negative offset!");
                return;
            }
            if (newt==0)                                    // we reached the end of the list
            {
                 this.regLogger.setText("All seems OK!");
                return;
            }
            else if (cur+newt == 0x3c320)
            {
               this.regLogger.setText("3c320 encountred , seems OK");
            } 
           // oldv=cur;
            cur+=newt;
            cur &= 0x7fffffff;
        }
    }
   */


/*
//  M68k - Java Amiga MachineCore
//  Copyright (c) 2008-2010, Tony Headford
//  All rights reserved.
//
//  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
//  following conditions are met:
//
//    o  Redistributions of source code must retain the above copyright notice, this list of conditions and the
//       following disclaimer.
//    o  Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
//       following disclaimer in the documentation and/or other materials provided with the distribution.
//    o  Neither the name of the M68k Project nor the names of its contributors may be used to endorse or promote
//       products derived from this software without specific prior written permission.
//
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
//  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
//  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
//  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
//  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
//  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
//  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
*/
