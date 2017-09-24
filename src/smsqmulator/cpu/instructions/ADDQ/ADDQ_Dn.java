package smsqmulator.cpu.instructions.ADDQ;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The ADDQ instruction where destination is Dn.
 
 * @author and copyright (c) 2012 2014 wolfgang lenerz
 * 
 * 
 *  0101ddd1ssmmmrrr
 *  
 *  where   ddd= data (0=8)
 *          ss= size (00=byte, 01 = word, 10 = long )
 *          mmm = ea mode =0, 
 *          rrr = ea reg
 * 
 * @version 1.01 addq.w and .b handle roll over to neg correctly.
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class ADDQ_Dn implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;

        for(int sz = 0; sz < 3; sz++)
        {
            if(sz == 0)                                     // add a byte
            {
                base = 0x5000;
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
                        int d=cpu.data_regs[reg]&0xff;// des value
                        boolean Dm;
                        if ((d&0x80)!=0)
                        {
                            Dm=true;
                            d|=0xffffff00;
                        }
                        else
                        {
                            Dm=false;
                        }
                        d+=s;
                        cpu.data_regs[reg]&=0xffffff00;
                        cpu.data_regs[reg]|=(d&0xff);  // set value in reg
                        
                        cpu.reg_sr&=0xffe0;            // all flags 0
                        boolean Rm=((d&0x80) !=0);          // result is neg
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
			  cpu.reg_sr+=0x11;            // set X & C flags
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
                base = 0x5040;
                i = new smsqmulator.cpu.Instruction()
                {
                   
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {   
                        int reg=opcode & 0x07;
                        int s = ((opcode >> 9) & 0x07);
                        if(s == 0)
                            s = 8;
                        int d=cpu.data_regs[reg]&0xffff;// des value
                        boolean Dm;
                        if ((d&0x8000)!=0)
                        {
                            Dm=true;
                            d|=0xffff0000; 
                        }
                        else
                            Dm=false;
                        d+=s;
                        cpu.data_regs[reg]&=0xffff0000;
                        cpu.data_regs[reg]|=(d&0xffff);    // set value in reg
                        cpu.reg_sr&=0xffe0;
                        boolean Rm=((d&0x8000) !=0);            // result is neg
                        //n z v c                               // flags, x will be set to c
                        //8 4 2 1
                        if (d == 0)                             // set Z flag
                            cpu.reg_sr+=4;
                        else if (Rm)                            // set N flag
                            cpu.reg_sr+=8;
                        if (!Dm && Rm)                          // set V flag
                            cpu.reg_sr+=2;
                        else if(!Rm && Dm)
                            cpu.reg_sr+=17;
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
                base = 0x5080;
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
                        boolean Dm =cpu.data_regs[reg]<0;
                        cpu.data_regs[reg] +=s;      
                        cpu.reg_sr&=0xffe0;            // all flags to 0
                        boolean Rm = cpu.data_regs[reg] < 0;       // result is neg
                        //n z v c                           // flags, x will be set to c
                        //8 4 2 1
                        if (cpu.data_regs[reg] == 0)
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
                        else if(!Rm && Dm) 
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
