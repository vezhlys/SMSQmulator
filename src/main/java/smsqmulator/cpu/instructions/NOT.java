package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;
/**
 * The NOT instruction in all of its variants.
 *  fedcba9876543210
 *  01000110ssmmmrrr = 4600
 *  where   rrr = reg for ea
 *          ss  = size
 *          mmm = mode
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class NOT implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;
       
        for(int sz = 0; sz < 3; sz++)                            // byte, word and long only
        {
            if (sz == 0)                                        // byte sized neg
            {
                base = 0x4600;
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int address=0;
                        int temp;
                        switch ((opcode >>3)&7)
                        {
                            case 0:                             // Dn
                                temp=cpu.data_regs[opcode&7]&0xff;
                                temp =~temp;
                                temp&=0xff;
                                cpu.data_regs[opcode&7]&=0xffffff00;
                                cpu.data_regs[opcode&7]|=temp;
                                cpu.reg_sr&=0xfff0;        // clear all flags ecept X (unaffacted)
                                if (temp==0)
                                    cpu.reg_sr|=4;
                                else if ((temp&0x80)!=0)
                                    cpu.reg_sr|=8;
                              
                                return;                       // DO NOT USE BREAK HERE

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
                        temp=cpu.readMemoryByte(address);
                        temp =(~temp);
                        temp&=0xff;
                        cpu.writeMemoryByte(address,temp);
                        cpu.reg_sr&=0xfff0;        // clear all flags ecept X (unaffacted)
                        if (temp==0)
                            cpu.reg_sr|=4;
                        else if ((temp&0x80)!=0)
                            cpu.reg_sr|=8;
                    }
                    
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Byte,cpu);
                    }   
                };
            }
            else if (sz==1)                                     // word sized not
            {
                base = 0x4640; 
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int address=0;
                        int temp;
                        switch ((opcode >>3)&7)
                        {
                            case 0:                             // Dn
                                temp=cpu.data_regs[opcode&7]&0xffff;
                                temp =(~temp);
                                temp&=0xffff;
                                cpu.data_regs[opcode&7]&=0xffff0000;
                                cpu.data_regs[opcode&7]|=temp;
                                cpu.reg_sr&=0xfff0;        // clear all flags ecept X (unaffacted)
                                if (temp==0)
                                    cpu.reg_sr|=4;
                                else if ((temp&0x8000)!=0)
                                    cpu.reg_sr|=8;                         
                                 return;                       // DO NOT USE BREAK HERE

                            case 2:                                     //(an)
                                address=cpu.addr_regs[opcode&7];
                                break;
                            case 3:                                     // (an)+
                                address=cpu.addr_regs[opcode&7];
                                cpu.addr_regs[opcode&7]+=2;
                                break;
                            case 4:                                     // -(an)
                                cpu.addr_regs[opcode&7]-=2;
                                address=cpu.addr_regs[opcode&7];
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
                        temp=cpu.readMemoryWord(address);
                        temp =~temp;
                        temp&=0xffff;
                        cpu.writeMemoryWord(address,temp);
                        cpu.reg_sr&=0xfff0;        // clear all flags ecept X (unaffacted)
                        if (temp==0)
                            cpu.reg_sr|=4;
                        else if ((temp&0x8000)!=0)
                            cpu.reg_sr|=8;
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
                base = 0x4680;                                  // long sized not
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int address=0;
                        int temp;
                        switch ((opcode >>3)&7)
                        {
                            case 0:                             // Dn
                                temp=cpu.data_regs[opcode&7];
                                temp=~temp;
                                cpu.data_regs[opcode&7] =temp;
                                cpu.reg_sr&=0xfff0;        // clear all flags ecept X (unaffacted)
                                if (temp==0)
                                    cpu.reg_sr|=4;
                                else if (temp<0)
                                    cpu.reg_sr|=8;
                                return;                       // DO NOT USE BREAK HERE

                            case 2:                                     //(an)
                                address=cpu.addr_regs[opcode&7];
                                break;
                            case 3:                                     // (an)+
                                address=cpu.addr_regs[opcode&7];
                                cpu.addr_regs[opcode&7]+=4;
                                break;
                            case 4:                                     // -(an)
                                cpu.addr_regs[opcode&7]-=4;
                                address=cpu.addr_regs[opcode&7];
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
                        temp=cpu.readMemoryLong(address);
                        temp=~temp;
                        cpu.writeMemoryLong(address,temp);
                        cpu.reg_sr&=0xfff0;        // clear all flags ecept X (unaffacted)
                            if (temp==0)
                                cpu.reg_sr|=4;
                            else if (temp<0)
                                cpu.reg_sr|=8;
                    }
                    
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Long,cpu);
                    }   
                };
            }
            for (int mode=0;mode<8;mode++)
            {
                if (mode==1)
                    continue;
                int rmode=mode<<3;
                for (int reg=0;reg<8;reg++)
                {
                    if (mode==7 & reg>1)
                        break;
                    cpu2.addInstruction(base+rmode + reg, i);
                }
            }
        }
    }
    
    /**
     * gets the displacement and increments the PC.
     * 
     * @param cpu the smsqmulator.cpu.MC68000Cpu used the cpu used.
     * 
     * @return  the displacement
     */
    protected int getDisplacement(smsqmulator.cpu.MC68000Cpu cpu)
    {
        int ext = cpu.readMemoryWordPCSignedInc();      // extention word, contains size + displacement+reg
        int displacement=(ext & 0x80)!=0?ext| 0xffffff00:ext&0xff;     //displacemnt
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
    protected final DisassembledInstruction disassembleOp(int address,int opcode, Size sz,smsqmulator.cpu.MC68000Cpu cpu)
    {
        DisassembledOperand src = cpu.disassembleDstEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), sz);
        return new DisassembledInstruction(address, opcode, "not" + sz.ext(), src);
    }
}
