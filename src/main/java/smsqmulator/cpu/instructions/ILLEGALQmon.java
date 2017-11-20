package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledInstruction;

/**
 * Illegal instruction used by Qmon (4afb) - set the correct pc address.
 * 
 * @author and copyright for my code (c) Wolfgang Lenerz 2013
 * 
 * Based on Tony Headford's  m68k.cpu.instruction (C) Tony Headford.
 *
 */
    

public class ILLEGALQmon implements smsqmulator.cpu.InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        cpu2.addInstruction(0x4afb, new smsqmulator.cpu.Instruction()
        {
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                cpu.pc_reg--;                         // reset the PC to the initial instruction
		cpu.raiseException(4);
                cpu.stopNow=-20;
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                    return new DisassembledInstruction(address, opcode, "illegal");
            }
        });
    }
}
