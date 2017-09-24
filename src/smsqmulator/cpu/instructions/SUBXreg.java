package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The SUBX instruction (mem to mem) in all of its variants.
 *  fedcba9876543210
 *  1001ddd1ss001rrr = 9100
 *  where   rrr = source reg
 *          ss  = size
 *          ddd = destination reg
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013-2014
 * 
 * @author and Copyright (c) for his code by Tony Headford, see licence below.
 */
public class SUBXreg implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;
       
        for(int sz = 0; sz < 3; sz++)                           // byte, word and long
        {
            if (sz == 0)                                        // byte sized axdx
            {
                base = 0x9100;
                i = new smsqmulator.cpu.Instruction()   
                {
                   
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int dreg=(opcode>>9)&7;                 // destination reg
                        int val=cpu.data_regs[opcode&7]&0xff;// source value
                        int d=cpu.data_regs[dreg]&0xff;    // destination value
                        boolean Sm, Rm,Dm;
                        if ((val & 0x80)!=0)
                        {
                            val|=0xffffff00;    
                            Sm=true;
                        }               
                        else
                            Sm=false;
                        if ((d & 0x80)!=0)
                        {
                            d|=0xffffff00;     
                            Dm=true;
                        }
                        else
                            Dm=false;


                        d-=val;
                        if ((cpu.reg_sr&16)!=0)
                            d--;
                        if ((d & 0x80)!=0)
                            Rm=true;
                        else
                            Rm=false;
                        d&=0xff;
                        cpu.data_regs[dreg]&=0xffffff00;
                        cpu.data_regs[dreg]|=d;
                        cpu.reg_sr&=0xffe4;                // all flags 0, except Z flag
                        //n z v c                               // flags, x will be set to c
                        //8 4 2 1
                        if (d != 0)                             // clear Z flag only if result is non zero
                        {
                            cpu.reg_sr&=~4;
                            if (Rm)                             // set N flag
                                cpu.reg_sr+=8;
                        }
                        if ((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                            cpu.reg_sr+=2;                 // set V flag

                        if ((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                            cpu.reg_sr+=17;                // set X and C flags
                    }
                    
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Byte,cpu);
                    }   
                };
            }
            else if (sz==1)                                     // word sized addx
            {
                base = 0x9140; 
                i = new smsqmulator.cpu.Instruction()   
                {
                     
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int dreg=(opcode>>9)&7;                 // destination reg
                        int val=cpu.data_regs[opcode&7]&0xffff;// source value
                        int d=cpu.data_regs[dreg]&0xffff;    // destination value
                        boolean Sm, Rm,Dm;
                        if ((val & 0x8000)!=0)
                        {
                            val|=0xffff0000;    
                            Sm=true;
                        }               
                        else
                            Sm=false;
                        if ((d & 0x8000)!=0)
                        {
                            d|=0xffff0000;     
                            Dm=true;
                        }
                        else
                            Dm=false;
                        d-=val;
                        if ((cpu.reg_sr&16)!=0)
                            d--;
                        if ((d & 0x8000)!=0)
                            Rm=true;
                        else
                            Rm=false;
                        d&=0xffff;
                        cpu.data_regs[dreg]&=0xffff0000;
                        cpu.data_regs[dreg]|=d;
                        cpu.reg_sr&=0xffe4;                // all flags 0, except Z flag
                        //n z v c                               // flags, x will be set to c
                        //8 4 2 1
                        if (d != 0)                             // clear Z flag only if result is non zero
                        {
                            cpu.reg_sr&=~4;
                            if (Rm)                             // set N flag
                                cpu.reg_sr+=8;
                        }
                        if ((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                            cpu.reg_sr+=2;                 // set V flag

                        if ((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                            cpu.reg_sr+=17;                // set X and C flags
                    }
                    
                     @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Word,cpu);
                    }   
                };
            }
            else
            {
                base = 0x9180;                                  // long sized addx
                i = new smsqmulator.cpu.Instruction()   
                {
                     
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int dreg=(opcode>>9)&7;                 // destination reg
                        int val=cpu.data_regs[opcode&7];   // source value
                        int d=cpu.data_regs[dreg];         // destination value
                        boolean Sm=val<0;
                        boolean Dm=d<0;
                        d-=val;
                        if ((cpu.reg_sr&16)!=0)
                            d--;
                        boolean Rm=d<0;
                        cpu.data_regs[dreg]=d;
                        cpu.reg_sr&=0xffe4;                // all flags 0, except Z flag
                        //n z v c                               // flags, x will be set to c
                        //8 4 2 1
                        if (d != 0)                             // clear Z flag only if result is non zero
                        {
                            cpu.reg_sr&=~4;
                            if (Rm)                             // set N flag
                                cpu.reg_sr+=8;
                        }
                        if ((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                            cpu.reg_sr+=2;                 // set V flag

                        if ((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                            cpu.reg_sr+=17;                // set X and C flags
                    }
                    
                     @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Long,cpu);
                    }   
                };
            }
            for (int dreg=0;dreg<8;dreg++)
            {
                int rmode=dreg<<9;
                for (int sreg=0;sreg<8;sreg++)
                {
                    cpu2.addInstruction(base+rmode + sreg, i);
                }
            }
        }
    }
            
    protected final DisassembledInstruction disassembleOp(int address,int opcode, Size sz,smsqmulator.cpu.MC68000Cpu cpu)
    {
	DisassembledOperand src = new DisassembledOperand("-(a" + (opcode & 0x07) + ")");
	DisassembledOperand dst = new DisassembledOperand("-(a" + ((opcode >> 9) & 0x07) + ")");
        return new DisassembledInstruction(address, opcode, "subx" + sz.ext(), src, dst);
    }
}
