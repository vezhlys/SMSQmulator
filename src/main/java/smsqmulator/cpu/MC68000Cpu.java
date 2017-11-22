package smsqmulator.cpu;

import smsqmulator.Localization;
import smsqmulator.Types;

/**
 * This is the "cpu" used.
 * Some implementation notes.
 * <ul>
 * <li> It has a provision for "external interrupt" handlers : before an smsqmulator2.cpu.Instruction is executed, a check is made whether an external 
 *      interrupt handler caused an interrupt. The CPU has a list of eternal interrupt handlers which are registered with it.</li>
 * <li> This CPU registers a few more "instructions" (from <code>JavaComm</code>) which are normally illegal (A line) 
 *      instructions but are used to communicate with the TrapDispatcher.</li>
 * <li> It acts on the trace bit by calling the corresponding exception routine.</li>
 * <li> For performance considerations, the object exposes the address, data and status registers and PC to the outside world as public arrays of int. This is so that the instructions 
 * can manipulate the registers directly.</li>
 * <li> The memory is directly incorporated into this object.
 * <li> This object implements memory as an array of shorts. Profiling of SMSQ/E and typical applications has shown that word sized access is by far the most frequent
 *      access to memory. YMMV. As a result, the PC is an index into the memory, not the "true" address. To get the true address,
 *      multiply the PC by 2.</li>
 * <li> The memory read and write operations for words and long word DO NOT CHECK that access is to an even address as it
 *      should be. If it isn't, then the read/write operation will take place at address-1!!!!!!!!!!!!!!!=)</li>
 * <li> It implements a "memory cutoff". Indeed, and unfortunately, QLiberator compiled programs use the MS bits of some 
 *      addresses to store some data (!!!). These must be filtered out, else the addresses used for reading/writing will be 
 *      wrong. This really sucks as this means that in every memory operation (read/write) these bits must be ANDed out.</li>
 * </ul>
 * 
 * The memory map will be :
 * <ul>
 *  <li>0 ...    X : usable RAM </li>
 *  <li>X+1 ...  Y : linkage block</li>
 *  <li>Y+1 ...  Z : Video Ram</li>
 *  <li>Z+1 ...    : "ROM" i.e. the OS: may be read from but not written to (except special case).</li>
 * </ul>
 * 
 * @author and copyright (c) 2012 - 2017 Wolfgang Lenerz
 * Based on Tony Headford's code, see his copyright in the attached file.
 * <p>
 * @version :
 *   2.13 writeSMSQEString : if string is empty but not null, write 0 word ; set and removeKeyrow : do not presume sysvars at $28000.
 *   2.12 RESET instruction is actually linked in (though it doesn't really do anything)..
 *   2.11 fillBlock, xorBlock deleted ; setEmuScreenMode fallthrough method implemented.
 *   2.10 setcopyScreen implemented.
 *   2.09 moveBlock : better handling of odd acses for mode 16 (aurota 8 bit colour) 
 *   2.08 traceFlag introduced and set at various states, avoids ORing reg_sr, newInterruptGeneated no longer volatile, use variable
 *        for RTE opcode in executeContinuous, readMemoryLongPCInc use ++ twice, testTrace() introduced. Read and write SMSQEstring :
 *	  don't check whether this is in screen memory, newScreen() removed, fillBlock and XorBlock streamlined ; reset() removed.
 *   2.07 when writing mem, no need to OR the value with $ffff when cast to short ; moveBlock does the RTS, copyMem handles all cases.
 *   2.06 main mem array made final. Needs also size of rom image when created ; readSmsqeString uses StringBuffer
 *   2.05 xorBlock set reg_sr correctly in case of error (only unset Z flag!).
 *   2.04 readFromFile returns -1 if EOF, not 0, writeSMSQeString uses charAt not substring
 *   2.03 only check for minor version if major version == smsqe version
 *   2.02 uses MOVEAn2bis instruction.
 *   2.01 setKeyrow pokes in value directly into keyboard linkage block, presumes sysvars are at $28000 (!) ; writeSmsqeString makes sure short doesn't overflow
 *   2.00 total revamp, dumped most of Tony Headford's code.("mainmemory" is now public, not protected)
 *   1.13 SMSQE minor version checked for ; set option for less cpu time when idle
 *   1.12 many replacement instructions.
 *   1.11 loadRomImage provides for embedded applet via a Security exception ; provisions for MEM device in setupSMSQE.
 *   1.10 screenStopW and screenStopL no longer used.
 *   1.09 screenStopW and screenStopL introduced to stop sprite clipping from overshoot ; implements xorBlock.
 *   1.08 faster writeToFile ; fillBlock fills in background OK. General cleanup. moveBlock : anything screen related is handled by the screen object. 
 *        Implement combineBlocks.
 *   1.07 warn on out of memory error ; fillBlock and moveBlock are also (partially) used for mode16 ; don't allocate mem on creation, only when loading OS.
 *   1.06 readMemoryShort implemented.
 *   1.05 checks ROM version, will not load older ones; copyMem uses system.arrayCopy.
 * <ul>
 *  <li> 1.03 reset: check for newer SMSQE version, warn if too old.</li>
 *  <li> 1.02 write to file : correct write if memory starts at an odd address.</li>
 *  <li> 1.01 Simpler way to use 50 Hz interrupt - there is only one interrupt after all.</li>
 *  <li> 1.00 Use time adjustment from Monitor.</li>
 *  <li> 0.04 writeToFile caters for the write starting at an odd address.</li>
 *  <li> 0.03 Makes character conversions in writeSmsqeString and readSmsqeString.<li>
 *  <li> 0.02 Totally revamped version, memory is "integrated" into the cpu, for faster access.</li>
 *  <li> 0.01 Added external interrupt handlers</li>
 *  <li> Initial version (based on Tony Headford's MC68000Cpu class)</li>
 * </ul>
 */
public class MC68000Cpu
{
    protected smsqmulator.ExternalInterruptHandler[] externalInterruptHandlersList=new smsqmulator.ExternalInterruptHandler[10];// a list of external interrupt handlers
    protected boolean []interruptSignaled=new boolean[10];      // will be true in an element if the corresponding interrupt was triggered
    protected int []interruptLevel=new int[10];                 // the interrupt level of each interrupt
    protected int nbrOfInterruptHandlers=0;                     // how many interrupt handlers there are
    protected boolean newInterruptGenerated=false;     // <code>true</code> if any interrupt handler generated an interrupt
    public smsqmulator.TrapDispatcher trapDispatcher;           // trap dispatcher for my DeviceDrivers
    protected final short[] mainMemory;                           // the array holding the memory -> word sized access
    public static final int cutOff= 0x0fffffff;                 // for SMSQE, some higher bits of an address must be CUT OFF!!!!!!!!!
    protected int screenStart=0;                                // start of screen ram (vram)
    protected int screenStop=0;                                 // end of screen ram
    public smsqmulator.Screen screen=null;                      // the screen used
    public int ramSize;                                         // nominal amount of "RAM" for SMSQ
    protected int totRamSize;                                   // amount of "RAM" + linkage block +vram= highest writable address.
    protected int totRamSizeForLong;                            // highest writable address if long word
    protected int totRamSizeForWord;                            // highest writable address if word

    protected int totMemSize;                                   // amount of RAM + ROM = highest readable address (aprt from the VRAM)
    private String romFile;                                     // name of the "rom" file = file containing the OS
    private inifile.IniFile iniFile;                            // file with initially configured values
    protected java.util.Random randomNumber=new java.util.Random();// make sure I can give a random number to SMSQ/E
    private boolean romLoadedOK=false;                          // = true if the OS was loaded ok, else false
       
    private boolean traceFlag;
    
    public final int[] data_regs;
    public final int[] addr_regs;
    public int pc_reg;
    public int reg_sr;
    public int reg_usp;
    public int reg_ssp;                                         // these should all be obvious
    
    protected int currentInstructionAddress=0;                  // used when in slow mode
    protected StringBuilder disasmBuffer;                       // used for disassembling an instruction
    protected final smsqmulator.cpu.Instruction[] i_table;      // this table contains all of the instructions
    protected smsqmulator.ExternalInterruptHandler ieh=null;    // the external interrupt handler
    
    public static final int INTERRUPT_FLAGS_MASK = 0x0700;
    public static final int SUPERVISOR_FLAG = 0x2000;
    public static final int TRACE_FLAG = 0x8000;
    public int stopNow;
    
    public int []pcs=new int [10];                              // used in slow mode  
 

    /**
     * Creates the "naked" object, this isn't really useful except for testing.
     * 
     * @param size the size of ram in bytes. This is allocated when the object is created.
     * @param romSize the size of the ROM image. Loaded after the RAM image.
     * @param videoRamSize size,in bytes, of screen memory to be allocated here.
    */
    public MC68000Cpu(int size,int romSize,int videoRamSize)
    {
        this.data_regs = new int[8];
        this.addr_regs = new int[8];

        if ((size&1)==1)
            size++;
        this.disasmBuffer = new StringBuilder(64);
        
        this.i_table = new smsqmulator.cpu.Instruction[65536];
        loadInstructionSet();
        
        this.ramSize=size;
        this.screenStart=this.ramSize+smsqmulator.Types.LINKAGE_LENGTH+10;// total size of ram :main ram + linkage block + spare
        this.totRamSize=this.screenStart+videoRamSize;
        this.screenStop=this.totRamSize;
        this.totRamSizeForLong=this.totRamSize-3;
        this.totRamSizeForWord=this.totRamSize-1;
        this.totMemSize=this.totRamSize+romSize;                        // there is no "ROM" - yet!
        this.mainMemory=new short[this.totMemSize/2];                //
    }
    
    /**
     * Creates the object.
     * 
     * @param size the size of ram in bytes.
     * @param iniFile my .ini file with options to set.
     * @param romSize the size of the ROM image. Loaded after the RAM image.
     * @param videoRamSize size,in bytes, of screen memory to be allocated here.
     */
    public MC68000Cpu(int size,inifile.IniFile iniFile,int romSize,int videoRamSize)
    {
        this(size,romSize,videoRamSize);
        this.iniFile=iniFile;
    }
    
    /**
     * Creates the object.
     * 
     * @param size the size of ram in bytes. This is NOT allocated when the object is created, but only when the OS is loaded.
     * @param screen the screen attached to the machine.
     * @param iniFile my .ini file with options to set. 
     * @param romSize the size of the ROM image. Loaded above RAM and VRAM.
     */
    public MC68000Cpu(int size,smsqmulator.Screen screen,inifile.IniFile iniFile,int romSize)
    {  
        this(size,iniFile,romSize,screen.getScreenSizeInBytes()); 
        this.screen=screen;
        if (screen.isQLScreen())
        {
            this.screenStart=0x20000;
            this.screenStop=0x28000-2;
        }
        this.screen.setVramBase(this.screenStart);              // set the base of the video ram, let the screen object handle the details
    }
    
    /* ---------------------------------  Execute instructions - monitor mode & fast(er) mode ---------------------------*/
    
    /**
     * Executes one single instruction.
     * @return  negative values for comm with the Java prog  or 0 : all went OK.
     */
    public final int execute()
    {
        if (this.newInterruptGenerated)
        {
            int old_sr = reg_sr;                                // SR BEFORE the exception
            if ((reg_sr & SUPERVISOR_FLAG) == 0)                // were we in supervisor mode already?....
            {                           
                reg_sr |= SUPERVISOR_FLAG;                      // ...no, so set supervisor bit
                reg_usp = addr_regs[7];                         // and change stack pointers
                addr_regs[7] = reg_ssp;
            }       
            this.addr_regs[7]-=6;                               // one word and one long word go onto the stack
            int op= this.addr_regs[7]/2;
            this.mainMemory[op]=(short)old_sr;                  // stack old status reg
            this.mainMemory[op+1]=(short)(((this.pc_reg*2)>>16)&0xffff);// stack program counter
            this.mainMemory[op+2]=(short)((this.pc_reg*2)&0xffff);
            this.reg_sr&=~TRACE_FLAG;                           // trace is OFF
            this.traceFlag=false;
            old_sr = readMemoryLong(0x68);                      // exception vector 2
            if(old_sr == 0)                                     // .. it doesn't exist
            {
                // the required interrupt vector is uninitialised : raise an uninitialised interrupt vector exception instead vector 15 == 0x003c
                old_sr = readMemoryLong(0x003c);

                if(old_sr == 0)                                 // if this is zero as well the CPU should halt
                {
                        throw new IllegalArgumentException("Interrupt vector not set for uninitialised interrupt vector while trapping uninitialised vector " + 26);
                }
            }
            this.pc_reg = old_sr/2;
            reg_sr &= ~(INTERRUPT_FLAGS_MASK);
            reg_sr |= 0x0200;
            this.newInterruptGenerated=false;
        }

        for (int i=0;i<9;i++)                                   // this is to allow the monitor "dp" command to work
        {
            pcs[i]=pcs[i+1];
        }
        pcs[9]=pc_reg;
        
        this.currentInstructionAddress = this.pc_reg;           // keep old instruction address
        int opcode = this.mainMemory[this.pc_reg]&0xffff;
        this.pc_reg ++;
        this.i_table[opcode].execute(opcode,this);              // execute this smsqmulator2.cpu.Instruction

        //if ((this.reg_sr &0x8000)!=0 && (opcode!=0x4e73)) 
        if (this.traceFlag && (opcode!=0x4e73))    // is trace bit set and are we not doing an rte?
            raiseException (9);                                 // yes, so raise trace exception
        return this.stopNow;
    }
    
