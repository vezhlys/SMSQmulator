package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The CHK instruction in all of its variants.
 * 
 * 0100ddd110mmmrrr = 4180
 *  where   rrr = eareg 
 *          mmm = mode
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 * 
 */
public class CHK implements InstructionSet
{
	
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i;
       
        base = 0x4180;
        i = new smsqmulator.cpu.Instruction()   
        {
            
            
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                cpu.reg_sr&=0xfff0;                        // X flag unaffected
                int val=cpu.data_regs[(opcode>>9)&7]&0xffff;
                if ((val&0x8000)!=0)                            // value is <0
                {
                    cpu.reg_sr|=8;                         // set N flag
                    cpu.raiseException(6);                 // raise the exception
                }
                int address=0;
                int reg=opcode&7;                  
                switch ((opcode >>3)&7)
                {
                    case 0:                                     // Dn
                        int comp=cpu.data_regs[reg]&0xffff;
                        if (((comp&0x8000)!=0)|| val>comp) 
                            cpu.raiseException(6);                 // raise the exception
                        return;

                    case 2:                                     //(an)
                        address=cpu.addr_regs[reg];
                        break;
                    case 3:                
                        address=cpu.addr_regs[reg];
                        cpu.addr_regs[reg]+=2;
                        break;
                    case 4:                                     // -(an)
                        cpu.addr_regs[reg]-=2;
                        address=cpu.addr_regs[reg];
                        break;                                  
                      case 5:                                     // d16(an)
                        address=cpu.addr_regs[reg] + cpu.readMemoryWordPCSignedInc();
                        break;
                    case 6:                                     // d8(an,xn)
                        address=getDisplacement(cpu)+cpu.addr_regs[reg];
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
                
                int comp=cpu.readMemoryWordSigned(address);
                if ((val&0x8000)!=0)
                    val|=0xffff0000;
                if (val>comp) 
                    cpu.raiseException(6);                 // raise the exception
                ;
            }
            
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
		DisassembledOperand src = new DisassembledOperand("d" + ((opcode >> 9) & 0x07));
		DisassembledOperand dst = cpu.disassembleDstEA(address + 2, (opcode >> 3) & 0x07, (opcode & 0x07), Size.Word);
		return new DisassembledInstruction(address, opcode, "chk", src, dst);
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
        for (int valr=0;valr<8;valr++)
        {
            int val=valr<<9;
            for (int mode=0;mode<8;mode++)
            {
                if (mode==1)
                    continue;
                int rmode=mode<<3;
                for (int reg=0;reg<8;reg++)
                {
                    if (mode==7 & reg>4)
                        break;
                    cpu2.addInstruction(base+ val+ rmode + reg, i);
                }
            }
        }
    }
}

