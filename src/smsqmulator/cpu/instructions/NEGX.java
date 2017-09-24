package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;
/**
 * The NEGX instruction in all of its variants.
 * 
 *  4000
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * @version 1.01 correctly ha,dle Z flag
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class NEGX implements InstructionSet
{
    @Override
    public final void register( final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;
       
        for(int sz = 0; sz < 3; sz++)                            // byte, word and long only
        {
            if (sz == 0)                                        // byte sized negx
            {
                base = 0x4000;
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int val;
                        int temp;
                        int address=0;
                        switch ((opcode >>3)&7)
                        {
                            case 0:                             // Dn
                                val=cpu.data_regs[opcode&7]&0xff;
                                temp=val;
                                if ((cpu.reg_sr&16)!=0)
                                    val=(0-(val+1))&0xff;
                                else
                                    val=(0-val)&0xff;
                                cpu.data_regs[opcode&7]&=0xffffff00;
                                cpu.data_regs[opcode&7]|=val;
                                cpu.reg_sr&=0xffe4;        // clear all flags except 0;
                                if (val!=0)
                                {
                                    cpu.reg_sr&=0xffe0;    // value not 0 clear Z flag
                                }
                                if ((val&0x80)!=0)              // value is negative
                                {
                                    cpu.reg_sr|=8;         // set N flag
                                    if ((temp&0x80)!=0)
                                        cpu.reg_sr|=2;     // set V flag if overflow
                                }
                                if (((val&0x80)!=0) ||((temp&0x80)!=0) )
                                    cpu.reg_sr|=17;        // set C & X flags if borrow;
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
                        val=cpu.readMemoryByte(address);
                        temp=val; 
                        if ((cpu.reg_sr&16)!=0)
                            val=(0-(val+1))&0xff;
                        else
                            val=(0-val)&0xff;
                        cpu.writeMemoryByte(address,val);
                        cpu.reg_sr&=0xffe4;        // clear all flags except 0;
                        if (val!=0)
                        {
                            cpu.reg_sr&=0xffe0;    // value not 0 clear Z flag
                        }
                        if ((val&0x80)!=0)              // value is negative
                        {
                            cpu.reg_sr|=8;         // set N flag
                            if ((temp&0x80)!=0)
                                cpu.reg_sr|=2;     // set V flag if overflow 
                        }
                        if (((val&0x80)!=0) ||((temp&0x80)!=0) )
                            cpu.reg_sr|=17;        // set C & X flags if borrow;
                    }
                    
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Byte,cpu);
                    }   
                };
            }
            else if (sz==1)                                     // word sized negx
            {
                base = 0x4040; 
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int val;
                        int temp;
                        int address=0;
                        switch ((opcode >>3)&7)
                        {
                            case 0:                             // Dn
                                val=cpu.data_regs[opcode&7]&0xffff;
                                temp=val;
                                if ((cpu.reg_sr&16)!=0)
                                    val=(0-(val+1))&0xffff;
                                else
                                    val=(0-val)&0xffff;
                                cpu.data_regs[opcode&7]&=0xffff0000;
                                cpu.data_regs[opcode&7]|=val;
                                cpu.reg_sr&=0xffe4;        // clear all flags except 0;
                                if (val!=0)
                                {
                                    cpu.reg_sr&=0xffe0;    // value not 0 clear Z flag
                                }
                                if ((val&0x8000)!=0)              // value is negative
                                {
                                    cpu.reg_sr|=8;         // set N flag
                                    if ((temp&0x8000)!=0)
                                        cpu.reg_sr|=2;     // set V flag if overflow
                                }
                                if (((val&0x8000)!=0) ||((temp&0x8000)!=0) )
                                    cpu.reg_sr|=17;        // set C & X flags if borrow;
                                return;                       
                        
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
                        val=cpu.readMemoryWord(address);
                        temp=val;
                        if ((cpu.reg_sr&16)!=0)
                            val=(0-(val+1))&0xffff;
                        else
                            val=(0-val)&0xffff;
                        cpu.writeMemoryWord(address,val);
                        cpu.reg_sr&=0xffe4;        // clear all flags except 0;
                        if (val!=0)
                        {
                            cpu.reg_sr&=0xffe0;    // value not 0 clear Z flag
                        }
                        if ((val&0x8000)!=0)              // value is negative
                        {
                            cpu.reg_sr|=8;         // set N flag
                            if ((temp&0x8000)!=0)
                                cpu.reg_sr|=2;     // set V flag if overflow
                        }
                        if (((val&0x8000)!=0) ||((temp&0x8000)!=0) )
                            cpu.reg_sr|=17;        // set C & X flags if borrow;
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
                base = 0x4080;                                  // long sized negx
                i = new smsqmulator.cpu.Instruction()   
                {
                     
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int val;
                        int temp;
                        int address=0;
                        switch ((opcode >>3)&7)
                        {
                            case 0:                             // Dn
                                val=cpu.data_regs[opcode&7];
                                temp=val;
                                if ((cpu.reg_sr&16)!=0)
                                    val=(0-(val+1));
                                else
                                    val=(0-val);
                                cpu.data_regs[opcode&7]=val;
                                cpu.reg_sr&=0xffe4;        // clear all flags except 0;
                                if (val!=0)
                                {
                                    cpu.reg_sr&=0xffe0;    // value not 0 clear Z flag
                                }
                                if (val<0)              // value is negative
                                {
                                    cpu.reg_sr|=8;         // set N flag
                                    if (temp<0)
                                        cpu.reg_sr|=2;     // set V flag if overflow
                                }
                                if ((val<0) ||(temp<0) )
                                    cpu.reg_sr|=17;        // set C & X flags if borrow;
                                
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
                        val=cpu.readMemoryLong(address);
                        temp=val;
                        if ((cpu.reg_sr&16)!=0)
                            val=(0-(val+1));
                        else
                            val=(0-val);
                        cpu.writeMemoryLong(address,val);
                        
                                
                        ;cpu.reg_sr&=0xffe4;        // clear all flags except 0;
                        if (val!=0)
                        {
                            cpu.reg_sr&=0xffe0;    // value not 0 clear Z flag
                        }
                        if (val<0)              // value is negative
                        {
                            cpu.reg_sr|=8;         // set N flag
                            if (temp<0)
                                cpu.reg_sr|=2;     // set V flag if overflow
                        }
                        if ((val<0) ||(temp<0) )
                            cpu.reg_sr|=17;        // set C & X flags if borrow;
                    }
                    
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
                    cpu2.addInstruction(base+ rmode + reg, i);
                }
            }
        }
    }
            
   /**
     * gets the displacement and increments the PC.
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
    protected final DisassembledInstruction disassembleOp(int address,int opcode, Size sz,smsqmulator.cpu.MC68000Cpu cpu)
    {
        DisassembledOperand src = cpu.disassembleDstEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), sz);
        return new DisassembledInstruction(address, opcode, "negx" + sz.ext(), src);
    }
}