    /**
     * Continuous execution loop. 
     * This is the fastest way to execute an MC 68000 prog.
     */
    public final void executeContinuous()
    { 
        int trace=0x4e73;
        while(true)
        {
            if (this.newInterruptGenerated)                     // an interrupt occurred : generate the exception directly
            {
                int old_sr = this.reg_sr;                       // SR BEFORE the exception
                if ((this.reg_sr & MC68000Cpu.SUPERVISOR_FLAG) == 0)  // were we in supervisor mode already?....
                {                           
                    this.reg_sr |= MC68000Cpu.SUPERVISOR_FLAG;        // ...no, so set supervisor bit
                    this.reg_usp = this.addr_regs[7];           // and change stack pointers
                    this.addr_regs[7] = reg_ssp;
                }       
                this.addr_regs[7]-=6;
                int op= this.addr_regs[7]/2;
                this.mainMemory[op]=(short)old_sr;
                this.pc_reg*=2;
                this.mainMemory[op+1]=(short)((this.pc_reg>>16)&0xffff);
                this.mainMemory[op+2]=(short)(this.pc_reg&0xffff);
                this.reg_sr&=~TRACE_FLAG;
                this.traceFlag=false;
                this.pc_reg = readMemoryLong(0x68)/2;             // exception vector 2 - let's just presume it exists
                if(this.pc_reg == 0)
                {
                    this.pc_reg = readMemoryLong(0x003c)/2;     // interrupt vector is uninitialised : raise an uninitialised interrupt vector exception instead (vector 15 == 0x003c)
                    if(this.pc_reg == 0)                        // if this is zero as well then the CPU should halt
                    { 
                        throw new IllegalArgumentException("Interrupt vector not set for uninitialised interrupt vector while trapping uninitialised vector " + 26);
                    }
                }
                
                this.reg_sr &= ~(MC68000Cpu.INTERRUPT_FLAGS_MASK);
                this.reg_sr |= 0x0200;
                this.newInterruptGenerated=false;
            }
            int opcode = this.mainMemory[this.pc_reg]&0xffff; // get the primary instruction
            this.pc_reg ++;                                   // point next Instruction or possible data for this instruction
            smsqmulator.cpu.Instruction i = this.i_table[opcode];
            i.execute(opcode,this);
            
            /*             
            // one would expect the following two instructions to be faster than the four preceding ones - but this isn't the case!!!!!
            int opcode = this.mainMemory[this.pc_reg++]&0xffff; // get the primary instruction and point next Instruction or possible data for this instruction
            this.i_table[opcode].execute(opcode);         // execute this instruction
            // remains true on 01.01.2016  
            */
            
            if (this.traceFlag && (opcode!=trace))// is trace bit set and are we not doing an rte?
            {
                raiseException (9);                             // yes, so raise trace exception
            }
            
        }
    }
    /* debug / profile
    public final void execute(int opcode)
    { 
            smsqmulator.cpu.Instruction i = this.i_table[opcode];
            i.execute(opcode,this);
    }
    */
    
    /* ---------------------------- Dealing with external interrupt handlers -----------------------------------------------*/
    
    
    //This is the simpler scheme now : just 1 interrupt (as there is only one for SMSQ/E, anyway)
     
    /**
     * This is called by an external level 2 interrupt handler wishing to signal an external interrupt.
     * It is presumed that this is a level 2 interrupt!
     * 
     */
   // public synchronized void generateInterrupt()
    public void generateInterrupt()
    {
        if ((this.reg_sr & 0x700)<0x0200)                   // only generate an interrupt if interrupt level <2
        {
             this.newInterruptGenerated=true;   
        }
    }
    
    /**
     * Registers an external interrupt handler with the CPU.
     * 
     * @param eh the ExternalInterruptHandler to register
     */
    public void registerInterruptHandler(smsqmulator.ExternalInterruptHandler eh)
    {
            this.ieh=eh;
    }
   /*------------ old scheme interrupt handling ------------------*/
    
    /**
     * Registers an external interrupt handler with the CPU - ATTENTION:
     * Only ONE external interrupt of the same class will be registered!
     * Moreover, only 10 InterruptHandlers can be registered!!! This is an arbitrary and artificial limitation.
     * 
     * @param eh the ExternalInterruptHandler to register
     * @param intLevel - the interrupt level that this interrupt handler generates.
     * 
     * @return the index into the interrupt handlers array.
     * 
     * @throws IllegalArgumentException if interruptLevel  &lt;0 or &gt;7
     * @throws java.util.InputMismatchException if an interrupt handler of this class is already registered with the CPU.
     * @throws java.util.TooManyListenersException  if one tried to register more than 10 exception handlers.
     */
    public int registerInterruptHandler(smsqmulator.ExternalInterruptHandler eh,int intLevel) throws IllegalArgumentException,java.util.InputMismatchException,java.util.TooManyListenersException
    {
        if (this.nbrOfInterruptHandlers>=this.externalInterruptHandlersList.length)
            throw new java.util.TooManyListenersException("Too manu Exception handlers!");
        for (int i=0;i<this.nbrOfInterruptHandlers;i++)
        {
            if (this.externalInterruptHandlersList[i].getClass()==eh.getClass())
                throw new java.util.InputMismatchException("An interrupt handler of this class is already registered with this  CPU");
        }
        if (intLevel<0 || intLevel>7)
            throw new IllegalArgumentException("Interrupt level must be between 0 & 7 inclusive!");
        this.externalInterruptHandlersList[this.nbrOfInterruptHandlers]=eh;
        this.interruptLevel[this.nbrOfInterruptHandlers]=intLevel;
        this.nbrOfInterruptHandlers++;
        return nbrOfInterruptHandlers-1;
    }
    
    /**
     * This is called by an external interrupt handler wishing to signal an external interrupt.
     * @param interruptIndex the index into the interrupt handlers array for this interruptHandler.
     * This is the index returned by the  <code>registerInterruptHandler</code> method.
     */
    public synchronized void generateInterrupt(int interruptIndex)
    {
        if (interruptIndex>-1 && interruptIndex<this.nbrOfInterruptHandlers)
        {
            this.interruptSignaled[interruptIndex]=true;
            this.newInterruptGenerated=true;
        }
    }
  
    /**
     * Clears all external interrupt handlers - after this there are no more external interrupt handlers registered with this CPU.
     */
    public void clearInterruptHandlers()
    {
        this.ieh=null;
        for (int i=0;i<this.nbrOfInterruptHandlers;i++)
        {
            if (this.externalInterruptHandlersList[i]!=null)
            {
                this.externalInterruptHandlersList[i].removeHandler();   
                this.externalInterruptHandlersList[i]=null;
                this.interruptSignaled[i]=false;
                this.interruptLevel[i]=0;
            }
        }
        this.nbrOfInterruptHandlers=0;
        this.ieh=null;
    }
    
    
    /*      ------------------------ Memory operations ---------------------------------*/
    
    
    
    
    /*       
     ²      NOTE : THIS DOES NOT CHECK WHETHER WORD/LONG SIZED ACCESSES ARE AT AN EVEN ADDRESS. IF AN ODD ADDRESS IS ACCESSED,    
     *      THIS WILL READ FROM THE LOWER EVEN ADDRESS 
     */
 
    
    /**
     * Reads an unsigned byte from memory.
     * 
     * @param address where to read from.
     * 
     * @return an int containing the unsigned byte read.
     */
    public final int readMemoryByte(int address)
    {
        address&=MC68000Cpu.cutOff;                         // !!! remove higher bits also means that address won't be <0
        if (address>this.totMemSize)
            return 0;                                       // ?? should I generate an address violation here ??   
        if ((address & 1) !=0)
            return this.mainMemory[address/2]&0xff;
        else
            return (this.mainMemory[address/2]>>>8)&0xff;
    }

    /**
     * Reads a signed byte from memory.
     * 
     * @param address where to read from.
     * 
     * @return an int containing the sign extended byte read.
     */
    public final int readMemoryByteSigned(int address)
    {  
        address&=MC68000Cpu.cutOff;                         // !!! remove higher bits  also means that address won't be <0
        if (address>this.totMemSize)
            return 0;                                       // ?? should I generate an address violation here ??
        int res=this.mainMemory[address/2];
        if ((address & 1) == 0)
            res>>>=8;
        res&=0xff;  
        if((res & 0x80) !=0)                                // sign extend
            return res |0xffffff00;
        else
            return res;
        
    }
    
    /**
     * Reads an unsigned word from memory.
     * This will give a wrong result if the address is uneven.
     * 
     * @param address where to read from.
     * 
     * @return an int containing the unsigned word read.
     */
    public final int readMemoryWord(int address)
    {  
        // debugMem(address);
        address&=MC68000Cpu.cutOff;                             // !!! remove higher bits  also means that address won't be <0
        if (address>this.totMemSize)
            return 0;      
        return (this.mainMemory[address/2] &0xffff);
    }
    
    /**
     * Reads a word and returns it as signed int.
     * This will give a wrong result if the address is uneven.
     * 
     * @param address where to read from.
     * 
     * @return a sign extended int containing the signed word read.
     */
    public final int readMemoryWordSigned(int address)
    {
        // debugMem(address);
        address&=MC68000Cpu.cutOff;                             // !!! remove higher bits  also means that address won't be <0
        if (address>this.totMemSize)
            return 0;        
        return this.mainMemory[address/2];                      // int will be sign extended automatically
    }
    
    /**
     * Reads an unsigned word at address (PC).
     * 
     * @return an int containing the unsigned word read.
     */
    public final int readMemoryWordPC()
    {  
        return this.mainMemory[this.pc_reg] &0xffff;
    } 
    
    /**
     * Reads an unsigned word at address (PC) and increases PC by 1.
     * 
     * @return an int containing the unsigned word read.
     */
    public final int readMemoryWordPCInc()
    {  
        return this.mainMemory[this.pc_reg++] &0xffff;
    } 

    /**
     * Reads a word at address PC and returns the word as a signed int.
     * This will give a wrong result if the address is uneven.
     * 
     * @return an int containing the signed word read.
     */
    public final int readMemoryWordPCSigned()
    {  
        return this.mainMemory[this.pc_reg];
    } 
    
    /**
     * Reads an word at address PC, increases PC by 1 and returns the word as a signed int.
     * This will give a wrong result if the address is uneven.
     * 
     * @return a signed int containing the signed word read.
     */
    public final int readMemoryWordPCSignedInc()
    {  
        return this.mainMemory[this.pc_reg++];
    }
    
    /**
     * Reads a word as signed short.
     * This will give a wrong result if the address is uneven.
     * 
     * @param address where to read from.
     * 
     * @return a short containing the unsigned word read.
     */
    public final short readMemoryShort(int address)
    {
        // debugMem(address);
        address&=MC68000Cpu.cutOff;                            // !!! remove higher bits  also means that address won't be <0
        if (address>this.totMemSize)
            return 0;   
        return this.mainMemory[address/2];
    }
    
    /**
     * Reads a long word.
     * 
     * @param address where to read from.
     * 
     * @return an int containing the long word read. 
     */
    public final int readMemoryLong(int address)
    {   
        // debugMem(address);
        address&=MC68000Cpu.cutOff;                         // !!! remove higher bits - also means that address won't be <0
        if (address>this.totMemSize-2)
            return 0;     
        address/=2;
        return (this.mainMemory[address]<<16) | (this.mainMemory[address+1]&0xffff);
    }
    
    /**
     * Reads a long word at (PC).
     * 
     * @return an int containing the long word read. 
     */
    public final int readMemoryLongPC()
    {
        return (this.mainMemory[pc_reg]<<16) | (this.mainMemory[pc_reg+1]&0xffff);
    }
    
    /**
     * Reads a long word  at (PC) and increases the PC by 2 (actually by 4).
     * 
     * @return an int containing the long word read. 
     */
    public final int readMemoryLongPCinc()
    {   
        int res=(this.mainMemory[this.pc_reg++]&0xffff)<<16;
        return res|(this.mainMemory[this.pc_reg++]&0xffff);
    }
    
    /**
     * Writes a byte to memory.
     * 
     * @param address where to write to.
     * 
     * @param value the byte to write (in the LSB of the int).
     */
    public void writeMemoryByte(int address, int value)
    {  
        address&=MC68000Cpu.cutOff;                             // !!! remove higher bits  also means that address won't be <0
        if (address>this.totMemSize)
            return;    
        int addr=address/2;
        int val=value;
        short res=(this.mainMemory[addr]);
        if((address&1)!=0)
        {
            res&=0xff00;
            value&=0xff;
        }
        else
        {
            res&=0x00ff;
            value=(value<<8)&0xff00;
        }
        value|=res;
        this.mainMemory[addr]=(short)(value);
        if(address>= this.screenStart)
        {
            this.screen.writeByteToScreen(address,val,value);       // trying to write to screen?
        }
       
    }
    
     /**
     * Writes a word to memory.
     * @param address where to write to.
     * @param value the word to write (in the LSW of the int).
     */
    public void writeMemoryWord(int address, int value)
    {   
        // debugMem(address);
        address&=MC68000Cpu.cutOff;                            // !!! remove higher bits  also means that address won't be <0
        if (address>this.totRamSizeForWord)
            return;  
        this.mainMemory[address/2]= (short)(value);
        if(address>= this.screenStart)
        {
            this.screen.writeWordToScreen(address,value);   // trying to write screen?     
        }
    }
    
