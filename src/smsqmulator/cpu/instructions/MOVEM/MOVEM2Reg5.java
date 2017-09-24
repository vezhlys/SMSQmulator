package smsqmulator.cpu.instructions.MOVEM;

/**
 * This is the MOVEM from .L memory to reg instruction.
 *      d
 * 010011001smmmrrr
 * where :
 * d = direction of transfer = 0
 * s = size : 0=Word 1=Long
 * mmm = mode = 111
 * rrr = register = 001
 * 
 * @author and copyright for my code (c) Wolfgang Lenerz 2013 - 2014.
 * 
 * Based on code by Tony Headford, see his licence in accompanying filen
 */


import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

public class MOVEM2Reg5 implements InstructionSet
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
                base = 0x4cb9;  
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int reglist = cpu.readMemoryWordPCInc();// the registers to treat           
                        int address = cpu.readMemoryLongPC();         // get mem sign extended
                        cpu.pc_reg+=2;  
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
                base = 0x4cf9;  
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int reglist = cpu.readMemoryWordPCInc();// the registers to treat
                        int address = cpu.readMemoryLongPC();         // get mem sign extended
                        cpu.pc_reg+=2;    
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
                    }
                   @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                            return disassembleOp(address, opcode, Size.Long, cpu);
                    }
                };
            }
            cpu2.addInstruction(base, i);
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
        src = cpu.disassembleSrcEA(address + 4, mode, reg, sz);
        dst = new DisassembledOperand(smsqmulator.Helper.regListToString(reglist, reversed), 2, reglist);
        return new DisassembledInstruction(address, opcode, "movem" + sz.ext(), src, dst);
    }
}
