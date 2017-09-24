package smsqmulator.cpu.instructions;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
/**
 * A redefinition of the RTE instruction.
 
 * @author and copyright (c) 2014 wolfgang Lenerz for my code.
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class RTE implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        cpu2.addInstruction(0x4e73,new smsqmulator.cpu.Instruction() 
        {
            
            
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                if (cpu.isSupervisorMode())
                {
                    int newsr=cpu.readMemoryWord(cpu.addr_regs[7]);
                    cpu.pc_reg=(cpu.readMemoryLong(cpu.addr_regs[7]+2)/2);
                    cpu.addr_regs[7]+=6;
                    cpu.reg_sr=newsr;                      // new value of SR, could be user mode or super mode
                    if ((cpu.reg_sr & 0x2000) == 0)        // if we changed back to user mode,change stack pointer
                    {
                        cpu.reg_ssp=cpu.addr_regs[7]; // keep supervisor stack pointer
                        cpu.addr_regs[7] = cpu.reg_usp ;// get user stack pointer
                    }
                    cpu.testTrace();
                }
		else    
                {// privilege violation
                    cpu.raiseException(8);
      //              cpu.stopNow=-30;
                }
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                return new DisassembledInstruction(address, opcode, "rte");
            }
        });
    }
}
