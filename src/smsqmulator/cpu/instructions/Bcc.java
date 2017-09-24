
package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
/**
 * The Bcc instructions, excluding BRA, BSR.
 * 
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 * 
 */
public class Bcc implements InstructionSet
{
    protected static final String[] names = {   "bra", "bsr", "bhi", "bls", "bcc", "bcs", "bne", "beq",
                                                "bvc", "bvs", "bpl", "bmi", "bge", "blt", "bgt", "ble"};
    
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base = 0x6000;
        smsqmulator.cpu.Instruction ib = new smsqmulator.cpu.Instruction() 
        {  
            private static final int C_FLAG = 1;
            private static final int V_FLAG = 2;
            private static final int Z_FLAG = 4;
            private static final int N_FLAG = 8;
            private static final int C_Z_FLAGS = C_FLAG | Z_FLAG;
            private static final int N_V_FLAGS = N_FLAG | V_FLAG;
            private static final int N_V_Z_FLAGS = N_FLAG | V_FLAG | Z_FLAG;
   
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                
                int ccr=cpu.reg_sr;
                switch((opcode >> 8) & 0x0f)
		{
                    case 2:		//HI:
                    {
                        if( (ccr & C_Z_FLAGS) == 0)
                            break;
                        return;
                    }
                    case 3:		//LS:
                    {
                        if ((ccr & C_Z_FLAGS) != 0)
                            break;
                        return;
                    }
                    case 4:		//CC:
                    {
                        if ((ccr & C_FLAG) == 0)
                            break;
                        return;
                    }
                    case 5:		//CS:
                    {
                        if ((ccr & C_FLAG) != 0)
                            break;
                        return;
                    }
                    case 6:		//NE:
                    {
                        if((ccr & Z_FLAG) == 0)
                            break;
                        return;
                    }
                    case 7:		//EQ:
                    {
                        if((ccr & Z_FLAG) != 0)
                            break;
                        return;
                    }
                    case 8:		//VC:
                    {
                        if((ccr & V_FLAG) == 0)
                            break;
                        return;
                    }
                    case 9:		//VS:
                    {
                        if ((ccr & V_FLAG) != 0)
                            break;
                        return;
                    }
                    case 10:	//PL:
                    {
                        if ((ccr & N_FLAG) == 0)
                            break;
                        return;
                    }
                    case 11:	//MI:
                    {
                        if ((ccr & N_FLAG) != 0)
                            break;
                        return;
                    }
                    case 12:	//GE:
                    {
                        int v = ccr & N_V_FLAGS;
                        if(v == 0 || v == N_V_FLAGS)
                            break;
                        return;
                    }
                    case 13:	//LT:
                    {
                        int v = ccr & N_V_FLAGS;
                        if (v == N_FLAG || v == V_FLAG)
                            break;
                        return;
                    }
                    case 14:	//GT:
                    {
                        int v = ccr & N_V_Z_FLAGS;
                        if(v == 0 || v == N_V_FLAGS)
                            break;
                        return;
                    }
                    case 15:	//LE:
                    {
                        int v = ccr & N_V_Z_FLAGS;
                        if ((v & Z_FLAG) != 0 || (v == N_FLAG) || (v == V_FLAG))
                            break;
                        return;
                    }
                }
                int dis = (opcode & 0xff);
                if ((dis&0x80) !=0)
                   // dis|=0xffffff00;
                    dis-=256;
                cpu.pc_reg +=(dis/2);
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                    return disassembleOp(address, opcode,cpu);
            }
        };

        smsqmulator.cpu.Instruction iw = new smsqmulator.cpu.Instruction() 
        {
            private static final int C_FLAG = 1;
            private static final int V_FLAG = 2;
            private static final int Z_FLAG = 4;
            private static final int N_FLAG = 8;
            private static final int C_Z_FLAGS = C_FLAG | Z_FLAG;
            private static final int N_V_FLAGS = N_FLAG | V_FLAG;
            private static final int N_V_Z_FLAGS = N_FLAG | V_FLAG | Z_FLAG;
   
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int ccr=cpu.reg_sr;
                int dis=cpu.readMemoryWordPCSigned();
                switch((opcode >> 8) & 0x0f)
		{
                    case 2:		//HI:
                    {
                        if( (ccr & C_Z_FLAGS) == 0)
                        {
                            cpu.pc_reg += (dis/2);
                        }
                        else
                        {
                            cpu.pc_reg ++;
                        }
                        return;
                    }
                    case 3:		//LS:
                    {
                        if ((ccr & C_Z_FLAGS) != 0)
                        {
                            cpu.pc_reg += (dis/2);
                        }
                        else
                        {
                            cpu.pc_reg ++;
                        }
                        return;
                    }
                    case 4:		//CC:
                    {
                        if ((ccr & C_FLAG) == 0)
                        {
                            cpu.pc_reg += (dis/2);
                        }
                        else
                        {
                            cpu.pc_reg ++;
                        }
                        return;
                    }
                    case 5:		//CS:
                    {
                        if ((ccr & C_FLAG) != 0)
                        {
                            cpu.pc_reg += (dis/2);
                        }
                        else
                        {
                            cpu.pc_reg ++;
                        }
                        return;
                    }
                    case 6:		//NE:
                    {
                        if((ccr & Z_FLAG) == 0)
                        {
                            cpu.pc_reg += (dis/2);
                        }
                        else
                        {
                            cpu.pc_reg ++;
                        }
                        return;
                    }
                    case 7:		//EQ:
                    {
                        if((ccr & Z_FLAG) != 0)
                        {
                            cpu.pc_reg += (dis/2);
                        }
                        else
                        {
                            cpu.pc_reg ++;
                        }
                        return;
                    }
                    case 8:		//VC:
                    {
                        if((ccr & V_FLAG) == 0)
                        {
                            cpu.pc_reg += (dis/2);
                        }
                        else
                        {
                            cpu.pc_reg ++;
                        }
                        return;
                    }
                    case 9:		//VS:
                    {
                        if ((ccr & V_FLAG) != 0)
                        {
                            cpu.pc_reg += (dis/2);
                        }
                        else
                        {
                            cpu.pc_reg ++;
                        }
                        return;
                    }
                    case 10:	//PL:
                    {
                        if ((ccr & N_FLAG) == 0)
                        {
                            cpu.pc_reg += (dis/2);
                        }
                        else
                        {
                            cpu.pc_reg ++;
                        }
                        return;
                    }
                    case 11:	//MI:
                    {
                        if ((ccr & N_FLAG) != 0)
                        {
                            cpu.pc_reg += (dis/2);
                        }
                        else
                        {
                            cpu.pc_reg ++;
                        }
                        return;
                    }
                    case 12:	//GE:
                    {
                       // return (ccr & N_FLAG) == (ccr & V_FLAG);
                        int v = ccr & N_V_FLAGS;
                        if(v == 0 || v == N_V_FLAGS)
                        {
                            cpu.pc_reg += (dis/2);
                        }
                        else
                        {
                            cpu.pc_reg ++;
                        }
                        return;
                    }
                    case 13:	//LT:
                    {
                        int v = ccr & N_V_FLAGS;
                        if (v == N_FLAG || v == V_FLAG)
                        //if ((ccr & N_V_FLAGS)!=0)
                        {
                            cpu.pc_reg += (dis/2);
                        }
                        else
                        {
                            cpu.pc_reg ++;
                        }
                        return;
                    }
                    case 14:	//GT:
                    {
                        int v = ccr & N_V_Z_FLAGS;
                        if(v == 0 || v == N_V_FLAGS)
                        {
                            cpu.pc_reg += (dis/2);
                        }
                        else
                        {
                            cpu.pc_reg ++;
                        }
                        return;
                    }
                    case 15:	//LE:
                    {
                        int v = ccr & N_V_Z_FLAGS;
                        if ((v & Z_FLAG) != 0 || (v == N_FLAG) || (v == V_FLAG))
                        {
                            cpu.pc_reg += (dis/2);
                        }
                        else
                        {
                            cpu.pc_reg ++;
                        }
                        return;
                    }
                }
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                    return disassembleOp(address, opcode,cpu);
            }
        };

        for(int cc = 2; cc < 16; cc++)
        {
            cpu2.addInstruction(base + (cc << 8), iw);
            for(int dis = 1; dis < 256; dis++)
            {
                cpu2.addInstruction(base + (cc << 8) + dis, ib);
            }
        }
    }

    protected final DisassembledInstruction disassembleOp(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
    {
            DisassembledOperand op;
            int cc = (opcode >> 8) & 0x0f;
            int dis = opcode & 0xff;
            if ((dis&0x80) !=0)
                dis|=0xffffff00;
            String name;

            if(dis != 0)
            {
                op = new DisassembledOperand(String.format("$%08x", dis + address + 2));
                name = names[cc] + ".s";
            }
            else
            {
                //word displacement
                dis = cpu.readMemoryWordSigned(address + 2);
                op = new DisassembledOperand(String.format("$%08x", dis + address + 2), 2, dis);
                name = names[cc] + ".w";
            }
            return new DisassembledInstruction(address, opcode, name, op);
    }
}
