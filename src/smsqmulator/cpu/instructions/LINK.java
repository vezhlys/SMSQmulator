package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;

/**
 * The LINK instruction in all of its variants.
 *  fedcba9876543210
 *  0100011001010rrr  0x4e50
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class LINK implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base=0x4e50;
        smsqmulator.cpu.Instruction i= new smsqmulator.cpu.Instruction()   
        {
            
            
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                cpu.addr_regs[7]-=4;
                cpu.writeMemoryLong(cpu.addr_regs[7],cpu.addr_regs[opcode&7]);
                cpu.addr_regs[opcode&7]=cpu.addr_regs[7];
                cpu.addr_regs[7]+=cpu.readMemoryWordPCSignedInc();
            }
            
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
		DisassembledOperand src = new DisassembledOperand("a" + (opcode & 0x07));
		int dis = cpu.readMemoryWordSigned(address + 2);
		DisassembledOperand dst = new DisassembledOperand(String.format("#$%04x", dis), 2, dis);
		return new DisassembledInstruction(address, opcode, "link", src, dst);
            }   
        };
        for (int reg=0;reg<8;reg++)
            cpu2.addInstruction(base+reg,i);
    }
}
