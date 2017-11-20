package smsqmulator.cpu.instructions;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;

/**
 * A redefinition of the RTR instruction.
 
 * @author and copyright (c) 2012 wolfgang lenerz for my code 
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class RTR implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        cpu2.addInstruction(0x4e77,new smsqmulator.cpu.Instruction() 
        {
            
            
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                cpu.reg_sr&=0xff00;
                cpu.reg_sr|=(cpu.readMemoryWordSigned(cpu.addr_regs[7])&0x1f);
                cpu.addr_regs[7] += 2;
                cpu.pc_reg=cpu.readMemoryLong(cpu.addr_regs[7])/2;
                cpu.addr_regs[7] += 4;
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                 return new DisassembledInstruction(address, opcode, "rtr");
            }
        });
    } 
}
