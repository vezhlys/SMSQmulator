package smsqmulator.cpu.instructions.ADD2EA;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The ADD instruction where the source is Dn and the destination is d8(An,Xn).
 *  
 * @author and copyright (c) 2013 -2014 wolfgang lenerz
 * 
 *  1101dddooommmrrr
 *  
 *  where   
 *          ddd is the destination register
 *          ooo is the opmode : 100 = byte, 101 = word , 110 =long
 *          mmm = ea mode =110, 
 *          rrr = source register
 * @version
 *  1.01 d must be cut to size (.b or .w) before testing whether it's 0.
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */

public class ADD_d8AnXn implements InstructionSet
{
    
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;

        for(int sz = 0; sz < 3; sz++)                       
        {
            if(sz == 0)
            {
                base = 0xd130;
                i = new smsqmulator.cpu.Instruction()    // byte sized add
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
                        int a=getDisplacement(cpu)+cpu.addr_regs[opcode &7];//desst address
                        int d=cpu.readMemoryByteSigned(a);
                        Dm=d<0;
                        
                        d+=s;
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
                        return disassembleOp(address, opcode, Size.Byte ,cpu);
                    }
                };
            }
            else if(sz == 1)
            {
                base = 0xd170;
                i = new smsqmulator.cpu.Instruction()    // word sized add
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
                        int a=getDisplacement(cpu)+cpu.addr_regs[opcode &7];//dest 
                        
                        int d=cpu.readMemoryWordSigned(a);
                        Dm=d<0;
                        
                        d+=s;
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
                        return disassembleOp(address, opcode, Size.Word ,cpu);
                    }
                };
            }
            else
            {
                base = 0xd1b0;
                i = new smsqmulator.cpu.Instruction()    // long sized add
                {
                   
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {                   
                        int reg=opcode&7;
                        int s=cpu.data_regs[(opcode>>9)&7];
                        boolean Sm=s<0; 
                        int a=getDisplacement(cpu)+cpu.addr_regs[reg];//dest 
                       
                        int d=cpu.readMemoryLong(a);
                        boolean Dm=d<0;
                        d+=s;
                        boolean Rm=d<0;
                        cpu.writeMemoryLong(a,d); 
                        cpu.reg_sr &= 0xffe0;            // all flags 0
                        //if (cpu.data_regs[reg] == 0)
                        if (d==0)
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
                        return disassembleOp(address, opcode, Size.Long ,cpu);
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
 
  /**
     * gets the displacement and increments the PC.
     * 
     * @param cpu the smsqmulator.cpu.MC68000Cpu used the cpu used.
     * 
     * @return  the displacement
     */
    protected int getDisplacement(smsqmulator.cpu.MC68000Cpu cpu)
    {
        int ext = cpu.readMemoryWordPCSignedInc();      // extention word, contains size + displacement+reg
        int displacement=(ext & 0x80)!=0?ext| 0xffffff00:ext&0xff;     //displacemnt
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
    protected final DisassembledInstruction disassembleOp(int address,int opcode, Size sz,smsqmulator.cpu.MC68000Cpu cpu)
    {
        DisassembledOperand src = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
        DisassembledOperand dst = cpu.disassembleDstEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), sz);
        return new DisassembledInstruction(address, opcode, "add" + sz.ext(), src, dst);
    }
}

/*
//  M68k - Java Amiga MachineCore
//  Copyright (c) 2008-2010, Tony Headford
//  All rights reserved.
//
//  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
//  following conditions are met:
//
//    o  Redistributions of source code must retain the above copyright notice, this list of conditions and the
//       following disclaimer.
//    o  Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
//       following disclaimer in the documentation and/or other materials provided with the distribution.
//    o  Neither the name of the M68k Project nor the names of its contributors may be used to endorse or promote
//       products derived from this software without specific prior written permission.
//
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
//  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
//  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
//  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
//  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
//  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
//  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
*/
