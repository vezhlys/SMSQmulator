package smsqmulator.cpu.instructions;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;

/**
 * The ILLEGAL instruction in all of its variants.
 * 
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013-2014
 * 
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
    

public class ILLEGAL implements InstructionSet
{
    @Override
    public final void register( final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        smsqmulator.cpu.Instruction is =new  smsqmulator.cpu.Instruction()
        {
            private smsqmulator.cpu.MC68000Cpu cpu=cpu2;
            
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
		cpu.raiseException(4);
                cpu.stopNow=-20;
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                return new DisassembledInstruction(address, opcode, "illegal");
            }
        };
        cpu2.addInstruction(0x4afc, is);
    }
}
