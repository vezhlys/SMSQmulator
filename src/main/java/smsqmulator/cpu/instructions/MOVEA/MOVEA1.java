package smsqmulator.cpu.instructions.MOVEA;

/**
 * MOVE where source is DN and destination is AN.
 * 
 * 00ssxxx001mmmrrr
 * s = size bit
 * x=dest register
 * m =source mode 000
 * r= source register
 * so 00ssxxx000mmm000 = base , s can 01 byte, 11 word,10 long
 * so base = 0x1000 for byte,0x2000 for long,0x3000 for word, 
 *
 * @author and copyright for my code (c) Wolfgang Lenerz 2013 - 2014.
 * 
 * Based on code by Tony Headford, see his licence in accompanying file
 */
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

public class MOVEA1 implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;
        for(int sz = 1; sz < 3; sz++)
        {
            if(sz == 1)
            {
                // move word
                base = 0x3040;
                i = new smsqmulator.cpu.Instruction() 
                { 
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {   
                        int s=cpu.data_regs[opcode &7]&0xffff;
                        if ((s&0x8000)!=0)
                            s|=0xffff0000;                                   // sign extend
                        cpu.addr_regs[(opcode>>9)&7]=s;
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
                // move long
                base = 0x2040;
                i = new smsqmulator.cpu.Instruction() 
                {
                    
                   
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        cpu.addr_regs[(opcode>>9)&7]=cpu.data_regs[opcode &7];
                    }
                   @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Long, cpu);
                    }
                };
            }
            for(int sea_reg = 0; sea_reg < 8; sea_reg++)
            {
                for(int dea_reg = 0; dea_reg < 8; dea_reg++)
                {
                    cpu2.addInstruction(base + (dea_reg << 9)  + sea_reg, i);
                }
            }
        }
    }

    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz, smsqmulator.cpu.MC68000Cpu cpu)
    {
            DisassembledOperand src = cpu.disassembleSrcEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), sz);
            DisassembledOperand dst = cpu.disassembleDstEA(address + 2 + src.bytes, (opcode >> 6) & 0x07, (opcode >> 9) & 0x07, sz);
            return new DisassembledInstruction(address, opcode, "move" + sz.ext(), src, dst);
    }
}
