package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;
/**
 * The ORI instruction in all of its variants.
 *  fedcba9876543210
 *  00000000ssmmmrrr = 0
 *  where   rrr = reg for ea
 *          ss  = size
 *          mmm = mode
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013-2014.
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class ORI implements InstructionSet
{
    @Override
    public final void register( final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;
       
        for(int sz = 0; sz < 3; sz++)                            // byte, word and long only
        {
            if (sz == 0)                                        // byte sized neg
            {
                base = 0;
                i = new smsqmulator.cpu.Instruction()   
                {
                    @Override
                    public final void execute(int opcode,final smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int val=cpu.readMemoryWordPCInc()&0xff;
                        int address=0;
                        int temp;
                        
                        switch ((opcode >>3)&7)
                        {
                            case 0:                             // Dn
                                temp=cpu.data_regs[opcode&7]&0xff;
                                temp|=val;
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
                        temp|=val;
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
            else if (sz==1)                                     // word sized andi
            {
                base = 0x40; 
                i = new smsqmulator.cpu.Instruction()   
                {
                    @Override
                    public final void execute(int opcode,final smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int val=cpu.readMemoryWordPCInc();
                        int address=0;
                        int temp;
                        switch ((opcode >>3)&7)
                        {
                            case 0:                             // Dn
                                temp=cpu.data_regs[opcode&7]&0xffff;
                                temp|=val;
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
                        temp|=val;
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
                base = 0x80;                                  // long sieed neg
                i = new smsqmulator.cpu.Instruction()   
                {
                    @Override
                    public final void execute(int opcode,final smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int val=cpu.readMemoryLongPC();
                        cpu.pc_reg+=2;;
                        int address=0;
                        int temp;
                        switch ((opcode >>3)&7)
                        {
                            case 0:                             // Dn
                                temp=cpu.data_regs[opcode&7];
                                temp|=val;
                                cpu.data_regs[opcode&7]=temp;
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
                        temp|=val;
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
        int imm_bytes;
        int imm;
        String is;

        switch(sz)
        {
            case Byte:
            {
                imm = cpu.readMemoryWord(address + 2);
                is = String.format("#$%02x", imm & 0x00ff);
                imm_bytes = 2;
                break;
            }
            case Word:
            {
                imm = cpu.readMemoryWord(address + 2);
                is = String.format("#$%04x", imm);
                imm_bytes = 2;
                break;
            }
            case Long:
            {
                imm = cpu.readMemoryLong(address + 2);
                is = String.format("#$%08x", imm);
                imm_bytes = 4;
                break;
            }
            default:
            {
                throw new IllegalArgumentException("Size unsized for ORI");
            }
        }

        DisassembledOperand src = new DisassembledOperand(is, imm_bytes, imm);
        DisassembledOperand dst = cpu.disassembleDstEA(address + 2 + imm_bytes, (opcode >> 3) & 0x07, (opcode & 0x07), sz);

        return new DisassembledInstruction(address, opcode, "ori" + sz.ext(), src, dst);
    }
}
