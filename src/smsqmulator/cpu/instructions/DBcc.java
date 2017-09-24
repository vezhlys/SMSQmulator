package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;

/**
 * A replacement DBcc instruction in all of its variants
 * 
 * This is based on Tony Headford's work.
 * 
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 * 
 */
public class DBcc implements InstructionSet
{
    protected static final String[] names = { "dbt", "dbra", "dbhi", "dbls", "dbcc", "dbcs", "dbne", "dbeq","dbvc", "dbvs", "dbpl", "dbmi", "dbge", "dblt", "dbgt", "dble"};
   
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base = 0x50c8;
        smsqmulator.cpu.Instruction i = new smsqmulator.cpu.Instruction()
        {			
            private static final int C_FLAG = 1;
            private static final int V_FLAG = 2;
            private static final int Z_FLAG = 4;
            private static final int N_FLAG = 8;
            private static final int C_Z_FLAGS = C_FLAG | Z_FLAG;
            private static final int N_V_FLAGS = N_FLAG | V_FLAG;

            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int reg = (opcode & 0x07);
		int count = cpu.data_regs[reg]&0xffff;
           //     if ((count &0x8000)!=0)
             //       count|=0xffff0000;
                
		if(!testCC((opcode >> 8) & 0x0f,cpu.reg_sr))
                {   
                    count--;
                    cpu.data_regs[reg]&=0xffff0000;
             //       cpu.data_regs[reg]|=(count&0xffff);
                    if(count != -1)
                    {
                        cpu.data_regs[reg]|=count;                          /////
                        cpu.pc_reg+= (cpu.readMemoryWordPCSignedInc()/2);
                    }
                    else
                    {
                        cpu.data_regs[reg]|=0xffff;                         /////
                        cpu.pc_reg++;
                    }
                }
                else
                    cpu.pc_reg++;
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
                        //return (ccr & N_V_FLAGS)!=0;
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
            
            @Override
            public final DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int cc = (opcode >> 8) & 0x0f;
                int dis = cpu.readMemoryWordSigned(address + 2);

                DisassembledOperand reg = new DisassembledOperand(String.format("d%d", (opcode & 0x07)));
                //word displacement
                DisassembledOperand where = new DisassembledOperand(String.format("$%08x", dis + address + 2), 2, dis);

                return new DisassembledInstruction(address, opcode, names[cc], reg, where);
            }
        };

        for(int cc = 0; cc < 16; cc++)
        {
            for(int r = 0; r < 8; r++)
            {
                cpu2.addInstruction(base + (cc << 8) + r, i);
            }
        }
    }
}
