package smsqmulator.cpu.instructions.TST;

/**
 * The TST instruction where data to be tested is in -(An).
 * 
 * 01001010ssmmmrrr = 0x4a00
 * 
 * ss = size : 00 byte, 01 word 10 long 
 * mmm = mode 100
 * rrr = register
 * 
 * @author and copyright for my code (c) Wolfgang Lenerz 2013
 * Based on code by Tony Headford, see his licence in accompanying file.
 */

import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

public class TST_MinusAn implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;

        for(int sz = 0; sz < 3; sz++)                       // byte sized
        {
            if(sz == 0)
            {
                base = 0x4a20;
                i= new smsqmulator.cpu.Instruction() 
                { 
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int reg=opcode &7;
                        if (reg==7)
                        {
                            cpu.addr_regs[reg]-=2;
                        }
                        else
                        {
                            cpu.addr_regs[reg]--;
                        }
                        int s=cpu.readMemoryByte(cpu.addr_regs[reg]);
                        cpu.reg_sr&=0xfff0;                            // keep  X, clear NZVC
                        if (s==0)
                        {
                            cpu.reg_sr+=4;
                        }
                        else if ((s&0x80)!=0)                              // negative?
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
            else if(sz == 1)                                // word sized
            {
                base = 0x4a60;
              i= new smsqmulator.cpu.Instruction() 
                { 
                   
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    { 
                        int reg=opcode &7;
                        cpu.addr_regs[reg]-=2;
                        int s=cpu.readMemoryWord(cpu.addr_regs[reg]);
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
            else                                            // long sized
            {  
                base = 0x4aa0;
                i= new smsqmulator.cpu.Instruction() 
                { 
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int reg=opcode &7;
                        cpu.addr_regs[reg]-=4;
                        int s=cpu.readMemoryLong(cpu.addr_regs[reg]);
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

            for (int ea_reg = 0; ea_reg < 8; ea_reg++)
            {
                cpu2.addInstruction(base + ea_reg, i);
            }
        }
    }

    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz, smsqmulator.cpu.MC68000Cpu cpu)
    {
            DisassembledOperand op = cpu.disassembleSrcEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), sz);
            return new DisassembledInstruction(address, opcode, "tst" + sz.ext(), op);
    }
}
