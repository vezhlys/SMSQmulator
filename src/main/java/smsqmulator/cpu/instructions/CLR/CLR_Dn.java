package smsqmulator.cpu.instructions.CLR;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The CLR instruction where the destination is Dn.
 * 
 * 01000010 =0x42
 * ssmmmrrr
 * where ss = size (00 =byte, 01= word, 10=long)
 * mmm =detination mode
 * rrr= destination reg
 * 
 * @author and copyright (C) wolfgang lenerz 2014.
 * 
 * Based on Tony Headford's code, see his licence in the accompanyng file
 */
public class CLR_Dn implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;

        for(int sz = 0; sz < 3; sz++)                       
        {
            if(sz == 0)
            {
                base = 0x4200;
                i = new smsqmulator.cpu.Instruction()    // byte sized clr
                {
                     
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        cpu.data_regs[opcode&7]&=0xffffff00;
                        cpu.reg_sr &= 0xfff0;
                        cpu.reg_sr+=4;
                    }
                     @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                            return disassembleOp(address, opcode, Size.Byte, cpu);
                    }
                };
            }
            else if(sz == 1)
            {
                base = 0x4240;
                i = new smsqmulator.cpu.Instruction()    // word sized clr
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        cpu.data_regs[opcode&7]&=0xffff0000;
                        cpu.reg_sr &= 0xfff0;
                        cpu.reg_sr+=4;
                    }
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                            return disassembleOp(address, opcode, Size.Word, cpu);
                    }
                };
            }
            else
            {
                base = 0x4280;
                i = new smsqmulator.cpu.Instruction()    // long sized clr
                {
                     
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        cpu.data_regs[opcode&7]=0;
                        cpu.reg_sr &= 0xfff0;
                        cpu.reg_sr+=4;
                    }
                     @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                            return disassembleOp(address, opcode, Size.Long, cpu);
                    }
                };
            }

            for(int ea_reg = 0; ea_reg < 8; ea_reg++)
            {
                cpu2.addInstruction(base + ea_reg, i);
            }
	}
    }

    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz, smsqmulator.cpu.MC68000Cpu cpu)
    {
        DisassembledOperand dst = cpu.disassembleDstEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), sz);
        return new DisassembledInstruction(address, opcode, "clr" + sz.ext(), dst);
    }
}
