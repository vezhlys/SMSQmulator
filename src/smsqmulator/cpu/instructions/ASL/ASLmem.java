package smsqmulator.cpu.instructions.ASL;

/**
 * The ASL instruction where data to be shifted is in memory.
 * 1110000111mmmrrr = 0xe1C0   
 * 
 * mmm = mode 010
 * rrr = data register to be shifted
 * ccc = dataregister containing the shift count (modulo 64)
 * ss =size :   00 = byte
 *              01 = word
 *              10 = long
 * 
 * 
 * 
 * @author and copyright for my code (c) Wolfgang Lenerz 2014
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */

import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

public class ASLmem implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        smsqmulator.cpu.Instruction i;
        int base = 0xe1c0;
        i = new smsqmulator.cpu.Instruction()   
        {
            
                    
                @Override
                public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                { 
                int address=0;
                int reg=opcode&7;
                switch((opcode>>3)&0x7)
                {
                    case 2:                                     //(an)
                        address=cpu.addr_regs[reg];
                        break;
                    case 3:                                     // (an)+
                        address=cpu.addr_regs[reg];
                        cpu.addr_regs[reg]+=2;
                        break;
                    case 4:                                     // -(an)
                        cpu.addr_regs[reg]-=2;
                        address=cpu.addr_regs[reg];
                        break;                                  
                    case 5:                                     // d16(an)
                        address=cpu.addr_regs[reg] + cpu.readMemoryWordPCSignedInc();
                        break;
                    case 6:                                     // d8(an,xn)
                        address=getDisplacement(cpu)+cpu.addr_regs[reg];
                        break;
                    case 7:
                        switch (opcode &7)
                        {
                            case 0:
                                address=cpu.readMemoryWordPCSignedInc();
                                break;
                            case 1:
                                address=cpu.readMemoryLongPC();
                                cpu.pc_reg+=2;
                                break;
                        }
                        break;
                }
                int val=cpu.readMemoryWord(address);
                int lastOut=val&0x8000;                         // highest bit before shift
                val<<=1;                                        // shift one bit up
                cpu.writeMemoryWord(address,val);          // write back
                cpu.reg_sr&=0xffe0;                        // all status flags set to 0,
                if ((val&0x8000)!=0)                            // ms bit set
                    cpu.reg_sr+=8;                         // so set N flag if (val==0)    
                else if (val==0)
                    cpu.reg_sr+=4;                         // set Z flag 
                if (lastOut!=(val&0x8000))
                    cpu.reg_sr+=2;                         // set V flag if ms bit changed at any time during the oeration
                if (lastOut!=0)
                    cpu.reg_sr|=17;                        // set X + C flags   
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                 DisassembledOperand src = cpu.disassembleDstEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), Size.Word);
                 return new DisassembledInstruction(address, opcode, "asl" + Size.Word.ext(), src);
            }   
            
            protected int getDisplacement(smsqmulator.cpu.MC68000Cpu cpu)
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
                return displacement;
            }
            
        };
        int mmode;    
        for (int mode=2;mode<7;mode++)
        {
            mmode=mode<<3;
            for (int reg=0;reg<8;reg++)
            {
                cpu2.addInstruction(base+ mmode + reg, i);
            }
        }
        mmode=7<<3;
        for (int reg=0;reg<2;reg++)
        {
            cpu2.addInstruction(base+ mmode + reg, i);
        }
    }
}