    /**
     * Writes a short to memory.
     * 
     * @param address where to write to.
     * @param value the short to write.
     */
    public void writeMemoryShort(int address, short value)
    {   
        // debugMem(address);
        address&=MC68000Cpu.cutOff;                            // !!! remove higher bits  also means that address won't be <0
        if (address>this.totRamSizeForWord)
            return;  
        this.mainMemory[address/2]= value;
        if(address>= this.screenStart)
        {
            this.screen.writeWordToScreen(address,value);   // trying to write screen?
        }
    }
    
    /**
     * Writes a long to the memory.
     * @param address the address where to write to.
     * @param value the value to write.
     */
    public void writeMemoryLong(int address, int value)
    {  
        // debugMem(address);
        address&=MC68000Cpu.cutOff;                            // !!! remove higher bits  also means that address won't be <0
        if (address>this.totRamSizeForLong)
            return;  
        int addr=address/2;
        this.mainMemory[addr]= (short)(value>>>16); 
        this.mainMemory[addr+1]= (short)(value); 
        
        if(address>= this.screenStart)
        {
            this.screen.writeLongToScreen(address,value);   // trying to write screen?
        }
    }
    
    
    private void debugMem(int add)
    {
        if ((add&1)!=0)
            add=add;
    }
    public int memBoundsOK(int address,int maxExten)
    {
        int result=address&MC68000Cpu.cutOff;
        if(address>= this.screenStart && address<this.screenStop)
        {
            return -1;
        }
        if (address+64>this.totMemSize)
            return-1;
        return address/2;
    }
   
    /**
     * Resets this CPU.
     * 
     * This puts the all regs to 0, except for the status reg, put at $2700 (spuervisor mode, no interrupts).
     */
    public void reset()
    {
        this.reg_sr = 0x2700;                               // set into supervisor mode, stop interrupts
        this.pc_reg=0;
        java.util.Arrays.fill(this.addr_regs,0);
        java.util.Arrays.fill(this.data_regs,0);
        this.reg_ssp=0;
        this.reg_usp=0;
        this.stopNow=0;
    }
    
    
    /**
     * Raises the exception of that vector.
     * 
     * @param vector the  exception vector to raise.
     */
    public void raiseException(int vector)
    {
        int address = (vector & 0x00ff) << 2;
        if (vector>32 && vector<36)
            this.trapDispatcher.resetCounter();
        int old_sr = this.reg_sr;                           // SR BEFORE the exception
        if ((this.reg_sr & SUPERVISOR_FLAG) == 0)           // were we in supervisor mode already?....
        {                           
            this.reg_sr |= SUPERVISOR_FLAG;                 // ...no, so set supervisor bit
            this.reg_usp = this.addr_regs[7];               // and change stack pointers
            this.addr_regs[7] = this.reg_ssp;
        }

        //save pc and status regs onto stack which might have changed
        this.addr_regs[7]-=4;
        writeMemoryLong(this.addr_regs[7],this.pc_reg*2);
        this.addr_regs[7]-=2;
        writeMemoryWord(this.addr_regs[7],old_sr);
        this.reg_sr&=~TRACE_FLAG;                           // exceptions unset the trace flag
        this.traceFlag =false;
        int xaddress = readMemoryLong(address);
        if(xaddress == 0)
        {
                //interrupt vector is uninitialised - raise an uninitialised interrupt vector exception instead : vector 15 == 0x003c
                xaddress = readMemoryLong(0x003c);          //if this is zero as well the CPU should halt
                if(xaddress == 0)
                {
                        throw new IllegalArgumentException("Interrupt vector not set for uninitialised interrupt vector while trapping uninitialised vector " + vector);
                }
        }
        this.pc_reg = xaddress/2;
    }

    /**
     * Special Status register exception.
     * 
     */
    public void raiseSRException()
    {
        //always a privilege violation - vector 8
        int address = 32;
        //switch to supervisor mode
        int old_sr = this.reg_sr;

        if((reg_sr & SUPERVISOR_FLAG) == 0)
        {
                reg_sr |= SUPERVISOR_FLAG;	//set supervisor bit
                //switch stacks
                reg_usp = addr_regs[7];
                addr_regs[7] = reg_ssp;
        }

        //subtly different in that the address of the instruction is pushed rather than the address of the next instruction
        //save pc and status regs - operands fetched in supervisor mode so PC at current address
        addr_regs[7] -= 4;
        writeMemoryLong(addr_regs[7], this.currentInstructionAddress);
        this.addr_regs[7] -= 2;
        writeMemoryWord(addr_regs[7], old_sr);

        int xaddress = readMemoryLong(address);
        if(xaddress == 0)
        {
                //interrupt vector is uninitialised - raise a uninitialised interrupt vector exception instead
                xaddress = readMemoryLong(0x003c);
                //if this is zero as well the CPU should halt
                if(xaddress == 0)
                {
                        throw new IllegalArgumentException("Interrupt vector not set for uninitialised interrupt vector while trapping uninitialised vector 8");
                }
        }
        this.pc_reg = xaddress/2;
        this.traceFlag =(this.reg_sr & MC68000Cpu.TRACE_FLAG)!=0;
    }

    /**
     * Sets the status register with a certain value.
     * 
     * @param value the value to set.
     */
    public void setSR(int value)
    {
        //check for supervisor bit change
        if(((this.reg_sr & SUPERVISOR_FLAG) ^ (value & SUPERVISOR_FLAG)) != 0)
        {
            //if changing via this method don't push/pop sr and pc - this is only called by andi/eori/ori to SR
            if((value & SUPERVISOR_FLAG) != 0)
            {
                    this.reg_usp = this.addr_regs[7];
                    this.addr_regs[7] = this.reg_ssp;
            }
            else
            {
                    //switch stacks
                    this.reg_ssp = this.addr_regs[7];
                    this.addr_regs[7] = this.reg_usp;
            }
        }
        this.reg_sr = value;
        this.traceFlag =(this.reg_sr & MC68000Cpu.TRACE_FLAG)!=0;
    }

    /**
     * Sets the trace flag.
     */
    public void testTrace()
    {
        this.traceFlag =(this.reg_sr & MC68000Cpu.TRACE_FLAG)!=0;
    } 
        
    /**
     * Sign extend a byte.
     * 
     * @param value the int containing the byte to be sign extended.
     * 
     * @return the sign extended int.
     */
    public static final int signExtendByte(int value)
    {
        if((value & 0x80)!=0)
        {
            return value | 0xffffff00;
        }
        else
        {
            return value & 0x000000ff;
        }
    }

     /**
     * Sign extend a word.
     * 
     * @param value the int containing the word to be sign extended.
     * 
     * @return the sign extended int.
     */
    public static final int signExtendWord(int value)
    {
        if((value & 0x8000) !=0)
        {
            return value | 0xffff0000;
        }
        else
        {
            return value & 0x0000ffff;
        }
    }

    public boolean isSupervisorMode()
    {
        return (this.reg_sr & SUPERVISOR_FLAG) !=0;
    }
    
    /**
     * Gets the displacement and increments the PC.
     
     * @return  the displacement
     */
    public final int getDisplacement()
    {
        int ext = readMemoryWordPCSignedInc();      // extention word, contains size + displacement+reg
        int displacement=(ext & 0x80)!=0?ext| 0xffffff00:ext&0xff;     //displacemnt
        if((ext & 0x8000) !=0)
        {
            int val = this.addr_regs[(ext >> 12) & 0x07];
            if((ext & 0x0800) == 0)                // word or long register displacement?
            {
                return displacement+  ((val&0x8000)!=0 ?val | 0xffff0000 : val& 0xffff);
            }
            else
            {
               return displacement+val;
            }
        }
        else
        {
            int val = this.data_regs[(ext >> 12) & 0x07];
            if((ext & 0x0800) == 0)                // word or long register displacement?
            {
                return displacement+ ((val&0x8000)!=0?val | 0xffff0000 : val & 0xffff);
            }
            else
            {
               return displacement+val;
            }
        }
    }
    
    
        
