import littlecube.bitutil.*;
import littlecube.unsigned.*;

public class Timer
{
	UnsignedShort div;
	UnsignedByte tima;
	UnsignedByte tma;
	UnsignedByte tac;
	
	int lastAND;
	
	int remainingClocks;
	
	public Timer()
	{
		init();
	}
	
	public void init()
	{
		div = new UnsignedShort();
		tima = new UnsignedByte();
		tma = new UnsignedByte();
		tac = new UnsignedByte();
		
		lastAND = -1;
		
		remainingClocks = 0;
	}
	
	public void clock(int clocks)
	{
		
		
		if (div.get() + clocks < 0xFF)
		{
			clocks = overflow(clocks);
		}
		
		for (int i = 0; i < clocks; i++)
		{
			div.add(1);
			
			int andResult = tac.getBit(2) & div.getBit(TACVal());
			
			if (lastAND == 1 && andResult == 0)
			{
				tima.add(1);
			}
			
			lastAND = andResult;
		}
	}
	
	int overflow(int clocks)
	{
		remainingClocks = 4;
		
		div.set(0x00);
		
		while (clocks > 0)
		{
			remainingClocks--;
			clocks--;
		}
		
		return clocks;
	}
	
	void remainingClocks(int clocks)
	{
		
	}
	
	void writeDIV()
	{
		GB.cpu.mmu.write(0xFF04, div.subByte(1));
	}
	
	void writeTIMA()
	{
		GB.cpu.mmu.write(0xFF05, tima.get());
	}
	
	void readTMA()
	{
		tma.set(GB.cpu.mmu.read(0xFF06));
	}
	
	void readTAC()
	{
		tac.set(GB.cpu.mmu.read(0xFF07));
	}
	
	int TACVal()
	{
		UnsignedByte tacval = new UnsignedByte();
		
		tacval.setBit(0, tac.getBit(0));
		tacval.setBit(1, tac.getBit(1));
		
		int returnBit = 0;
		
		switch (tacval.get())
		{
			case 0:
			{
				returnBit = 9;
			}
			
			case 1:
			{
				returnBit = 3;
			}
			
			case 2:
			{
				returnBit = 5;
			}
			
			case 3:
			{
				returnBit = 7;
			}
		}
		
		return returnBit;
	}
}