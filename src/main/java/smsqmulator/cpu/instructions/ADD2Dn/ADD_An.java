package smsqmulator.cpu.instructions.ADD2Dn;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The ADD and ADDA instruction where the destination is a data/addr register and the source is An.
 *  
 * @author and copyright (c) 2013 -2014 wolfgang lenerz
 * 
 *  1101dddooommmrrr
 *  
 *  where   
 *          ddd is the destination register
 *          ooo is the opmode : 000 = byte, 001 = word , 010 =long, 011 = word ADDA, 111 = long ADDA
 *          mmm = ea mode =001, 
 *          rrr = source register
 * @version
 *  1.01 d must be cut to size (.b or .w) before testing whether it's 0.
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class ADD_An implements InstructionSet
{
   
    @Override
    public final void register( final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;

        for(int sz = 1; sz < 5; sz++)                       // in this ea mode, word and long only
        {
            if(sz == 1)
            {
                base = 0xd048;
                i = new smsqmulator.cpu.Instruction()    // word sized add
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        boolean Sm, Rm,Dm;
                        int reg=(opcode>>9)&7;
                        int s=cpu.addr_regs[opcode&7]&0xffff;
                        if ((s & 0x8000)!=0)
                        {
                            s|=0xffff0000;    
                            Sm=true;
                        }               
                        else
                            Sm=false;
                        int d=cpu.data_regs[reg]&0xffff;
                        if ((d & 0x8000)!=0)
                        {
                            d|=0xffff0000;     
                            Dm=true;
                        }
                        else
                        {
                            Dm=false;
                        }
                        d+=s;
                        d &=0xffff;
                        if((d & 0x8000)!=0)
                        {
                            Rm=true;
                        }
                        else
                        {
                            Rm=false;
                        }
                        cpu.data_regs[reg]&=0xffff0000;
                        cpu.data_regs[reg]|=d;
                        
                        cpu.reg_sr&=0xffe0;            // all flags 0
                       
                        if (d == 0)
                        {
                            cpu.reg_sr|=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr|=8;             // set N flag
                        }
                       
                        if ((Sm && Dm && !Rm) || (!Sm && !Dm && Rm))
                        {
                            cpu.reg_sr+=2;              // set V flag
                        }
				
                        if ((Sm && Dm) || (!Rm && Dm) || (Sm && !Rm))
                        {
                            cpu.reg_sr+=17;              // set X and C flags
                        }
                    }
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Word,"d",cpu);
                    }

                };
            }
            else if (sz==2)
            {
                base = 0xd088;
                i = new smsqmulator.cpu.Instruction()    // long sized add
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int reg=(opcode>>9)&7;
                        int s=cpu.addr_regs[opcode&7];
                        boolean Sm= s<0;
                        boolean Dm= cpu.data_regs[reg]<0;
                        cpu.data_regs[reg]+=s;
                        boolean Rm= cpu.data_regs[reg]<0;
                        cpu.reg_sr &= 0xffe0;            // all flags 0
                        if (cpu.data_regs[reg] == 0)
                        {
                            cpu.reg_sr+=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr+=8;             // set N flag
                        }
                       
                        if ((Sm && Dm && !Rm) || (!Sm && !Dm && Rm))
                        {
                            cpu.reg_sr+=2;             // set V flag
                        }
				
                        if ((Sm && Dm) || (!Rm && Dm) || (Sm && !Rm))
                        {
                            cpu.reg_sr+=17;            // set X and C flags
                        }
                    }
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Long,"d",cpu);
                    }

                };
            }
            else if(sz == 3)
            {
                base = 0xd0c8;
                i = new smsqmulator.cpu.Instruction()    // word sized adda
                {
                     
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int s=cpu.addr_regs[opcode&7]&0xffff;
                        if ((s & 0x8000)!=0)
                        {
                            s|=0xffff0000;    
                        }   
                        cpu.addr_regs[(opcode>>9)&7]+=s;
                    }
                     @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Word,"a",cpu);
                    }

                };
            }
            else 
            {
                base = 0xd1c8;
                i = new smsqmulator.cpu.Instruction()    // long sized adda
                {
                   
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        cpu.addr_regs[(opcode>>9)&7]+=cpu.addr_regs[opcode&7];
                    }
                   @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Long,"a",cpu);
                    }

                };
            }
            for(int reg = 0; reg < 8; reg++)
            {
                for(int ea_reg = 0; ea_reg < 8; ea_reg++)
                {
                    cpu2.addInstruction(base + (reg << 9) + ea_reg, i);
                }
                    
            }
        }
    }
    
 
    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz,String reg,smsqmulator.cpu.MC68000Cpu cpu)
    {
        DisassembledOperand src = cpu.disassembleSrcEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), sz);
        DisassembledOperand dst = new DisassembledOperand(reg + ((opcode >> 9) & 0x07));
        if (reg.equals("a"))
            return new DisassembledInstruction(address, opcode, "adda" + sz.ext(), src, dst);
        else
            return new DisassembledInstruction(address, opcode, "add" + sz.ext(), src, dst);
    }
}
