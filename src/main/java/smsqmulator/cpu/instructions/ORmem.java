package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The OR instruction where the destination is a mem location, in all of its variants.
 *  fedcba9876543210
 *  1000dddooommmrrr = 8100
 * 
 *  
 *  where   rrr = ea reg
 *          mmm = ea mode
 *          ooo = opmode/size  = 1ss
 *          ddd = source data reg
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013-2014
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 * 
 */
public class ORmem implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;
       
        for(int sz = 0; sz < 3; sz++)                           // byte, word and long 
        {
            if (sz == 0)                                        // byte sized and
            {
                base = 0x8100;
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int address=0;
                        int val=cpu.data_regs[(opcode>>9)&0x7]&0xff;  // content of source reg byte
                        switch ((opcode >>3)&7)                 // select on dest ea mode
                        {
                            case 2:                                     //(an)
                                address=cpu.addr_regs[opcode&7];
                                break;
                            case 3:                                     // (an)+
                                int src=opcode&7;
                                address=cpu.addr_regs[src];
                                if (src==7)
                                    cpu.addr_regs[src]+=2;
                                else
                                    cpu.addr_regs[src]++;
                                break;
                            case 4:                                     // -(an)
                                src=opcode&7;
                                if (src==7)
                                    cpu.addr_regs[src]-=2;
                                else
                                    cpu.addr_regs[src]--;
                                address=cpu.addr_regs[src];
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
                                    case 0:                             // .W
                                        address=cpu.readMemoryWordPCSignedInc();
                                        break;
                                    case 1:                             // .L
                                        address=cpu.readMemoryLongPC();
                                        cpu.pc_reg+=2;
                                        break; 
                                 
                                }
                                break;
                        }
                        val|=cpu.readMemoryByte(address);
                        cpu.writeMemoryByte(address, val);
                        cpu.reg_sr&=0xfff0;        // clear all flags except X (unaffected)
                        if (val==0)
                            cpu.reg_sr|=4;
                        else if ((val&0x80)!=0)
                            cpu.reg_sr|=8;
                    }
                    
                    @Override
                   public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {   
                        DisassembledOperand dst = cpu.disassembleSrcEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), Size.Byte);
                        DisassembledOperand src = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
                        return new DisassembledInstruction(address, opcode, "or" + Size.Byte.ext(), src, dst);
                    }   
                };
            }
            else if (sz==1)                                     // word sized and
            {
                base = 0x8140; 
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int address=0;
                        int val=cpu.data_regs[(opcode>>9)&0x7]&0xffff;  // content of source reg word
                        switch ((opcode >>3)&7)                 // select on dest ea mode
                        {
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
                                    case 0:                             // .W
                                        address=cpu.readMemoryWordPCSignedInc();
                                        break;
                                    case 1:     
                                        address=cpu.readMemoryLongPC();
                                        cpu.pc_reg+=2;
                                        break; 
                                 
                                }
                                break;
                        }
                        val|=cpu.readMemoryWord(address);
                        cpu.writeMemoryWord(address, val);
                        cpu.reg_sr&=0xfff0;        // clear all flags except X (unaffected)
                        if (val==0)
                            cpu.reg_sr|=4;
                        else if ((val&0x8000)!=0)
                            cpu.reg_sr|=8;
                    }
                    
                    @Override
                   public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {   
                        DisassembledOperand dst = cpu.disassembleSrcEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), Size.Word);
                        DisassembledOperand src = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
                        return new DisassembledInstruction(address, opcode, "or" + Size.Word.ext(), src, dst);
                    }   
                };
            }
            else
            {
                base = 0x8180;                                  // long sized and
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int address=0;
                        int val=cpu.data_regs[(opcode>>9)&0x7];  // content of source reg
                        switch ((opcode >>3)&7)                 // select on dest ea mode
                        {
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
                                    case 0:                             // .W
                                        address=cpu.readMemoryWordPCSignedInc();
                                        break;
                                    case 1:     
                                        address=cpu.readMemoryLongPC();
                                        cpu.pc_reg+=2;
                                        break; 
                                 
                                }
                                break;
                        }
                        val|=cpu.readMemoryLong(address);
                        cpu.writeMemoryLong(address, val);
                        cpu.reg_sr&=0xfff0;        // clear all flags except X (unaffected)
                        if (val==0)
                            cpu.reg_sr|=4;
                        else if (val<0)
                            cpu.reg_sr|=8;
                    }
                    
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {   
                        DisassembledOperand dst = cpu.disassembleSrcEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), Size.Long);
                        DisassembledOperand src = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
                        return new DisassembledInstruction(address, opcode, "or" + Size.Long.ext(), src, dst);
                    }   
                };
            }
            for (int dreg=0;dreg<8;dreg++)
            {
                int dereg=dreg<<9;
                for (int mode=2;mode<8;mode++)
                {
                    int rmode=mode<<3;
                    for (int reg=0;reg<8;reg++)
                    {
                        if (mode==7 & reg>1)
                            break;
                        cpu2.addInstruction(base+ dereg+ rmode + reg, i);
                    }
                }
            }
        }
    }
            
    /**
     * gets the displacement and increments the PC.
     * 
     * @param cpu the smsqmulator.cpu.MC68000Cpu used  the cpu used.
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
}
