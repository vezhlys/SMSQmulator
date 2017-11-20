package smsqmulator.cpu.instructions.ROX;

/**
 * The ROXR instruction where data to be rotated is in a data reg and the rotate count is immediate.
 * 1110ccc0ss010rrr = 0xe010   
 * 
 * mmm = mode 010
 * rrr = data register to be shifted
 * ccc = the shift count
 * ss =size :   00 = byte
 *              01 = word
 *              10 = long
 * 
 * 
 * 
 * @author and copyright for my code (c) Wolfgang Lenerz 2014.
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */

import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

public class ROXRimm implements InstructionSet
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
                base = 0xe010;
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int nbrShifts = ((opcode>>9)&0x7 );     // how often we rotate
                        if (nbrShifts==0)
                            nbrShifts=8;
                        int val=cpu.data_regs[opcode&0x7]&0xff;// byte to be shifted
                       
                        int lastOut=val&0x1;
                        int xBit=((cpu.reg_sr &16)==0)?0:1;
                        val>>>=1;
                        if (xBit!=0)
                            val|=0x80;                          // set x bit       
                        for (int cnt=1;cnt<nbrShifts;cnt++)     // WATCH the start count : 1, not 0
                        {
                            lastOut=val&0x1;                    // lowest bit before shift
                            val>>>=1;                           // shift one bit down
                            if (lastOut!=0)
                                val|=0x80;                      // set 
                        } 
                        cpu.data_regs[opcode&0x7]&=0xffffff00; //get rid of ls byte
                        cpu.data_regs[opcode&0x7]|=val;// and fill it in with new value
                        cpu.reg_sr&=0xffe0;                // status flags set to 0, X can't be unaffected : no 0 shift count here
                        if (val==0)
                            cpu.reg_sr|=4;                 // set Z flag 
                        else if ((val&0x80)!=0)                 // ms bit set
                            cpu.reg_sr|=8;                 // so set N flag (this may be set if the X dlag was st, even if last bit rotatd out wand't)
                        if (lastOut!=0)
                            cpu.reg_sr|=17;                // so set C + X flags
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
                base = 0xe050; 
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int nbrShifts = ((opcode>>9)&0x7 );     // how often we shift
                        if (nbrShifts==0)
                            nbrShifts=8;
                        int val=cpu.data_regs[opcode&0x7]&0xffff;// word to be shifted
                        int lastOut=val&0x1;
                        int xBit=((cpu.reg_sr &16)==0)?0:1;
                        val>>>=1;
                        if (xBit!=0)
                            val|=0x8000;                        // set x bit       
                        for (int cnt=1;cnt<nbrShifts;cnt++)     // WATCH the start count : 1, not 0
                        {
                            lastOut=val&0x1;                    // lowest bit before shift
                            val>>>=1;                           // shift one bit up
                            if (lastOut!=0)
                                val|=0x8000;                    // set 
                        } 
                        cpu.data_regs[opcode&0x7]&=0xffff0000; //get rid of ls byte
                        cpu.data_regs[opcode&0x7]|=val;// and fill it in with new value
                        cpu.reg_sr&=0xffe0;                // status flags set to 0, X can't be unaffected : no 0 shift count here
                        if (val==0)
                            cpu.reg_sr|=4;                 // set Z flag 
                        else if ((val & 0x8000)!=0)             // ms bit set
                            cpu.reg_sr|=8;                 // so set N flag (this may be set if the X dlag was st, even if last bit rotatd out wand't)
                        if (lastOut!=0)
                            cpu.reg_sr|=17;                // so set C + X flags
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
                base = 0xe090; 
                i = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        int nbrShifts = ((opcode>>9)&0x7 );     // how often we shift
                        if (nbrShifts==0)
                            nbrShifts=8;
                        int val=cpu.data_regs[opcode&0x7]; // long word to be shifted
                        int lastOut=val&0x1;
                        int xBit=((cpu.reg_sr &16)==0)?0:1;
                        val>>>=1;
                        if (xBit!=0)
                            val|=0x80000000;                    // set x bit       
                        for (int cnt=1;cnt<nbrShifts;cnt++)     // WATCH the start count : 1, not 0
                        {
                            lastOut=val&0x1;                    // lowest bit before shift
                            val>>>=1;                           // shift one bit up
                            if (lastOut!=0)
                                val|=0x80000000;                // set 
                        } 
                        cpu.data_regs[opcode&0x7]=val;     // and fill it in with new value
                        cpu.reg_sr&=0xffe0;                // status flags set to 0, X can't be unaffected : no 0 shift count here
                        if (val==0)
                            cpu.reg_sr|=4;                 // set Z flag 
                        else if (val<0)                         // ms bit set
                            cpu.reg_sr|=8;                 // so set N flag (this may be set if the X dlag was st, even if last bit rotatd out wand't)
                        if (lastOut!=0)
                            cpu.reg_sr|=17;                // so set C + X flags
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
        int count = (opcode >> 9) & 0x07;
        if(count == 0)
                count = 8;
        DisassembledOperand src = new DisassembledOperand("#" + count);
	DisassembledOperand dst = new DisassembledOperand("d" + (opcode & 0x07));
        return new DisassembledInstruction(address, opcode, "roxr" + sz.ext(), src, dst);
    }
}
