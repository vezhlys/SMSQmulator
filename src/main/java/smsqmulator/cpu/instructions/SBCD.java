package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
/**
 * The ABCD instruction in all of its variants.
 * 
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * @author and Copyright (c) for his code by Tony Headford, see licence below.
 * 
 */
public class SBCD implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        smsqmulator.cpu.Instruction i= new smsqmulator.cpu.Instruction()   
        {
            private smsqmulator.cpu.MC68000Cpu cpu=cpu2;
                        
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int reg=(opcode>>9)&7;                         // dest reg                        
                switch (opcode & 8)
                {
                    case 0:                                     // data regs
                        int s= cpu.data_regs[opcode&7]&0xff;
                        int d= cpu.data_regs[reg]&0xff;
                        cpu.data_regs[reg]&=0xffffff00;
                        cpu.data_regs[reg]|=doCalc(s,d);   // this also sets the flags
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
                        cpu.writeMemoryByte(cpu.addr_regs[reg],doCalc(s,d));   
                        break;
                }
            }
            
            protected final int doCalc (int s, int d)
            {
		int x=((cpu.reg_sr&16)==0)?0:1;
		int c=0;
		int lo=(d & 0x0f) - (s & 0x0f) - x;
                int hi=(d & 0xf0) - (s & 0xf0);
                int val=lo+hi;
                if ((lo & 0xf0)!=0)
                {
                    val-=6;
                    c=6;
                }
                if (((((d & 0xff) - (s & 0xff) - x)) & 0x100) > 0xff)
                {
                    val-=0x60;
                }
                cpu.reg_sr&=0xffee;                        // clear x and c flags
                if ((((d & 0xff) - (s & 0xff) - c - x) & 0x300) > 0xff)
                    cpu.reg_sr|=17;                        // set X & C flags
		val&=0xff;
		if(val != 0)
		{
                    cpu.reg_sr&=0xFFFb;                    // clear Z flag
		}
		return val;
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
		return new DisassembledInstruction(address, opcode, "sbcd", src, dst);
            }
        };
        int base=0x8100;
        for (int mode=0;mode<8;mode++)
        {
            int rmode=mode<<9;
            for (int reg=0;reg<8;reg++)
            {
                cpu2.addInstruction(base+ rmode + reg, i);
            }
        } 
        base=0x8108;
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
