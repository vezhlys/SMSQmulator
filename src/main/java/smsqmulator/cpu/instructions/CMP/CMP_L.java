package smsqmulator.cpu.instructions.CMP;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The CMP and CMPA instructions where source is .L absolute.
 
 * @author and copyright (c) 2012 -2014 Wolfgang lenerz
 * 
 *  1011dddsssmmmrrr
 *  
 *  where   ddd= destination reg
 *          sss= size (000=byte, 001 = word, 010 = long, 011 = word address reg,111 = long address reg ))
 *          mmm = mode =111, 
 *          rrr = source reg =001
 * @version 1.01
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class CMP_L implements InstructionSet
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
                base = 0xb039;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int s = cpu.readMemoryLongPC();         // get mem sign extended
                        cpu.pc_reg+=2;
                        s=cpu.readMemoryByte(s);
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
                base = 0xb079;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {  
                        int s = cpu.readMemoryLongPC();         // get mem sign extended
                        cpu.pc_reg+=2;
                        s=cpu.readMemoryWord(s);
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
            else if (sz==2)
            {
                base = 0xb0b9;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                       int s = cpu.readMemoryLongPC();         // get mem sign extended
                        cpu.pc_reg+=2;
                        s=cpu.readMemoryLong(s);
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
                base = 0xb0F9;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {  
                        int s = cpu.readMemoryLongPC();         // get mem sign extended
                        cpu.pc_reg+=2;
                        s=cpu.readMemoryWordSigned(s);
                        int d=cpu.addr_regs[(opcode >>>9) & 0x07];// des value
                        int r = d-s;
                        cpu.reg_sr&=0xfff0;            // X bit not affected by cmp
                        boolean Sm = s< 0;                  // source is neg
                        boolean Dm = (d <0);                // dist is neg
                        boolean Rm = (r <0);                // result is neg
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
                        return disassembleOp(address, opcode, Size.Word,"a", cpu);
                    }
                };
            }
            else
            {
                base = 0xb1F9;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int s = cpu.readMemoryLongPC();         // get mem sign extended
                        cpu.pc_reg+=2;
                        s=cpu.readMemoryLong(s);
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
