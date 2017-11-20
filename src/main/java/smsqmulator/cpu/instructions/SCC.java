package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The SCC instruction in all of its variants.
 * 
 * 0101cccc11mmmrrr = 50c0
 *  where   rrr = data reg to be swapped
 *          mmm = mode
 *          cccc= condition
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class SCC implements InstructionSet
{
    
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;
       
        base = 0x50c0;
        i = new smsqmulator.cpu.Instruction()   
        {
            private static final int C_FLAG = 1;
            private static final int V_FLAG = 2;
            private static final int Z_FLAG = 4;
            private static final int N_FLAG = 8;
            private static final int C_Z_FLAGS = C_FLAG | Z_FLAG;
            private static final int N_V_FLAGS = N_FLAG | V_FLAG;
            
            private final String[] names = { "st", "sf", "shi", "sls", "scc", "scs", "sne", "seq",
                                            "svc", "svs", "spl", "smi", "sge", "slt", "sgt", "sle"};
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int address=0;
                switch ((opcode >>3)&7)
                {
                    case 0:                                     // Dn
                        if (testCC((opcode >> 8) & 0x0f,cpu.reg_sr))
                            cpu.data_regs[opcode&7]|=0xff;
                        else
                            cpu.data_regs[opcode&7]&=0xffffff00;
                        return;                               // DO NOT USE BREAK HERE

                    case 2:                                     //(an)
                        address=cpu.addr_regs[opcode&7];
                        break;
                    case 3:                                     // (an)+
                        int reg=opcode&7;
                        address=cpu.addr_regs[reg];
                        if (reg==7)
                            cpu.addr_regs[reg]+=2;
                        else
                            cpu.addr_regs[reg]++;
                        break;
                    case 4:                                     // -(an)
                        reg=opcode&7;
                        if (reg==7)
                            cpu.addr_regs[reg]-=2;
                        else
                            cpu.addr_regs[reg]--;
                        address=cpu.addr_regs[reg];
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
                            cpu.pc_reg+=2;
                            break;
                        }
                        break;
                }
                
                if (testCC((opcode >> 8) & 0x0f,cpu.reg_sr))
                    cpu.writeMemoryByte(address,0xff);
                else
                    cpu.writeMemoryByte(address,0);
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                DisassembledOperand op = cpu.disassembleDstEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), Size.Byte);
                return new DisassembledInstruction(address, opcode, names[(opcode >> 8) & 0x0f], op);
            }   
            
            private boolean testCC(int cc,int ccr)
            {
		switch(cc)
		{
                    case 0:		// T
                    {
                            return true;
                    }
                    case 1:		// F
                    {
                            return false;
                    }
            
                    case 2:		//HI:
                    {
                            return (ccr & C_Z_FLAGS) == 0;
                    }
                    case 3:		//LS:
                    {
                            return (ccr & C_Z_FLAGS) != 0;
                    }
                    case 4:		//CC:
                    {
                            return (ccr & C_FLAG) == 0;
                    }
                    case 5:		//CS:
                    {
                            return (ccr & C_FLAG) != 0;
                    }
                    case 6:		//NE:
                    {
                            return ((ccr & Z_FLAG) == 0);
                    }
                    case 7:		//EQ:
                    {
                            return (ccr & Z_FLAG) != 0;
                    }
                    case 8:		//VC:
                    {
                            return (ccr & V_FLAG) == 0;
                    }
                    case 9:		//VS:
                    {
                            return (ccr & V_FLAG) != 0;
                    }
                    case 10:	//PL:
                    {
                            return (ccr & N_FLAG) == 0;
                    }
                    case 11:	//MI:
                    {
                            return (ccr & N_FLAG) != 0;
                    }
                    case 12:	//GE:
                    {
                            int v = ccr & N_V_FLAGS;
                            return (v == 0 || v == N_V_FLAGS);
                    }
                    case 13:	//LT:
                    {
                        int v = ccr & N_V_FLAGS;
                        return (v == N_FLAG || v == V_FLAG);
                      //  return ((ccr & N_V_FLAGS)!=0);
                    }
                    case 14:	//GT:
                    {
                            int v = ccr & (N_V_FLAGS | Z_FLAG);
                            return (v == 0 || v == N_V_FLAGS);
                    }
                    case 15:	//LE:
                    {
                            int v = ccr & (N_V_FLAGS | Z_FLAG);
                            return ((v & Z_FLAG) != 0 || (v == N_FLAG) || (v == V_FLAG));
                    }
                }
                throw new IllegalArgumentException("Invalid Condition Code value!");
            } 
            
            protected int getDisplacement(smsqmulator.cpu.MC68000Cpu cpu)
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
        };
         
        for (int cc=0;cc<16;cc++)
        {
            int ccs=cc<<8;
            for (int mode=0;mode<8;mode++)
            {
                if (mode==1)
                    continue;
                int rmode=mode<<3;
                for (int reg=0;reg<8;reg++)
                {
                    if (mode==7 & reg>1)
                        break;
                    cpu2.addInstruction(base+ ccs+ rmode + reg, i);
                }
            }
        }
    }
            
  
}
