
package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The AND instruction where the destination is a register, in all of its variants.
 *  fedcba9876543210
 *  1100dddooommmrrr = c000
 *  
 *  where   rrr = ea reg
 *          mmm = ea mode
 *          ooo = opmode/size  = 0ss
 *          ddd = destination data reg
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class ANDreg implements InstructionSet
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
                base = 0xc000;
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int val=0;
                        int address=0;
                        int reg=(opcode>>9)&0x7;                // destination data register
                        boolean readByte=true;
                        int temp=cpu.data_regs[reg]&0xff;  // content of dest reg byte
                        switch ((opcode >>3)&7)                 // select on source ea mode
                        {
                            case 0:                             // Dn
                                val=cpu.data_regs[opcode&7]&0xff;
                                readByte=false;
                                break;
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
                                switch (opcode&7)
                                {
                                    case 0:                             // .W
                                        address=cpu.readMemoryWordPCSignedInc();
                                        break;
                                    case 1:                             // .L
                                        address=cpu.readMemoryLongPC();
                                        cpu.pc_reg+=2;
                                        break; 
                                    case 2:                             // (d16,PC)
                                        address=cpu.pc_reg*2+ cpu.readMemoryWordPCSignedInc();
                                        break;
                                    case 3:                             // (d8,PC,Xn)
                                        address= cpu.pc_reg*2+getDisplacement(cpu);
                                        break;
                                    case 4:                             // immediate data
                                        val=cpu.readMemoryWordPCInc()&0xff;//source 
                                        readByte=false;
                                        break;
                                }
                                break;
                        }
                        
                        if (readByte)
                            val=cpu.readMemoryByte(address);
                        val&=temp; 
                        cpu.data_regs[reg]&=0xffffff00;
                        cpu.data_regs[reg]|=val;
                        cpu.reg_sr&=0xfff0;        // clear all flags except X (unaffected)
                        if (val==0)
                            cpu.reg_sr|=4;
                        else if ((val&0x80)!=0)
                            cpu.reg_sr|=8;
                    }
                    
                    @Override
                   public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {   
                        DisassembledOperand  src = cpu.disassembleSrcEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), Size.Byte);
                        DisassembledOperand dst = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
                        return new DisassembledInstruction(address, opcode, "and" + Size.Byte.ext(), src, dst);
                    }   
                };
            }
            
            else if (sz==1)                                     // word sized neg
            {
                base = 0xc040; 
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int val=0;
                        int address=0;
                        int reg=(opcode>>9)&0x7;                // destination data register
                        boolean readByte=true;
                        int temp=cpu.data_regs[reg]&0xffff;// content of dest reg word
                        switch ((opcode >>3)&7)                 // select on source ea mode
                        {
                            case 0:                             // Dn
                                val=cpu.data_regs[opcode&7]&0xffff;
                                readByte=false;
                                break;
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
                                    case 1:                             // .L
                                        address=cpu.readMemoryLongPC();
                                        cpu.pc_reg+=2;
                                        break; 
                                    case 2:                             // (d16,PC)
                                        address=cpu.pc_reg*2 + cpu.readMemoryWordPCSignedInc();
                                        break;
                                    case 3:                             // (d8,PC,Xn)
                                        address=cpu.pc_reg*2+ getDisplacement(cpu);
                                        break;
                                    case 4:                             // immediate data
                                        val=cpu.readMemoryWordPCInc();//source value
                                        readByte=false;
                                        break;
                                }
                                break;
                        }
                        if (readByte)
                            val=cpu.readMemoryWord(address);
                        val&=temp; 
                        cpu.data_regs[reg]&=0xffff0000;
                        cpu.data_regs[reg]|=val;
                        cpu.reg_sr&=0xfff0;        // clear all flags except X (unaffected)
                        if (val==0)
                            cpu.reg_sr|=4;
                        else if ((val&0x8000)!=0)
                            cpu.reg_sr|=8;
                    }
                    
                    @Override
                   public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {   
                        DisassembledOperand  src = cpu.disassembleSrcEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), Size.Word);
                        DisassembledOperand dst = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
                        return new DisassembledInstruction(address, opcode, "and" + Size.Word.ext(), src, dst);
                    }   
                };
            }
            else
            {
                base = 0xc080;                                  // long sized and
                i = new smsqmulator.cpu.Instruction()   
                {
                                            
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int val=0;
                        int address=0;
                        int reg=(opcode>>9)&0x7;                // destination data register
                        boolean readByte=true;
                        int temp=cpu.data_regs[reg];       // content of dest reg
                        switch ((opcode >>3)&7)                 // select on source ea mode
                        {
                            case 0:                             // Dn
                                val=cpu.data_regs[opcode&7];
                                readByte=false;
                                break;
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
                                    case 1:                             // .L
                                        address=cpu.readMemoryLongPC();
                                        cpu.pc_reg+=2;
                                        break; 
                                    case 2:                             // (d16,PC)
                                        address=cpu.pc_reg*2+ cpu.readMemoryWordPCSignedInc();
                                        break;
                                    case 3:                             // (d8,PC,Xn)
                                        address= cpu.pc_reg*2+getDisplacement(cpu);
                                        break;
                                    case 4:                             // immediate data
                                        val=cpu.readMemoryLongPC();//source value
                                        cpu.pc_reg+=2;
                                        readByte=false;
                                        break;
                                }
                                break;
                        }
                        if (readByte)
                            val=cpu.readMemoryLong(address);
                        val&=temp; 
                        cpu.data_regs[reg]=val;
                        cpu.reg_sr&=0xfff0;        // clear all flags except X (unaffected)
                        if (val==0)
                            cpu.reg_sr|=4;
                        else if (val<0)
                            cpu.reg_sr|=8;
                    }
                    
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {   
                        DisassembledOperand  src = cpu.disassembleSrcEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), Size.Long);
                        DisassembledOperand dst = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
                        return new DisassembledInstruction(address, opcode, "and" + Size.Long.ext(), src, dst);
                    }   
                };
            }
            for (int dreg=0;dreg<8;dreg++)
            {
                int dereg=dreg<<9;
                for (int mode=0;mode<8;mode++)
                {
                    if (mode==1)
                        continue;
                    int rmode=mode<<3;
                    for (int reg=0;reg<8;reg++)
                    {
                        if (mode==7 & reg>4)
                            break;
                        cpu2.addInstruction(base+ dereg+ rmode + reg, i);
                    }
                }
            }
        }
    }
    
    /**
     * Gets the displacement and increments the PC.
     * 
     * @param cpu the smsqmulator.cpu.MC68000Cpu used the cpu used.
     * 
     * @return the displacement
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
