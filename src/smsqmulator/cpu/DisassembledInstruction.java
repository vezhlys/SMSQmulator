package smsqmulator.cpu;

/*
//  M68k - Java Amiga MachineCore
//  Copyright (c) 2008-2010, Tony Headford
//  All rights reserved.
//
//  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
//  following conditions are met:
//
//    o  Redistributions of source code must retain the above copyright notice, this list of conditions and the
//       following disclaimer.
//    o  Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
//       following disclaimer in the documentation and/or other materials provided with the distribution.
//    o  Neither the name of the M68k Project nor the names of its contributors may be used to endorse or promote
//       products derived from this software without specific prior written permission.
//
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
//  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
//  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
//  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
//  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
//  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
//  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
*/
/**
 * 
 * @author wolf
 */
public class DisassembledInstruction
{
    public final int address;
    public final int opcode;
    public final int num_operands;
    public final String instruction;
    private int size;

    public final DisassembledOperand op1;
    public final DisassembledOperand op2;

    // no operands
    /**
     * 
     * @param address where to start disassembling.
     * @param opcode the opcode to disassemble.
     * @param instruction the instruction string corresponding to the opcode.
     */
    public DisassembledInstruction(int address, int opcode, String instruction)
    {
        this.address = address;
	this.opcode = opcode;
	this.instruction = instruction;
	num_operands = 0;
	op1 = null;
	op2 = null;
        this.size=2;
    }

	// one operand
    public DisassembledInstruction(int address, int opcode, String instruction, DisassembledOperand dop)
    {
	this.address = address;
	this.opcode = opcode;
	this.instruction = instruction;
	num_operands = 1;
	op1 = dop;
	op2 = null;
        this.size=this.op1.bytes+2;
    }

	// two operands
    public DisassembledInstruction(int address, int opcode, String instruction, DisassembledOperand dop1, DisassembledOperand dop2)
    {
	this.address = address;
	this.opcode = opcode;
	this.instruction = instruction;
	num_operands = 2;
	op1 = dop1;
	op2 = dop2;
        this.size=this.op1.bytes+2+this.op2.bytes;
        
    }

    public int size()
    {
    	return this.size;
    }

    public void shortFormat(StringBuilder buffer)
    {       
	buffer.append(String.format("%08x   ", address));
        buffer.append(instruction);
        switch(num_operands)
        {
            case 1:
            {
                int ilen = instruction.length();
                while(ilen < 9)
                {
                    buffer.append(" ");
                    ilen++;
                }
                buffer.append(op1.operand);
                break;
            }
            case 2:
            {
                int ilen = instruction.length();
                while(ilen < 9)
                {
                    buffer.append(" ");
                    ilen++;
                }
                buffer.append(op1.operand).append(",").append(op2.operand);
                break;
            }
        }
    }

	public void formatInstruction(StringBuilder buffer)
	{
		buffer.append(String.format("%08x   %04x", address, opcode));

		switch(num_operands)
		{
			case 0:
			{
				// 20 spaces
				buffer.append("                    ").append(instruction);
				break;
			}
			case 1:
			{
				if(op1.bytes == 2)
				{
					buffer.append(String.format(" %04x               ", op1.memory_read));
				}
				else if(op1.bytes == 4)
				{
					buffer.append(String.format(" %08x           ", op1.memory_read));
				}
				else
				{
					// 20 spaces
					buffer.append("                    ");
				}

				int ilen = instruction.length();
				buffer.append(instruction);
				while(ilen < 9)
				{
					buffer.append(" ");
					ilen++;
				}
				buffer.append(op1.operand);
				break;
			}
			case 2:
			{
				int len = 0;

				if(op1.bytes == 2)
				{
					buffer.append(String.format(" %04x", op1.memory_read));
					len += 5;
				}
				else if(op1.bytes == 4)
				{
					buffer.append(String.format(" %08x", op1.memory_read));
					len += 9;
				}

				if(op2.bytes == 2)
				{
					buffer.append(String.format(" %04x", op2.memory_read));
					len += 5;
				}
				else if(op2.bytes == 4)
				{
					buffer.append(String.format(" %08x", op2.memory_read));
					len += 9;
				}

				while(len < 21)
				{
					buffer.append(" ");
					len++;
				}
				int ilen = instruction.length();
				buffer.append(instruction);
				while(ilen < 9)
				{
					buffer.append(" ");
					ilen++;
				}

				buffer.append(op1.operand).append(",").append(op2.operand);
				break;
			}
		}
	}

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder(80);
        formatInstruction(buffer);
        return buffer.toString();
    }
}
