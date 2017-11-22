package smsqmulator;

/**
 * This dispatches the various "traps" called from SMSQE.
 * It is the primary interface between SMSQe and java.
 * This is used for Trap#2 and Trap#3 calls to external device driver. Each device driver handles the calls to
 * itself, and is thus a trap handler.
 * <p>The trap handlers must register with this object for it to know them.
 * 
 * There are also a number of additional "traps" for other functionalites.
 * <p>
 * The trap calls are implemented via illegal instructions for the MC68000. These then cause an object of this here class to be called in the CPU's execute() 
 * or executeContinuous() loops (possibly via the javacom instruction).
 * <p>
 * Traps handled so far are:
 * <ul>
 *   <li>   2 - corresponds to SMSQ/E TRAP#2</li>
 *   <li>   3 - corresponds to SMSQ/E TRAP#3</li>
 *   <li>   5 - for many miscellaneous routines</li>
 *   <li>   6 - called for positioning the pointer in my screen object from within SMSQ/E</li>
 *   <li>   7 - called on scheduler loop to check whether prog should sleep a bit (energy saving mode)</li>
 *   <li>   8 - handles floppy writing/reading/formatting</li>
 *   <li>   9 - handle IP protocols</li>
 *   <li>   A - reserved</li>
 *   <li>   B - de-iconify only</li>
 *   <li>   C - QL Screen emulation </li>
 *   <li>   D - Scrap  clipboard operations </li>
 *   <li>   ab00 +   handle some maths ops</li>

 * </ul>
 * @author and copyright (c) 2012-2017 Wolfgang Lenerz
 * 
 * @version
 * 1.22 don't add fileseparator at end of name if it is for win or mem drive ; get/setNamesForDives: if device not found in map (different
 *      usage name) use getDeviceFromMapValues ; expand scrap operations to include starting/stopping of clipboard monitor thread, all
 *      scrap ops now in TRAP D ; setDirForDrive : passing a single space is the same as no name at all ; trapC extended to take parameters.
 * 1.21 get drivename (trap5,15) get name even if device has different usage name.
 *      trap 5; case 28,29, 30, 36,37 : interface change, needs smsqe 3.30.0002.
 * 1.20 resetDrives, use map.values directly.
 * 1.19 trap5,36 for SMSQmulator version implemented ; Trap5, cases 32-35 for better mode 32 and 16 screen fill and xor,
 *      these no longer call the CPU but directly the screen object ;  drive query return empty string if no
 *      drive defined ; setDirForDrive also stores setting in inifile (calls MonitorGui) and shows them in Gui ;
 *      use different way to check whether machine is idle ; setDirForDrive and setNames use forceRemoval parameter. ; 
 *      reset force resets the drives, resetDrives() implemented ; Trap $b for popup
 * 1.18 implement trap#C for original QL screen copying, Trap5, cases 32-35 for better mode 32 screen fill and xor.
 * 1.17 fixed reset.
 * 1.16 implements sending screen update value to monitor (trap 5, case 29).
 * 1.15 devices are held in a map, no need to call every device in turn when opening files/ using them ; xxx_USE$ works correctly ; 
 *      setDirForDrive adds file separator at name end if it isn't there already.
 * 1.14 reset calls closeAllFIles for all device drivers, setup Arith object and (possibly) use it, use A-line for java commo instead of $eb00.
 * 1.13 implements "TRAP9" for IP sockets ; "Trap#5"-15 ; correct handling of A1 and where the data will be written.
 * 1.12 floppy check for drive change added (special win handling trap (trap 9) implemented but commented out).
 * 1.11 special floppy handling trap (trap 8) implemented.
 * 1.10 implement trap#7, put thread to sleep if machine isn't busy ; if this.sam!=null added
 * 1.09 implement trap#5, 27 and 28 (set/get timer) ; set error return if Trap#3 call not valid for directory device drivers to err.ipar, not err.nimp, to keep old Turbo progs happy.
 * 1.08 before using <code>this.gui</code> check that it isn't null, which it will be if embedded applet ; add trap 5 , 22
 * 1.07 name is passed to open trap as an array of bytes ; implements Trap#5,D0=21 (xorBlock) ; provides for sound volume setting ; implements trap#5, d0=22 (set sound).
 * 1.06 Trap#5 D0=20 implemented.provides for beep volume setting.
 * 1.05 do not set pointer position if mouse not in screen.
 * 1.04 implement "trap#6" (set ptr position) - for that, a robot object is created which then moves the mouse.
 * 1.03 implement trap#5, d0=18 (read rtc directly from java)
 * 1.02 copy clipboard string content to scrap : added (trap 5 d0=16).
 * 1.01 throttle may be "interrupted" by key typed.
 * 1.00 throttle is implemented as configurable value.
 * 0.00 initial version.
 */
