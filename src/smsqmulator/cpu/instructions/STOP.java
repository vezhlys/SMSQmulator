package smsqmulator.cpu.instructions;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;

/**
 * The STOP instructions.
 * 
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013-2014
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
    
public class STOP implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        smsqmulator.cpu.Instruction is =new  smsqmulator.cpu.Instruction()
        {
            private smsqmulator.cpu.MC68000Cpu cpu=cpu2;
            
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                if ((cpu.reg_sr&0x2000)==0)
                    cpu.raiseException(8);
                else
                {
                    int val=cpu.readMemoryWordPCInc()&0xf71f;// mask out bits 5,6,7 & 11 : they are always 0!
                    cpu.reg_sr=val;
                }
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                return new DisassembledInstruction(address, opcode, "stop");
            }
        };
        cpu2.addInstruction(0x4e72, is);
    }
}
