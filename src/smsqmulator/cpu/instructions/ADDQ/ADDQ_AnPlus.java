package smsqmulator.cpu.instructions.ADDQ;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 *The ADDQ instruction where destination is (An)+.
 
 * @author and copyright (c) 2012 wolfgang lenerz
 * 
 *  0101ddd1ssmmmrrr
 *  
 *  where   ddd= data (0=8)
 *          ss= size (00=byte, 01 = word, 10 = long )
 *          mmm = ea mode =011, 
 *          rrr = ea reg
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 * v.1.02 word and byte: V and N flags set correctly.
 * v.1.01 streamlined
 */
public class ADDQ_AnPlus implements InstructionSet
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
                base = 0x5018;
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
                        int d=cpu.readMemoryByteSigned(cpu.addr_regs[reg]);// des value
                        boolean Dm = d<0;                   // dest is neg
                        d+=s;
                        cpu.writeMemoryByte(cpu.addr_regs[reg],d);
                        if (reg==7)
                        {
                            cpu.addr_regs[reg]+=2;
                        }  
                        else
                        {
                            cpu.addr_regs[reg]++;
                        }
                            
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
			      cpu.reg_sr+=2;           // set V flag
                        }
			if(!Rm && Dm) 
                        {
			  cpu.reg_sr+=0x11;   
                        }
                    }
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Byte, cpu);
                    }
                };
            }
            else if(sz == 1)
            {
                base = 0x5058;
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
                        int d=cpu.readMemoryWordSigned(cpu.addr_regs[reg]);// dest value
                        boolean Dm = d<0;                   // dst is neg
                        d+=s;
                        cpu.writeMemoryWord(cpu.addr_regs[reg],d);
                        cpu.addr_regs[reg]+=2;
                        cpu.reg_sr&=0xffe0;            // all flags 0
                        boolean Rm=((d&0x8000) !=0);          // result is neg
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
                        return disassembleOp(address, opcode, Size.Word, cpu);
                    }
                };
            }
            else
            {
                base = 0x5098;
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
                        int d=cpu.readMemoryLong(cpu.addr_regs[reg]);// des value
                        boolean Dm = d< 0;                  // dest is neg
                        d+=s;
                        cpu.writeMemoryLong(cpu.addr_regs[reg],d);
                        cpu.addr_regs[reg]+=4;
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

    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz,smsqmulator.cpu.MC68000Cpu cpu)
    {
        DisassembledOperand src = new DisassembledOperand("#" + ((opcode >> 9) & 0x07));
        DisassembledOperand dst = cpu.disassembleDstEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), sz);
        return new DisassembledInstruction(address, opcode, "addq" + sz.ext(), src, dst);
    }
}
