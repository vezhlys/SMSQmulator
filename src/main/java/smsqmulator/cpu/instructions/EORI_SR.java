package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;
import smsqmulator.cpu.Size;

/**
 * The EORI_TO_SR instruction in all of its variants.
 *  fedcba9876543210
 *  a7c
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 * 1.01 use Types.SRMask.
 * 1.00 initial version
 */
public class EORI_SR implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base;
        smsqmulator.cpu.Instruction i= new smsqmulator.cpu.Instruction()   
        {
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int val=cpu.readMemoryWordPCInc()&smsqmulator.Types.SRMask;// mask out bits 5,6,7 & 11 : they are always 0!
                if((cpu.reg_sr&0x2000)!=0) 
                {
                    val ^= cpu.reg_sr; 
                    val&=smsqmulator.Types.SRMask;
                    cpu.setSR(val);
                }
                else
                    cpu.raiseSRException();
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
		DisassembledOperand dst = cpu.disassembleDstEA(address + 2 + imm_bytes, (opcode >> 3) & 0x07, (opcode & 0x07), Size.Word);
		return new DisassembledInstruction(address, opcode, "eori" + Size.Word.ext(), src, dst);
            }   
        };
        cpu2.addInstruction(0xa7c,i);
    }
}
