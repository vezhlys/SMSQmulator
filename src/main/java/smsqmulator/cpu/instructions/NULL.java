package smsqmulator.cpu.instructions;
import smsqmulator.cpu.Instruction;
import smsqmulator.cpu.DisassembledInstruction;

/**
 * This is a "null" instruction - actually means that an illegal opcode will be "executed".
 * The purpose of this is to fill all remaining instructions in the CPU(s opcode table with this one.
 * Then, when this is called it means that there is an instruction that wasn't emulated yet.
 * 
 * This is better than to test at every loop iteration during program execution whether an opcode is null or not.
 * 
 * @author and copyright (c) 2012 wolfgang lenerz
 */
public class NULL
{
    /**
     * This registers this instruction for all null elements in the table of instructions.
     * 
     * @param mcpu  the cpu used.
     * 
     * @param i_table the table to register with.
     */
    public final void register(smsqmulator.cpu.Instruction[] i_table,final smsqmulator.cpu.MC68000Cpu mcpu)
    {
        Instruction inst = new Instruction() 
        {
            
            protected final smsqmulator.cpu.MC68000Cpu cpu=mcpu;
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                System.err.format("Illegal (NULL) Instruction $%04x at $%x\n", opcode, cpu.pc_reg*2);
                cpu.raiseException(4);
                cpu.stopNow=-20;
            }
            
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                   return new DisassembledInstruction(address, opcode, "NULL");
            }
        };
        
        for (int i=0;i<i_table.length;i++)
            if (i_table[i]==null)
                i_table[i]=inst;
    }
}
