package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;

/**
 * The TRAP instruction in all of its variants.
 * 
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013-2014
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class TRAP implements InstructionSet
{
    
    @Override
    public final void register( final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base = 0x4e40;
        
        smsqmulator.cpu.Instruction i = new smsqmulator.cpu.Instruction() 
        {
            
                    
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
               cpu.raiseException(32+(opcode&15));
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                DisassembledOperand op = new DisassembledOperand(String.format("#%d", (opcode & 0x0f)));
                return new DisassembledInstruction(address, opcode, "trap", op);
            }
            
        };
        for(int trap = 0; trap<16; trap++)
        {
            cpu2.addInstruction(base + trap, i);
        }
    }
}
