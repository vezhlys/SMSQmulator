package smsqmulator.cpu.instructions.MOVEM;

/**
 * This is the MOVEM from  memory (An)+  to regs instruction.
 * MOVEM.L (A7)+,D1/D2
 *      d
 * 010011001smmmrrr
 * where :
 * d = direction of transfer = 1
 * s = size : 0=Word 1=Long
 * mmm = mode = 011
 * rrr = register
 * 
 * @author and copyright for my code (c) Wolfgang Lenerz 2013 - 2014.
 * 
 * Based on code by Tony Headford, see his licence in accompanying file
 * 
 *  // wl when the ea is specified by a pre-decreasing mode (eg movem.l range,-(an) , then, if the addressing register
            // itself is part of the range and for a 68000 and 68010, the value stored is the INITIAL value of the addressing reg. 
            // For a 68020 and up, the value written is the initial value minus the size (word or long) of the operation.
        // THIS CODE IS FOR A 68008,68000,68010
 * 
 */

import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

public class MOVEM2Reg6 implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;

        // reg to mem
        for(int sz = 0; sz < 2; sz++)
        {
            if(sz == 0)
            {
                base = 0x4c98;
                i = new smsqmulator.cpu.Instruction()
                {
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int address=cpu.addr_regs[opcode &7];      
                        int reglist = cpu.readMemoryWordPCInc();// the registers to treat
                        int bit = 1;
                        //assumes d0 is first bit
                        for (int n = 0; n < 8; n++)
                        {
                            if((reglist & bit) != 0)
                            {
                                cpu.data_regs[n]=cpu.readMemoryWordSigned(address);
                                address += 2;  
                            }
                            bit <<= 1;
                        }
                        for (int n = 0; n < 8; n++)
                        {
                            if((reglist & bit) != 0)
                            {
                                cpu.addr_regs[n]=cpu.readMemoryWordSigned(address);
                                address += 2;
                            }
                            bit <<= 1;
                        }
                        cpu.addr_regs[opcode &7]= address;
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
                base = 0x4cd8;
                i = new smsqmulator.cpu.Instruction()
                {
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int address=cpu.addr_regs[opcode &7];      
                        int reglist = cpu.readMemoryWordPCInc();// the registers to treat
                        int bit = 1;      
                        //assumes d0 is first bit
                        for (int n = 0; n < 8; n++)
                        {
                            if((reglist & bit) != 0)
                            {
                                cpu.data_regs[n]=cpu.readMemoryLong(address);
                                address += 4;
                            }
                            bit <<= 1;
                        }
                        for (int n = 0; n < 8; n++)
                        {
                            if((reglist & bit) != 0)
                            {
                                cpu.addr_regs[n]=cpu.readMemoryLong(address);
                                address += 4;
                            }
                            bit <<= 1;
                        }
                        cpu.addr_regs[opcode &7]= address;
                    }
                    
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Long, cpu);
                    }
                };
            }

            for(int ea_reg = 0; ea_reg < 8; ea_reg++)
            {
                cpu2.addInstruction(base + ea_reg, i);
            }
        }
    }

    public final DisassembledInstruction disassembleOp(int address, int opcode, Size sz,smsqmulator.cpu.MC68000Cpu cpu)
    {
        int mode = (opcode >> 3) & 0x07;
        int reg = (opcode & 0x07);
        int reglist = cpu.readMemoryWord(address + 2);
        boolean reversed = (mode == 4);
        DisassembledOperand src = cpu.disassembleSrcEA(address + 4, mode, reg, sz);
        DisassembledOperand dst = new DisassembledOperand(smsqmulator.Helper.regListToString(reglist, reversed), 2, reglist);
        return new DisassembledInstruction(address, opcode, "movem" + sz.ext(), src, dst);
    }
}