public class TrapDispatcher 
{
    private final SampledSound sam;                             // sampled sound system for SMSQE's SSS.
    private final Beep beep;                                    // and an object to create SMSQ/E beep sounds.
    private boolean throttleStop=false;                         // do we throttle at all?
    private ClipboardXfer cxfer=null;                           // object to transfer text(!!!) to/from the system clipboard to SMSQ/E
    private final MonitorGui gui;                               // the GUI with the emulated screen
    private int xLoc,yLoc;                                      // the x,y location of the screen object, in screen coordinates (not coordinates within the gui)
    private java.awt.Robot robot=null;                          // used for moving the mouse about when pointer position is set in SMSQ/E.
    private final SoundDevice sound;                            // to make sounds
    private inifile.IniFile inifile;                            // file with init info
    private long currentClock;                                  // used for the QL timer extension
    private long lastTime;                                      // "
    private FloppyDriver floppy;                                // floppy disk image handler
    private final IPHandler ipHandler;                          // handle ll IP traps
    private Arith arithpkg = new Arith();
    private final java.util.HashMap<Integer,DeviceDriver> devicesMap=new java.util.HashMap<>();// devices for I/Oops
    private volatile int schedCounter=0;                        
//    private SWinDriver swindrive;
    
    
    /**
     * Creates the object.
     * 
     * @param sam a SampledSound object to be used here.
     * @param throt an int, giving (in milliseconds) the time the job will suspend itself when blinking the cursor.
     * @param mgui the MonitorGui used here.
     * @param beepVolume the volume of sound SMSQmulator produces (0...100).
     * @param sound the soundDevice object.
     * @param inifile the ".ini" file object.
     * @param ipHandler handler for (TCP/)IP calls.
     */
    public TrapDispatcher(SampledSound sam,int throt,MonitorGui mgui,int beepVolume, SoundDevice sound,inifile.IniFile inifile,
                            IPHandler ipHandler)
    {
        this.sam=sam;
        this.beep=new Beep(beepVolume);
        this.sound=sound;
        this.gui=mgui;
        this.inifile=inifile;
        this.ipHandler=ipHandler;
        try
        {
            this.robot=new java.awt.Robot();                    // create the robot for mouse movement
            this.robot.setAutoDelay(0);
            this.robot.setAutoWaitForIdle(false);
        }
        catch (Exception e)
        {
            /*NOP*/                                             // if robot can't be created, there will be no pointer pos setting from within SMSQE
        }
    }
  
