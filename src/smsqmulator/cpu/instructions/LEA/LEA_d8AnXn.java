package smsqmulator.cpu.instructions.LEA;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The LEA instruction where source is d8(An,Xn).
 * 
 * 0100dddd111mmmrrr
 * 
 * 
 * where dd = destination reg
 * mmm = source mode = 110
 * rrr = source reg
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 * 
 */
public class LEA_d8AnXn implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base = 0x41F0;
        
        smsqmulator.cpu.Instruction i= new smsqmulator.cpu.Instruction() 
        { 
            
                    
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int ext = cpu.readMemoryWordPCSignedInc();// extention word, contains size + displacement+reg
                int displacement=(ext & 0x80)!=0?ext| 0xffffff00:ext&0xff;    //displacemnt
                if((ext & 0x8000) !=0)
                {
                    if((ext & 0x0800) == 0)                // word or long register displacement?
                    {
                        displacement+= cpu.signExtendWord(cpu.addr_regs[(ext >> 12) & 0x07]);
                    }
                    else
                    {
                        displacement+= cpu.addr_regs[(ext >> 12) & 0x07];
                    }
                }
                else
                {
                    if((ext & 0x0800) == 0)                // word or long register displacement?
                    {
                        displacement+= cpu.signExtendWord(cpu.data_regs[(ext >> 12) & 0x07]);
                    }
                    else
                    {
                        displacement+= cpu.data_regs[(ext >> 12) & 0x07];
                    }
                }                
                cpu.addr_regs[(opcode >> 9) & 0x07]=displacement+cpu.addr_regs[opcode &7];
            }
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                DisassembledOperand src = cpu.disassembleSrcEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), Size.Long);
                DisassembledOperand dst = new DisassembledOperand("a" + ((opcode >> 9) & 0x07));
                return new DisassembledInstruction(address, opcode, "lea", src, dst);
            }
        };
        for(int ea_reg = 0; ea_reg < 8; ea_reg++)
        {
            for(int r = 0; r < 8; r++)
            {
                cpu2.addInstruction(base + (r << 9) + ea_reg, i);
            }
        }
    }
}
