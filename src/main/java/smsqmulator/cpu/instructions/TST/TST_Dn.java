package smsqmulator.cpu.instructions.TST;

/**
 * The TST instruction where data to be tested is in Dn.
 * 
 * 01001010ssmmmrrr = 0x4a00
 * 
 * ss = size : 00 byte, 01 word 10 long 
 * mmm = mode 000
 * rrr = register
 * 
 * @author and copyright for my code (c) Wolfgang Lenerz 2013
 * Based on code by Tony Headford, see his licence in accompanying file.
 */

import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

public class TST_Dn implements InstructionSet
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
                base = 0x4a00;
                i= new smsqmulator.cpu.Instruction() 
                { 
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int s=cpu.data_regs[opcode &7]&0xff;
                        cpu.reg_sr&=0xfff0;            // keep  X, clear NZVC
                        if (s==0)
                        {
                            cpu.reg_sr+=4;
                        }
                        else if ((s&0x80)!=0)            // negative?
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
                base = 0x4a40;
                 i= new smsqmulator.cpu.Instruction() 
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int s=cpu.data_regs[opcode &7]&0xffff;
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
                base = 0x4a80;
                i= new smsqmulator.cpu.Instruction() 
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int s=cpu.data_regs[opcode &7];
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
