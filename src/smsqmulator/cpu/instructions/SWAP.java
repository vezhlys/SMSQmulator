package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
/**
 * The SWAP instruction in all of its variants.
 * 
 *  0100100001000rrr =0x4840
 *  where   rrr = data reg to be swapped
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * @author and Copyright (c) for his code by Tony Headford, see licence below.
 */
public class SWAP implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base=0x4840;
        smsqmulator.cpu.Instruction i = new smsqmulator.cpu.Instruction() 
        {                
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int reg=opcode&7;
                int temp=cpu.data_regs[reg];
                temp>>>=16;
                cpu.data_regs[reg]<<=16;
                cpu.data_regs[reg]|=temp;
                cpu.reg_sr&=0xfff0;
                if (cpu.data_regs[reg]<0)
                    cpu.reg_sr|=8;
                else if (cpu.data_regs[reg]==0)
                    cpu.reg_sr|=4;
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
		return new DisassembledInstruction(address, opcode, "swap", new DisassembledOperand("d" + (opcode & 0x07)));
            }
        };
       
        for (int src=0;src<8;src++)
        {
            cpu2.addInstruction(base+src, i);
        }
    }
}
