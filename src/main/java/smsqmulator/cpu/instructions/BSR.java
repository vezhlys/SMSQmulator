package smsqmulator.cpu.instructions;
import smsqmulator.cpu.DisassembledOperand;
import smsqmulator.cpu.InstructionSet;
import smsqmulator.cpu.DisassembledInstruction;

/**
 * The emulated BSR instruction in all of its variants.
 * 
 * @author and Copyright (c) for my code Wolfgang Lenerz 2013-2014
 * 
 * Based on code by Tony Headford, see his licence in accompanying file.
 * 
 */
public class BSR implements InstructionSet
{
    @Override
    public final void register(final smsqmulator.cpu.MC68000Cpu cpu2)
    {
        int base = 0x6100;
        smsqmulator.cpu.Instruction ib = new smsqmulator.cpu.Instruction() 
        {
            
            
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                int dis = (opcode & 0xff);
                if ((dis&0x80) !=0)
                    dis|=0xffffff00;
                cpu.addr_regs[7] -= 4;
                cpu.writeMemoryLong(cpu.addr_regs[7],cpu.pc_reg*2);
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
            
           
            @Override
            public final void execute(int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                cpu.addr_regs[7] -= 4;
                cpu.writeMemoryLong(cpu.addr_regs[7],(cpu.pc_reg+1)*2);
                cpu.pc_reg +=(cpu.readMemoryWordPCSignedInc()/2);
            }
            @Override
            public DisassembledInstruction disassemble(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
            {
                    return disassembleOp(address, opcode,cpu);
            }
        };
        cpu2.addInstruction(base, iw);
        for(int dis = 1; dis < 256; dis++)
        {
            cpu2.addInstruction(base + dis, ib);
        }
    }

    public DisassembledInstruction disassembleOp(int address, int opcode,smsqmulator.cpu.MC68000Cpu cpu)
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
                name =  "bsr.s";
        }
        else
        {
                //word displacement
                dis = cpu.readMemoryWordSigned(address + 2);
                op = new DisassembledOperand(String.format("$%08x", dis + address + 2), 2, dis);
                name = "bsr.w";
        }
        return new DisassembledInstruction(address, opcode, name, op);
    }
}