    /**
     * This loads all instructions.
     */
    private void loadInstructionSet()
    {        
        // these instructions are always loaded
        new smsqmulator.cpu.instructions.JMP.JMP1().register(this); 
        new smsqmulator.cpu.instructions.JMP.JMP2().register(this); 
        new smsqmulator.cpu.instructions.JMP.JMP3().register(this); 
        new smsqmulator.cpu.instructions.JMP.JMP4().register(this); 
        new smsqmulator.cpu.instructions.JMP.JMP5().register(this); 
        new smsqmulator.cpu.instructions.JMP.JMP6().register(this); 
        new smsqmulator.cpu.instructions.JMP.JMP7().register(this);
        
        new smsqmulator.cpu.instructions.JSR.JSR1().register(this); 
        new smsqmulator.cpu.instructions.JSR.JSR2().register(this); 
        new smsqmulator.cpu.instructions.JSR.JSR3().register(this); 
        new smsqmulator.cpu.instructions.JSR.JSR4().register(this); 
        new smsqmulator.cpu.instructions.JSR.JSR5().register(this); 
        new smsqmulator.cpu.instructions.JSR.JSR6().register(this); 
        new smsqmulator.cpu.instructions.JSR.JSR7().register(this);
        
        new JavaComm().register(this);                                      // add my traps for commo with java prog
       
        new smsqmulator.cpu.instructions.ILLEGALQmon().register(this); 
            
   //     new smsqmulator.cpu.instructions.TEST().register(this);             // one test instruction
        
        /*  -------------------------  total replacement instructions ----------------------------------*/
        
        new smsqmulator.cpu.instructions.ABCD().register(this); 
        new smsqmulator.cpu.instructions.ADD2Dn.ADD_Dn().register(this); 
        new smsqmulator.cpu.instructions.ADD2Dn.ADD_An().register(this); 
        new smsqmulator.cpu.instructions.ADD2Dn.ADD_AnContent().register(this); //nt b
        new smsqmulator.cpu.instructions.ADD2Dn.ADD_AnPlus().register(this);
        new smsqmulator.cpu.instructions.ADD2Dn.ADD_MinusAn().register(this);//nt l al
        new smsqmulator.cpu.instructions.ADD2Dn.ADD_d16An().register(this); 
        new smsqmulator.cpu.instructions.ADD2Dn.ADD_d8AnXn().register(this); 
        new smsqmulator.cpu.instructions.ADD2Dn.ADD_W().register(this); //nt
        new smsqmulator.cpu.instructions.ADD2Dn.ADD_L().register(this); //nt b w aw
        new smsqmulator.cpu.instructions.ADD2Dn.ADD_I().register(this); 
        new smsqmulator.cpu.instructions.ADD2Dn.ADD_d16PC().register(this); //b w
        new smsqmulator.cpu.instructions.ADD2Dn.ADD_d8PCXn().register(this); //nt b l aw al
        new smsqmulator.cpu.instructions.ADD2EA.ADD_AnContent().register(this); 
        new smsqmulator.cpu.instructions.ADD2EA.ADD_AnPlus().register(this); 
        new smsqmulator.cpu.instructions.ADD2EA.ADD_MinusAn().register(this); //nt b w 
        new smsqmulator.cpu.instructions.ADD2EA.ADD_d16An().register(this); 
        new smsqmulator.cpu.instructions.ADD2EA.ADD_d8AnXn().register(this); //nt b w
        new smsqmulator.cpu.instructions.ADD2EA.ADD_W().register(this); //nt
        new smsqmulator.cpu.instructions.ADD2EA.ADD_L().register(this); //nt b w
        new smsqmulator.cpu.instructions.ADDQ.ADDQ_Dn().register(this);    
        new smsqmulator.cpu.instructions.ADDQ.ADDQ_An().register(this);     
        new smsqmulator.cpu.instructions.ADDQ.ADDQ_AnPtr().register(this);  // nt    
        new smsqmulator.cpu.instructions.ADDQ.ADDQ_AnPlus().register(this); // nt b,w   
        new smsqmulator.cpu.instructions.ADDQ.ADDQ_MinusAn().register(this);    
        new smsqmulator.cpu.instructions.ADDQ.ADDQ_d16An().register(this);  // nt w   
        new smsqmulator.cpu.instructions.ADDQ.ADDQ_d8AnXn().register(this);  // nt w   
        new smsqmulator.cpu.instructions.ADDQ.ADDQ_W().register(this);  // nt w   
        new smsqmulator.cpu.instructions.ADDQ.ADDQ_L().register(this);  // nt w   
        new smsqmulator.cpu.instructions.ADDI().register(this);  // nt   
        new smsqmulator.cpu.instructions.ADDXreg().register(this);  // nt   
        new smsqmulator.cpu.instructions.ADDXmem().register(this);  // nt   
        new smsqmulator.cpu.instructions.ANDreg().register(this);// nt
        new smsqmulator.cpu.instructions.ANDmem().register(this);// nt
        new smsqmulator.cpu.instructions.ANDI().register(this);// nt some in all of them
        new smsqmulator.cpu.instructions.ANDI_SR().register(this);// 
        new smsqmulator.cpu.instructions.ANDI_CCR().register(this);// nt
        new smsqmulator.cpu.instructions.ASL.ASLreg().register(this);//nt .b .w
        new smsqmulator.cpu.instructions.ASL.ASLimm().register(this);// all tested
        new smsqmulator.cpu.instructions.ASL.ASLmem().register(this);// nt
        new smsqmulator.cpu.instructions.ASR.ASRimm().register(this); // all ok
        new smsqmulator.cpu.instructions.ASR.ASRreg().register(this); // nt b
        new smsqmulator.cpu.instructions.ASR.ASRmem().register(this); // none tested

        new smsqmulator.cpu.instructions.Bcc().register(this);
        new smsqmulator.cpu.instructions.BRA().register(this);
        new smsqmulator.cpu.instructions.BSR().register(this);
        new smsqmulator.cpu.instructions.BCHGimm().register(this);//nt some modes
        new smsqmulator.cpu.instructions.BCHGreg().register(this);//nt some modes
        new smsqmulator.cpu.instructions.BCLRimm().register(this);//nt some modes
        new smsqmulator.cpu.instructions.BCLRreg().register(this);//nt
        new smsqmulator.cpu.instructions.BSETimm().register(this);//nt some modes
        new smsqmulator.cpu.instructions.BSETreg().register(this);//nt some modes
        new smsqmulator.cpu.instructions.BTST.BTST_Dn().register(this);
        new smsqmulator.cpu.instructions.BTST.BTST_AnContent().register(this);
        new smsqmulator.cpu.instructions.BTST.BTST_AnPlus().register(this); // nt
        new smsqmulator.cpu.instructions.BTST.BTST_MinusAn().register(this); //nt
        new smsqmulator.cpu.instructions.BTST.BTST_d16An().register(this); 
        new smsqmulator.cpu.instructions.BTST.BTST_d8AnXn().register(this); 
        new smsqmulator.cpu.instructions.BTST.BTST_W().register(this); //nt
        new smsqmulator.cpu.instructions.BTST.BTST_L().register(this); //nt
        new smsqmulator.cpu.instructions.BTST.BTST_d16PC().register(this); //nt
        new smsqmulator.cpu.instructions.BTST.BTST_d8PCXn().register(this); 
        
        new smsqmulator.cpu.instructions.CHK().register(this); 
        new smsqmulator.cpu.instructions.CLR.CLR_Dn().register(this);   
        new smsqmulator.cpu.instructions.CLR.CLR_AnContent().register(this);  
        new smsqmulator.cpu.instructions.CLR.CLR_AnPlus().register(this);
        new smsqmulator.cpu.instructions.CLR.CLR_MinusAn().register(this);   
        new smsqmulator.cpu.instructions.CLR.CLR_d16An().register(this);   
        new smsqmulator.cpu.instructions.CLR.CLR_d8AnXn().register(this);   
        new smsqmulator.cpu.instructions.CLR.CLR_W().register(this);   
        new smsqmulator.cpu.instructions.CLR.CLR_L().register(this);
        
        new smsqmulator.cpu.instructions.CMP.CMP_Dn().register(this); 
        new smsqmulator.cpu.instructions.CMP.CMP_An().register(this); // nt aW
        new smsqmulator.cpu.instructions.CMP.CMP_AnContent().register(this); 
        new smsqmulator.cpu.instructions.CMP.CMP_AnPlus().register(this);  
        new smsqmulator.cpu.instructions.CMP.CMP_MinusAn().register(this); // nt L
        new smsqmulator.cpu.instructions.CMP.CMP_d16An().register(this);
        new smsqmulator.cpu.instructions.CMP.CMP_d8AnXn().register(this);
        new smsqmulator.cpu.instructions.CMP.CMP_W().register(this); // nt b w l
        new smsqmulator.cpu.instructions.CMP.CMP_L().register(this); //nt b
        new smsqmulator.cpu.instructions.CMP.CMP_I().register(this); 
        new smsqmulator.cpu.instructions.CMP.CMP_d16PC().register(this); //nt b
        new smsqmulator.cpu.instructions.CMP.CMP_d8PCXn().register(this); // nt w l
        
        new smsqmulator.cpu.instructions.CMPI.CMPI_Dn().register(this);
        new smsqmulator.cpu.instructions.CMPI.CMPI_AnContent().register(this);
        new smsqmulator.cpu.instructions.CMPI.CMPI_AnPlus().register(this);
        new smsqmulator.cpu.instructions.CMPI.CMPI_MinusAn().register(this);
        new smsqmulator.cpu.instructions.CMPI.CMPI_d16An().register(this);
        new smsqmulator.cpu.instructions.CMPI.CMPI_d8AnXn().register(this);
        new smsqmulator.cpu.instructions.CMPI.CMPI_W().register(this); // nt w l
        new smsqmulator.cpu.instructions.CMPI.CMPI_L().register(this); //nt
        new smsqmulator.cpu.instructions.CMP.CMPM().register(this);
        
        new smsqmulator.cpu.instructions.DBcc().register(this);
        new smsqmulator.cpu.instructions.DIVSW().register(this);    // nt case 7(1,2,3), case 3,4,6
        new smsqmulator.cpu.instructions.DIVUW().register(this);    // nt : case 7 (all 4)

        new smsqmulator.cpu.instructions.EOR().register(this); // nt
        new smsqmulator.cpu.instructions.EORI().register(this); // nt
        new smsqmulator.cpu.instructions.EORI_CCR().register(this); // nt
        new smsqmulator.cpu.instructions.EORI_SR().register(this); // nt
        new smsqmulator.cpu.instructions.EXG().register(this);
        new smsqmulator.cpu.instructions.EXT().register(this);
        
        new smsqmulator.cpu.instructions.ILLEGAL().register(this); 
        
        new smsqmulator.cpu.instructions.LEA.LEA_An().register(this);
        new smsqmulator.cpu.instructions.LEA.LEA_d16An().register(this);
        new smsqmulator.cpu.instructions.LEA.LEA_d8AnXn().register(this);
        new smsqmulator.cpu.instructions.LEA.LEA_W().register(this);
        new smsqmulator.cpu.instructions.LEA.LEA_L().register(this);
        new smsqmulator.cpu.instructions.LEA.LEA_d16PC().register(this);
        new smsqmulator.cpu.instructions.LEA.LEA_d8PCXn().register(this);
        new smsqmulator.cpu.instructions.LINK().register(this); 
        new smsqmulator.cpu.instructions.LSL.LSLreg().register(this);// 
        new smsqmulator.cpu.instructions.LSL.LSLimm().register(this);// 
        new smsqmulator.cpu.instructions.LSL.LSLmem().register(this);// nt
        new smsqmulator.cpu.instructions.LSR.LSRreg().register(this);// nt b
        new smsqmulator.cpu.instructions.LSR.LSRimm().register(this);// ok
        new smsqmulator.cpu.instructions.LSR.LSRmem().register(this);// nt b
       
        new smsqmulator.cpu.instructions.MOVE.MOVEDn1().register(this);  // move where source is DN
        new smsqmulator.cpu.instructions.MOVE.MOVEDn2().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEDn3().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEDn4().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEDn5().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEDn6().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEDn7().register(this);      //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEDn8().register(this);      // nt w l

        new smsqmulator.cpu.instructions.MOVE.MOVEAn1().register(this);
       new smsqmulator.cpu.instructions.MOVE.MOVEAn2bis().register(this); // special case for MOVE.L a0,(a2) if A2 =$10 : DO NOT WRITE THIS
        new smsqmulator.cpu.instructions.MOVE.MOVEAn2().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAn3().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAn4().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAn5().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAn6().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAn7().register(this);   
        new smsqmulator.cpu.instructions.MOVE.MOVEAn8().register(this);
     
        new smsqmulator.cpu.instructions.MOVE.MOVEAnC1().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnC2().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnC3().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnC4().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnC5().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnC6().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnC7().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnC8().register(this);
       
        new smsqmulator.cpu.instructions.MOVE.MOVEAnP1().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnP2().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnP3().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnP4().register(this);              
        new smsqmulator.cpu.instructions.MOVE.MOVEAnP5().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnP6().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnP7().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnP8().register(this);     //nt

        new smsqmulator.cpu.instructions.MOVE.MOVEAnM1().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnM2().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnM3().register(this);     //nt     
        new smsqmulator.cpu.instructions.MOVE.MOVEAnM4().register(this);        
        new smsqmulator.cpu.instructions.MOVE.MOVEAnM5().register(this);     //nt W   
        new smsqmulator.cpu.instructions.MOVE.MOVEAnM6().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnM7().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnM8().register(this);     //nt

        new smsqmulator.cpu.instructions.MOVE.MOVEAnD1().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnD2().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnD3().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnD4().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnD5().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnD6().register(this);     //nt B W
        new smsqmulator.cpu.instructions.MOVE.MOVEAnD7().register(this);     //
        new smsqmulator.cpu.instructions.MOVE.MOVEAnD8().register(this);     //nt W L
 
        new smsqmulator.cpu.instructions.MOVE.MOVEAnI1().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnI2().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnI3().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnI4().register(this);     //nt B
        new smsqmulator.cpu.instructions.MOVE.MOVEAnI5().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnI6().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnI7().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnI8().register(this);     //nt

        new smsqmulator.cpu.instructions.MOVE.MOVEAnW1().register(this);     
        new smsqmulator.cpu.instructions.MOVE.MOVEAnW2().register(this);     // nt  
        new smsqmulator.cpu.instructions.MOVE.MOVEAnW3().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnW4().register(this);     // nt B W
        new smsqmulator.cpu.instructions.MOVE.MOVEAnW5().register(this);     //nt B L
        new smsqmulator.cpu.instructions.MOVE.MOVEAnW6().register(this);     // ntnt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnW7().register(this);     // nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnW8().register(this);  
  
        new smsqmulator.cpu.instructions.MOVE.MOVEAnL1().register(this);     //nt w l   
        new smsqmulator.cpu.instructions.MOVE.MOVEAnL2().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnL3().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnL4().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnL5().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnL6().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnL7().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnL8().register(this);     //nt
       
        new smsqmulator.cpu.instructions.MOVE.MOVEAnT1().register(this);     // MOVE where source is immediate daTa
        new smsqmulator.cpu.instructions.MOVE.MOVEAnT2().register(this);  
        new smsqmulator.cpu.instructions.MOVE.MOVEAnT3().register(this);  
        new smsqmulator.cpu.instructions.MOVE.MOVEAnT4().register(this);  
        new smsqmulator.cpu.instructions.MOVE.MOVEAnT5().register(this);  
        new smsqmulator.cpu.instructions.MOVE.MOVEAnT6().register(this);  
        new smsqmulator.cpu.instructions.MOVE.MOVEAnT7().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnT8().register(this);     //nt W

        new smsqmulator.cpu.instructions.MOVE.MOVEAnY1().register(this);     // MOVE where source is D16(6C) 
        new smsqmulator.cpu.instructions.MOVE.MOVEAnY2().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnY3().register(this);
        new smsqmulator.cpu.instructions.MOVE.MOVEAnY4().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnY5().register(this);     
        new smsqmulator.cpu.instructions.MOVE.MOVEAnY6().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnY7().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnY8().register(this);     //nt 

        new smsqmulator.cpu.instructions.MOVE.MOVEAnX1().register(this);   
        new smsqmulator.cpu.instructions.MOVE.MOVEAnX2().register(this);     //nt  L
        new smsqmulator.cpu.instructions.MOVE.MOVEAnX3().register(this);   
        new smsqmulator.cpu.instructions.MOVE.MOVEAnX4().register(this);     //nt  b w
        new smsqmulator.cpu.instructions.MOVE.MOVEAnX5().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnX6().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnX7().register(this);     //nt
        new smsqmulator.cpu.instructions.MOVE.MOVEAnX8().register(this);     //nt

        new smsqmulator.cpu.instructions.MOVEA.MOVEA1().register(this);  
        new smsqmulator.cpu.instructions.MOVEA.MOVEA2().register(this);     
        new smsqmulator.cpu.instructions.MOVEA.MOVEA3().register(this);       // 
        new smsqmulator.cpu.instructions.MOVEA.MOVEA4().register(this);       // 
        new smsqmulator.cpu.instructions.MOVEA.MOVEA5().register(this);       //nt W
        new smsqmulator.cpu.instructions.MOVEA.MOVEA6().register(this);       // 
        new smsqmulator.cpu.instructions.MOVEA.MOVEA7().register(this);       // 
        new smsqmulator.cpu.instructions.MOVEA.MOVEA8().register(this);       // 
        new smsqmulator.cpu.instructions.MOVEA.MOVEA9().register(this);       //nt
        new smsqmulator.cpu.instructions.MOVEA.MOVEA10().register(this);      // 
        new smsqmulator.cpu.instructions.MOVEA.MOVEA11().register(this);      //nt W
        new smsqmulator.cpu.instructions.MOVEA.MOVEA12().register(this);      //
        
        new smsqmulator.cpu.instructions.MOVEM.MOVEM2Mem1().register(this); // nt w
        new smsqmulator.cpu.instructions.MOVEM.MOVEM2Mem2().register(this); 
        new smsqmulator.cpu.instructions.MOVEM.MOVEM2Mem3().register(this); 
        new smsqmulator.cpu.instructions.MOVEM.MOVEM2Mem4().register(this); // nt
        new smsqmulator.cpu.instructions.MOVEM.MOVEM2Mem5().register(this); // nt
        new smsqmulator.cpu.instructions.MOVEM.MOVEM2Mem6().register(this);

        new smsqmulator.cpu.instructions.MOVEM.MOVEM2Reg1().register(this); // nt w
        new smsqmulator.cpu.instructions.MOVEM.MOVEM2Reg2().register(this);
        new smsqmulator.cpu.instructions.MOVEM.MOVEM2Reg3().register(this); 
        new smsqmulator.cpu.instructions.MOVEM.MOVEM2Reg4().register(this); // nt
        new smsqmulator.cpu.instructions.MOVEM.MOVEM2Reg5().register(this); // nt
        new smsqmulator.cpu.instructions.MOVEM.MOVEM2Reg6().register(this); 
        new smsqmulator.cpu.instructions.MOVEM.MOVEM2Reg7().register(this); // nt w
        new smsqmulator.cpu.instructions.MOVEM.MOVEM2Reg8().register(this); //
                 
        new smsqmulator.cpu.instructions.MOVEQ().register(this);  
        new smsqmulator.cpu.instructions.MOVEPr2m().register(this);  // nt
        new smsqmulator.cpu.instructions.MOVEPm2r().register(this);  //nt
        new smsqmulator.cpu.instructions.MOVE_USP().register(this);  //nt
        new smsqmulator.cpu.instructions.MOVE_FROM_CCR().register(this);  //case 0,7-4 tested ok, all others nt
        new smsqmulator.cpu.instructions.MOVE_TO_CCR().register(this);  //case 0,7-4 tested ok, all others nt
        new smsqmulator.cpu.instructions.MOVE_FROM_SR().register(this);  //some nt
        new smsqmulator.cpu.instructions.MOVE_TO_SR().register(this);  // some nt
        new smsqmulator.cpu.instructions.MULSW().register(this);    // nt case 7(1,2,3), case 3,4,6
        new smsqmulator.cpu.instructions.MULUW().register(this);    // nt : case 7 (all 4)
     
        new smsqmulator.cpu.instructions.NBCD().register(this); 
        new smsqmulator.cpu.instructions.NEG().register(this); 
        new smsqmulator.cpu.instructions.NEGX().register(this); //nt
        new smsqmulator.cpu.instructions.NOT().register(this); // ok
        new smsqmulator.cpu.instructions.NOP().register(this); // ok
        
        new smsqmulator.cpu.instructions.ORreg().register(this); // some of all sizes : ok    
        new smsqmulator.cpu.instructions.ORmem().register(this); // some of all sizes : ok    
        new smsqmulator.cpu.instructions.ORI().register(this); // some of all sizes : ok 
        new smsqmulator.cpu.instructions.ORI_SR().register(this); // 
        new smsqmulator.cpu.instructions.ORI_CCR().register(this); // 
        
        new smsqmulator.cpu.instructions.PEA().register(this); // nt 7-3

        new smsqmulator.cpu.instructions.ROR.RORreg().register(this);//
        new smsqmulator.cpu.instructions.ROR.RORimm().register(this);// 
        new smsqmulator.cpu.instructions.ROR.RORmem().register(this);// 
        new smsqmulator.cpu.instructions.ROL.ROLreg().register(this);// nt
        new smsqmulator.cpu.instructions.ROL.ROLimm().register(this);// ok
        new smsqmulator.cpu.instructions.ROL.ROLmem().register(this);// nt except case 6
        new smsqmulator.cpu.instructions.ROX.ROXLreg().register(this);// nt
        new smsqmulator.cpu.instructions.ROX.ROXLimm().register(this);// nt b
        new smsqmulator.cpu.instructions.ROX.ROXLmem().register(this);// nt 
        new smsqmulator.cpu.instructions.ROX.ROXRreg().register(this);// nT
        new smsqmulator.cpu.instructions.ROX.ROXRimm().register(this);// nt 
        new smsqmulator.cpu.instructions.ROX.ROXRmem().register(this);// nt 
        new smsqmulator.cpu.instructions.RTS().register(this); 
        new smsqmulator.cpu.instructions.RTE().register(this); 
        new smsqmulator.cpu.instructions.RTR().register(this); //nt
        new smsqmulator.cpu.instructions.RESET().register(this); 
        
        new smsqmulator.cpu.instructions.SBCD().register(this); 
        new smsqmulator.cpu.instructions.SCC().register(this); // nt all of case 7
        new smsqmulator.cpu.instructions.STOP().register(this); 
        new smsqmulator.cpu.instructions.SUB2Dn.SUB_Dn().register(this); 
        new smsqmulator.cpu.instructions.SUB2Dn.SUB_An().register(this);
        new smsqmulator.cpu.instructions.SUB2Dn.SUB_AnContent().register(this); //nt aw
        new smsqmulator.cpu.instructions.SUB2Dn.SUB_AnPlus().register(this);
        new smsqmulator.cpu.instructions.SUB2Dn.SUB_MinusAn().register(this);//nt
        new smsqmulator.cpu.instructions.SUB2Dn.SUB_d16An().register(this); //nt aw
        new smsqmulator.cpu.instructions.SUB2Dn.SUB_d8AnXn().register(this); // nt aw al
        new smsqmulator.cpu.instructions.SUB2Dn.SUB_W().register(this); //nt
        new smsqmulator.cpu.instructions.SUB2Dn.SUB_L().register(this); //nt b w aw 
        new smsqmulator.cpu.instructions.SUB2Dn.SUB_I().register(this); 
        new smsqmulator.cpu.instructions.SUB2Dn.SUB_d16PC().register(this); //nt
        new smsqmulator.cpu.instructions.SUB2Dn.SUB_d8PCXn().register(this); //nt
        new smsqmulator.cpu.instructions.SUB2EA.SUB_AnContent().register(this); 
        new smsqmulator.cpu.instructions.SUB2EA.SUB_AnPlus().register(this); 
        new smsqmulator.cpu.instructions.SUB2EA.SUB_MinusAn().register(this); //nt b w 
        new smsqmulator.cpu.instructions.SUB2EA.SUB_d16An().register(this); 
        new smsqmulator.cpu.instructions.SUB2EA.SUB_d8AnXn().register(this); //nt b w
        new smsqmulator.cpu.instructions.SUB2EA.SUB_W().register(this); //nt
        new smsqmulator.cpu.instructions.SUB2EA.SUB_L().register(this); //nt b w
        new smsqmulator.cpu.instructions.SUBI().register(this);
        new smsqmulator.cpu.instructions.SUBQ.SUBQ_Dn().register(this);    
        new smsqmulator.cpu.instructions.SUBQ.SUBQ_An().register(this);     
        new smsqmulator.cpu.instructions.SUBQ.SUBQ_AnPtr().register(this);  // nt    
        new smsqmulator.cpu.instructions.SUBQ.SUBQ_AnPlus().register(this); // nt b,w   
        new smsqmulator.cpu.instructions.SUBQ.SUBQ_MinusAn().register(this);       
        new smsqmulator.cpu.instructions.SUBQ.SUBQ_d16An().register(this);  // nt w   
        new smsqmulator.cpu.instructions.SUBQ.SUBQ_d8AnXn().register(this);  // nt w   
        new smsqmulator.cpu.instructions.SUBQ.SUBQ_W().register(this);  // nt w   
        new smsqmulator.cpu.instructions.SUBQ.SUBQ_L().register(this);  // nt w    
        new smsqmulator.cpu.instructions.SUBXmem().register(this); //nt
        new smsqmulator.cpu.instructions.SUBXreg().register(this); //nt
        new smsqmulator.cpu.instructions.SWAP().register(this);  // nt w     

        new smsqmulator.cpu.instructions.TAS().register(this); 
        new smsqmulator.cpu.instructions.TRAP().register(this); 
        new smsqmulator.cpu.instructions.TRAPV().register(this); 
        new smsqmulator.cpu.instructions.TST.TST_Dn().register(this);      
 //       new smsqmulator2.cpu.instructions.TST.TST_An().register(this);      // for 68020+
        new smsqmulator.cpu.instructions.TST.TST_AnContent().register(this);      // nt b w
        new smsqmulator.cpu.instructions.TST.TST_AnPlus().register(this);      
        new smsqmulator.cpu.instructions.TST.TST_MinusAn().register(this);      // nt b w
        new smsqmulator.cpu.instructions.TST.TST_d16An().register(this);      
        new smsqmulator.cpu.instructions.TST.TST_d8AnXn().register(this);     
        new smsqmulator.cpu.instructions.TST.TST_W().register(this);      // nt b w
        new smsqmulator.cpu.instructions.TST.TST_L().register(this);      // nt w
  //      new smsqmulator2.cpu.instructions.TST.TST_imm().register(this);     // nt  for 68020+

        new smsqmulator.cpu.instructions.UNLK().register(this); 
        new smsqmulator.cpu.instructions.NULL().register(this.i_table,this); //!!!!! make sure there are no more null instructions.       
    }
   
