package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;

/**
 * The TRAPV instruction in all of its variants.
 * 
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */

public class TRAPV implements InstructionSet
{
    @Override
    public final void register( final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        smsqmulator.cpu.Instruction i = new smsqmulator.cpu.Instruction() 
        {
           
                    
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                if ((cpu.reg_sr&2)!=0)
                    cpu.raiseException(7);
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                DisassembledOperand op = new DisassembledOperand(String.format("#%d", (opcode & 0x0f)));
                return new DisassembledInstruction(address, opcode, "trapv", op);
            }
        };
        cpu2.addInstruction(0x4e76, i);
    }
}
