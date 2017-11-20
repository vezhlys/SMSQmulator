package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;


/**
 * The MOVEPr2m instruction where  move is made from reg to mem.
 *  fedcba9876543210
 *  0000rrr11s001aaa = 188
 *  where   rrr = data reg to be swapped
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013-2014
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class MOVEPr2m implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;
       
        for(int sz = 0; sz < 2; sz++)                           // word and long only
        {
            if (sz == 0)                                        // word sized
            {
                base = 0x188;
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int val=cpu.data_regs[(opcode>>9)&7];
                        int address=cpu.addr_regs[opcode&7]+cpu.readMemoryWordPCSignedInc();
                        cpu.writeMemoryByte(address, (val >>> 8) & 0xff);
                        cpu.writeMemoryByte(address + 2, val & 0xff);
                    }
                    
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Word,cpu);
                    }   
                };
            }
            else
            {
                base = 0x01c8;                                  // long sieed neg
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int val=cpu.data_regs[(opcode>>9)&7];
                        int address=cpu.addr_regs[opcode&7]+cpu.readMemoryWordPCSignedInc();
                        cpu.writeMemoryByte(address, (val >>> 24) & 0xff);
                        cpu.writeMemoryByte(address + 2, (val >>> 16) & 0xff);
                        cpu.writeMemoryByte(address + 4, (val >>> 8) & 0xff);
                        cpu.writeMemoryByte(address + 6, val & 0xff);
                    }
                    
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Long,cpu);
                    }   
                };
            }
            for (int dreg=0;dreg<8;dreg++)
            {
                int rdreg=dreg<<9;
                for (int reg=0;reg<8;reg++)
                {
                    cpu2.addInstruction(base+ rdreg + reg, i);
                }
            }
        }
    }
            
    protected final DisassembledInstruction disassembleOp(int address,int opcode, Size sz,smsqmulator.cpu.MC68000Cpu cpu)
    {
        int dis = cpu.readMemoryWordSigned(address + 2);
        DisassembledOperand src = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
	DisassembledOperand dst = new DisassembledOperand(String.format("#$%04x(a%d)", dis, (opcode & 0x07)), 2, dis);
        return new DisassembledInstruction(address, opcode, "movep" + sz.ext(), src,dst);
    }
}