    /**
     * Adds an instruction to the list (array) of instructions
     * @param opcode the opcode of the instruction to be added to the cpu's list of instructions. This is the index into the instructions array.
     * @param i the instruction to add.
     */
    public void addInstruction(int opcode, smsqmulator.cpu.Instruction i)
    {
        if(i_table[opcode] == null)
        {
            this.i_table[opcode]=i;
        }
    }
    
    
    /* ------------------------- Disassembling ------------------------------------*/
  
    public smsqmulator.cpu.DisassembledOperand disassembleSrcEA(int address, int mode, int reg, smsqmulator.cpu.Size sz)
    {
        return disassembleEA(address, mode, reg, sz, true);
    }

    
    public smsqmulator.cpu.DisassembledOperand disassembleDstEA(int address, int mode, int reg, smsqmulator.cpu.Size sz)
    {
        return disassembleEA(address, mode, reg, sz, false);
    }

    protected smsqmulator.cpu.DisassembledOperand disassembleEA(int address, int mode, int reg, smsqmulator.cpu.Size sz, boolean is_src)
    {
        int bytes_read = 0;
        int mem = 0;
        disasmBuffer.delete(0, disasmBuffer.length());

        switch(mode)
        {
            case 0:
            {
                disasmBuffer.append("d").append(reg);
                break;
            }
            case 1:
            {
                disasmBuffer.append("a").append(reg);
                break;
            }
            case 2:
            {
                disasmBuffer.append("(a").append(reg).append(")");
                break;
            }
            case 3:
            {
                disasmBuffer.append("(a").append(reg).append(")+");
                break;
            }
            case 4:
            {
                disasmBuffer.append("-(a").append(reg).append(")");
                break;
            }
            case 5:
            {
                mem = readMemoryWordSigned(address);
                disasmBuffer.append(String.format("$%04x",(short)mem)).append("(a").append(reg).append(")");
                bytes_read = 2;
                break;
            }
            case 6:
            {
                mem = readMemoryWord(address);
                int dis = signExtendByte(mem);
                disasmBuffer.append(String.format("$%02x",(byte)dis)).append("(a").append(reg).append(",");
                disasmBuffer.append(((mem & 0x8000) != 0 ? "a" : "d")).append((mem >> 12) & 0x07).append(((mem & 0x0800) != 0 ? ".l" : ".w")).append(")");
                bytes_read = 2;
                break;
            }
            case 7:
            {
                switch(reg)
                {
                    case 0:
                    {
                        mem = readMemoryWord(address);
                        disasmBuffer.append(String.format("$%04x", mem));
                        bytes_read = 2;
                        break;
                    }
                    case 1:
                    {
                        mem = readMemoryLong(address);
                        disasmBuffer.append(String.format("$%08x", mem));
                        bytes_read = 4;
                        break;
                    }
                    case 2:
                    {
                        mem = readMemoryWordSigned(address);
                        disasmBuffer.append(String.format("$%04x(pc)",(short)mem));
                        bytes_read = 2;
                        break;
                    }
                case 3:
                {
                        mem = readMemoryWord(address);
                        int dis = signExtendByte(mem);
                        disasmBuffer.append(String.format("$%02x(pc,", (byte)dis));
                        disasmBuffer.append(((mem & 0x8000) != 0 ? "a" : "d")).append((mem >> 12) & 0x07).append(((mem & 0x0800) != 0 ? ".l" : ".w")).append(")");
                        bytes_read = 2;
                        break;
                    }
                    case 4:
                    {
                        if(is_src)
                        {
                                if(sz == smsqmulator.cpu.Size.Long)
                                {
                                    mem = readMemoryLong(address);
                                    bytes_read = 4;
                                    disasmBuffer.append(String.format("#$%08x", mem));
                                }
                                else
                                {
                                    mem = readMemoryWord(address);
                                    bytes_read = 2;
                                    disasmBuffer.append(String.format("#$%04x", mem));

                                    if(sz == smsqmulator.cpu.Size.Byte)
                                    {
                                        mem &= 0x00ff;
                                    }
                                }
                        }
                        else
                        {
                            if(sz == smsqmulator.cpu.Size.Byte)
                            {
                                disasmBuffer.append("ccr");
                            }
                            else
                            {
                                disasmBuffer.append("sr");
                            }
                        }
                            break;
                    }
                    default:
                    {
                        throw new IllegalArgumentException("Invalid reg specified for mode 7: " + reg);
                    }
                }
                break;
            }
            default:
            {
                throw new IllegalArgumentException("Invalid mode specified: " + mode);
            }
        }
        return new smsqmulator.cpu.DisassembledOperand(disasmBuffer.toString(), bytes_read, mem);
    }
       
    
    
     /**
     * Gets the instruction for an opcode.
     * 
     * @param opcode the opcode for which to get the instruction.
     * 
     * @return the instruction for this opcode.
     */
    public smsqmulator.cpu.Instruction getInstructionFor(int opcode)
    {
        return i_table[opcode];
    }
    
    /* ------------------------------------------------------  SMSQE specific routines  -----------------------------*/  
    
    
    /*---------------------------------- Methods involving the screen -----------------------*/
    
