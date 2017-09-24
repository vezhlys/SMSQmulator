
package smsqmulator.cpu.instructions.MOVE;

/**
 * MOVE.L A0,(A2)
 * If A2 = $10, do NOT write this.
 * 00ssxxxyyymmmrrr
 * s = size bit
 * x=dest register (An)
 * y = destination mode =010
 * m =source mode 001
 * r= source register
 * so 00ssxxx010mmm000 = base , s can 01 byte, 11 word,10 long
 * so base = 0x1080 for byte,0x2080 for long,0x3080 for word, 
 * @author and copyright for my code (c) Wolfgang Lenerz 2013 - 2014.
 * 
 * Based on code by Tony Headford, see his licence in accompanying file
 */
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

public class MOVEAn2bis implements InstructionSet
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
                int s=cpu.addr_regs[0];
                if (cpu.addr_regs[2]!=0x10)
                    cpu.writeMemoryLong(cpu.addr_regs[2], s);
                cpu.reg_sr&=0xfff0;
                if (s==0)
                {
                    cpu.reg_sr+=4;
                }
                else if (s<0)
                {
                    cpu.reg_sr+=8;
                }
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                return disassembleOp(address, opcode, Size.Long, cpu);
            }
        };
        cpu2.addInstruction(0x2488, is);
    }

    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz, smsqmulator.cpu.MC68000Cpu cpu)
    {
        DisassembledOperand src = cpu.disassembleSrcEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), sz);
        DisassembledOperand dst = cpu.disassembleDstEA(address + 2 + src.bytes, (opcode >> 6) & 0x07, (opcode >> 9) & 0x07, sz);
        return new DisassembledInstruction(address, opcode, "move" + sz.ext(), src, dst);
    }
}
