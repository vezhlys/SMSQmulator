
package smsqmulator.cpu.instructions.BTST;

/**
 * The BTST instruction where data to be tested is in d16(An) .
 * 
 * 00000ddd100mmmrrr = 0x0128   for dynamic mode
 * 00001000000mmmrrr = 0x0828   for static mode
 * 
 * mmm = mode 101
 * rrr = register
 * 
 * @author and copyright for my code (c) Wolfgang Lenerz 2013
 * Based on code by Tony Headford, see his licence in accompanying file.
 */

import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

public class BTST_d16An implements InstructionSet
{
   
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base=0x128;
        smsqmulator.cpu.Instruction i;      
        i= new smsqmulator.cpu.Instruction() 
        { 
            
                    
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int bit;
                if ((opcode & 0x100)!=0  )
                    bit= cpu.data_regs[(opcode >> 9) & 0x07] &7;// modulo 7, only byte sized op allowed
                else
                {
                    bit=cpu.readMemoryWordPCInc()&7;
                }
                bit =1 << bit;                      // this is the bit to test
                //n z v c
                //8 4 2 1
                int dest=cpu.readMemoryByte(cpu.addr_regs[opcode&7] + cpu.readMemoryWordPCSignedInc());
                if ((dest & bit)==0)// bit was clear
                {
                    cpu.reg_sr |= 4;                   // set zero flag
                }
                else
                {
                    cpu.reg_sr&=~4;
                }
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                    return disassembleOp(address, opcode, Size.Byte, cpu);
            }
        };
        // dynamic modes    
        for (int ea_reg = 0; ea_reg < 8; ea_reg++)
        {
            for (int r=0;r<8;r++)
            {
                cpu2.addInstruction(base + (r<<9) +ea_reg, i);
            }
        }
        // static modes
        base=0x828;
        for (int ea_reg = 0; ea_reg < 8; ea_reg++)
        {
            cpu2.addInstruction(base + ea_reg, i);
        }
        
    }
    
    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz, smsqmulator.cpu.MC68000Cpu cpu)
    {
            DisassembledOperand src;
            int bytes = 2;

            if((opcode & 0x0100) != 0)
            {
                    //dynamic mode
                    src = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
            }
            else
            {
                    //static mode
                    int ext = cpu.readMemoryWord(address + 2);
                    int val;
                    if(((opcode >> 3) & 0x07) == 0)
                    {
                            val = ext & 0x1f;
                    }
                    else
                    {
                            val = ext & 0x07;
                    }
                    src = new DisassembledOperand(String.format("#$%x", val), 2, ext);
                    bytes += 2;
            }

            DisassembledOperand dst = cpu.disassembleDstEA(address + bytes, (opcode >> 3) & 0x07, (opcode & 0x07), sz);

            return new DisassembledInstruction(address, opcode, "btst", src, dst);
    }
}
