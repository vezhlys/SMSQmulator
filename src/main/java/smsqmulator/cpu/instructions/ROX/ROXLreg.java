package smsqmulator.cpu.instructions.ROX;

/**
 * The LSL instruction where data to be shifted is in a data reg and the shift count is in another data reg.
 * 1110ccc1ss110rrr = 0xe130  
 * 
 * mmm = mode 010
 * rrr = data register to be shifted
 * ccc = dataregister containing the shift count (modulo 64)
 * ss =size :   00 = byte
 *              01 = word
 *              10 = long
 * 
 * 
 *  @author and copyright for my code (c) Wolfgang Lenerz 2013 - 2014.
 * 
 * Based on code by Tony Headford, see his licence in accompanying file
 */

import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

public class ROXLreg implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;
        for(int sz = 0; sz < 3; sz++)                       // byte, word and long only
        {
            if (sz == 0)                                     // shift lsb
            {
                base = 0xe130;
                i = new smsqmulator.cpu.Instruction()   
                {
                   
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    { 
                        int nbrShifts = (cpu.data_regs[(opcode>>9)&0x7])&63;// how often we shift
                        int val=cpu.data_regs[opcode&0x7]&0xff;// byte to be shifted 
                        int lastOut=0;
                        int xBit=((cpu.reg_sr &16)==0)?0:1;
                        for (int cnt=0;cnt<nbrShifts;cnt++)
                        {
                            lastOut=val&0x80;                   // highest bit before shift
                            val<<=1;                            // shift one bit up
                            if (xBit!=0)
                                val|=1;
                            xBit=lastOut;
                        }
                        val&=0xff;
                        cpu.data_regs[opcode&0x7]&=0xffffff00; //get rid of ls byte
                        cpu.data_regs[opcode&0x7]|=val;    // and fill it in with new value
                        cpu.reg_sr&=0xfff0;                // all status flags set to 0, except for X
                        if ((val&0x80)!=0)                      // ms bit set
                            cpu.reg_sr+=8;                 // so set N flag if (val==0)    
                        else if (val==0)
                            cpu.reg_sr+=4;                 // set Z flag 
                        if (nbrShifts!=0)                       // X flag is unaffected by 0 count shift, C is cleared by it
                        {
                            if (lastOut!=0)
                                cpu.reg_sr|=17;            // set X + C flags
                            else
                                 cpu.reg_sr&=~16;          // clear X flag (C flag already cleared)
                        }     
                    }
                    
                   @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Byte, cpu);
                    }   
                };
            }
            else if (sz==1)                                     // word sized shift
            {
                base = 0xe170; 
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    { 
                        int nbrShifts = (cpu.data_regs[(opcode>>9)&0x7])&63;// how often we shift
                        int val=cpu.data_regs[opcode&0x7]&0xffff;// word to be shifted
                        int lastOut=0;
                        int xBit=((cpu.reg_sr &16)==0)?0:1;
                        for (int cnt=0;cnt<nbrShifts;cnt++)
                        {
                            lastOut=val&0x8000;                 // highest bit before shift
                            val<<=1;                            // shift one bit up
                            if (xBit!=0)
                                val|=1;
                            xBit=lastOut;
                        }
                        val&=0xffff;
                        cpu.data_regs[opcode&0x7]&=0xffff0000; //get rid of ls byte
                        cpu.data_regs[opcode&0x7]|=val;// and fill it in with new value
                        cpu.reg_sr&=0xfff0;                // all status flags set to 0, except for X
                        if ((val&0x8000)!=0)                      // ms bit set
                            cpu.reg_sr+=8;                 // so set N flag if (val==0)    
                        else if (val==0)
                            cpu.reg_sr+=4;                 // set Z flag 
                        if (nbrShifts!=0)                       // X flag is unaffected by 0 count rotate, C is cleared by it
                        {
                            if (lastOut!=0)
                                cpu.reg_sr|=17;            // set X + C flags
                            else
                                 cpu.reg_sr&=~16;          // clear X flag (C flag already cleared)
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
                base = 0xe1b0; 
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    { 
                        int nbrShifts = (cpu.data_regs[(opcode>>9)&0x7])&63;// how often we shift
                        int val=cpu.data_regs[opcode&0x7];// long word to be shifted 
                        int lastOut=0;
                        int xBit=((cpu.reg_sr &16)==0)?0:1;
                        for (int cnt=0;cnt<nbrShifts;cnt++)
                        {
                            lastOut=val&0x80000000;  
                            val<<=1;                            // shift one bit up
                            if (xBit!=0)
                                val|=1;
                            xBit=lastOut;
                        }
                        cpu.data_regs[opcode&0x7]=val;     // and fill it in with new value
                        cpu.reg_sr&=0xfff0;                // all status flags set to 0, except for X
                        if ((val&0x80)!=0)                      // ms bit set
                            cpu.reg_sr+=8;                 // so set N flag if (val==0)    
                        else if (val==0)
                            cpu.reg_sr+=4;                 // set Z flag 
                        if (nbrShifts!=0)                       // X flag is unaffected by 0 count rotate, C is cleared by it
                        {
                            if (lastOut!=0)
                                cpu.reg_sr|=17;            // set X + C flags
                            else
                                 cpu.reg_sr&=~16;          // clear X flag (C flag already cleared)
                        }
                    }
                    
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Long, cpu);
                    }   
                };
            }
            for (int regcnt=0;regcnt<8;regcnt++)
            {
                for (int destreg=0;destreg<8;destreg++)
                {
                    cpu2.addInstruction(base+ (regcnt<<9) + destreg, i);
                }
            }
        }
    }
    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz, smsqmulator.cpu.MC68000Cpu cpu)
    {
           //reg mode
            DisassembledOperand src = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
            DisassembledOperand dst = new DisassembledOperand("d" + (opcode & 0x07));
            return new DisassembledInstruction(address, opcode, "roxl" + sz.ext(), src, dst);
    }
}
