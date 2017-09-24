package smsqmulator.cpu.instructions.MOVE;

/**
 * MOVE where source is (AN)+ and destination is d8(An,Xn) - (An) with displacement & index.
 * * 
 * 00ssxxxyyymmmrrr
 * s = size bit
 * x=dest register (An)
 * y = destination mode =110
 * m =source mode 011
 * r= source register
 * so 00ssxxx010mmm000 = base , s can 01 byte, 11 word,10 long
 
 * 
 * @author and copyright for my code (c) Wolfgang Lenerz 2013 - 2014.
 * 
 * Based on code by Tony Headford, see his licence in accompanying filelf
 */
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

public class MOVEAnP6 implements InstructionSet
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
                base = 0x1198;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int reg=opcode &7;
                        int s=cpu.readMemoryByte(cpu.addr_regs[reg])&0xff;
                        if (reg==7)
                        {
                            cpu.addr_regs[reg]+=2;
                        }
                        else
                        {
                            cpu.addr_regs[reg]++;
                        }
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
                base = 0x3198;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int reg=opcode &7;
                        short s=cpu.readMemoryShort(cpu.addr_regs[reg]);
                        cpu.addr_regs[reg]+=2;
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
                        cpu.writeMemoryShort(cpu.addr_regs[(opcode>>9)&7]+displacement, s);
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
                        return disassembleOp(address, opcode, Size.Word, cpu);
                    }
                };
            }
            else
            {
                // move long
                base = 0x2198;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int reg=opcode &7;
                        int s=cpu.readMemoryLong(cpu.addr_regs[reg]);
                        cpu.addr_regs[reg]+=4;
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