    /** 
     * The dispatcher's main method : Gets the correct trap handler and dispatches the trap to it.
     * 
     * @param trapType the type of trap we're dealing with (should be #2 to #8).
     * @param cpu the cpu to be used.
     */
    public void dispatchTrap(int trapType,smsqmulator.cpu.MC68000Cpu cpu)
    {
        trapType=-trapType;
        if (trapType>0xaaff)
        {
            this.arithpkg.handleOp(cpu,trapType-0xab00);
            return;
        }
        int trapKey=cpu.data_regs[0];                           // what kind of trap call is this?
        int A0;                                                 // A0 =channel definition block
        DeviceDriver dd;      
        
        switch (trapType)
        {
            case Types.TRAP3:                                   // handle trap #3 calls for file devices. Only a subset of the trap#3 calls need to be 
                                                                // handled, i.e. 0-7 inclusive and $40 to $4f inclusvie
                if ((trapKey<0) || (trapKey>0x4f) || (trapKey>7 && trapKey<0x40))
                {
                    cpu.data_regs[0]=Types.ERR_IPAR;            // this is not a trap for a filing system device driver
                    break;
                }
                
                dd=this.devicesMap.get(cpu.readMemoryLong(cpu.addr_regs[3]+0x3e));
                if (dd==null)
                    cpu.data_regs[0]=Types.ERR_NIMP;
                else
                {
                    A0=cpu.addr_regs[0];                        // handled, i.e. 0-7 inclusive and $40 to $4f inclusvie
                    dd.trap3OK(cpu.readMemoryByte(A0+0x5d),trapKey,A0,cpu.readMemoryWord(A0+0x1e));// drv number, fileid
                }
                break;
          
            case Types.TRAP2:                                   // file management traps :open/close/delete files, format medium
                A0=cpu.addr_regs[0];                            // A0 =channel definition block
                switch (trapKey)
                {
                    case 1:                                     // open file
                        int namelength=cpu.readMemoryWord(A0+0x32);
                        if (cpu.readMemoryByte(A0+0x33+namelength)==Types.UNDERSCORE)
                            namelength--;
                        byte []name=new byte[namelength];
                        byte []uncased=new byte[namelength];
                        for (int i=0;i<namelength;i++)
                        {
                            name[i]=(byte)cpu.readMemoryByte(A0+0x34+i); 
                            uncased[i]=WinDrive.LOWER_CASE[name[i]&0xff];
                        }
                        
                        int driveNumber=cpu.readMemoryByte(cpu.addr_regs[1]+0x14)-1; // drive number (starts at 1 for drive 1)
                        int openType=cpu.readMemoryByte(A0+0x1c);  // what kind of open?
                        int deviceID=cpu.readMemoryLong(cpu.addr_regs[3]+0x3e);      // fixed name of device (eg NFA0, = 3 upper cased letters, 0 at end))
                        dd=this.devicesMap.get(deviceID);
                        if (dd==null)
                            cpu.data_regs[0]=Types.ERR_FDNF;
                        else
                        {
                            if (dd.openFile(cpu.addr_regs[3], A0, openType,driveNumber,name,uncased))
                            { 
                                cpu.writeMemoryByte(A0+0x2e, 0xff);// set byte at $2e in channel defn block - don't create slave block (is this necessary?)
                                cpu.writeMemoryByte(A0+0x5d, driveNumber);
                            }
                        }
                        break;
                        
                    case 2:                                 // close file
                        dd=this.devicesMap.get(cpu.readMemoryLong(cpu.addr_regs[3]+0x3e));
                        if (dd==null)
                            cpu.data_regs[0]=Types.ERR_ICHN;
                        else
                            dd.closeFile(cpu.readMemoryByte(A0+0x5d),cpu.readMemoryWord(A0+0x1e));// drive br, file id
                        break;
                        
                    case 3:                                 // format medium
                        String s=cpu.readSmsqeString(cpu.addr_regs[1]);// format name
                        if (s.length()<6)
                        {
                            cpu.data_regs[0]=Types.ERR_IPAR;
                            break;
                        }
                        dd=this.devicesMap.get(Helper.convertUsageName(s.substring(0,3)));
                        if (dd==null)
                            cpu.data_regs[0]=Types.ERR_FDNF;
                        else
                            dd.formatMedium(s,this.inifile);
                        break;
                        
                    /*
                    case 4:                                     // delete a file -this, I think isn't even impemented in SMSQE, it's a trap#3 call
                        A1=cpu.getAddrRegisterLong(1); // A0 =physical defn block
                        driveNumber=cpu.readMemoryByte(A1+0x14)-1; // drive number (starts at 1 for drive 1)
                        break;
                    */
                        
                    default:                                    // huh, what's that???????????
                        cpu.data_regs[0]=Types.ERR_NIMP;
                        Helper.reportError(Localization.Texts[54], Localization.Texts[55]+trapKey, null);
                        break;
                }
                break;
                
                 
            // catch MISC "traps"
            case Types.TRAP5:                                   // these are some misc "traps" - D0 tells me what it is
                switch(cpu.data_regs[0])
                {
                    case 1:                                     // in QL mode, switch display mode,D1 contains mode (0 or 8)
                        cpu.setScreenMode(cpu.data_regs[1]&0xff);
                        break;
                        
                    case 2:                                     // reset
                        resetDrives();
                        this.gui.getMonitor().getCPU().setupSMSQE(true); 
                        break;
                        
                    case 3:                                     // sleep a bit when toggling cursor, no longer implemented
                        cpu.data_regs[0]=0;     
                        break;  
                        
                    case 4:                                     // set device/drve pointer d7 = device,d6=drive nbr,a6,a1.l = pointer to name
                        setDirForDrive(cpu.data_regs[7],cpu.data_regs[6]&0xffff,
                        cpu.readSmsqeString(cpu.addr_regs[6]+cpu.addr_regs[1]),cpu,false);
                        break;
                        
                    case 5:                                     // set emulator screen mode 
                        cpu.setEmuScreenMode(cpu.data_regs[1]&0xff);
                        break;
                       
                    case 6:                                     // move a block of (screen?) memory about
                        cpu.moveBlock();                        
                        break;
                        
                    case 7:                                     // set USE name of a device, A0 points to device defn block
                        int usage=cpu.readMemoryLong(cpu.addr_regs[0]+0x26);     // upper case usage name
                        dd=this.devicesMap.get(cpu.data_regs[7]);// true device name in D7
                        if (dd==null)
                        {
                            dd=getDeviceFromMapValues(cpu.data_regs[7]);
                            if (dd==null)
                            {
                                cpu.data_regs[0]=Types.ERR_FDNF;
                                return;                         // OOOPS
                            }
                        }
                        dd.setUsage(usage);
                        setUsage(cpu.data_regs[7],usage);
                        cpu.data_regs[0]=0;
                        break;
                        
                    case 8:                                     // initialise ssss   
                        if (this.sam!=null)
                            this.sam.fillPointers(cpu);
                        break;
                        
                    case 9:                                     // ssss kill sound
                        if (this.sam!=null)
                            this.sam.killSound(cpu);
                        break;
                        
                    case 10:       
                        if (this.sam!=null)                     // ssss notify that there is a sound to be played.
                            this.sam.playSample(cpu);
                        break;
                        
                    case 11:                                    // sound queryolume, doesn't work
                  /*      int ret=0;
                        switch (cpu.data_regs[1])
                        {
                            case 1:
                                ret = this.sam.queryVolume();
                              break;  
                            
                        }
                        cpu.data_regs[1]=ret;*/
                        cpu.data_regs[0]=0;
                        break;
                        
                    case 12:                                    // do QL beep
                        this.beep.play(cpu);
                        cpu.data_regs[0]=0;
                        cpu.data_regs[1]=0;
                        break;
                        
                    case 13:                                    // stop beep 
                        this.beep.killSound(cpu);
                        cpu.data_regs[0]=0;
                        break;                                    
                        
                    case 14:
                        if (this.sam!=null)
                            this.sam.closeSound(true);          // close ssss sound
                        break;                                
                        
                    case 15 :                                   // query drives : at D7=drive ID, at 4(a6,a1)= word with drive nbr 0 = query drive, 1 - 8 = query files/dirs
                        int addr=cpu.addr_regs[6]+cpu.addr_regs[1];// start address of where string will lie
                        int driveNbr=cpu.readMemoryWord(addr);  // driver number is on ari stack
                        dd=this.devicesMap.get(cpu.data_regs[7]);
                        if (dd==null)
                          dd=getDeviceFromMapValues(cpu.data_regs[7]);
                        cpu.data_regs[0]=Types.ERR_ITNF ;       // preset error no drive defined
                        if (driveNbr<0 ||driveNbr>8 || dd==null)
                        {
                            cpu.data_regs[0]=Types.ERR_IPAR;    // wrong drive number
                            return;                             // PREMATURE EXIT
                        }
                        
                        if (driveNbr==0)                        // we're looking for the usage name
                        {
                            driveNbr=dd.getUsage();             // get usage name for device
                            if (driveNbr!=0)                    // nothing found
                            {
                                addr-=6;
                                cpu.writeMemoryWord(addr, 3);   // return a three letter string
                                cpu.writeMemoryLong(addr+2, driveNbr);
                                cpu.addr_regs[1]=addr-cpu.addr_regs[6];
                                cpu.data_regs[0]=0;    // NO ERROR
                            }
                            return;                             // PREMATURE EXIT
                        }
                        String name=dd.getName(driveNbr);
                        if (name!=null&&!name.isEmpty())        // this device wasn't found anywhere (???)
                        {
                            addr=(addr-2-name.length())&0xfffffffe; // this is where we write
                            cpu.addr_regs[1]=addr-cpu.addr_regs[6]; // A1 is relative to A6
                            cpu.writeSmsqeString(addr, name,256);
                            cpu.data_regs[0]=0;
                        }
                        else
                        {
                            cpu.addr_regs[1]-=2;
                            cpu.writeMemoryShort(addr-2, (short)0);
                            cpu.data_regs[0]=0;   
                        }
                        break;
                        
                    // old 16,17 moved to trapd    
                    case 16:                                    // get host os name & version to SMSQE
                        String p = System.getProperty("os.name")+" "+System.getProperty("os.version");
                        cpu.writeSmsqeString(cpu.readMemoryLong(cpu.addr_regs[1]+4), p, true, 40);//write string
                        cpu.data_regs[0]=0;
                        cpu.reg_sr|=4;
                        break;
                        
                    case 17:                                    // returns 1 if sound is still playing
                        if (this.sam.isStillPlaying(cpu))
                            cpu.writeMemoryLong(cpu.readMemoryLong(cpu.addr_regs[1]+4),0x00010001);
                        else
                            cpu.writeMemoryLong(cpu.readMemoryLong(cpu.addr_regs[1]+4),0);
                        cpu.data_regs[0]=0;
                        cpu.reg_sr |=4;    
                        break;
                       
                    case 18:                                    // get time into D1
                        int tx=(int)((System.currentTimeMillis()/1000)+Monitor.TIME_OFFSET); // ** magic offset
                        cpu.data_regs[1]=tx;                    // write the time
                        cpu.data_regs[0]=0;
                        break;
                        
                    case 19:                                    // make menu bar visible
                        if (this.gui !=null)
                            this.gui.menuBarVisible(true);
                        cpu.data_regs[0]=0;
                        break;
                        
                    case 20:                                    // combine two blocks with alpha blending
                        cpu.combineBlocks();
                        break;
                 
                    case 22:                                    // set sound volume
                        cpu.data_regs[0]=0;
                        setSoundVolume(cpu.data_regs[1]&0xffff);
                        break;
                        
                    case 23:                                    // write back for mem drive
                        driveNbr=cpu.data_regs[2]-1;            // drive nbr in D2
                        dd=this.devicesMap.get(cpu.data_regs[1]);// device ID is in D1
                        if (driveNbr<0 ||driveNbr>7 || dd==null)
                        {
                            cpu.data_regs[0]=Types.ERR_IPAR;
                            return;                             // PREMATURE EXIT
                        }
                        dd.writeBack(driveNbr);
                        break;
                        
                    case 24:                                    // open channel to sampledsound2±
                        this.sound.openChannel(cpu);
                        break;
                        
                    case 25:                                    // close channel to sampledsound2
                        this.sound.closeChannel();
                        break;
                        
                    case 26:                                    // add bytes to sampledsound2
                        this.sound.doIO(cpu);
                        break;
                        
                    case 27:                                    // timer set
                        this.currentClock=System.currentTimeMillis();
                        cpu.data_regs[0]=0;
                        cpu.reg_sr |=4;    
                        break;
                        
                    case 28:                                    // timer get
                        cpu.writeMemoryLong(cpu.readMemoryLong(cpu.addr_regs[1]+4),(int) (System.currentTimeMillis()-this.currentClock));
                   //     cpu.data_regs[1]=(int) (System.currentTimeMillis()-this.currentClock);
                        cpu.data_regs[0]=0;
                        cpu.reg_sr |=4;    
                        break;
                        
                    case 29:                                    // set screen update rate
                        if (this.gui!=null)
                        {    
                            Monitor monitor=this.gui.getMonitor();
                            monitor.setScreenUpdateInterval(cpu.readMemoryWord(cpu.addr_regs[1]+2));
                        }
                        cpu.data_regs[0]=0;
                        cpu.reg_sr |=4;    
                        break;
                        
                    case 30:                                    // get the status of the menu bar
                        if (this.gui!=null)
                            addr=this.gui.menuBarIsVisible();
                        else
                            addr=-1;
                        cpu.writeMemoryWord(cpu.readMemoryLong(cpu.addr_regs[1]+4), addr);
                        noError(cpu);;        
                        break;
                        
                    case 31:                                    // shut down program
			System.exit(0);
			break;
                        
                    case 32:                                    // resolve stipple & fill a block with colour 
                    case 33:                                    // fill a block with colour
                        Screen screen=cpu.getScreen();
                        screen.fillBlock(cpu, cpu.data_regs[0]==32);
                        cpu.data_regs[0]=0;                    // show all was OK 
                        cpu.reg_sr |=4;    
                        cpu.pc_reg=cpu.readMemoryLong(cpu.addr_regs[7])/2;
                        cpu.addr_regs[7] += 4;                  // do an RTS here        
                        break;
                        
		    case 34:                                    // resolve stipple & xor a block with colour
                    case 35:                                    // xor a block with colour
                        screen=cpu.getScreen();
                        boolean x= cpu.data_regs[0]==34;
                        cpu.data_regs[0]=0;                     // show all was OK , may be modified by screen object
                        cpu.reg_sr |=4;    
                        screen.xorBlock(cpu, x);
                        cpu.pc_reg=cpu.readMemoryLong(cpu.addr_regs[7])/2;
                        cpu.addr_regs[7] += 4;                  // do an RTS here            
                        break;
		 	
                    case 36:                                    // get current version
                        cpu.data_regs[1]=Localization.getQLVersion();
                        noError(cpu);;        
                        break;
                        
                    case 37:                                    // set title for window
                        String s=cpu.readSmsqeString(cpu.addr_regs[1]);
                        this.gui.setTitle(s);
                        noError(cpu);;        
                        break;
                        
                    default:                                    // oops
                        cpu.data_regs[0]=Types.ERR_NIMP;       
                        break;
                }
                break;
  
            case Types.TRAP6:                                   // position pointer in java screen from within SMSQ/E
                int pos=cpu.data_regs[1];                       // new ptr position is in D1
                int x=(pos>>>16);                               // xpos
                int y=pos&0xffff;                               // ypos
                int a3=cpu.addr_regs[3];
                
                if ((x>=cpu.readMemoryWord(a3+0xf2)) || (y>=cpu.readMemoryWord(a3+0xf4)))//is ptr beyond screen?
                {
                    cpu.data_regs[0]=Types.ERR_ORNG;   // moveq #error, d0
                    cpu.pc_reg= (cpu.readMemoryLong(cpu.addr_regs[7]))/2;
                    cpu.addr_regs[7]+=4;                        // rts !!!!!
                }
                else
                {
                    cpu.writeMemoryByte(a3+0x8f,0);
                    cpu.writeMemoryByte(a3+0x52,0);
                    cpu.writeMemoryLong(a3+0x38,pos);
                    cpu.addr_regs[7]-=4;
                    cpu.writeMemoryLong(cpu.addr_regs[7],cpu.addr_regs[1]);//move.l a1,-(a7)
                    if (this.gui !=null && this.gui.getMouseIsInScreen())
                    {
                        boolean isDouble=this.gui.setMousePosition(x, y);
                        if (isDouble)
                        {
                            x+=x;
                            y+=y;
                        }
                        if (this.robot!=null)                   
                            this.robot.mouseMove(x+xLoc,y+yLoc);
                    }
                }
                break;
                
            /**
             * Called on every scheduler loop. The doc says that this gets called every 50/60th of a second when the machine is busy, more frequently when
             * it is idle.
             * or goto sleep after every fifth scheduler run, unless key, mouse,traps 1-3
             */
            case Types.TRAP7:                                   // called on every scheduler loop   
                this.schedCounter++;    
                if (this.schedCounter>5)
                /*
                long ctime=System.currentTimeMillis();          // current time - removed in 1.19
                long tdiff=(ctime-this.lastTime);
                this.lastTime=ctime;
                
                if (tdiff==0)                                   // system is idle*/
                
                {   
                    try
                    {
                        Thread.sleep (10);
                    }
                    catch (Exception e)
                    {
                        /* nop */
                    }
                    this.schedCounter=0;
                }
                cpu.data_regs[0]=0;
                break;
                
                
            /**
             * Handle floppy interface.
             */
            case Types.TRAP8:
                if (this.floppy == null)
                {
                    cpu.data_regs[0]=Types.ERR_ITNF;
                    return;                                     // floppy not linked in
                }
                switch (cpu.data_regs[0])
                {
                    case 0 :
                        this.floppy.readSector();
                        break;
                        
                    case 1:
                        this.floppy.writeSector();
                        break;
                        
                    case 2:
                        this.floppy.formatDrive();
                        break;
                    case 3:
                        this.floppy.checkWriteProtect();
                        break;
                        
                    case 4:
                        this.floppy.checkDriveStatus();
                        break;
                        
                    case 5:
                        this.floppy.setDrive();                 // set native file for a floppy disk image
                        break;
                        
                    case 6:
                        this.floppy.getDrive();                 // get native filename of floppy disk image
                        break;
                }  
                break;
                
                /**
                 * Handle IP calls.
                 */
            case Types.TRAP9:                                   // IP OPEN/ALL
                this.ipHandler.handleTrap(cpu);
                break;
  /*             
            case Types.TRAPA:                                   // win driver using SMSQ/E code (unused except for tsting purposes)
                if (this.swindrive == null)
                {
                    cpu.addr_regs[7]+=4;
                    cpu.data_regs[0]=Types.ERR_MCHK;
                    return;                                     // sdc/whn not linked in
                }
                switch (cpu.data_regs[0])
                {
                    case 0 :
                        this.swindrive.readSector();
                        break;
                    case 1:
                        this.swindrive.writeSector();
                        break;
                    case 2:
                        this.swindrive.formatDrive();
                        break;
                    case 3:
                        this.swindrive.checkWriteProtect();
                        break;
                    default :
                        break;
                }
                if (cpu.data_regs[0]==0)
                    cpu.reg_sr |=4;
                else
                    cpu.reg_sr&=~4;
                break; 
     */       
            // contract window to icon or make icon into window again.
            case Types.TRAPB:
                switch (cpu.data_regs[0])
                {
                    case 0:                                   
                        this.gui.deIconify();
                        noError(cpu);
                        break;
                    case 1:
                        this.gui.iconify();
                        noError(cpu);
                        break;
                }
                break;
                
            case Types.TRAPC:                                   // sets whether QL screen should be copied to extended screen.
                switch (cpu.data_regs[0])
                {
                    case 0 :
                        this.gui.getMonitor().setCopyScreen(cpu.data_regs[1]&0xffff,cpu.data_regs[5]);
                        //                                      mode            origins (is ignored) 
                        break;
                    case 1:
                        cpu.data_regs[1]=(cpu instanceof smsqmulator.cpu.CPUforScreenEmulation)?1:0;
                        break;
                }
                break;
                
            // SMSQE scrap <-> clipboard interface
            case Types.TRAPD:
                switch (cpu.data_regs[0])
                {
                    case 0:                                    // copy string from clipboard to scrap
                        if (this.cxfer==null)
                            this.cxfer=new ClipboardXfer();     // create object
                        this.cxfer.transferClipboardContentsToScrap(cpu);//copy string from it.
                        break;
                        
                    case 1:                                    // copy string from scrap to clipboard
                        if (this.cxfer==null)
                            this.cxfer=new ClipboardXfer();     // create object
                        this.cxfer.transferScrapToClipboard(cpu);//copy string from it.
                        break;
          
                    case 2:                                     // query change counter
                        if (this.cxfer==null)
                            this.cxfer=new ClipboardXfer();     // create object
                        this.cxfer.getChangeCounter(cpu);       // on entry d1 = current count, puts result into d0
                        break;
                  
                    case 3:                                     // start monitoring the clipboard
                        if (this.cxfer==null)
                            this.cxfer=new ClipboardXfer();     
                        this.cxfer.startMonitoring();            
                        break;

                    case 4:                                     // stop monitoring the clipboard
                        if (this.cxfer==null)
                            this.cxfer=new ClipboardXfer();     
                        this.cxfer.stopMonitoring();            
                        break;
                }
                break;
           
            default:                                            // catch other traps or errors
                break;
        }   
    }
    
