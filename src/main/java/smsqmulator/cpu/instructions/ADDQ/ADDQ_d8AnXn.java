package smsqmulator.cpu.instructions.ADDQ;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * A redefinition of the ADDQ instruction where destination is d16(An).
 
 * @author and copyright (c) 2012-2014 wolfgang lenerz
 * 
 *  0101ddd1ssmmmrrr
 *  
 *  where   ddd= data (0=8)
 *          ss= size (00=byte, 01 = word, 10 = long )
 *          mmm = ea mode =110, 
 *          rrr = ea reg
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class ADDQ_d8AnXn implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;

        for(int sz = 0; sz < 3; sz++)
        {
            if(sz == 0)                                     // sub a byte
            {
                base = 0x5030;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int reg=opcode & 0x07;
                        int s = ((opcode >> 9) & 0x07);
                        if(s == 0)
                        {
                            s = 8;
                        }
                      
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
                        int d=cpu.readMemoryByteSigned(cpu.addr_regs[reg] + displacement);
                        boolean Dm = d<0;                   // dist is neg
                        d+=s;
                        cpu.writeMemoryByte(cpu.addr_regs[reg]+displacement,d);
                        cpu.reg_sr&=0xffe0;            // all flags 0
                        boolean Rm=((d&0x80) !=0);          // result is neg (1.02, was : Rm=d<0;)
                        //n z v c                           // flags, x will be set to c
                        //8 4 2 1
                        if(d == 0)
                        {
                            cpu.reg_sr+=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr+=8;             // set N flag
                        }
                        if (!Dm && Rm)
                        {
			      cpu.reg_sr+=2;              // set V flag
                        }
			if(!Rm && Dm) 
                        {
			  cpu.reg_sr+=0x11;   
                        }
                    }
                    
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Byte, cpu);
                    }
                };
            }
            else if(sz == 1)
            {
                base = 0x5070;
                i = new smsqmulator.cpu.Instruction()
                {
                   
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    { 
                        int reg=opcode & 0x07;
                        int s = ((opcode >> 9) & 0x07);
                        if(s == 0)
                        {
                            s = 8;
                        }
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
                        int d=cpu.readMemoryWordSigned(cpu.addr_regs[reg] + displacement);
                        boolean Dm = d<0;                   // dist is neg
                        d+=s;
                        cpu.writeMemoryWord(cpu.addr_regs[reg]+displacement,d);
                        cpu.reg_sr&=0xffe0;            // all flags 0
                        boolean Rm=((d&0x8000) !=0);        // result is neg (1.02, was : Rm=d<0;)
                        //n z v c                           // flags, x will be set to c
                        //8 4 2 1
                        if (d == 0)
                        {
                            cpu.reg_sr+=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr+=8;             // set N flag
                        }
                        if (!Dm && Rm)
                        {
			      cpu.reg_sr+=2;              // set V flag
                        }
			if(!Rm && Dm) 
                        {
			  cpu.reg_sr+=0x11;   
                        }
                    }
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Word, cpu);
                    }
                };
            }
            else
            {
                base = 0x50b0;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int reg=opcode & 0x07;
                        int s = ((opcode >> 9) & 0x07);
                        if(s == 0)
                        {
                            s = 8;
                        }
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
                        int d=cpu.readMemoryLong(cpu.addr_regs[reg] + displacement);
                        boolean Dm = d<0;                   // dist is neg
                        d+=s;
                        cpu.writeMemoryLong(cpu.addr_regs[reg]+displacement,d);
                        cpu.reg_sr&=0xffe0;            // all flags to 0
                        boolean Rm = d < 0;                 // result is neg
                        //n z v c                           // flags, x will be set to c
                        //8 4 2 1
                        if (d == 0)
                        {
                            cpu.reg_sr+=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr+=8;             // set N flag
                        }
                         if (!Dm && Rm)
                        {
			      cpu.reg_sr+=2;              // set V flag
                        }
			if(!Rm && Dm) 
                        {
			  cpu.reg_sr+=0x11;   
                        }
                    }
                    
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                            return disassembleOp(address, opcode, Size.Long, cpu);
                    }
                };
            }
                     
            for(int ea_reg = 0; ea_reg < 8; ea_reg++)
            {
                for(int r = 0; r < 8; r++)
                {
                    cpu2.addInstruction(base + (r << 9) + ea_reg, i);
                }
            }
        }
    }

    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz, smsqmulator.cpu.MC68000Cpu cpu)
    {
        DisassembledOperand src = new DisassembledOperand("#" + ((opcode >> 9) & 0x07));
        DisassembledOperand dst = cpu.disassembleDstEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), sz);
        return new DisassembledInstruction(address, opcode, "addq" + sz.ext(), src, dst);
    }
}
