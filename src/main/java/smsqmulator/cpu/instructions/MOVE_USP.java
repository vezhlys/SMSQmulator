
package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;

/**
 * A redefinition of the MOVE TO/FROM USP instructions.
 
 * @author and copyright (c) 2012 wolfgang lenerz for my code
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class MOVE_USP implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction ins;
        for (int i=0;i<2;i++)
        {
            if (i==0)
            {
                base=0x4e60;                                    // move to usp
                ins = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        if(!cpu.isSupervisorMode())
                            cpu.raiseSRException();
                        else
                            cpu.reg_usp = cpu.addr_regs[opcode&7];
                    }
                    
                   public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {   
                        DisassembledOperand src = new DisassembledOperand("a" + (opcode & 0x07));
			DisassembledOperand dst = new DisassembledOperand("usp");
                        return new DisassembledInstruction(address, opcode, "move", src, dst);
                    }   
                };
            }
            else
            {
                base=0x4e68;                                    // move from usp
                ins = new smsqmulator.cpu.Instruction()   
                {
                    
                    
                    @Override
                    public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {
                        if(!cpu.isSupervisorMode())
                            cpu.raiseSRException();
                        else
                            cpu.addr_regs[opcode&7]=cpu.reg_usp;
                    }
                    
                   public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
                    {   
                        DisassembledOperand dst = new DisassembledOperand("a" + (opcode & 0x07));
			DisassembledOperand src = new DisassembledOperand("usp");
                        return new DisassembledInstruction(address, opcode, "move", src, dst);
                    }   
                };
            }
            for (int reg=0;reg<8;reg++)
                cpu2.addInstruction(base+reg,ins);
        }
    }
}