    /**
     * Gets the address of the start and end of the screen.
     * 
     * @return an array with the address of the start and end of the screen.
     */
    public int []getScreenAddresses()
    {
        int[]v={this.screenStart,this.screenStop};
        return v;
    }
    
     
    /**
     * Move a block of pixels around (for 8 and 16 bit modes only!).
     * 
     * Do not use for QL screen mode!
     * 
     * Remember the VRAM is part of the mainMemory array, so moving pixels about is just moving bytes/words in the 
     * main memory.
     * 
     * Attention however, the block size is given in "pixels". This means that, in mode 32, each pixel represents a
     * word, which suits me just dandy as my mem is in words, too. In 8 bit mode, however, each pixels is represented by a 
     * byte. So, if any X parameter (either size or origs) is not even, I chicken out and let SMSQ/E handle the move.
     * 
     * If the move is handled here directly, this also does the RTS.
     * The return to MSQ/E  is thus either right after the call to this routine, with the Z flag unset, or to the caller of
     * the mblock routine in SMSQ/E, with the Z flag set.
     */
    public void moveBlock()
    {
        
        if (this.data_regs[1]!=0)
        {
            int A4 = this.addr_regs[4];                         // source address
            int A5 = this.addr_regs[5];                         // destination address
    //        try
      //      {
            if (copyMem (A4,A5))                                    // first copy within memory
    //        }
       /*     catch (Exception e)
            {
                e.printStackTrace();
                copyMem (A4,A5);  
            }
         */
            {
            if (A5>=this.screenStart &&  A5< this.screenStop)   // if dest is within screen, paint the block on the screen
            {
                try
                {
                boolean copyFromScreen=(A4>=this.screenStart &&  A4< this.screenStop); //source is within screen
                this.screen.moveBlock(this,copyFromScreen);     // show block in screen object
                }
                catch (Exception e)
                {
                e.printStackTrace();
                }
            }
            }
        }
            
        this.pc_reg=readMemoryLong(this.addr_regs[7])/2;
        this.addr_regs[7] += 4;                                 // do rts
        this.data_regs[0]=0;                                    // preset all OK
        this.reg_sr |=4;
    }
        
     /**
      * Copies a block of memory within the main memory or within or to/from screen.
      * !!USE ONLY IN 8 OR 16 BIT COLOUR MODES !!!
      * 
      * The source and the destination might overlap.
      * 
      * @param A4 base address of source 
      * @param A5 base address of destination
      */
    private boolean copyMem(int srcStart,int destStart)
    {
        int xs = (this.data_regs[1]>>>16)&0xffff;
        int ys=this.data_regs[1]&0xffff;                        //  x|y size of block Attenti0n = in mode32, the width is in words, not in bytes
        if ((xs&0x8000)!=0)
            return false;
        if ((ys&0x8000)!=0)
            return false;
        int srcXo=(this.data_regs[2]>>>16)&0xffff;              //  x|y origin of source
        int srcYo=this.data_regs[2]&0xffff;
        int destXo= (this.data_regs[3]>>>16) &0xffff;
        int destYo=this.data_regs[3]&0xffff ;                   // same for destination
        int srcInc=this.addr_regs[2];
        int destInc=this.addr_regs[3];
        int divisor=this.screen.getDivisor();                   // 1 for screen mode 32; 2 for 16

        if( divisor ==2 && ( ((destXo & 1)!=0) || ((srcXo & 1)!=0) ||((srcStart &1)!=0) || ((destStart &1)!=0) || ((srcInc &1 )!=0) || ((destInc &1)!=0) ||((xs &1)!=0)))
        {
                                                                // any address is uneven, or size to move is uneven
            divisor = divisor==1?2:1;                           // flip divisor
            xs*=divisor;
            srcXo*=divisor;
            destXo*=divisor;                                    // 1 for screen mode 32; 2 for 16

            destInc*=divisor;
            srcInc*=divisor;                                    // adjust for word sized accesses
            srcStart+=(srcYo*srcInc)+srcXo;                     // index into array for first pixel : src
            destStart+=(destYo*destInc)+destXo;                 // index into array for first pixel : dest 
            if (srcStart>destStart)
            {
                for (int iY=0;iY<ys;iY++)
                {
                    int destl=destStart;
                    for (int iX=srcStart;iX<srcStart+xs;iX++,destStart++)
                    {
                        writeMemoryByte(destStart,readMemoryByteSigned(iX));
                    }
                    srcStart+=srcInc;                           // pop down to next source line 
                    destStart=destl+destInc;                    // same for destination line
                }
            }
            else
            {
                srcStart+=(ys-1)*srcInc;
                destStart+=(ys-1)*destInc;
                for (int iY=0;iY<ys;iY++)
                {   
                    int destl=destStart;
                    for (int iX=srcStart;iX<srcStart+xs;iX++,destStart++)
                    {
                        writeMemoryByte(destStart,readMemoryByteSigned(iX));
                    }
                    srcStart-=srcInc;                           // pop down to next source line 
                    destStart=destl-destInc;                    // same for destination line
                }
            } 
        }
        else
        {                                               
            xs/=divisor;
            srcXo/=divisor;
            destXo/=divisor;                                    // 1 for screen mode 32; 2 for 16

            srcStart+=srcInc*srcYo + srcXo*2;
            srcStart/=2;
            destStart+=destInc*destYo + destXo*2;
            destStart/=2;
            
            srcInc/=2;
            destInc/=2;
            
            if (srcStart>destStart)
            {
                for (int iY=0;iY<ys;iY++)
                {   
                    System.arraycopy(this.mainMemory,srcStart, this.mainMemory,destStart,xs);
                    srcStart+=srcInc;                               // pop down to next source line 
                    destStart+=destInc;                             // same for destination line
                }
            }
            else
            {
                srcStart+=(ys-1)*srcInc;
                destStart+=(ys-1)*destInc;
                for (int iY=0;iY<ys;iY++)
                {   
                    System.arraycopy(this.mainMemory,srcStart, this.mainMemory,destStart,xs);
                    srcStart-=srcInc;                               // pop down to next source line 
                    destStart-=destInc;                             // same for destination line
                }
            } 
        }
        return true;
    }
    
     /**
     * This combines two blocks (source 1, source2) with alpha blending and puts the result into the destination.
     * The destination MUST be the screen, else this returns to SMSQ/E with an error in D0.
     * Source2 must have a row increment of destination row increment + 4.
     */
    public void combineBlocks()
    { 
        if (addr_regs[5]!=this.screenStart)                 // check that destination is the screen.
        {
            this.data_regs[0]=-14;
            return;
        }
        this.screen.combineBlocks(this);
    }
    
    /**
     * This switches QL screen emulation on or off. If on, any writes to the QL screen area are copied to the true display.
     * This ALWAYS FAILS HERE with 'not implemented' error.
     * 
     * @param origins origins in y screen which should correspond to (0,0) in QL screen.
     * @param QLScreenMode the screen mode the QL screen is supposed to be in.
     */
    public void setCopyScreen(int QLScreenMode,int origins)
    {
        this.data_regs[0]=Types.ERR_NIMP;
        this.reg_sr&=~4;                                // clear Z flag : error
    }
        
     /*   Convenience methods to read / write SMSQE style strings to/from memory.*/
    
    
   /**
    * Returns a java String from an SMSQE string at a certain address MAKING accented chars conversion.
    * 
    * @param address where the string lies in memory.
    * 
    * @return the String, may be "" but will not be <code>null</code>.
    * <p> This makes character conversion from SMSQQE charset to Java charset.
    */
    public String readSmsqeString(int address)
    {
        address&=smsqmulator.cpu.MC68000Cpu.cutOff;             // !!! remove higher bits  also means that address won't be <0
        short res;
        if (address>this.totMemSize)
            return"";  
               
        short count=this.mainMemory[address/2];             // nbr of bytes in string
        if (count==0 || ((address+count+2)>this.totMemSize))// don't go above max ROM address
            return "";
        address/=2;                                         // word sized mem access
        address++;                                          // I already got the length word
        StringBuilder result=new StringBuilder(count);
        for (int i=0;i<count;i+=2,address++)
        {   
            res=this.mainMemory[address];
            result.append(smsqmulator.Helper.convertToJava((byte)(res>>>8)));
            result.append(smsqmulator.Helper.convertToJava((byte)(res&0xff)));
        }
        if ((count & 1) == 1)                               // string was one too long
            result.setLength(result.length()-1);
        return result.toString();
    }
    
    /**
    * Writes a java String as an SMSQE string to a certain address MAKING accented chars conversion.
    * 
    * @param address - where to start writing.
    * @param s the <code>String</code> to write.
    * @param maxLength the max length of the string to write NOT INCLUDING THE LENGTH WORD, -1 if no max length.
    */
    public void writeSmsqeString(int address,String s,int maxLength)
    {
        writeSmsqeString(address,s,true,maxLength);
    }
   
    /**
    * Writes a java String as an SMSQE string to a certain address MAKING accented chars conversion.
    * 
    * @param address - where to start writing.
    * @param s the <code>String</code> to write.
    * @param writeLength <code>true</code> if the length of the string should be prepended to the string (as would be usual in SMSQE)
    * @param maxLength the max length of the string to write NOT INCLUDING THE LENGTH WORD, -1 if no max length.
    */
    public void writeSmsqeString(int address,String s,boolean writeLength,int maxLength)
    {
        address&=MC68000Cpu.cutOff;
        if(s==null)
            return;                                         // strange string, or writing normal string to screen mem : that doesn't make sense here!
        if  (s.isEmpty())
        {
            if (writeLength)
                this.mainMemory[address/2]=0;
            return;
        }
        int count=s.length(); 
        if ((maxLength!=-1) && (count>maxLength))
        {
            count=maxLength;
        }
        
        if (count>32200)
            count=32200;
        if (((2+address+count)>this.totMemSize))            // don't write above max ROM address
            return;
        address/=2;                                         // memory is array of shorts
        boolean odd=(count&1)==1;
        if (writeLength)
            this.mainMemory[address++]=(short)count;
        count&=0xfffffffe;                                  // make even if odd count
        short res,res1;
        for (int i=0;i<count;i+=2,address++)
        {
            res=(byte)smsqmulator.Helper.convertToSMSQE(s.charAt(i)); // convert char if need be
         //   res=(byte)smsqmulator2.Helper.convertToSMSQE(s.substring(i,i+1)); // convert char if need be
            res<<=8;
            res1=(byte) (smsqmulator.Helper.convertToSMSQE(s.charAt(i+1)));
            res|=(res1&0xff);
            this.mainMemory[address]=res;
        }
        if (odd)
        {
            count=(byte)smsqmulator.Helper.convertToSMSQE(s.charAt(s.length()-1));
            count<<=8;
            res=this.mainMemory[address];
            res&=0xff;
            this.mainMemory[address]=(short)(count|res);
        }
    }
   
    /* ---------------------------- FIle operations --------------------------------*/
    
    /**
     * Write bytes read from memory to a filechannel.
     * 
     * @param address where to start reading.
     * @param nbrOfBytes how many bytes to write.
     * @param outChannel the channel to write to.
     * 
     * @return nbr of bytes written to channel.
     * 
     * @throws java.io.IOException from the file channed I/O operations.
     */
    public int writeToFile(int address,int nbrOfBytes, java.nio.channels.FileChannel outChannel) throws java.io.IOException
    {
        if (nbrOfBytes==0)
            return 0;                                       // no need to do anything
        address&=MC68000Cpu.cutOff;
        if ((address+nbrOfBytes)>this.totMemSize)           // don't read above max ROM address
        {
            nbrOfBytes=this.totMemSize-address;
            if (nbrOfBytes<1)
                return 0;                                   // should this generate an error ?
        }
        // first make a bytebuffer of sufficient capacity
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(nbrOfBytes);
        if ((address&1)==1)                                 // start at an odd address
        {
            for (int i=0;i<nbrOfBytes;i++,address++)
                buffer.put(i,(byte)(readMemoryByte(address)));
        }
        else
        {
            address/=2;
            for (int i=0;i<nbrOfBytes-1;i+=2,address++)
            {
                buffer.putShort(i, this.mainMemory[address]);
            }
            
            if (((nbrOfBytes) & 1) == 1)
                buffer.put(nbrOfBytes-1, (byte)(this.mainMemory[address]>>8));
        }
            // now write to file
        return outChannel.write(buffer);                    // write and return nbr of bytes written
    }
    
    /**
     * Creates a ByteBuffer and writes bytes read from the memory to it.
     * The start address where to take the bytes from and length (nbr of bytes to get) are in the registers A1 and D2 (long)
     * 
     * @param offsetInBuffer nbr of bytes to add to the size, and also start of where to put these bytes in the newly created buffer.
     * 
     * @return the newly created ByteBuuffer or null if there was an error.
     * 
     * NOTE THIS PRESUMES THAT THE START ADDRESS IS EVEN
     * 
     */
    public java.nio.ByteBuffer writeToBuffer(int offsetInBuffer)
    {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(this.data_regs[2]+offsetInBuffer);
        if(writeToBuffer(buffer,offsetInBuffer)==0)
            return null;
        return buffer;
    }
    
