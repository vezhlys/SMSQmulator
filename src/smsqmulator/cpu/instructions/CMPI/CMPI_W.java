package smsqmulator.cpu.instructions.CMPI;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The CMPI instruction where destination is .W absolute.
 
 * @author and copyright (c) 2013 wolfgang lenerz
 * 
 *  00001100ssmmmrrr
 *  
 *  where   
 *          ss= size (000=byte, 001 = word, 010 = long)
 *          mmm = ea mode =111, 
 *          rrr = 0
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class CMPI_W implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;

        for(int sz = 0; sz < 3; sz++)
        {
            if(sz == 0)
            {
                base = 0x0c38;
                i = new smsqmulator.cpu.Instruction()    // byte sized compare
                {
                   
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int s=cpu.readMemoryWordPCInc()&0xff;//source value
                        if ((s & 0x80) != 0)
                            s|=0xffffff00;
                        int d=cpu.readMemoryWordPCSignedInc();         // get mem sign extended
                        d=cpu.readMemoryByteSigned(d);
                        int r = d-s;
                        cpu.reg_sr&=0xfff0;            // X bit not affected by cmp
                        boolean Sm = s<0;                   // source is neg
                        boolean Dm = d<0;                   // dest is neg
                        boolean Rm = r<0;                   // result is neg
                        //n z v c
                        //8 4 2 1
                        if(r == 0)
                        {
                            cpu.reg_sr+=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr+=8;             // set N flag
                        }
                           
                        if((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                        {
                           cpu.reg_sr+=2;              // set V flag
                        }
                        if((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                        {
                            cpu.reg_sr++;              // set C flag
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
                base = 0x0c78;
                i = new smsqmulator.cpu.Instruction()
                {
                   
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    { 
                        int s=cpu.readMemoryWordPCSignedInc();//source value
                        int d=cpu.readMemoryWordPCSignedInc();         // get mem sign extended
                        d=cpu.readMemoryWordSigned(d);
                        int r = d-s;
                        cpu.reg_sr&=0xfff0;            // X bit not affected by cmp
                        boolean Sm = s<0;                   // source is neg
                        boolean Dm = d<0;                   // dest is neg
                        boolean Rm = r<0;                   // result is neg
                        //n z v c
                        //8 4 2 1
                        if(r == 0)
                        {
                            cpu.reg_sr+=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr+=8;             // set N flag
                        }
                           
                        if((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                        {
                           cpu.reg_sr+=2;              // set V flag
                        }
                        if((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                        {
                            cpu.reg_sr++;              // set C flag
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
                base = 0x0cb8;
                i = new smsqmulator.cpu.Instruction()
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int s=cpu.readMemoryLongPC();//source value
                        cpu.pc_reg+=2;
                        int d = cpu.readMemoryWordPCSignedInc();         // get mem sign extended
                        d=cpu.readMemoryLong(d);
                        int r = d-s;
                        cpu.reg_sr&=0xfff0;            // X bit not affected by cmp
                        boolean Sm = (s< 0);                // source is neg
                        boolean Dm = (d<0);                 // dist is neg
                        boolean Rm = (r<0);                 // result is neg
                        //n z v c
                        //8 4 2 1
                        if(r == 0)
                        {
                            cpu.reg_sr+=4;             // set Z flag
                        }
                        else if (Rm)
                        {
                            cpu.reg_sr+=8;             // set N flag
                        }
                           
                        if((!Sm && Dm && !Rm) || (Sm && !Dm && Rm))
                        {
                           cpu.reg_sr+=2;              // set V flag
                        }
                        if((Sm && !Dm) || (Rm && !Dm) || (Sm && Rm))
                        {
                            cpu.reg_sr++;              // set C flag
                        }
                    }
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                            return disassembleOp(address, opcode, Size.Long, cpu);
                    }
                };
            }
            cpu2.addInstruction(base,i);
           
        }
    }

    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz, smsqmulator.cpu.MC68000Cpu cpu)
    {
        int val;
        int bytes_read;
        String op;

        switch(sz)
        {
                case Byte:
                {
                        val = cpu.readMemoryWord(address + 2);
                        bytes_read = 2;
                        op = String.format("#$%02x", (val & 0xff));
                        break;
                }
                case Word:
                {
                        val = cpu.readMemoryWord(address + 2);
                        bytes_read = 2;
                        op = String.format("#$%04x", (val & 0x0000ffff));
                        break;
                }
                case Long:
                {
                        val = cpu.readMemoryLong(address + 2);
                        bytes_read = 4;
                        op = String.format("#$%08x", val);
                        break;
                }
                default:
                {
                        throw new IllegalArgumentException("Invalid size for CMPI");
                }
        }

        DisassembledOperand src = new DisassembledOperand(op, bytes_read, val);
        DisassembledOperand dst = cpu.disassembleDstEA(address + 2 + bytes_read, (opcode >> 3) & 0x07, (opcode & 0x07), sz);
        return new DisassembledInstruction(address, opcode, "cmpi" + sz.ext(), src, dst);
    }
}
