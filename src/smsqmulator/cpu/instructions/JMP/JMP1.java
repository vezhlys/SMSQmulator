package smsqmulator.cpu.instructions.JMP;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The JMP instruction where source is (An).
 
 * @author and copyright (c) 2012 wolfgang lenerz
 * 
 *  01001110 = 0x4e
 *  11mmmrrr 
 *  where m = mode =010
 *
 * Based on code by Tony Headford, see his licence in accompanying file..
 */
public class JMP1  implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base= 0x4ed0;
        smsqmulator.cpu.Instruction i;
        i = new smsqmulator.cpu.Instruction()
        {
           
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {

		cpu.pc_reg=(cpu.addr_regs[opcode &7]&smsqmulator.cpu.MC68000Cpu.cutOff)/2;
            }
           @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                    return disassembleOp(address, opcode, Size.Long, cpu);
            }
        };

        for(int ea_reg = 0; ea_reg < 8; ea_reg++)
        {
            cpu2.addInstruction(base + ea_reg, i);
        }                           
    }
    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz, smsqmulator.cpu.MC68000Cpu cpu)
    {
            DisassembledOperand op = cpu.disassembleSrcEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), sz);
            return new DisassembledInstruction(address, opcode, "jmp", op);
    }   
}
