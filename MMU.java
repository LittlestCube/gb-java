import littlecube.bitutil.*;
import littlecube.unsigned.*;

import java.util.Random;

public class MMU
{
	public MMU()
	{
		resetMemory();
	}
	
	public void resetMemory()
	{
		GB.cpu.memory = new UnsignedByte[0x10000];
		
		Random rand = new Random();
		
		for (int i = 0; i < GB.cpu.memory.length; i++)
		{
			GB.cpu.memory[i] = new UnsignedByte(rand.nextInt());
		}
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
		GB.cpu.memory[offset].set(value);
	}
	
	public void writeBit(int offset, int position, int value)
	{
		GB.cpu.memory[offset].setBit(position, value);
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
			
			case 0xFFFF:
			
			case 0xFF0F:
			{
				value.setBit(5, 1);
				value.setBit(6, 1);
				value.setBit(7, 1);
				break;
			}
		}
		
		return value;
	}
}