    /**
     * Sets the new absolute screen coordinates of the screen object (normally called at startup and after every window move operation).
     * 
     * @param x the new absolute x screen coordinates of the screen object.
     * @param y the new absolute y screen coordinates of the screen object.
     */
    public void setScreenCoordinates(int x,int y)
    {        
        this.xLoc=x;
        this.yLoc=y;
    }

    /**
     * Show that no error occurred: set Z flag, set D0 to 0.
     * @param cpu 
     */
    private static void noError(smsqmulator.cpu.MC68000Cpu cpu)
    {    
        cpu.data_regs[0]=0;                      
        cpu.reg_sr |=4;
    }
    
    /**
     * Sets the status of the throttle
     * 
     * @param status a boolean : true = throttle is in effect, false = throttle must be disregarded.
     */
    public synchronized void setThrottleStatus(boolean status)
    {
        this.throttleStop=status;
    }
    
    /**
     * This resets the scheduler calls counter.
     * This may be called from the emulation thread, but also from the EDT (key press, mouse mv)!
     */
    public void resetCounter()
    {
        this.schedCounter=0;
    }
    
    /**
     * Checks whether the throttle is in effect.
     * 
     * @return true if throttle is on, false if throttle should be disregarded.
     */
    private synchronized boolean checkThrottle()
    {
        return this.throttleStop;
    }
     