    /**
     * Writes bytes read from the memory to a ByteBuffer.
     * The start address where to take the bytes from and length (nbr of bytes to get) are in the registers A1 and D2 (long).<p>
     * <b>NOTE THIS PRESUMES THAT THE START ADDRESS IS EVEN!</b>
     * 
     * @param buffer the ByteBuffer into which the bytes read from memory are to be written.
     * @param offsetInBuffer nbr of bytes to add to the size, and also start of where to put these bytes in the newly created buffer.
     * 
     * @return nbr of bytes written, 0 if error
     */
    public int writeToBuffer(java.nio.ByteBuffer buffer,int offsetInBuffer)
    {
        int A1=this.addr_regs[1];
        if ((A1&1)==1)
        {
            smsqmulator.Helper.reportError(Localization.Texts[45], Localization.Texts[67], null);
            return 0;
        }
        int total=this.data_regs[2];
        if (total+offsetInBuffer > buffer.capacity())
            return 0;                                       // this would overshoot
        if ((A1+total+offsetInBuffer)>this.totMemSize)      // don't read above max ROM address
        {
            total=this.totMemSize-this.addr_regs[1];
            if (total<offsetInBuffer)
                return 0;
        }
        total/=2;
        A1/=2; 
        buffer.position(offsetInBuffer);
        for (int i=0;i<total;i++,A1++) 
        {
            buffer.putShort(i*2+offsetInBuffer, this.mainMemory[A1]);
        }
        total=this.data_regs[2];
        if (((total) & 1) == 1)
            buffer.put(offsetInBuffer+total-1, (byte)(this.mainMemory[A1]>>8));
        return total;
    }
    
    /**
     * Writes bytes read from the memory to a ByteBuffer.
     * The bytes will be put at the current position in the buffer. THE LIMIT oF THIS BUFFER WILL BE SET TO ITS CAPACITY.
     * 
     * @param buffer the ByteBuffer into which the bytes read from memory are to be written at the current position of this buffer.
     *               THE LIMIT oF THIS BUFFER WILL BE SET TO ITS CAPACITY.
     * @param A1 where to start reading the bytes from.
     * @param bytesToWrite nbr of bytes to write to the buffer.
     * 
     * @return nbr of bytes written, -1 if buffer doesn't contain enough space. In that case NOTHING is written to the buffer.
     */
    public int writeToBuffer(java.nio.ByteBuffer buffer,int A1,int bytesToWrite)
    {
        if (buffer.position()+bytesToWrite > buffer.capacity())
       // if (buffer.position()+bytesToWrite >= buffer.capacity())
            return -1;
        boolean isOdd;
        int nbr=bytesToWrite;                               // keep
        buffer.limit(buffer.capacity());
        if ((A1 & 1 ) !=0)                                  // start of mem read is odd
        {
            buffer.put((byte)(readMemoryByte(A1++)));       // make it even
            bytesToWrite--;                                 // one less byte to read
            if (bytesToWrite==0)
                return 1;                                   // just one byte to write
        }
        isOdd = (bytesToWrite & 1 ) !=0;
        int address=A1+bytesToWrite-1;
        bytesToWrite/=2;                                    // nbr of words to write
        A1/=2;                                              // where in mem to take them from
        for (int i=0;i<bytesToWrite;i++,A1++)
            
        {  
            buffer.putShort(this.mainMemory[A1]);           // copy from mem to buffer
        }
        if (isOdd)                                          // there remained an odd number of bytes to move
        {
            buffer.put((byte)(readMemoryByte(address)));
        }
        return nbr;
    }
    
    /**
     * Read bytes from a filechannel and write them into memory.
     * 
     * @param address where to read to.
     * @param nbrOfBytes how many bytes to read.
     * @param inChannel the channel to read from.
     * 
     * @return nbr of bytes read.
     * 
     * @throws java.io.IOException from the file channed I/O operations.
     */
    public int readFromFile(int address,int nbrOfBytes, java.nio.channels.FileChannel inChannel) throws java.io.IOException
    {
        return readFromFile(address,nbrOfBytes,inChannel,false);
    }
    
    /**
     * Read bytes from a filechannel and write them into memory.
     * This creates a temporary ByteBuffer, reads the file into it and then copies from that ByteBuffer into the memory
     * @param address where to read to.
     * @param nbrOfBytes how many bytes to read.
     * @param inChannel the channel to read from.
     * @param specialRead =<code>true</code> ONLY when loading the OS, must be <code>false</code> at all other times!
     * 
     * @return nbr of bytes read, -1if EOF
     * 
     * @throws java.io.IOException from the file channed I/O operations.
     */
    private int readFromFile(int address,int nbrOfBytes, java.nio.channels.FileChannel inChannel,boolean specialRead) throws java.io.IOException
    {
        if (nbrOfBytes<1)
            return 0;                                       // there is nothing to read!
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(nbrOfBytes);        // first make a bytebuffer of sufficient capacity
        nbrOfBytes=inChannel.read(buffer);                  // read into buffer
        if (nbrOfBytes<0)
            return -1;
        return readFromBuffer(address,nbrOfBytes,buffer,0,specialRead);
    }
    
    /**
     * Reads bytes from a ByteBuffer and writes them into memory.
     * 
     * @param address where to read to.
     * @param nbrOfBytes how many bytes are to be read.
     * @param buffer the buffer to read from.
     * @param startInBuffer where in the buffer to start reading from.
     * @param specialRead =<code>true</code> ONLY when loading the OS, must be <code>false</code> at all other times!
     * 
     * @return nbr of bytes read.
     */
    protected int readFromBuffer(int address,int nbrOfBytes,java.nio.ByteBuffer buffer,int startInBuffer,boolean specialRead)
    {
       if (nbrOfBytes<1)
            return 0;                                       // there is nothing to read!
        address&=MC68000Cpu.cutOff;
        if (specialRead)
        {
            if ((address+nbrOfBytes)>this.totMemSize)       // don't write above max writable ROM address
            {
                nbrOfBytes=this.totMemSize-address;
                if (nbrOfBytes<1)
                    return 0;
            }
        }
        else
        {
            if ((address+nbrOfBytes)>this.totRamSize)       // don't write above max writable RAM address
            {
                nbrOfBytes=this.ramSize-address;
                if (nbrOfBytes<1)
                    return 0;                               // nothing was read
            }
        }
        boolean toScreen=false;
        if ((address>=this.screenStart && address<=this.screenStop) || (address+nbrOfBytes>=this.screenStart && address+nbrOfBytes<=this.screenStop)) 
        {
            if (!specialRead) 
            {
                toScreen=true;                              // check whether we're LBYTESing to the screen
            }
        }
        int start=address;
        buffer.limit(buffer.capacity());
        if (nbrOfBytes+startInBuffer>buffer.capacity()) 
        {
            nbrOfBytes = buffer.capacity()-startInBuffer;
        }
        boolean startAddressIsOdd=(address&1)==1;

        address/=2;
        if (startAddressIsOdd)
        {
           nbrOfBytes--; 
           short p=(short)(this.mainMemory[address]&0xff00);
           short m=(short) (buffer.get(startInBuffer)&0xff);
           this.mainMemory[address]=(short) (p|m);
           address++;
           startInBuffer++;
        }
       
        // now copy from byte buffer to my array in word sized chunks
        int i;
        for (i=startInBuffer;i<nbrOfBytes+startInBuffer-1;i+=2,address++)
        {
            this.mainMemory[address]=buffer.getShort(i);
        }
        
        if ((nbrOfBytes & 1) == 1)                          // handle possible last byte (not word)
        {
            short res=this.mainMemory[address];
            res&=0xff;
            res|=(buffer.get(i)<<8);
            this.mainMemory[address]=res;
        }
        if (startAddressIsOdd) 
        {
            nbrOfBytes++;
        }
        if (toScreen) 
        {
            this.screen.displayRegion(this,start,nbrOfBytes);
        }
        return nbrOfBytes;
    }
    
    /**
     * Reads bytes from a buffer and writes them into memory.
     * 
     * @param address where to read to.
     * @param nbrOfBytes how many bytes to read.
     * @param buffer the buffer to read from.
     * @param startInBuffer the first byte in the buffer to read from.
     * 
     * @return nbr of bytes read ,-1 if EOF
     */
    public int readFromBuffer(int address,int nbrOfBytes,java.nio.ByteBuffer buffer,int startInBuffer)
    {
        return readFromBuffer(address,nbrOfBytes,buffer,startInBuffer,false);
    }
        
    
    
    
      /* ----------------------------               MISC   ------------------------------------*/
    
      /**
     * Finds a string in memory.
     * The length of the string MUST be even.
     * 
     * @param startAddress where to start searching.
     * @param stopAddress where to stop searching.
     * @param toFind the string to find, must have an even number of characters.
     * 
     * @return the address where the first short can be found, -1 if none.
     */
    public int findInMemory(int startAddress,int stopAddress,String toFind)
    {
        byte [] add=toFind.getBytes();
        if ((add.length&1)!=0)
        {
            return -1;
        }
        short [] totToFind=new short[add.length/2];
        short res;
        int index=0;
        for (int i=0;i<totToFind.length;i++,index++)
        {
            res=(short)(add[index++]<<8);
            res|=(short)(add[index]);
            totToFind[i]=res;
        }
        return findInMemory(startAddress,stopAddress,totToFind);
    }
    
     /**
     * Finds consecutive shorts followed by a string in memory.
     * 
     * @param startAddress where to start searching.
     * @param stopAddress where to stop searching.
     * @param toFind the array with the consecutive shorts to find, in the order they are in that array.
     * @param addString th additional string to find, the length of it MUST BE EVEN
     * 
     * @return the address where the first short can be found, -1 if none.
     */
    public int findInMemory(int startAddress,int stopAddress,short [] toFind,String addString)
    {
        byte [] add=addString.getBytes();
        if ((add.length&1)!=0)
        {
            return -1;
        }
        int l=toFind.length;
        short [] totToFind=new short[l+add.length/2];
        System.arraycopy(toFind, 0, totToFind, 0, l);
        short res;
        int index=0;
        for (int i=l;i<totToFind.length;i++,index++)
        {
            res=(short)(add[index++]<<8);
            res|=(short)(add[index]);
            totToFind[i]=res;
        }
        return findInMemory(startAddress,stopAddress,totToFind);
    }
    /**
     * Finds consecutive shorts in memory.
     * 
     * @param startAddress where to start searching.
     * @param stopAddress where to stop searching.
     * @param toFind the array with the consecutive shorts to find, in the order they are in that array.
     * 
     * @return the address where the first short can be found, -1 if none.
     */
    public int findInMemory(int startAddress,int stopAddress,short [] toFind)
    {
        int l=toFind.length;
        if (l==0 || startAddress<0 || stopAddress>this.totMemSize)
            return 0;                                       // nothing found
        short first=toFind[0];
        startAddress/=2;
        stopAddress=stopAddress/2-l;
        int add;
        boolean foundit;
        while(startAddress<stopAddress)
        {
            if (this.mainMemory[startAddress++]==first)
            {
                foundit=true;
                add=startAddress;
                for (int i=1;i<l;i++)
                {
                    if (this.mainMemory[add++]!=toFind[i])
                    {
                        foundit=false;
                        break;
                    }
                }
                if (foundit)
                {
                    return --startAddress*2;
                }
            }
        }
        return -1;
    }
    
    /**
     * Sets the trap dispatcher for this CPU, which is called whenever a JAVAComm instruction (such as for some device drivers) is encountered.
     * 
     * @param td the trap dispatcher
     */
    public void setTrapDispatcher(smsqmulator.TrapDispatcher td)
    {
        this.trapDispatcher=td;
    }
    
    
    /**
     * Sets the keyrow parameter for this key (row and col).
     * 
     * @param row what row (0 to 7)
     * @param col what col (0 to 7)
     */
    public void setKeyrow(int row,int col)
    {
        int add=readMemoryLong(0x400);
        add=readMemoryLong(add+0xe8)+0x22+row;
        int current=readMemoryByte(add);
        col=1<<col;
        current |=col;
        writeMemoryByte(add,current);
    } 
    
     /**
     * Unsets the keyrow parameter for this key (row and col).
     * 
     * @param row what row (0 to 7)
     * @param col what col (0 to 7)
     */
    public void removeKeyrow(int row,int col)
    {
        int current;        
        int add=readMemoryLong(0x400);
        add=readMemoryLong(add+0xe8)+0x22+row;
        try
        {
            current=readMemoryByte(add);
            col=1<<col;
            current &=~col;
            writeMemoryByte(add,current);
        }
        catch (Exception e)
        {
            try                                             // try this AGAIN !!!!!!
            {
                current=readMemoryByte(add);
                col=1<<col;
                current &=~col;
                writeMemoryByte(add,current);
            }
            catch (Exception ex)
            {
                /*NOP*/
            }
        }
    }
    
    /**
     * Gets the <code>Inifile</code> used when creating this object.
     * 
     * @return  the <code>Inifile</code> used when creating this object.
     */
    public inifile.IniFile getInifile()
    {
        return this.iniFile;
    }
    
    /**
     * Sets the display mode for a QL compatible screen, a fall through to the corresponding screen routine.
     * 
     * @param mode (0 or 8)
     */
    public void setScreenMode(int mode)
    {
        this.screen.setMode(mode);
    }
    /**
     * Sets the display mode when emulating a QL compatible screen, a fall through to the corresponding screen routine.
     * 
     * @param mode (0 or 8)
     */
    public void setEmuScreenMode(int mode)
    {
        this.screen.setEmuMode(mode);
    }
    
    /**
     * Gets the highest readable (not necessarily writable) memory location.
     * 
     * @return the highest readable (not necessarily writable) memory location, i.e. the end of the "ROM".
     */
    public int readableSize()
    {
        return this.totMemSize;
    }
    
    /**
     * Gets the address of the start of the linkage block = end of RAM.
     * 
     * @return the address of the start of the linkage block
     */
    public int getLinkageBlock()
    {
        return this.ramSize;
    }
  
