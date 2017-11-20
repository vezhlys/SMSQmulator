package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The NBCD instruction in all of its variants.
 * 
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class NBCD implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        smsqmulator.cpu.Instruction i= new smsqmulator.cpu.Instruction()   
        {
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {              
                int address=0;
                int s;
                switch ((opcode >>3)&7)
                {
                    case 0:                                     // Dn
                        int reg=opcode&7;
                        s=doCalc(opcode,cpu.data_regs[reg]&0xff,cpu);
                        cpu.data_regs[reg]&=0xffffff00;
                        cpu.data_regs[reg]|=s;
                        return;
                    case 2:                                     //(an)
                        address=cpu.addr_regs[opcode&7];
                        break;
                    case 3:                
                        reg=opcode&7;
                        address=cpu.addr_regs[reg];
                        if (reg==7)
                            cpu.addr_regs[opcode&7]+=2;
                        else
                            cpu.addr_regs[opcode&7]++;
                        break;
                    case 4:                                     // -(an)
                        reg=opcode&7;
                        if (reg==7)
                            cpu.addr_regs[opcode&7]-=2;
                        else
                            cpu.addr_regs[opcode&7]--;
                        address=cpu.addr_regs[opcode&7];
                        break;                                  
                      case 5:                                     // d16(an)
                        address=cpu.addr_regs[opcode&7] + cpu.readMemoryWordPCSignedInc();
                        break;
                    case 6:                                     // d8(an,xn)
                        address=getDisplacement(cpu)+cpu.addr_regs[opcode&7];
                        break;
                    case 7:
                        switch (opcode &7)
                        {
                        case 0:
                            address=cpu.readMemoryWordPCSignedInc();
                            break;
                        case 1:
                            address=cpu.readMemoryLongPC();
                            cpu.pc_reg++;
                            break;
                        }
                        break;
                }
                s=cpu.readMemoryByte(address);
                cpu.writeMemoryByte(address, doCalc(opcode,s,cpu));
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
		DisassembledOperand op = cpu.disassembleDstEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), Size.Byte);
		return new DisassembledInstruction(address, opcode, "nbcd", op);
            }
            protected final int getDisplacement(smsqmulator.cpu.MC68000Cpu cpu)
            {
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
                return displacement;
            }
                       
            protected final int doCalc (int opcode, int s,smsqmulator.cpu.MC68000Cpu cpu)
            {
         
		int x = ((cpu.reg_sr&16)==0)?0:1;
		int c;

		int lo = 10 - (s & 0x0f) - x;
		if(lo < 10)
		{
			c = 1;
		}
		else
		{
			lo = 0;
			c = 0;
		}

		int hi = 10 - ((s >> 4) & 0x0f) - c;
		if(hi < 10)
		{
			c = 1;
		}
		else
		{
			c = 0;
			hi = 0;
		}

		int result = (hi << 4) + lo;
                if (c==0)
                    cpu.reg_sr&=0xffee;                    // clear x & c flags
                else
                    cpu.reg_sr|=17;                        // set X & C flags
		result&=0xff;
		if(result != 0)
                    cpu.reg_sr&=0xFFFb;                    // clear Z flag

		return result;
            }
            
        };
        int base=0x4800;
        for (int mode=0;mode<8;mode++)
        {
            if (mode==1)
                continue;
            int rmode=mode<<3;
            for (int reg=0;reg<8;reg++)
            {
                if (mode==7 & reg>1)
                    break;
                cpu2.addInstruction(base+ rmode + reg, i);
            }
        }
    }
}