    /**
     * Sets the throttle value.
     * 
     * @param throt the value to be set.
     */
    public void setThrottle (int throt)
    {
       // this.throttle=throt;
    }
    
    /**
     * Sets the FloppyDriver object.
     * @param floppy the FloppyDriver object to be set.
     */
    public void setFloppy(FloppyDriver floppy)
    {
        this.floppy=floppy;
    }
   
    /**
     * Registers a device driver with this handler.
     * <p>An already registered device driver class will not be registered again.
     * 
     * @param dd the device driver to register.
     */
    public void register (DeviceDriver dd)
    {
        for (DeviceDriver p : this.devicesMap.values())
        {
            if (dd.getClass()==p.getClass())
                return;                                     // a device driver like this is already registered here.
        }
        this.devicesMap.put(dd.getDeviceID(),dd);
    }
    
    /**
     * Sets the names for the dirs for the drives.
     * 
     * @param deviceID the device driver ID.
     * @param names must be an 8 element string array.
     * @param forceRemove = <code>true</code> if devices should be removed then remounted
     */
    public void setNamesForDrives (int deviceID,String[]names,boolean forceRemove)
    {
        DeviceDriver dd = this.devicesMap.get(deviceID);
        if (dd==null)
            dd=getDeviceFromMapValues(deviceID);
        if (dd!=null)
        {
           dd.setNames(names,this.inifile,forceRemove,false);
           this.gui.setNewDeviceNames(deviceID,names);
        }
    }
    
