package smsqmulator.cpu.instructions.MOVE;

/**
 * MOVE where source is d16(AN) and destination is d16(AN) - (An) with displacement.
 * 
 * 00ssxxxyyymmmrrr
 * s = size bit
 * x=dest register (An)
 * y = destination mode =101
 * m =source mode 101
 * r= source register
 * so 00ssxxx010mmm000 = base , s can 01 byte, 11 word,10 long
 * so base = 0x1140 for byte,0x2140 for long,0x3140 for word, 
 * 
 * @author and copyright for my code (c) Wolfgang Lenerz 2013 - 2014.
 * 
 * Based on code by Tony Headford, see his licence in accompanying file
 */
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

public class MOVEAnD5 implements InstructionSet
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
                // move byte
                base = 0x1168;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int s=cpu.readMemoryByte(cpu.addr_regs[opcode &7]+ cpu.readMemoryWordPCSignedInc());
                        int displacement = cpu.readMemoryWordPCSignedInc();
                        cpu.writeMemoryByte(cpu.addr_regs[(opcode>>9)&7]+displacement, s);
                        cpu.reg_sr&=0xfff0;
                        if (s==0)
                        {
                            cpu.reg_sr+=4;
                        }
                        else if ((s&0x80)!=0)
                        {
                            cpu.reg_sr+=8;
                        }
         /*               
                        if ((cpu.readMemoryByte(cpu.addr_regs[(opcode>>9)&7]+displacement)&0xff) != s)
                            s=s;//debug
                        */
                        
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
                // move word
                base = 0x3168;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int s=cpu.readMemoryWord(cpu.addr_regs[opcode &7]+ cpu.readMemoryWordPCSignedInc())&0xffff;
                        int displacement = cpu.readMemoryWordPCSignedInc();
                        cpu.writeMemoryWord(cpu.addr_regs[(opcode>>9)&7]+displacement, s);
                        cpu.reg_sr&=0xfff0;
                        if (s==0)
                        {
                            cpu.reg_sr+=4;
                        }
                        else if ((s&0x8000)!=0)
                        {
                            cpu.reg_sr+=8;
                        }
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
                base = 0x2168;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int s=cpu.readMemoryLong(cpu.addr_regs[opcode &7]+ cpu.readMemoryWordPCSignedInc());
                        int displacement = cpu.readMemoryWordPCSignedInc();
                        cpu.writeMemoryLong(cpu.addr_regs[(opcode>>9)&7]+displacement, s);
                        cpu.reg_sr&=0xfff0;
                        if (s==0)
                        {
                            cpu.reg_sr+=4;
                        }
                        else if (s<0)
                        {
                            cpu.reg_sr+=8;
                        }
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
