package smsqmulator.cpu.instructions.CMP;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The CMPM instructions
 
 * @author and copyright (c) 2012 -2014 wolfgang lenerz
 * 
 *  1011dddsssmmmrrr
 *  
 *  where   ddd= destination reg
 *          sss= size (000=byte, 001 = word, 010 = long, 011 = word adresser reg,111 = long address reg )
 *          mmm = mode =001, 
 *          rrr = source reg
 * NB no byte sized move allowed
 * 
 * Based on Tony Headford's code, see his licence
 */
public class CMPM implements InstructionSet
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
                base = 0xb108;
                i = new smsqmulator.cpu.Instruction()
                {
                   
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    { 
                        int ax = (opcode >> 9) & 0x07;
                        int ay = (opcode & 0x07);
                        int s = cpu.readMemoryByte(cpu.addr_regs[ay]);
                        cpu.addr_regs[ay]++;
                        int d = cpu.readMemoryByte(cpu.addr_regs[ax]);
                        cpu.addr_regs[ax]++;
                        boolean Sm =(s & 0x80) != 0;
                        boolean Dm = (d & 0x80) != 0;
                        d-=s;
                        cpu.reg_sr&=0xfff0;            // X bit not affected by cmp
                        boolean Rm = (d & 0x80) != 0;          // result is neg
                        //n z v c
                        //8 4 2 1
                        if(d == 0)
                        {
                            cpu.reg_sr+=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr+=8;             // set N flag
                        }
                           
                        if((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                        {
                           cpu.reg_sr+=2;              // set V flag
                        }
                        if((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                        {
                            cpu.reg_sr++;              // set C flag
                        }
                    }
                   @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Byte, cpu);
                    }
                };
            }
            else if (sz==1)
            {
                base = 0xb148;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int ax = (opcode >> 9) & 0x07;
                        int ay = (opcode & 0x07);

                        int s = cpu.readMemoryWord(cpu.addr_regs[ay]);
                        cpu.addr_regs[ay]+=2;
                        int d = cpu.readMemoryWord(cpu.addr_regs[ax]);
                        cpu.addr_regs[ax]+=2;
                        boolean Sm =(s & 0x8000) != 0;
                        boolean Dm = (d & 0x8000) != 0;
                        d-=s;
                        cpu.reg_sr&=0xfff0;            // X bit not affected by cmp
                        boolean Rm = (d & 0x8000) != 0;          // result is neg
                        //n z v c
                        //8 4 2 1
                        if(d == 0)
                        {
                            cpu.reg_sr+=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr+=8;             // set N flag
                        }
                           
                        if((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                        {
                           cpu.reg_sr+=2;              // set V flag
                        }
                        if((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                        {
                            cpu.reg_sr++;              // set C flag
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
                base = 0xb188;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    { 
                        int ax = (opcode >> 9) & 0x07;
                        int ay = (opcode & 0x07);

                        int s = cpu.readMemoryLong(cpu.addr_regs[ay]);
                        cpu.addr_regs[ay]+=4;
                        int d = cpu.readMemoryLong(cpu.addr_regs[ax]);
                        cpu.addr_regs[ax]+=4;
                        int r = d - s;

                        cpu.reg_sr&=0xfff0;            // X bit not affected by cmp
                        boolean Sm = s<0;       // source is neg
                        boolean Dm = d<0;       // dist is neg
                        boolean Rm = r< 0;       // result is neg
                        //n z v c
                        //8 4 2 1
                        if(r == 0)
                        {
                            cpu.reg_sr+=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr+=8;             // set N flag
                        }
                           
                        if((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                        {
                           cpu.reg_sr+=2;              // set V flag
                        }
                        if((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                        {
                            cpu.reg_sr++;              // set C flag
                        }
                    }
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Long, cpu);
                    }
                };
            }
            for(int ax = 0; ax < 8; ax++)
            {
                for(int ay = 0; ay < 8; ay++)
                {
                    cpu2.addInstruction(base + (ax << 9) + ay, i);
                }
            }
        }
    }

    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz, smsqmulator.cpu.MC68000Cpu cpu)
    {
        DisassembledOperand src = new DisassembledOperand("(a" + (opcode & 0x07) + ")+");
        DisassembledOperand dst = new DisassembledOperand("(a" + ((opcode >> 9) & 0x07) + ")+");
        return new DisassembledInstruction(address, opcode, "cmpm" + sz.ext(), src, dst);
    }
}
