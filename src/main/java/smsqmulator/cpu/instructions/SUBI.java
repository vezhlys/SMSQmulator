package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The SUBI instruction in all of its variants.
 *  fedcba9876543210
 *  00000100ssmmmrrr = 400
 *  where   rrr = reg for ea
 *          ss  = size
 *          mmm = mode
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013-2014
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class SUBI implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;
       
        for(int sz = 0; sz < 3; sz++)                           // byte, word and long
        {
            if (sz == 0)                                        // byte sized subi
            {
                base = 0x400;
                i = new smsqmulator.cpu.Instruction()   
                {
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int val=cpu.readMemoryWordPCInc()&0xff;
                        int address=0;
                        int d;
                        boolean Sm, Rm,Dm;
                        cpu.reg_sr&=0xffe0;            // all flags 0
                        if ((val & 0x80)!=0)
                        {
                            val|=0xffffff00;    
                            Sm=true;
                        }               
                        else
                        {
                            Sm=false;
                        }
                        
                        switch ((opcode >>3)&7)
                        {
                            case 0:                             // Dn
                                int reg=opcode&7;
                                int regv=cpu.data_regs[reg];
                                d=regv&0xff;
                                if ((d & 0x80)!=0)
                                {
                                    d|=0xffffff00;     
                                    Dm=true;
                                }
                                else
                                {
                                    Dm=false;
                                }
                                d-=val;
                                if ((d & 0x80)!=0)
                                    Rm=true;
                                else
                                    Rm=false;

                                regv&=0xffffff00;
                                regv|=(d&0xff); 
                                cpu.data_regs[reg]=regv;
                                val=0;
                                if (d == 0)
                                    val|=4;             // set Z flag
                                else if (Rm)
                                    val|=8;             // set N flag
                                
                                if((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                                    val|=2;             // set V flag

                                if((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                                    val|=17;            // set X and C flags
                                cpu.reg_sr|=val;
                                return;                       

                            case 2:                                     //(an)
                                address=cpu.addr_regs[opcode&7];
                                break;
                            case 3:                                     // (an)+
                                reg=opcode&7;
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
                        d=cpu.readMemoryByteSigned(address); 
                        Dm =d<0;
                        d-=val;  
                        if ((d & 0x80)!=0)
                            Rm=true;
                        else
                            Rm=false;
                        val=0;
                        cpu.writeMemoryByte(address,d);
                        if (d == 0)
                            val+=4;             // set Z flag
                        else if (Rm)
                            val+=8;             // set N flag

                        if((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                            val+=2;             // set V flag

                        if((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                            val+=17;            // set X and C flags
                        cpu.reg_sr|=val;
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
                base = 0x440; 
                i = new smsqmulator.cpu.Instruction()   
                {
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int val=cpu.readMemoryWordPCSignedInc();
                        int address=0;
                        int d;
                        boolean Rm,Dm;
                        boolean Sm=val<0;
                        cpu.reg_sr&=0xffe0;            // all flags 0                        
                        switch ((opcode >>3)&7)
                        {
                            case 0:                             // Dn
                                int reg=opcode&7;
                                int regv=cpu.data_regs[reg];
                                d=regv&0xffff;
                                if ((d & 0x8000)!=0)
                                {
                                    d|=0xffff0000;     
                                    Dm=true;
                                }
                                else
                                {
                                    Dm=false;
                                }
                                d-=val;
                                if ((d & 0x8000)!=0)
                                    Rm=true;
                                else
                                    Rm=false;

                                regv&=0xffff0000;
                                regv|=(d&0xffff); 
                                cpu.data_regs[reg]=regv;
                                val=0;
                                if (d == 0)
                                    val+=4;             // set Z flag
                                else if (Rm)
                                    val+=8;             // set N flag
                                
                                if((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                                    val+=2;             // set V flag

                                if((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                                    val+=17;            // set X and C flags
                                cpu.reg_sr|=val;
                                return;                       

                            case 2:                                     //(an)
                                address=cpu.addr_regs[opcode&7];
                                break;
                            case 3:                                     // (an)+
                                reg=opcode&7;
                                address=cpu.addr_regs[reg];
                                cpu.addr_regs[reg]+=2;
                                break;
                            case 4:                                     // -(an)
                                reg=opcode&7;
                                cpu.addr_regs[reg]-=2;
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
                        d=cpu.readMemoryWordSigned(address); 
                        Dm=d<0;
                        d-=val;  
                        if ((d & 0x8000)!=0)
                            Rm=true;
                        else
                            Rm=false;
                        val=0;
                        cpu.writeMemoryWord(address,d);
                          if (d == 0)
                            val+=4;             // set Z flag
                        else if (Rm)
                            val+=8;             // set N flag

                        if((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                            val+=2;             // set V flag

                        if((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                            val+=17;            // set X and C flags
                        cpu.reg_sr|=val;
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
                base = 0x480;                                  // long sized subi
                i = new smsqmulator.cpu.Instruction()   
                {
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int val=cpu.readMemoryLongPC();
                        cpu.pc_reg+=2;
                        int address=0;
                        int d;
                        boolean Sm, Rm,Dm;
                        Sm=val<0;
                        cpu.reg_sr&=0xffe0;            // all flags 0
                        switch ((opcode >>3)&7)
                        {
                            case 0:                             // Dn
                                d=cpu.data_regs[opcode&7];
                                Dm=d<0;
                                d-=val;
                                Rm=d<0;
                                cpu.data_regs[opcode&7]=d; 
                                cpu.reg_sr&=0xffe0;            // all flags 0
                       
                                if (d == 0)
                                {
                                    cpu.reg_sr+=4;             // set Z flag
                                }
                                else if (Rm)
                                {
                                    cpu.reg_sr+=8;             // set N flag
                                }
                                if((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                                {
                                    cpu.reg_sr+=2;             // set V flag
                                }


                                if((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                                {
                                    cpu.reg_sr+=17;            // set X and C flags
                                }
                                return;                       
                            case 2:                                     //(an)
                                address=cpu.addr_regs[opcode&7];
                                break;
                            case 3:                                     // (an)+
                                int reg=opcode&7;
                                address=cpu.addr_regs[reg];
                                cpu.addr_regs[reg]+=4;
                                break;
                            case 4:                                     // -(an)
                                reg=opcode&7;
                                cpu.addr_regs[reg]-=4;
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
                        d=cpu.readMemoryLong(address); 
                        Dm=d<0;
                        d-=val;  
                        Rm=d<0;

                        cpu.writeMemoryLong(address,d);
                        cpu.reg_sr&=0xffe0;            // all flags 0
                         if (d == 0)
                        {
                            cpu.reg_sr+=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr+=8;             // set N flag
                        }
                        if((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                        {
                            cpu.reg_sr+=2;             // set V flag
                        }


                        if((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                        {
                            cpu.reg_sr+=17;            // set X and C flags
                        }
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
                throw new IllegalArgumentException("Size unsized for SUBI");
            }
        }

        DisassembledOperand src = new DisassembledOperand(is, imm_bytes, imm);
        DisassembledOperand dst = cpu.disassembleDstEA(address + 2 + imm_bytes, (opcode >> 3) & 0x07, (opcode & 0x07), sz);

        return new DisassembledInstruction(address, opcode, "subi" + sz.ext(), src, dst);
    }
}
