package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The BCLR instruction where the bit number is contained in a register, in all of its variants.
 *  fedcba9876543210
 *  0000bbb110mmmrrr = 180
 *  where   bbb = reg with bit number
 *          rrr = reg for ea
 *          mmm = mode
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013-2014
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 * 
 */
public class BCLRreg implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base=0x180;
        smsqmulator.cpu.Instruction i = new smsqmulator.cpu.Instruction()   
        {
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int address=0;
                int bit=cpu.data_regs[(opcode>>9)&7];      // the bit number
                switch ((opcode >>3)&7)
                {
                    default:
                    case 0:                             // Dn
                        bit&=31;
                        bit=1<<bit;
                        int d=cpu.data_regs[opcode&7];
                        if ((d & bit)!=0)                       // the bit is set
                        {
                            cpu.reg_sr&=~4;                // clear Z flag since bit wasn't 0
                            d&=~bit;                            // clear bit
                        }
                        else
                            cpu.reg_sr|=4;
                        cpu.data_regs[opcode&7]=d;
                        return;        
                    case 2:                                     //(an)
                        address=cpu.addr_regs[opcode&7];
                        break;
                    case 3:                                     // (an)+
                        int reg=opcode&7;
                        address=cpu.addr_regs[reg];
                        if (reg==7)
                            cpu.addr_regs[reg]+=2;
                        else
                            cpu.addr_regs[reg]++;
                        break;
                    case 4:                                     // -(an)
                        reg=opcode&7;
                        if (reg==7)
                            cpu.addr_regs[reg]-=2;
                        else
                            cpu.addr_regs[reg]--;
                        address=cpu.addr_regs[reg];
                        break;                                  
                    case 5:                                     // d16(an)
                        address=cpu.addr_regs[opcode&7] + cpu.readMemoryWordPCSignedInc();
                        break;
                    case 6:                                     // d8(an,xn)
                        address=getDisplacement(cpu)+cpu.addr_regs[opcode&7];
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
                bit&=7;
                bit=1<<bit;
                int d=cpu.readMemoryByteSigned(address); 
                if ((d & bit)!=0)                               // the bit is set
                {
                    d&=~bit;                                    // unset bit   
                    cpu.reg_sr&=~4;                        // clear Z flag since bit wasn't 0
                }
                else
                    cpu.reg_sr|=4;
                cpu.writeMemoryByte(address,d);
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
		int bytes = 2;
                DisassembledOperand dst ;
                DisassembledOperand src = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
                if (((opcode>>3)&7)==0)
                    dst = cpu.disassembleDstEA(address + bytes, (opcode >> 3) & 0x07, (opcode & 0x07), Size.Long);
                else
                    dst = cpu.disassembleDstEA(address + bytes, (opcode >> 3) & 0x07, (opcode & 0x07), Size.Byte);
		return new DisassembledInstruction(address, opcode, "bclr", src, dst);
            }   
            
            private int getDisplacement(smsqmulator.cpu.MC68000Cpu cpu)
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
        for (int dreg=0;dreg<8;dreg++)
        {
            int doreg=dreg<<9;
            for (int mode=0;mode<8;mode++)
            {
                if (mode==1)
                    continue;
                int rmode=mode<<3;
                for (int reg=0;reg<8;reg++)
                {
                    if (mode==7 & reg>1)
                        break;
                    cpu2.addInstruction(base+doreg+rmode + reg, i);
                }
            }
        }
    }
}
