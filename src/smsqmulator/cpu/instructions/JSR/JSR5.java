package smsqmulator.cpu.instructions.JSR;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The JSR instruction where source is .W absolute
 * Based on code by Tony Headford, see his licence in accompanying file.
 
 * @author and copyright (c) 2012 wolfgang lenerz
 * 
 *  01001110 = 0x4e
 *  10mmmrrr 
 *  where m = mode =111
 */
public class JSR5 implements InstructionSet
{
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base= 0x4eB9;
        smsqmulator.cpu.Instruction i;
        i = new smsqmulator.cpu.Instruction()
        {  
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int s = cpu.readMemoryLongPCinc();         // get mem sign extended
                cpu.addr_regs[7] -= 4;
                cpu.writeMemoryLong(cpu.addr_regs[7],cpu.pc_reg*2);               
		cpu.pc_reg=(s&smsqmulator.cpu.MC68000Cpu.cutOff)/2;
            }
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                    return disassembleOp(address, opcode, Size.Long, cpu);
            }
        };

        cpu2.addInstruction(base, i);                         
    }
    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz, smsqmulator.cpu.MC68000Cpu cpu)
    {
            DisassembledOperand op = cpu.disassembleSrcEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), sz);
            return new DisassembledInstruction(address, opcode, "jsr", op);
    }
}
