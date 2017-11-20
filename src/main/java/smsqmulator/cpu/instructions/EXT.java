
package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
/**
 * The EXT instruction in all of its variants.
 * 
 * fedcba9876543210
 * 0100100ooo000rrr
 * where    oo = opmode = 010 for b. to  w  011 for w to l, 111 for b to l
 *          rrr = reg to be extended
 *        ooooo = opmode    01000 = exchange data regs
 *                          01001 = exchange addr regs
 *                          10001 = xchange addr and data regs, the data reg is the dest reg
 * 
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class EXT implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        smsqmulator.cpu.Instruction i = new smsqmulator.cpu.Instruction() 
        {
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int src=opcode&0x7;
                int temp= cpu.data_regs[src];
                cpu.reg_sr&=0xffff0;                           // x flag nt affected
                switch (opcode & 0x1c0)
                {
                    case 0x80:                                      // extend b to w
                        if ((temp&0x80)!=0)
                        {
                            temp|=0xff00;
                            cpu.reg_sr|=8;
                        }
                        else
                            temp&=0xffff00ff;
                        cpu.data_regs[src]=temp;
                        if ((temp&0xffff)==0)
                            cpu.reg_sr|=4;
                        return ;
                        
                    case 0xC0:                                      // extend w to l
                        if ((temp&0x8000)!=0)
                        {
                            temp|=0xffff0000;
                            cpu.reg_sr|=8;
                        }
                        else
                            temp&=0xffff;
                        cpu.data_regs[src]=temp;
                        if (temp==0)
                            cpu.reg_sr|=4;
                       return ;
                    /*                                              higer processors only
                    case 0x1c0:                                     // extend .b to .l
                        if ((temp&0x80)!=0)
                        {
                            temp|=0xffffff00;
                            cpu.reg_sr|=8;
                        }
                        else
                            temp&=0xff;
                        if (temp==0)
                            cpu.reg_sr|=4;
                        cpu.data_regs[src]=temp;
                        return ;
                        */
                }
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
		DisassembledOperand src = new DisassembledOperand("d" + (opcode & 0x07));
                switch(opcode & 0x1c0)
                {
                    case 0x80:                                      // extend b to w
                        return new DisassembledInstruction(address, opcode, "ext.b", src);
                    case 0xc0:                                      // extend b to w
                        return new DisassembledInstruction(address, opcode, "ext.w" , src);
                    default:
                    case 0x1c0:                                      // extend b to w
                        return new DisassembledInstruction(address, opcode, "ext.l", src);
                }
            }
            
        };
        int base=0x4880;
        for (int src=0;src<8;src++)
        {
            cpu2.addInstruction(base+src, i);
        }
        base=0x48c0;
        for (int src=0;src<8;src++)
        {
            cpu2.addInstruction(base+src, i);
        }
        
        /*
        
        //  .b to .l (higher processors only)
        base=0x49c0;
        for (int src=0;src<8;src++)
        {
            cpu2.addInstruction(base+src, i);
        }
        */
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
