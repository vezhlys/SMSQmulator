package smsqmulator.cpu.instructions.SUBQ;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The SUBQ instruction where destination is .W absolute.
 
 * @author and copyright (c) 2012 wolfgang lenerz
 * 
 *  0101ddd1ssmmmrrr
 *  
 *  where   ddd= data (0=8)
 *          ss= size (00=byte, 01 = word, 10 = long )
 *          mmm = ea mode =010, 
 *          rrr = ea reg
 * 
 * @author and copyright for my code (c) Wolfgang Lenerz 2013 - 2014.
 * 
 * Based on code by Tony Headford, see his licence in accompanying file
 */
public class SUBQ_L implements InstructionSet
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
                base = 0x5139;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int s = ((opcode >> 9) & 0x07);
                        if(s == 0)
                        {
                            s = 8;
                        }
                        int addr = cpu.readMemoryLongPC();         // get mem sign extended
                        cpu.pc_reg+=2;
                        int d=cpu.readMemoryByteSigned(addr);
                        int r = d-s;
                        cpu.writeMemoryByte(addr,r);
                        cpu.reg_sr&=0xffe0;            // all flags 0
                        boolean Sm = false;                 // source is never neg
                        boolean Dm = (d & 0x80) != 0;       // dist is neg
                        boolean Rm = (r & 0x80) != 0;       // result is neg
                        //n z v c                           // flags, x will be set to c
                        //8 4 2 1
                        if(r == 0)
                        {
                            cpu.reg_sr+=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr+=8;             // set N flag
                        }
                        if (!Sm && Dm && !Rm)
                        {
                             cpu.reg_sr+=2;            // set V flag
                        }
                        if  (Rm && !Dm)
                        {
                            cpu.reg_sr+=0x11;          // set C & X flags
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
                base = 0x5179;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    { 
                        int s = ((opcode >> 9) & 0x07);
                        if(s == 0)
                        {
                            s = 8;
                        }
                         int addr = cpu.readMemoryLongPC();         // get mem sign extended
                        cpu.pc_reg+=2;
                        int d=cpu.readMemoryWordSigned(addr);
                        int r = d-s;
                        cpu.writeMemoryWord(addr,r);
                        cpu.reg_sr&=0xffe0;            // all flags 0
                        boolean Sm = false;                 // source is never neg
                        boolean Dm = (d & 0x8000) != 0;     // dist is neg
                        boolean Rm = (r & 0x8000) != 0;     // result is neg
                        //n z v c                           // flags, x will be set to c
                        //8 4 2 1
                        if (r == 0)
                        {
                            cpu.reg_sr+=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr+=8;             // set N flag
                        }
                        if (!Sm && Dm && !Rm)
                        {
                             cpu.reg_sr+=2;              // set V flag
                        }
                        if  (Rm && !Dm)
                        {
                                 cpu.reg_sr+=0x11;              // set C & X flags
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
                base = 0x51b9;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int s = ((opcode >> 9) & 0x07);
                        if(s == 0)
                        {
                            s = 8;
                        }
                        int addr = cpu.readMemoryLongPC();         // get mem sign extended
                        cpu.pc_reg+=2;
                        int d=cpu.readMemoryLong(addr);
                        int r = d-s;
                        cpu.writeMemoryLong(addr,r);
                        cpu.reg_sr&=0xffe0;            // all flags to 0
                        boolean Sm = false;                 // source is never neg
                        boolean Dm = d< 0;                  // dist is neg
                        boolean Rm = r < 0;                 // result is neg
                        //n z v c                           // flags, x will be set to c
                        //8 4 2 1
                        if (r == 0)
                        {
                            cpu.reg_sr+=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr+=8;             // set N flag
                        }
                        if (!Sm && Dm && !Rm)
                        {
                             cpu.reg_sr+=2;              // set V flag
                        }
                        if  (Rm && !Dm)
                        {
                                 cpu.reg_sr+=0x11;              // set C & X flags
                        }
                    }
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                            return disassembleOp(address, opcode, Size.Long, cpu);
                    }
                };
            }
                     
            for(int r = 0; r < 8; r++)
            {
                cpu2.addInstruction(base + (r << 9), i);
            }
        }
    }

    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz, smsqmulator.cpu.MC68000Cpu cpu)
    {
            DisassembledOperand src = new DisassembledOperand("#" + ((opcode >> 9) & 0x07));
            DisassembledOperand dst = cpu.disassembleDstEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), sz);

            return new DisassembledInstruction(address, opcode, "subq" + sz.ext(), src, dst);
    }
}