    /**
     * Resets all drives by removing them and then setting them again.
     */
    public void resetDrives()
    {
        for (DeviceDriver dd :this.devicesMap.values() )
        {
            String[] names=dd.getNames();
            dd.setNames(names, this.inifile, true,true);   
        }
    }
    
    /**
     * Gets the names of the directories for the drives.
     * 
     * @param deviceID the device driver ID.
     * 
     * @return names an 8 element string array or null if this device driver wasn't found.
     */
    public String[] getNamesForDrives (int deviceID)
    {
        DeviceDriver dd = this.devicesMap.get(deviceID);
        if (dd==null)                                           // device not found, probably because usage name is different
            dd=getDeviceFromMapValues(deviceID);                // try to find it in a slower way 
        return dd==null?null:dd.getNames();
    }
    
    /**
     * Sets the name of the native directories for one drive.
     * 
     * @param deviceDriver the device driver ID.
     * @param driveNbr the drive number, must be >0 and <9.
     * @param newname the new name of the native dir for this drive
     */
    private void setDirForDrive(int deviceDriver,int driveNbr,String newname,smsqmulator.cpu.MC68000Cpu cpu,
                                boolean forceRemoval)
    {
        String []names=getNamesForDrives(deviceDriver);
        if (names==null || driveNbr<1 || driveNbr>8)
        {
            cpu.data_regs[0]=Types.ERR_ITNF;// this device/drive wasn't found
            return;
        }
        if (newname.equals(" "))
            newname="";
        switch (deviceDriver)
        {
            case Types.WINDriver:
            case Types.MEMDriver:
            case Types.FLPDriver:                               // these mustn't have a file separator at end
                if (newname.endsWith(java.io.File.separator))
                    newname=newname.substring(0,newname.length());
                break;
            default:
                if (!newname.endsWith(java.io.File.separator))
                    newname+=java.io.File.separator;            // and these must!
                
        }
        
        names[driveNbr -1]=newname;
        setNamesForDrives(deviceDriver,names,forceRemoval);
        cpu.data_regs[0]=0;
    }
    
