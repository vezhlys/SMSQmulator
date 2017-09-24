package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;

/**
 * A replacement MOVEQ instruction in all of its variants
 * 
 * This is based on Tony Headford's work.
 * 
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class MOVEQ implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base = 0x7000;
        smsqmulator.cpu.Instruction i = new smsqmulator.cpu.Instruction()
        {
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int s = opcode & 0xff;
                cpu.reg_sr&=0xfff0;
                if ((s & 0x80)==0x80)
                {
                    s+=0xffffff00;                          // sign extend value
                    cpu.reg_sr+=8;
                }
                else if (s==0)
                {
                    cpu.reg_sr+=4;
                }
                cpu.data_regs[opcode >> 9 & 0x07]= s;
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                    return disassembleOp(address, opcode);
            }
        };

        for(int reg = 0; reg < 8; reg++)
        {
            for(int imm = 0; imm < 256; imm++)
            {
                cpu2.addInstruction(base + (reg << 9) + imm, i);
            }
        }
    }

    protected final DisassembledInstruction disassembleOp(int address, int opcode)
    {
        DisassembledOperand src = new DisassembledOperand(String.format("#$%02x", opcode & 0xff));
        DisassembledOperand dst = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));

        return new DisassembledInstruction(address, opcode, "moveq", src, dst);
    }
}
