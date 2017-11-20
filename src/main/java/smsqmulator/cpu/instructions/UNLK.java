package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
/**
 * The UNLK instruction in all of its variants.
 *  fedcba9876543210
 *  0100011001011rrr  0x4e58
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013-2014
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class UNLK implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base=0x4e58;
        smsqmulator.cpu.Instruction i= new smsqmulator.cpu.Instruction()   
        {
            
            
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                cpu.addr_regs[7]=cpu.addr_regs[opcode&7];
                cpu.addr_regs[opcode&7]=cpu.readMemoryLong(cpu.addr_regs[7]);
                cpu.addr_regs[7]+=4;
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
		DisassembledOperand src = new DisassembledOperand("a" + (opcode & 0x07));
		return new DisassembledInstruction(address, opcode, "unlk", src);
            }   
        };
        for (int reg=0;reg<8;reg++)
            cpu2.addInstruction(base+reg,i);
    }
}
