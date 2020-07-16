import littlecube.bitutil.*;
import littlecube.unsigned.*;

public class Timer
{
	UnsignedShort div;
	UnsignedByte tima;
	UnsignedByte tma;
	UnsignedByte tac;
	
	int lastAND;
	
	int waitClocks;
	
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
		
		waitClocks = 0;
	}
	
	public void clock(int clocks)
	{
		TMA();
		TAC();
		
		clocks = waitClocks(clocks);
		
		if (waitClocks == 0)
		{
			TIMA(tma.get());
			waitClocks = -1;
			GB.cpu.mmu.writeBit(0xFF0F, 2, 1);
		}
		
		if (waitClocks == -1)
		{
			if (div.get() + clocks < 0xFF)
			{
				clocks = overflow(clocks);
			}
			
			for (int i = 0; i < clocks; i++)
			{
				DIV(div.get() + 1);
				
				int andResult = tac.getBit(2) & div.getBit(TACVal());
				
				if (lastAND == 1 && andResult == 0)
				{
					TIMA(tima.get() + 1);
				}
				
				lastAND = andResult;
			}
		}
	}
	
	int overflow(int clocks)
	{
		waitClocks = 4;
		
		div.set(0x00);
		
		clocks = waitClocks(clocks);
		
		return clocks;
	}
	
	int waitClocks(int clocks)
	{
		while (waitClocks > 0 && clocks > 0)
		{
			waitClocks--;
			clocks--;
		}
		
		return clocks;
	}
	
	void DIV(int newDiv)
	{
		div.set(newDiv);
		GB.cpu.mmu.write(0xFF04, div.subByte(1));
	}
	
	void TIMA(int newTima)
	{
		tima.set(newTima);
		GB.cpu.mmu.write(0xFF05, tima.get());
	}
	
	void TMA()
	{
		tma.set(GB.cpu.mmu.read(0xFF06));
	}
	
	void TAC()
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