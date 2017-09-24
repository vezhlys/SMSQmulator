package smsqmulator.cpu;

public interface Instruction
{
    public void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu);
    public smsqmulator.cpu.DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu);
}
