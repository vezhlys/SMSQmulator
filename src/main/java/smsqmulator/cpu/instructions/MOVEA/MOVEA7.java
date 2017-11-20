package smsqmulator.cpu.instructions.MOVEA;

/**
 * MOVE where source is d(AN,Xn) and destination is An.
 * 
 * 00ssxxx001mmmrrr
 * s = size bit
 * x=dest register
 * m =source mode 110
 * r= source register
 * 
 * @author and copyright for my code (c) Wolfgang Lenerz 2013 - 2014.
 * 
 * Based on code by Tony Headford, see his licence in accompanying file
 */
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

public class MOVEA7 implements InstructionSet
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
                base = 0x3070;
                i = new smsqmulator.cpu.Instruction()
                {
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int displacement= getDisplacement(cpu)+cpu.addr_regs[opcode &7];
                        cpu.addr_regs[(opcode>>9)&7]=cpu.readMemoryWordSigned(displacement);
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
                base = 0x2070;
                i = new smsqmulator.cpu.Instruction()
                {
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int displacement= getDisplacement(cpu)+cpu.addr_regs[opcode &7];
                        cpu.addr_regs[(opcode>>9)&7]=cpu.readMemoryLong(displacement);
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
    
    protected int getDisplacement(smsqmulator.cpu.MC68000Cpu cpu)
    {
        int ext = cpu.readMemoryWordPCSignedInc();// extention word, contains size + displacement+reg
        int displacement=(ext & 0x80)!=0?ext| 0xffffff00:ext&0xff;    //displacemnt
        if((ext & 0x8000) !=0)
        {
            if((ext & 0x0800) == 0)                // word or long register displacement?
            {
                displacement+= cpu.signExtendWord(cpu.addr_regs[(ext >> 12) & 0x07]);
            }
            else
            {
                displacement+= cpu.addr_regs[(ext >> 12) & 0x07];
            }
        }
        else
        {
            if((ext & 0x0800) == 0)                // word or long register displacement?
            {
                displacement+= cpu.signExtendWord(cpu.data_regs[(ext >> 12) & 0x07]);
            }
            else
            {
                displacement+= cpu.data_regs[(ext >> 12) & 0x07];
            }
        }
        return displacement;
    }
}
