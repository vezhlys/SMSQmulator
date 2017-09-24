package smsqmulator.cpu.instructions.MOVEM;

/**
 * This is the MOVEM from d8(An,Xn) to memory instruction.
 *      d
 * 010010001smmmrrr
 * where :
 * d = direction of transfer = 0
 * s = size : 0=Word 1=Long
 * mmm = mode = 110
 * rrr = register
 * 
 * @author and copyright for my code (c) Wolfgang Lenerz 2013 - 2014.
 * 
 * Based on code by Tony Headford, see his licence in accompanying file
 */

import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

public class MOVEM2Mem3 implements InstructionSet
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
                base = 0x48b0;
              
                i = new smsqmulator.cpu.Instruction()
                {
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int address=cpu.addr_regs[opcode &7];      
                        int reglist = cpu.readMemoryWordPCInc();// the registers to treat
                        address+= getDisplacement(cpu);// the (an) register = the address we're talking about
                        int bit = 1;
                        //assumes d0 is first bit
                        for (int n = 0; n < 8; n++)
                        {
                            if((reglist & bit) != 0)
                            {
                                cpu.writeMemoryWord(address, cpu.data_regs[n]);
                                address += 2;
                            }
                            bit <<= 1;
                        }
                        for (int n = 0; n < 8; n++)
                        {
                            if((reglist & bit) != 0)
                            {
                                cpu.writeMemoryWord(address, cpu.addr_regs[n]);
                                address += 2;
                            }
                            bit <<= 1;
                        }
                    }
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Word, cpu);
                    }
                    
                    protected int getDisplacement(smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int ext = cpu.readMemoryWordPCSignedInc();// extention word, contains size + displacement+reg
                        int displacement=(ext & 0x80)!=0?ext| 0xffffff00:ext&0xff;    //displacemnt
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
    
                };
            }
            else
            {
                base = 0x48f0;
                i = new smsqmulator.cpu.Instruction()
                {
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int address=cpu.addr_regs[opcode &7];     
                        int reglist = cpu.readMemoryWordPCInc();// the registers to treat
                        address+= getDisplacement(cpu); // the (an) register = the address we're talking about
                        int bit = 1;
                        //assumes d0 is first bit
                        for (int n = 0; n < 8; n++)
                        {
                            if((reglist & bit) != 0)
                            {
                                cpu.writeMemoryLong(address, cpu.data_regs[n]);
                                address += 4;
                            }
                            bit <<= 1;
                        }
                        for (int n = 0; n < 8; n++)
                        {
                            if((reglist & bit) != 0)
                            {
                                cpu.writeMemoryLong(address, cpu.addr_regs[n]);
                                address += 4;
                            }
                            bit <<= 1;
                        }
                    }
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                            return disassembleOp(address, opcode, Size.Long, cpu);
                    }
                    
                    protected int getDisplacement(smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int ext = cpu.readMemoryWordPCSignedInc();// extention word, contains size + displacement+reg
                        int displacement=(ext & 0x80)!=0?ext| 0xffffff00:ext&0xff;    //displacemnt
                        if((ext & 0x8000)!=0)
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
        DisassembledOperand src;
        DisassembledOperand dst;
        int mode = (opcode >> 3) & 0x07;
        int reg = (opcode & 0x07);
        int reglist = cpu.readMemoryWord(address + 2);
        boolean reversed = (mode == 4);
        src = new DisassembledOperand(smsqmulator.Helper.regListToString(reglist, reversed), 2, reglist);
        dst = cpu.disassembleDstEA(address + 4, mode, reg, sz);
        return new DisassembledInstruction(address, opcode, "movem" + sz.ext(), src, dst);
    }
}
