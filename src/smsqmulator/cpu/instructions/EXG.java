package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;

/**
 * The EXG instruction in all of its variants.
 * 
 *  1100ddd1ooooosss =0xc100
 * where    ddd = dest reg
 *          rrr = src reg
 *        ooooo = opmode    01000 = exchange data regs
 *                          01001 = exchange addr regs
 *                          10001 = xchange addr and data regs, the data reg is the dest reg
 * 
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class EXG implements InstructionSet
{
    @Override
    public final void register( final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        smsqmulator.cpu.Instruction i = new smsqmulator.cpu.Instruction() 
        {
            
                        
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int temp;
                int dest=(opcode>>9)&0x7;
                int src=opcode&0x7;
                switch (opcode & 0xf8)
                {
                    case 0x40:                                      // xchange data regs
                        temp = cpu.data_regs[src];
                        cpu.data_regs[src]=cpu.data_regs[dest];
                        cpu.data_regs[dest]=temp;
                        return ;
                    case 0x48:                                      // xchange addr regs
                        temp = cpu.addr_regs[src];
                        cpu.addr_regs[src]=cpu.addr_regs[dest];
                        cpu.addr_regs[dest]=temp;
                        return;
                    case 0x88:                                      // xchange addr & data regs
                        temp = cpu.addr_regs[src];
                        cpu.addr_regs[src]=cpu.data_regs[dest];
                        cpu.data_regs[dest]=temp;
                        return ;
                }
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                return disassembleOp(address,opcode);
            }
            
        };
        int base=0xc140;
        for (int dest=0;dest<8;dest++)
        {
            int temp=(dest<<9);
            for (int src=0;src<8;src++)
            {
                cpu2.addInstruction(base+temp+src, i);
            }
        }
        base=0xc148;
        for (int dest=0;dest<8;dest++)
        {
            int temp=(dest<<9);
            for (int src=0;src<8;src++)
            {
                cpu2.addInstruction(base+temp+src, i);
            }
        }
        base=0xc188;
        for (int dest=0;dest<8;dest++)
        
        {
            int temp=(dest<<9);
            for (int src=0;src<8;src++)
            {
                cpu2.addInstruction(base+temp+src, i);
            }
        }
    }
    protected final DisassembledInstruction disassembleOp(int address,int opcode)
    {
        DisassembledOperand src;
        DisassembledOperand dst;
        switch(opcode & 0xf8)
        {
            case 0x40:
            {
                src = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
                dst = new DisassembledOperand("d" + (opcode & 0x07));
                break;
            }
            case 0x48:
            {
                src = new DisassembledOperand("a" + ((opcode >> 9) & 0x07));
                dst = new DisassembledOperand("a" + (opcode & 0x07));
                break;
            }
            case 0x88:
            {
                src = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
                dst = new DisassembledOperand("a" + (opcode & 0x07));
                break;
            }
            default:
            {
                throw new IllegalArgumentException("Invalid exg type specified");
            }
        }

        return new DisassembledInstruction(address, opcode, "exg", src, dst);
    }
}
