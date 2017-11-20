package smsqmulator.cpu.instructions.SUB2EA;

import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The SUB instruction where the source is Dn and the destination is d16(An).
 *  
 * 
 *  1101dddooommmrrr
 *  
 *  where   
 *          ddd is the destination register
 *          ooo is the opmode : 100 = byte, 101 = word , 110 =long
 *          mmm = ea mode =101, 
 *          rrr = source register
 * @version
 *  1.01 d must be cut to size (.b or .w) before testing whether it's 0.
 * 
 * @author and copyright for my code (c) Wolfgang Lenerz 2013 - 2014.
 * 
 * Based on code by Tony Headford, see his licence in accompanying file
 */

public class SUB_d16An implements InstructionSet
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
                base = 0x9128;
                i = new smsqmulator.cpu.Instruction()    // byte sized SUB
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        boolean Sm, Rm,Dm;
                        int s=cpu.data_regs[(opcode>>9)&7]&0xff;
                        if ((s & 0x80)!=0)
                        {
                            s|=0xffffff00;     
                            Sm=true;
                        }
                        else
                        {
                            Sm=false;
                        }
                        int a=cpu.addr_regs[opcode&7] + cpu.readMemoryWordPCSignedInc();//source address
                        
                        int d=cpu.readMemoryByteSigned(a);
                        Dm=d<0;
                        
                        d-=s;
                        if ((d & 0x80)!=0)
                            Rm=true;
                        else
                            Rm=false;
                        cpu.writeMemoryByte(a,d); 
                        cpu.reg_sr&=0xffe0;            // all flags 0
                       
                        if ((d&0xff) == 0)
                        {
                            cpu.reg_sr+=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr+=8;             // set N flag
                        }
                        if((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                        {
                            cpu.reg_sr+=2;             // set V flag
                        }

                        if((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                        {
                            cpu.reg_sr+=17;            // set X and C flags
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
                base = 0x9168;
                i = new smsqmulator.cpu.Instruction()    // word sized SUB
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        boolean Sm, Rm,Dm;
                       
                        int s=cpu.data_regs[(opcode>>9)&7]&0xffff;
                        if ((s & 0x8000)!=0)
                        {
                            s|=0xffff0000;     
                            Sm=true;
                        }
                        else
                        {
                            Sm=false;
                        }
                        int a=cpu.addr_regs[opcode&7] + cpu.readMemoryWordPCSignedInc();//source address
                        
                        int d=cpu.readMemoryWordSigned(a);
                        Dm=d<0;
                        
                        d-=s;
                        if ((d & 0x8000)!=0)
                            Rm=true;
                        else
                            Rm=false;
                        cpu.writeMemoryWord(a,d);  
                        cpu.reg_sr&=0xffe0;            // all flags 0
                       
                        if ((d&0xffff) == 0)
                        {
                            cpu.reg_sr+=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr+=8;             // set N flag
                        }
                        if((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                        {
                            cpu.reg_sr+=2;             // set V flag
                        }

                        if((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                        {
                            cpu.reg_sr+=17;            // set X and C flags
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
                base = 0x91a8;
                i = new smsqmulator.cpu.Instruction()    // long sized SUB
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {                   
                        int reg=opcode&7;
                        int s=cpu.data_regs[(opcode>>9)&7];
                        boolean Sm=s<0; 
                        int a=cpu.addr_regs[reg] + cpu.readMemoryWordPCSignedInc();//source address
                       
                        int d=cpu.readMemoryLong(a);
                        boolean Dm=d<0;
                        d-=s;
                        boolean Rm=d<0;
                        cpu.writeMemoryLong(a,d); 
                        cpu.reg_sr &= 0xffe0;            // all flags 0
                    //    if (cpu.data_regs[reg] == 0)
                        if (d==0)
                        {
                            cpu.reg_sr+=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr+=8;             // set N flag
                        }
                        if((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                        {
                            cpu.reg_sr+=2;             // set V flag
                        }

                        if((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                        {
                            cpu.reg_sr+=17;            // set X and C flags
                        }
                    }
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Long, cpu);
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
    
    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz, smsqmulator.cpu.MC68000Cpu cpu)
    {
        DisassembledOperand src = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
        DisassembledOperand dst = cpu.disassembleDstEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), sz);
        return new DisassembledInstruction(address, opcode, "SUB" + sz.ext(), src, dst);
    }
}
