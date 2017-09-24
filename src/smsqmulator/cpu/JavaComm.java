package smsqmulator.cpu;

/**
 * This is used to communicate with the Java monitor for TRAP calls. It makes some unused 68000 instructions return to the emulator.
 * This class is used to communicate with the Java monitor for TRAP calls.
 * 
 * The actual "illegal instructions" that are used are defined as constants in the Types object.
 * 
 * v. 1.01 use Aline instructions for "traps".
 * 
 * @author and copyright (c) 2012 -2015 Wolfgang Lenerz.
 * 
 */
    
public class JavaComm implements smsqmulator.cpu.InstructionSet
{
    private final int base=smsqmulator.Types.RETURN_BASE;                     // unused instructions by the MC 680000

    /**
     * Registers the instruction with the CPU.
     * 
     * @param cpu2 the cpu with which to register.
     */
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        for (int i=0;i<13;i++)
        {
            cpu2.addInstruction(this.base+i, new smsqmulator.cpu.Instruction() 
            {
                private smsqmulator.cpu.MC68000Cpu cpu=cpu2;
                
                @Override
                public void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                {                          
                    if (opcode==base)
                    {
                        this.cpu.stopNow=-10;
                        return;
                    }
                    if (this.cpu.trapDispatcher!=null)
                        this.cpu.trapDispatcher.dispatchTrap(-opcode,cpu); // this is used for communications with the java prog start at -10 and go lower
                }
                @Override
                public smsqmulator.cpu.DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                {
                        return new smsqmulator.cpu.DisassembledInstruction(address, opcode, "JavaCom  TRAP #" + (opcode-base));
                }
            });
        }
        
        for (int i=0xb00;i<0xb20;i++)
        {
            cpu2.addInstruction(this.base+i, new smsqmulator.cpu.Instruction() 
            {
                private smsqmulator.cpu.MC68000Cpu cpu=cpu2;
                
                @Override
                public void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                {                          
                    if (this.cpu.trapDispatcher!=null)
                        this.cpu.trapDispatcher.dispatchTrap(-opcode,cpu); // this is used for communications with the java prog start at -10 and go lower
                }
                @Override
                public smsqmulator.cpu.DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                {
                    return new smsqmulator.cpu.DisassembledInstruction(address, opcode, "JavaCom  ARIOP " + (base+0xb00-opcode));
                }
            });
        }
        
    }
}

