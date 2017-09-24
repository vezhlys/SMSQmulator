package smsqmulator.cpu.instructions.CMP;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The CMP instruction where source is immediate data.
 
 * @author and copyright (c) 2012 wolfgang lenerz
 * 
 *  1011dddsssmmmrrr
 *  
 *  where   ddd= destination reg
 *          sss= size (000=byte, 001 = word, 010 = long, 011 = word address reg,111 = long address reg ))
 *          mmm = mode =111, 
 *          rrr = source reg =100
 * @version 1.01
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class CMP_I implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;

        for(int sz = 0; sz < 5; sz++)
        {
            if(sz == 0)
            {
                base = 0xb03C;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int s = cpu.readMemoryWordPCInc()&0xff;
                        int d=cpu.data_regs[(opcode >>>9) & 0x07]&0xff;// des value
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
                        return disassembleOp(address, opcode, Size.Byte,"d", cpu);
                    }
                };
            }
            else if(sz == 1)
            {
                base = 0xb07c;
                i = new smsqmulator.cpu.Instruction()
                {
                     
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {  
                        int s = cpu.readMemoryWordPCInc();
                        int d=cpu.data_regs[(opcode >>>9) & 0x07]&0xffff;// des value
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
                        return disassembleOp(address, opcode, Size.Word,"d", cpu);
                    }
                };
            }
            else if(sz == 2)
            {
                base = 0xb0bc;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int s = cpu.readMemoryLongPC();         // get mem sign extended
                        cpu.pc_reg+=2;
                        int d=cpu.data_regs[(opcode >>>9) & 0x07];// dest value
                        int r = d-s;
                        cpu.reg_sr&=0xfff0;            // X bit not affected by cmp
                        boolean Sm = (s< 0);                // source is neg
                        boolean Dm = (d<0);                 // dist is neg
                        boolean Rm = (r<0);                 // result is neg
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
                            return disassembleOp(address, opcode, Size.Long,"d", cpu);
                    }
                };
            }
            else if(sz == 3)
            {
         //       if (sz==3)
       //             continue;
                base = 0xb0fc;
                i = new smsqmulator.cpu.Instruction()
                {
                     
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {  
                        int s = cpu.readMemoryWordPCSignedInc();         // get mem sign extended
                        int d=cpu.addr_regs[(opcode >>>9) & 0x07];// des value
                        int r = d-s;
                        cpu.reg_sr&=0xfff0;            // X bit not affected by cmp
                        boolean Sm = (s< 0);                // source is neg
                        boolean Dm = (d<0);                 // dist is neg
                        boolean Rm = (r<0);                 // result is neg
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
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Word,"a", cpu);
                    }
                };
            }
            else
            {
                base = 0xb1fc;
                i = new smsqmulator.cpu.Instruction()
                {
                     
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int s = cpu.readMemoryLongPC();         // get mem sign extended
                        cpu.pc_reg+=2;
                        int d=cpu.addr_regs[(opcode >>>9) & 0x07];// dest value
                        int r = d-s;
                        cpu.reg_sr&=0xfff0;            // X bit not affected by cmp
                        boolean Sm = (s< 0);                // source is neg
                        boolean Dm = (d<0);                 // dist is neg
                        boolean Rm = (r<0);                 // result is neg
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
                            return disassembleOp(address, opcode, Size.Long,"a", cpu);
                    }
                };
            }
            for(int r = 0; r < 8; r++)
            {
                cpu2.addInstruction(base + (r << 9), i);
            }
        }
    }

    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz,String reg, smsqmulator.cpu.MC68000Cpu cpu)
    {
            DisassembledOperand src = cpu.disassembleSrcEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), sz);
            DisassembledOperand dst = new DisassembledOperand(reg + ((opcode >> 9) & 0x07));
            if (reg.equals("a"))
            {
                return new DisassembledInstruction(address, opcode, "cmpa" + sz.ext(), src, dst);
            }
            else
            {
                return new DisassembledInstruction(address, opcode, "cmp" + sz.ext(), src, dst);
            }
    }
}
