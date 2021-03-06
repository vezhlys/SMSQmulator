package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;
/**
 * The emulated DIVU (word sized) instruction in all of its variants.
 * 
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 * 
 * v. 1.01 quotient reduced to a word
 */
public class DIVUW implements InstructionSet
{
    
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base = 0x80c0;
        smsqmulator.cpu.Instruction i = new smsqmulator.cpu.Instruction() 
        {
            
            
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int destreg=(opcode>>>9)&7;                     // destination reg
                int value=cpu.data_regs[destreg];          // destination reg value
                int sourcereg=opcode&7;
                int secondvalue=0;
                switch ((opcode>>>3)&0x7)
                {
                    case 0:                                     // DN
                        secondvalue=cpu.data_regs[sourcereg]&0xffff;
                        break;
                    case 2:                                     // (an)
                        secondvalue=cpu.readMemoryWord(cpu.addr_regs[sourcereg]);
                        break;
                    case 3:                                     // (an)+
                        secondvalue=cpu.readMemoryWord(cpu.addr_regs[sourcereg]);
                        cpu.addr_regs[sourcereg]+=2;
                        break;
                    case 4:                                     // -(an)
                        cpu.addr_regs[sourcereg]-=2;
                        secondvalue=cpu.readMemoryWord(cpu.addr_regs[sourcereg]);
                        break;
                    case 5:                                     // d16(an)
                        int displacement =  cpu.readMemoryWordPCSignedInc();
                        secondvalue=cpu.readMemoryWord(cpu.addr_regs[sourcereg] + displacement);
                        break;
                    case 6:                                     //d8(an,xn)
                        int ext = cpu.readMemoryWordPCSignedInc();// extention word, contains size + displacement+reg
                        displacement = cpu.signExtendByte(ext);    //displacemnt
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
                        secondvalue=cpu.readMemoryWord(cpu.addr_regs[sourcereg] + displacement);
                        break;
                    case 7:
                        switch (sourcereg)
                        { 
                            case 0:                             //.W
                                int addr = cpu.readMemoryWordPCSignedInc();         // get mem sign extended
                                secondvalue=cpu.readMemoryWord(addr);
                                break;
                            case 1:                             //.L
                                addr = cpu.readMemoryLongPC();   // get mem sign extended
                                cpu.pc_reg+=2;
                                secondvalue=cpu.readMemoryWord(addr);
                                break;
                            case 2:                             // d16PC      
                                secondvalue=(cpu.readMemoryWord(cpu.pc_reg*2 + cpu.readMemoryWordPCSignedInc()));// 
                                break;
                            case 3:
                                secondvalue= cpu.pc_reg*2+ getDisplacement(cpu);
                                secondvalue=cpu.readMemoryWord(secondvalue);
                                break;
                            case 4:                             // data
                                secondvalue=cpu.readMemoryWordPCInc();
                                break;
                        }
                    break;
                }
                cpu.reg_sr&=0xfff0;                        // X bit not affected by mulu, c always cleared
                if (secondvalue==0)
                {
                    cpu.raiseException(5);                 // sr reg undefined if this occurs
                    return;
                }
                int quotient=value/secondvalue;
                if ((quotient & 0xffff0000)!=0)                 // overflow if quotient > signed 16 bit int
                {
                    cpu.reg_sr+=2;                         // set flag
                    return;                                   
                }
                quotient&=0xffff;
                value %= secondvalue;
                cpu.data_regs[destreg]=value<<16;
                cpu.data_regs[destreg]|=quotient;
                if (quotient==0)
                    cpu.reg_sr +=4;                        // set Z flag
                else if ((quotient & 0x8000) != 0)
                    cpu.reg_sr +=8;                        // set N flag
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
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
		DisassembledOperand src = cpu.disassembleSrcEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), Size.Word);
		DisassembledOperand dst = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
		return new DisassembledInstruction(address, opcode, "divu", src, dst);
            }            
        };
        
        for(int ea_mode = 0; ea_mode < 8; ea_mode++)
        {
            if (ea_mode == 1)
                continue;

            for( int ea_reg = 0; ea_reg < 8; ea_reg++)
            {
                if(ea_mode == 7 && ea_reg > 4)
                    break;

                for(int r = 0; r < 8; r++)
                {
                    cpu2.addInstruction(base + (r << 9) + (ea_mode << 3) + ea_reg, i);
                }
            }
        }
    }
}
