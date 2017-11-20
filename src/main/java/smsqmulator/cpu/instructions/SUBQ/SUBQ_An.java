package smsqmulator.cpu.instructions.SUBQ;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;


/**
 * The SUB instruction where destination is An.
 * This affects the entire reg and no condition codes are changed.
 
 * 
 *  0101ddd1ssmmmrrr
 *  
 *  where   ddd= data (0=8)
 *          ss= size (00=byte, 01 = word, 10 = long )
 *          mmm = ea mode =001, 
 *          rrr = ea reg
 * 
 * @author and copyright for my code (c) Wolfgang Lenerz 2013 - 2014.
 * 
 * Based on code by Tony Headford, see his licence in accompanying file
 */
public class SUBQ_An implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;

        for(int sz = 1; sz < 3; sz++)
        {
           if(sz == 1)
            {
                base = 0x5148;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    { 
                        int s=((opcode >> 9) & 0x07);
                        if (s==0)
                            s=8;
                        cpu.addr_regs[opcode & 0x07]-=s;
                    }
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Word, cpu);
                    }
                };
            }
            else
            {
                base = 0x5188;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int s=((opcode >> 9) & 0x07);
                        if (s==0)
                            s=8;
                        cpu.addr_regs[opcode & 0x07]-=s;
                    }
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                            return disassembleOp(address, opcode, Size.Long, cpu);
                    }
                };
            }
                     
            for(int ea_reg = 0; ea_reg < 8; ea_reg++)
            {
                for(int r = 0; r < 8; r++)
                {
                    cpu2.addInstruction(base + (r << 9) + ea_reg, i);
                }
            }
        }
    }

    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz, smsqmulator.cpu.MC68000Cpu cpu)
    {
            DisassembledOperand src = new DisassembledOperand("#" + ((opcode >> 9) & 0x07));
            DisassembledOperand dst = cpu.disassembleDstEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), sz);

            return new DisassembledInstruction(address, opcode, "subq" + sz.ext(), src, dst);
    }
}
