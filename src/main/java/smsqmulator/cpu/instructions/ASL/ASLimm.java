package smsqmulator.cpu.instructions.ASL;

/**
 * The ASL instruction where data to be shifted is in a data reg and the shift count is immediate.
 * 1110ccc1ss000rrr = 0xe100   
 * 
 * mmm = mode 010
 * rrr = data register to be shifted
 * ccc = the shift count
 * ss =size :   00 = byte
 *              01 = word
 *              10 = long
 * 
 * @author and copyright for my code (c) Wolfgang Lenerz 2014.
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */

import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

public class ASLimm implements InstructionSet
{
   
    @Override
    public final void register( final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;
        for(int sz = 0; sz < 3; sz++)                       // byte, word and long only
        {
            if (sz == 0)                                     // shift lsb
            {
                base = 0xe100;
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int nbrShifts = ((opcode>>9)&0x7 );// how often we shift
                        if (nbrShifts==0)
                            nbrShifts=8;
                        int val=cpu.data_regs[opcode&0x7]&0xff;// byte to be shifted
                        boolean stateChanged=false;
                        int lastOut=0;
                        for (int cnt=0;cnt<nbrShifts;cnt++)
                        {
                            lastOut=val&0x80;                   // highest bit before shift
                            val<<=1;                            // shift one bit up
                            if (!stateChanged)
                            {
                                stateChanged= (lastOut!=(val&0x80));
                            }
                        }
                        val&=0xff;
                        cpu.data_regs[opcode&0x7]&=0xffffff00; //get rid of ls byte
                        cpu.data_regs[opcode&0x7]|=val;// and fill it in with new value
                        cpu.reg_sr&=0xffe0;                // all status flags set to 0,
                        if ((val&0x80)!=0)                      // ms bit set
                            cpu.reg_sr+=8;                 // so set N flag if (val==0)    
                        else if (val==0)
                            cpu.reg_sr+=4;                 // set Z flag 
                        if (stateChanged)
                            cpu.reg_sr+=2;                 // set V flag if ms bit changed at any time during the oeration
                       
                        if (lastOut!=0)
                            cpu.reg_sr|=17;                // set X + C flags   
                    }
                    
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Byte);
                    }   
                };
            }
            else if (sz==1)                                     // word sized shift
            {
                base = 0xe140; 
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int nbrShifts = ((opcode>>9)&0x7 );// how often we shift
                        if (nbrShifts==0)
                            nbrShifts=8;
                        int val=cpu.data_regs[opcode&0x7]&0xffff;// word to be shifted
                        boolean stateChanged=false;
                        int lastOut=0;
                        for (int cnt=0;cnt<nbrShifts;cnt++)
                        {
                            lastOut=val&0x8000;                   // highest bit before shift
                            val<<=1;                            // shift one bit up
                            if (!stateChanged)
                            {
                                stateChanged= (lastOut!=(val&0x8000));
                            }
                        }
                        val&=0xffff;
                        cpu.data_regs[opcode&0x7]&=0xffff0000; //get rid of ls byte
                        cpu.data_regs[opcode&0x7]|=val;// and fill it in with new value
                        cpu.reg_sr&=0xffe0;                // all status flags set to 0
                        if ((val&0x8000)!=0)                    // ms bit set
                            cpu.reg_sr|=8;                 // so set N flag
                        else if (val==0)
                            cpu.reg_sr|=4;                 // set Z flag 
                        if (stateChanged)
                            cpu.reg_sr|=2;                 // set V flag if ms bit changed at any time during the oeration
                        if (lastOut!=0)
                            cpu.reg_sr|=17;                // set X + C flags
                    }
                    
                    @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Word);
                    }   
                };
            }
            else
            {
                base = 0xe180; 
                i = new smsqmulator.cpu.Instruction()   
                {
                   
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int nbrShifts = ((opcode>>9)&0x7 );// how often we shift
                        if (nbrShifts==0)
                            nbrShifts=8;
                        int val=cpu.data_regs[opcode&0x7];// long word to be shifted
                        int lastOut=0;            // current state of highest bit
                        boolean stateChanged=false;
                        for (int cnt=0;cnt<nbrShifts;cnt++)
                        {
                            lastOut=val&0x80000000;  
                            val<<=1;                            // shift one bit up
                            if (!stateChanged)
                            {
                                stateChanged= (lastOut!=(val&0x80000000));
                            }
                        }
                        cpu.data_regs[opcode&0x7]=val;// and fill it in with new value
                        cpu.reg_sr&=0xffe0;                // all status flags set to 0
                        if (val<0)                // ms bit set
                            cpu.reg_sr+=8;                 // so set N flag if (val==0)    
                        else if (val==0)
                            cpu.reg_sr+=4;                 // set Z flag 
                        if (stateChanged)
                            cpu.reg_sr+=2;                 // set V flag if ms bit changed at any time during the oeration
                        if (lastOut!=0)
                            cpu.reg_sr|=17;                // set X + C flags
                    }
                    
                   @Override
                    public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        return disassembleOp(address, opcode, Size.Long);
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
    protected final DisassembledInstruction disassembleOp(int address, int opcode, Size sz)
    {
        int count = (opcode >> 9) & 0x07;
        if(count == 0)
                count = 8;
        DisassembledOperand src = new DisassembledOperand("#" + count);
	DisassembledOperand dst = new DisassembledOperand("d" + (opcode & 0x07));
        return new DisassembledInstruction(address, opcode, "asl" + sz.ext(), src, dst);
    }
}
          
