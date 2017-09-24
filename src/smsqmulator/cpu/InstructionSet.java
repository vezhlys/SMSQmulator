package smsqmulator.cpu;

/**
 * Interface for InstructionSets, they must be able to register.
 * 
 * @author wolf
 */
public interface InstructionSet
{   
    /**
     * Registers the InstructionSet with the cpu.
     * @param cpu the cpu to use.
     */
    public void register (smsqmulator.cpu.MC68000Cpu cpu);
}
