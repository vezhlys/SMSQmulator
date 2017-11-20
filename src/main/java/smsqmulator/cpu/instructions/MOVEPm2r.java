package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The MOVEPr2m instruction where move is made from mem to reg.
 *  fedcba9876543210
 *  0000rrr10s001aaa = 108
 *  where   rrr = data reg to be swapped
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class MOVEPm2r implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;
       
        for(int sz = 0; sz < 2; sz++)                           // word and long only
        {
            if (sz == 0)                                        // word sized
            {
                base = 0x108;
                i = new smsqmulator.cpu.Instruction()   
                {
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int address=cpu.addr_regs[opcode&7]+cpu.readMemoryWordPCSignedInc();
                        int val = cpu.readMemoryByte(address) << 8;
                        val |= cpu.readMemoryByte(address + 2);
                        cpu.data_regs[(opcode>>9)&7]&=0xffff0000;
                        cpu.data_regs[(opcode>>9)&7]|=val;
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
                base = 0x0148;                                  // long sized
                i = new smsqmulator.cpu.Instruction()   
                {
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int address=cpu.addr_regs[opcode&7]+cpu.readMemoryWordPCSignedInc();
                        int val = cpu.readMemoryByte(address) << 24;
                        val |= (cpu.readMemoryByte(address + 2)<<16);
                        val |= (cpu.readMemoryByte(address+4) << 8);
                        val |= cpu.readMemoryByte(address + 6);
                        cpu.data_regs[(opcode>>9)&7]=val;
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
                int rdreg=dreg<<9;
                for (int reg=0;reg<8;reg++)
                {
                    cpu2.addInstruction(base+ rdreg + reg, i);
                }
            }
        }
    }
            
    protected final DisassembledInstruction disassembleOp(int address,int opcode, Size sz,smsqmulator.cpu.MC68000Cpu cpu)
    {
        int dis = cpu.readMemoryWordSigned(address + 2);
	DisassembledOperand src = new DisassembledOperand(String.format("#$%04x(a%d)", dis, (opcode & 0x07)), 2, dis);
	DisassembledOperand dst = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
        return new DisassembledInstruction(address, opcode, "movep" + sz.ext(), src,dst);
    }
}
