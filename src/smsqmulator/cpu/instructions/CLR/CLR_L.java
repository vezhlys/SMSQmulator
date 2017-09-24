package smsqmulator.cpu.instructions.CLR;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The CLR instruction where the destination is L absolute).
 * 
 * 01000010 =0x42
 * ssmmmrrr
 * where ss = size (00 =byte, 01= word, 10=long)
 * mmm =detination mode =111
 * rrr= 1
 * 
 * @author and copyright (C) wolfgang lenerz 2014.
 * 
 * based on Tony Headford's CLR instruction, (C) Tony Headford, see his licence..
 */
public class CLR_L implements InstructionSet
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
                base = 0x4239;
                i = new smsqmulator.cpu.Instruction()    // byte sized clr
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        cpu.writeMemoryByte(cpu.readMemoryLongPC(),0); 
                        cpu.pc_reg+=2;
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
                base = 0x4279;
                i = new smsqmulator.cpu.Instruction()    // word sized clr
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        cpu.writeMemoryWord(cpu.readMemoryLongPC(),0); 
                        cpu.pc_reg+=2;
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
                base = 0x42b9;
                i = new smsqmulator.cpu.Instruction()    // long sized clr
                {
                     
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        cpu.writeMemoryLong(cpu.readMemoryLongPC(),0); 
                        cpu.pc_reg+=2;
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

            cpu2.addInstruction(base, i);
	}
    }
     
    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz, smsqmulator.cpu.MC68000Cpu cpu)
    {
        DisassembledOperand dst = cpu.disassembleDstEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), sz);
        return new DisassembledInstruction(address, opcode, "clr" + sz.ext(), dst);
    }
}