    /**
     * Checks whether ROM was loaded OK (or some other error occurred).
     * 
     * @return <code>true</code> if loaded OK (CPU may proceed executing).
     */
    public boolean isRomLoadedOk()
    {
        return this.romLoadedOK;
    }
    
    
    /**
     * Resets this CPU, sets up SMSQ/E and prepares the CPU for starting execution..
     * 
     * This may clear all memory, puts the PC at top of mem, sets screen etc.
     * 
     * @param clearMem = true if memory should be cleared.
     */
    public void setupSMSQE(boolean clearMem)
    {
        reset();
        int addr=this.totRamSize/2;
        if (clearMem)
        {
            for (int i=0;i<addr;i++)
                this.mainMemory[i]=0;                       // clear out entire memory, except for "ROM"
        }
        if (!this.romLoadedOK)
            return;                                         // no  rom loaded yet, nothing to do.
    
        writeMemoryLong(0x1000a,0x476f6c64);                // ******************************************** necesaary for some c progs (why?)
        this.pc_reg=this.totRamSize/2;                      // set PC to start of "ROM" = image that was loaded
        writeMemoryLong(this.ramSize-4,this.ramSize);       // show top of ram
        this.reg_ssp=this.ramSize-8;                             // set SSP to top of usable ram
        this.addr_regs[7]=this.ramSize-8;                   // and point A7 to it
        writeMemoryLong(4, this.ramSize);                   // address at $4 points to linkage block
        
        int temp;
        String s=this.iniFile.getOptionValue("NFA_USE");    // config NFA etc use name
        if (!s.isEmpty())
        {
            temp=smsqmulator.Helper.convertUsageName(s);
            writeMemoryLong(this.ramSize+smsqmulator.Types.LINKAGE_NFA_USE,temp);
        }
        s=this.iniFile.getOptionValue("SFA_USE");     
        if (!s.isEmpty())
        {
            temp=smsqmulator.Helper.convertUsageName(s);
            writeMemoryLong(this.ramSize+smsqmulator.Types.LINKAGE_SFA_USE,temp);
        }
        s=this.iniFile.getOptionValue("WIN_USE");
        if (!s.isEmpty())
        {
            temp=smsqmulator.Helper.convertUsageName(s);
            writeMemoryLong(this.ramSize+smsqmulator.Types.LINKAGE_WIN_USE,temp);
        }
        s=this.iniFile.getOptionValue("FLP_USE");
        if (!s.isEmpty())
        {
            temp=smsqmulator.Helper.convertUsageName(s);
            writeMemoryLong(this.ramSize+smsqmulator.Types.LINKAGE_FLP_USE,temp);
        }
        s=this.iniFile.getOptionValue("MEM_USE");
        if (!s.isEmpty())
        {
            temp=smsqmulator.Helper.convertUsageName(s);
            writeMemoryLong(this.ramSize+smsqmulator.Types.LINKAGE_MEM_USE,temp);
        }
        
        writeMemoryLong(this.ramSize+smsqmulator.Types.LINKAGE_BOOT_DEVICE,smsqmulator.Types.WINDriver+1);//boot device
        
        int tx=(int)((System.currentTimeMillis()/1000)+smsqmulator.Monitor.TIME_OFFSET); // ** magic offset for current time (this is no longer useful?)
        writeMemoryLong(this.ramSize+smsqmulator.Types.LINKAGE_RTC,tx);
        
        tx=randomNumber.nextInt(65535);                     // set next random number
        writeMemoryWord(this.ramSize+smsqmulator.Types.LINKAGE_RANDOM,tx);
        
        this.newInterruptGenerated=false;
         
        // now find out whether the ROM loaded is OK.
        addr=findInMemory(this.totRamSize,this.totRamSize+1000,"SMSQXqXq");// find my marker
        if (addr==-1)
        {                                                   // not found : error, rom is too old
            smsqmulator.Helper.reportError(Localization.Texts[45], Localization.Texts[103], null);
            this.romLoadedOK=false;
            return;
        }
        
        int version=readMemoryLong(addr+8);                     // major version, corresponds to SMQE release         
        if (version<smsqmulator.Helper.convertStringToInt(smsqmulator.Types.MINIMUM_VERSION_NEEDED))     
        {                                                   // not found or too old
            smsqmulator.Helper.reportError(Localization.Texts[45], Localization.Texts[103], null);
            this.romLoadedOK=false;
            return;
        }
        if (version == smsqmulator.Helper.convertStringToInt(smsqmulator.Types.MINIMUM_VERSION_NEEDED))// check minor version if both majors are the same
        {
            version=readMemoryLong(addr+12);                        // minor version, corresponds to SMSQmulator specific SMSQE release (if any)  
          if (version<smsqmulator.Helper.convertStringToInt(smsqmulator.Types.MINIMUM_MINOR_VERSION_NEEDED))     
            {                                                   // not found or too old
                smsqmulator.Helper.reportError(Localization.Texts[45], Localization.Texts[103], null);
                this.romLoadedOK=false;
                return;
            }
        }
        
        // the next parts find and set whether devices should be linked into SMSQE
        // !!!!!!!!!!!!!!!!!!!!!! KEEP THEM IN THE CORRECT ORDER §§§§§§§§§§§§§§§§§§§§§§
        addr=findInMemory(addr,this.totMemSize,smsqmulator.Types.SMSQMULATOR_CONFIG_FLAG,"WIN0");
        if (addr!=-1)
        {
            tx=addr+smsqmulator.Types.SMSQMULATOR_CONFIG_FLAG.length*2+4;
            tx/=2;
            temp=  this.mainMemory[tx]&0xff;
            if (this.iniFile.getTrueOrFalse("DISABLE-WIN-DEVICE"))
                temp+=0x100;
            this.mainMemory[tx]=(short)(temp&0xffff);       
        }  
        
        addr=findInMemory(addr,this.totMemSize,smsqmulator.Types.SMSQMULATOR_CONFIG_FLAG,"MEM0");
        if (addr!=-1)
        {
            tx=addr+smsqmulator.Types.SMSQMULATOR_CONFIG_FLAG.length*2+4;
            tx/=2;
            temp=  this.mainMemory[tx]&0xff;
            if (this.iniFile.getTrueOrFalse("DISABLE-MEM-DEVICE"))
                temp+=0x100;
            this.mainMemory[tx]=(short)(temp&0xffff);
        }
       
        addr=findInMemory(addr,this.totMemSize,smsqmulator.Types.SMSQMULATOR_CONFIG_FLAG,"NFA0");
        if (addr!=-1)
        {
            tx=addr+smsqmulator.Types.SMSQMULATOR_CONFIG_FLAG.length*2+4;
            tx/=2;
            temp=  this.mainMemory[tx]&0xff;
            if (this.iniFile.getTrueOrFalse("DISABLE-NFA-DEVICE"))
                temp+=0x100;
            this.mainMemory[tx]=(short)(temp&0xffff);
        } 
        
        addr=findInMemory(addr,this.totMemSize,smsqmulator.Types.SMSQMULATOR_CONFIG_FLAG,"SFA0");
        if (addr!=-1)
        {
            tx=addr+smsqmulator.Types.SMSQMULATOR_CONFIG_FLAG.length*2+4;
            tx/=2;
            temp=  this.mainMemory[tx]&0xff;
            if (this.iniFile.getTrueOrFalse("DISABLE-SFA-DEVICE"))
                temp+=0x100;
            this.mainMemory[tx]=(short)(temp&0xffff);
        }
        
        addr=findInMemory(addr,this.totMemSize,smsqmulator.Types.SMSQMULATOR_CONFIG_FLAG,"FLP0");
        if (addr!=-1)
        {
            tx=addr+smsqmulator.Types.SMSQMULATOR_CONFIG_FLAG.length*2+4;
            tx/=2;
            temp=  this.mainMemory[tx]&0xff;
            if (this.iniFile.getTrueOrFalse("DISABLE-FLP-DEVICE"))
                temp+=0x100;
            this.mainMemory[tx]=(short)(temp&0xffff);
        }
        
        // prepare screen and setup screen info for SMSQ/E
        if (this.screen!=null)
        {
            this.screen.clearScreen();
            writeMemoryLong(this.ramSize+smsqmulator.Types.LINKAGE_SCREENBASE,this.screenStart);
            writeMemoryLong(this.ramSize+smsqmulator.Types.LINKAGE_SCREENSIZE,this.screen.getScreenSizeInBytes());
            this.mainMemory[(this.ramSize+smsqmulator.Types.LINKAGE_SCREEN_LINE_SIZE)/2]=(short)this.screen.getLineSize();
            this.mainMemory[(this.ramSize+smsqmulator.Types.LINKAGE_SCREEN_XSIZE)/2]=(short)this.screen.getXSize();
            this.mainMemory[(this.ramSize+smsqmulator.Types.LINKAGE_SCREEN_YSIZE)/2]=(short)this.screen.getYSize();
            temp=this.screen.getMode();
            switch (temp)
            {
                case 0:
                case 8:
                    temp=0;
                    break;
                case 16:
                    temp=2;
                    break;
                case 32:
                    temp=3;
                    break;
            }
            // find the config info & set screen mode in it
            addr=findInMemory(this.totRamSize,this.totMemSize,smsqmulator.Types.SMSQMULATOR_CONFIG_FLAG,"SMSQ");
            if (addr==-1)
            {
                smsqmulator.Helper.reportError(Localization.Texts[45], Localization.Texts[103], null);// oops, this smsqe is too old
                this.romLoadedOK=false;
                return;
            }
            else
            {
                tx=addr+smsqmulator.Types.SMSQMULATOR_CONFIG_FLAG.length*2+4+12+10; // jump over general config flag, special config flag (4) ,version info  (12) and into the config block (10)
                tx/=2;
                temp=  this.mainMemory[tx]&0xff00 +temp;
                this.mainMemory[tx]=(short)(temp&0xffff);   // set screen mode in config block
                temp=this.iniFile.getOptionAsInt("LESS-CPU-WHEN-IDLE",1)<<8;
                tx=addr+smsqmulator.Types.SMSQMULATOR_CONFIG_FLAG.length*2+4+12+22; // jump over general config flag, special config flag (4) ,version info  (12) and into the config block (22)
                tx/=2;
                temp=  (this.mainMemory[tx]&0xff)|temp;
                this.mainMemory[tx]=(short)(temp&0xffff);   // set CPU use mode in config block
            }
            this.romLoadedOK=true;
        }
        else
            this.romLoadedOK=false;                         // if there is no screen something funny happened
    }
    
    /**
     * Loads a rom image into the address space.
     * 
     * This allocates an entire new memory area of current size + linkage block + size of ROM image.
     * This also resets the CPU entirely.
     * 
     * @param filePath name of file to load. This is present when the next parameter is null.
     * @param fileURL url of the file to load. this must be present when the previous parameter is null.
     * 
     * @return <code>true</code> if ROM image loaded OK, else <code>false</code>.
     */
    public boolean loadRomImage(String filePath,java.net.URL fileURL)
    {
        java.io.RandomAccessFile inputFile=null;
        if (filePath==null)
            filePath=this.romFile;
        this.romLoadedOK=false;
        java.util.Arrays.fill(this.mainMemory,(short) 0);
        try
        {
            this.romFile=filePath;                          // keep name of rom file.
            inputFile = new java.io.RandomAccessFile(filePath, "r");
            java.nio.channels.FileChannel inChannel = inputFile.getChannel();
            int fileSize=(int)inChannel.size();
            readFromFile(this.totRamSize,fileSize,inChannel,true);// get rom image into memory 
            inputFile.close();                              // done
            this.romLoadedOK=true;                          // rom was loaded OK
            setupSMSQE(false);                              // prepare everything for SMSQ/E execution
            return this.romLoadedOK;                        // this might have changed
        }
        
        catch (java.lang.OutOfMemoryError e)
        {
            smsqmulator.Helper.reportError(Localization.Texts[45], Localization.Texts[108], null);
            return false;                                   // OOOOPs out of memory
        }
        
        catch (Exception m)                                 // I might be running as an applet
        {
            try
            {
                if (inputFile!=null)
                {
                    try
                    {
                        inputFile.close();                      // this also closes inChannel
                    }
                    catch (java.io.IOException whatever)
                    { /*nop*/ }
                }
                
                java.io.InputStream is=fileURL.openStream();
                int fileSize=350000;
                byte[]sms=new byte[fileSize];
                int p=0;
                int offset =0;
                while (p!=-1)
                {
                    p=is.read(sms, offset, fileSize-offset);
                    offset+=p;
                }
                is.close();
                for (int i=0;i<offset+3;i+=2)
                {
                    this.mainMemory[(this.totRamSize+ i)/2]=(short)((sms[i]<<8) |(sms[i+1]&0xff));
                }
                this.romLoadedOK=true;                          // rom was loaded OK
                setupSMSQE(false);                              // prepare everything for SMSQ/E execution
                return this.romLoadedOK;                        // this might have changed
            }
            
            catch (Exception e2)
            {
                smsqmulator.Helper.reportError(Localization.Texts[45], Localization.Texts[50], null, e2);
                return false;
            }
        }
    }

   /**
    * Gets the screen used by this object.
    * 
    * @return the screen used.
    */
    public smsqmulator.Screen getScreen()
    {
        return this.screen;
    }
    
    /**
     * Returns the name of the last romfile loaded.
     * 
     * @return the name of the last romfile loaded, or <code>null</code> if none.
     */
    public String getRomFile()
    {
        return this.romFile;
    }
    
    /**
     * Get the memory used by this cpu for some sort of "dma".
     * Be careful what you do with this.
     * 
     * @return the memory (array of <code>short</code>) used by the cpu.
     */
    public short[] getMemory()
    {
        return this.mainMemory;
    }    
}

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