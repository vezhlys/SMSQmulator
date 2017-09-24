package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The MOVE_TO_CCR instruction in all of its variants.
 * 
 * 0100010011mmmrrr = 44c0
 *  where   rrr = eareg 
 *          mmm = mode
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 * v. 1.01  d16PC corrected
 */
public class MOVE_TO_CCR implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;
       
        base = 0x44c0;
        i = new smsqmulator.cpu.Instruction()   
        {
            
            
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int address=0;
                cpu.reg_sr&=0xffe0;           
                switch ((opcode >>3)&7)
                {
                    case 0:                                     // Dn
                        int val=cpu.data_regs[opcode&7]&0x1f;
                        cpu.reg_sr|=val;
                        return;

                    case 2:                                     //(an)
                        address=cpu.addr_regs[opcode&7];
                        break;
                    case 3:                
                        address=cpu.addr_regs[opcode&7];
                        cpu.addr_regs[opcode&7]+=2;
                        break;
                    case 4:                                     // -(an)
                        cpu.addr_regs[opcode&7]-=2;
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
                            cpu.pc_reg+=2;
                            break;
                        case 2:                             // d16PC      
                            address=cpu.pc_reg*2 + cpu.readMemoryWordPCSignedInc();// 
                            break;
                        case 3:
                            address= cpu.pc_reg*2+getDisplacement(cpu);
                            break;
                        case 4:                             // data
                            address=cpu.pc_reg*2;
                            cpu.pc_reg++;
                            break;
                        }
                        break;
                }
                
                int val=cpu.readMemoryWordSigned(address)&0x1f;
                cpu.reg_sr|=val;
                ;
            }
            
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {	
		DisassembledOperand src = cpu.disassembleSrcEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), Size.Word);
		DisassembledOperand dst = new DisassembledOperand("ccr");
		return new DisassembledInstruction(address, opcode, "move", src, dst);
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
        for (int mode=0;mode<8;mode++)
        {
            if (mode==1)
                continue;
            int rmode=mode<<3;
            for (int reg=0;reg<8;reg++)
            {
                if (mode==7 & reg>4)
                    break;
                cpu2.addInstruction(base+ rmode + reg, i);
            }
        }
    }
}