    /**
     * Sets whether a filename's case should be changed.
     * 
     * @param deviceID the device driver ID.
     * @param change 0 = unchanged, 1=all upper case, 2=all lower case.
     */
    public void setFilenameChange(int deviceID,int change)
    {
        DeviceDriver dd = this.devicesMap.get(deviceID);
        if (dd!=null)
        {
            dd.setFilenameChange(change);
        }
    }
    
    /**
     * Closes all files on all device drives registered here, remove reference to the cpu.
     */
    public void closeAllFiles()
    {
        for (DeviceDriver dd : this.devicesMap.values())
        {
            if (dd!=null)
                dd.closeAllFiles();
        }
    }
    
    /**
     * Sets the cpu used by all device drivers.
     * 
     * @param cpu the cpu to be used.
     */
    public void setCpu (smsqmulator.cpu.MC68000Cpu cpu)
    {
        for (DeviceDriver dd : this.devicesMap.values())
        {
            if (dd!=null)
                dd.setCpu (cpu);
        }
    }
    
    /**
     * Sets the usage name for a device.
     * 
     * @param deviceID the device to set the usage name for, an int containing a three letter word+'0', eg. 'QXL0'..
     * @param usage the usage name, an int containing a three letter word+'0', eg. 'QXL0'.
     */
    public void setUsage(int deviceID,int usage)
    {
        DeviceDriver dd = this.devicesMap.get(deviceID);        // get device driver with that name
        if (dd==null)                                           // not found (already has different usage name)
        {
           dd=getDeviceFromMapValues(deviceID);
        }
            
        if (dd!=null)
        {
            dd.setUsage(usage);
            this.devicesMap.remove(deviceID);
            this.devicesMap.put(usage, dd);
        }
    }
    
    /**
     * Slower way to get the device driver from the type, should work even with usage names.
     * 
     * @param deviceID the ID of the device to find.
     * 
     * @return the DeviceDriver, or null if not found.
     */
    private DeviceDriver getDeviceFromMapValues(int deviceID)
    {
        for (DeviceDriver dd : this.devicesMap.values())
        {
            if (dd.getDeviceID()==deviceID)
                return dd; 
        }
        return null;
    }
    
    /**
     * Sets the sound volume for the Beep and SampledSound objects.
     * 
     * @param vol volume from 0 (no sound) to 100 (loudest).
     */
    public void setSoundVolume(int vol)
    {
        if (this.beep!=null)
            this.beep.setVolume(vol);
        if (this.sam!=null)
            this.sam.setVolume(vol);
    }
    
    /**
     * Reset the machine.
     * 
     */
    public void reset()
    {   
        for (DeviceDriver dd : this.devicesMap.values())
            dd.closeAllFiles();
        resetDrives();
    }
 
    /*
    public void setSwin(SWinDriver d)
    {
        this.swindrive=d;
    }
 */   
}
