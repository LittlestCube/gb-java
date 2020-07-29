import littlecube.bitutil.*;
import littlecube.unsigned.*;

import java.util.Random;

public class MMU
{
	public MMU()
	{
		resetMemory();
	}
	
	public void initValues()
	{
		write(CPU.IE, 0x00);
		write(CPU.IF, 0x00);
		
		write(Timer.DIV, 0x00);
		write(Timer.TIMA, 0x00);
		write(Timer.TMA, 0x00);
		write(Timer.TAC, 0x00);
	}
	
	public void resetMemory()
	{
		GB.cpu.memory = new UnsignedByte[0x10000];
		
		Random rand = new Random();
		
		for (int i = 0; i < GB.cpu.memory.length; i++)
		{
			GB.cpu.memory[i] = new UnsignedByte(rand.nextInt());
		}
		
		initValues();
	}
	
	public UnsignedByte read(int offset)
	{
		UnsignedByte value = new UnsignedByte();
		
		value.set(GB.cpu.memory[offset]);
		
		value = retainConstants(offset, value);
		
		return value;
	}
	
	public void write(int offset, int value)
	{
		writeEffects(offset, value);
		
		GB.cpu.memory[offset].set(value);
	}
	
	public void writeBit(int offset, int position, int value)
	{
		UnsignedByte ub = new UnsignedByte();
		
		ub.set(read(offset));
		
		ub.setBit(position, value);
		
		write(offset, ub.get());
	}
	
	UnsignedByte retainConstants(int offset, UnsignedByte value)
	{
		switch (offset)
		{
			case 0xFF02:
			{
				value.setBit(1, 1);
				value.setBit(2, 1);
				value.setBit(3, 1);
				value.setBit(4, 1);
				value.setBit(5, 1);
				value.setBit(6, 1);
				break;
			}
			
			case CPU.IE:
			
			case CPU.IF:
			{
				value.setBit(5, 1);
				value.setBit(6, 1);
				value.setBit(7, 1);
				break;
			}
		}
		
		return value;
	}
	
	void writeEffects(int offset, int value)
	{
		switch (offset)
		{
			case 0xFF04:
			{
				value = 0;
			}
		}
	}
}