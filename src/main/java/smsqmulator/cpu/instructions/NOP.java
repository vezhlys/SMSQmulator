package smsqmulator.cpu.instructions;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;

/**
 * The NOP instruction.
 * 
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013-2014
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
    
public class NOP implements InstructionSet
{
    @Override
    public final void register( smsqmulator.cpu.MC68000Cpu cpu2)
    {
        smsqmulator.cpu.Instruction is =new  smsqmulator.cpu.Instruction()
        {
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
            }
            
            @Override
            public final DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                return new DisassembledInstruction(address, opcode, "NOP");
            }
        };
        cpu2.addInstruction(0x4e71, is);
    }
}
    
