package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The ORI_TO_CCR instruction in all of its variants.
 *  fedcba9876543210
 *  003c
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 */
public class ORI_CCR implements InstructionSet
{
    @Override
    public final void register( final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i= new smsqmulator.cpu.Instruction()   
        {
            
            
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int val=cpu.readMemoryWordPCInc()&0x1f;
                cpu.reg_sr|=val;    
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int imm_bytes;
		int imm;
		String is;
		imm = cpu.readMemoryWord(address + 2);
                is = String.format("#$%04x", imm);
                imm_bytes = 2;
		DisassembledOperand src = new DisassembledOperand(is, imm_bytes, imm);
		DisassembledOperand dst = cpu.disassembleDstEA(address + 2 + imm_bytes, (opcode >> 3) & 0x07, (opcode & 0x07), Size.Byte);
		return new DisassembledInstruction(address, opcode, "ori" + Size.Word.ext(), src, dst);
            }   
        };
        cpu2.addInstruction(0x003c,i);
    }
}
