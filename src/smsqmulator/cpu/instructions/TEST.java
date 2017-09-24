package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledInstruction;

/**
 * Illegal instruction used by Qmon (4afb) - set the correct pc address.
 * 
 * @author and copyright for my code (c) Wolfgang Lenerz 2013
 * 
 * Based on Tony Headford's  m68k.cpu.instruction (C) Tony Headford.
 *
 */
    

public class TEST implements smsqmulator.cpu.InstructionSet
{
    private final int base=0xa600;                     // unused instructions by the MC 680000

    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        cpu2.addInstruction(base, new smsqmulator.cpu.Instruction()
        {
            private static final int C_FLAG = 1;
            private static final int V_FLAG = 2;
            private static final int Z_FLAG = 4;
            private static final int N_FLAG = 8;
            private static final int C_Z_FLAGS = C_FLAG | Z_FLAG;
            private static final int N_V_FLAGS = N_FLAG | V_FLAG;
            private static final int N_V_Z_FLAGS = N_FLAG | V_FLAG | Z_FLAG;
   
            @Override
            //public final void execute(final int opcode,final smsqmulator.cpu.MC68000Cpu cpu)
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                if(testCC((opcode >> 8) & 0x0f,cpu))
                {  
                    int dis = (opcode & 0xff);
                    if ((dis&0x80) !=0)
                       // dis|=0xffffff00;
                        dis-=256;
                    cpu.pc_reg +=(dis/2);
                }
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                    return new DisassembledInstruction(address, opcode, "TEST");
            } 
            
           // private boolean testCC(final int cc,final int ccr)                  
            private boolean testCC(int cc,smsqmulator.cpu.MC68000Cpu cpu)
            {
		switch(cc)
		{
         /*           case 2:		//HI:
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
                    }*/
                    case 6:		//NE:
                    {
           //                 return !cpu.Zflag;
                    }
                    case 7:		//EQ:
                    {
             //               return cpu.Zflag;
                    }
               /*     case 8:		//VC:
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
                       // return (ccr & N_FLAG) == (ccr & V_FLAG);
                        int v = ccr & N_V_FLAGS;
                        return (v == 0 || v == N_V_FLAGS);
                    }
                    case 13:	//LT:
                    {
                        //    int v = ccr & N_V_FLAGS;
                          //  return (v == N_FLAG || v == V_FLAG);
                        return (ccr & N_V_FLAGS)!=0;
                    }
                    case 14:	//GT:
                    {
                            int v = ccr & N_V_Z_FLAGS;
                            return (v == 0 || v == N_V_FLAGS);
                    }
                    case 15:	//LE:
                    {
                            int v = ccr & N_V_Z_FLAGS;
                            return ((v & Z_FLAG) != 0 || (v == N_FLAG) || (v == V_FLAG));
                    }
                       */
                }
                throw new IllegalArgumentException("Invalid Condition Code value!");
            }
        });
    }
}
