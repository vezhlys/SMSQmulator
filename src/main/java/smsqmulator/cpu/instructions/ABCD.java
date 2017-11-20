package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;

/**
 * The ABCD instruction in all of its variants.
 * This works well unless the entry values aren't valid BDC.
 * If they aren't, this works according to the GIGO principle.
 * 
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see accompanying file.
 * 
 */
public class ABCD implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        smsqmulator.cpu.Instruction i= new smsqmulator.cpu.Instruction()   
        {
            
            
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int reg=(opcode>>9)&7;                          // dest reg                        
                switch (opcode & 8)
                {
                    case 0:                                     // data regs
                        int s= cpu.data_regs[opcode&7]&0xff;
                        int d= cpu.data_regs[reg]&0xff;
                        cpu.data_regs[reg]&=0xffffff00;
                        cpu.data_regs[reg]|=doCalc(s,d,cpu);   // this also sets the flags
                        break;
                    case 8:                                     // address regs
                        int sreg=opcode&7;
                        if (sreg==7)
                            cpu.addr_regs[sreg]-=2;
                        else
                            cpu.addr_regs[sreg]--;
                        if (reg==7)
                            cpu.addr_regs[reg]-=2;
                        else
                            cpu.addr_regs[reg]--;
                        s=cpu.readMemoryByte(cpu.addr_regs[sreg]);
                        d=cpu.readMemoryByte(cpu.addr_regs[reg]);
                        cpu.writeMemoryByte(cpu.addr_regs[reg],doCalc(s,d,cpu));   
                        break;
                }
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
		DisassembledOperand src;
		DisassembledOperand dst;
		if((opcode&8)==0)
		{
			src = new DisassembledOperand("d" + (opcode & 0x07));
			dst = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
		}
		else
		{
			src = new DisassembledOperand("-(a" + (opcode & 0x07) + ")");
			dst = new DisassembledOperand("-(a" + ((opcode >> 9) & 0x07) + ")");
		}
		return new DisassembledInstruction(address, opcode, "abcd", src, dst);
            }
            
            // calculate this the way Intel handles the DAA instruction - thanks Marcel!
            protected final int doCalc(int s, int d,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int x = ((cpu.reg_sr&16)==0)?0:1;          // extend (carry flag)
                int c = 0;
               
                int r = s+d+x;                                  // total result, binary
                if (r>255)
                   c=1;                                         // carry for total operation
               
                int lo = (s & 0x0f) + (d & 0x0f) + x;           // lower nibble
               
                if(((r&0x0f) > 9) || (lo&0xf0)!=0)
                {
                   r+= 6;
                   if (r>255)
                       c=1;
                }
                if (((r&0xf0)>0x90) || (c==1))
                {
                   r+=0x60;
                   c=1;
                }
                else
                   c=0;
               
                if(c != 0)
                {
                    cpu.reg_sr|=17;                    // set X & C flags
                }
                else
                {
                    cpu.reg_sr&=0xffee;
                }
                r&=0xff;
                if(r != 0)
                {
                    cpu.reg_sr&=0xFFFb;                // clear Z flag
                }
                return r;
           } 
        };
        int base=0xc100;
        for (int mode=0;mode<8;mode++)
        {
            int rmode=mode<<9;
            for (int reg=0;reg<8;reg++)
            {
                cpu2.addInstruction(base+ rmode + reg, i);
            }
        } 
        base=0xc108;
        for (int mode=0;mode<8;mode++)
        {
            int rmode=mode<<9;
            for (int reg=0;reg<8;reg++)
            {
                cpu2.addInstruction(base+ rmode + reg, i);
            }
        }
    }
}

