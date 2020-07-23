import littlecube.bitutil.*;
import littlecube.unsigned.*;

public class Timer
{
	UnsignedShort div;
	UnsignedByte tima;
	UnsignedByte tma;
	UnsignedByte tac;
	
	static final int DIV = 0xFF04;
	static final int TIMA = 0xFF05;
	static final int TMA = 0xFF06;
	static final int TAC = 0xFF07;
	
	int lastAND;
	
	int lastTIMA;
	
	int clocks;
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
		
		lastTIMA = -1;
		
		clocks = 0;
		waitClocks = -1;
	}
	
	public void clock(int newClocks)
	{
		clocks += newClocks;
		
		readTIMA();
		readTMA();
		readTAC();
		
		if (waitClocks == -1)
		{
			while (clocks > 0)
			{
				div.add(1);
				writeDIV();
				
				int andResult = tac.getBit(2) & div.getBit(TACVal());
				
				if (lastAND == 1 && andResult == 0)
				{
					if ((tima.get() + 1) > 0xFF)
					{
						overflow();
						waitClocks();
						
						if (clocks > 0)
						{
							continue;
						}
						
						else
						{
							return;
						}
					}
					
					tima.add(1);
					writeTIMA();
				}
				
				lastAND = andResult;
				
				clocks--;
			}
		}
		
		else if (waitClocks == 0)
		{
			GB.cpu.mmu.writeBit(CPU.IF, 2, 1);
			
			tima.set(tma.get());
			writeTIMA();
			
			waitClocks = -1;
		}
		
		else if (waitClocks > 0)
		{
			waitClocks();
			
			if (clocks > 0)
			{
				clock(0);
			}
		}
	}
	
	void overflow()
	{
		waitClocks = 4;
		
		tima.set(0x00);
		writeTIMA();
	}
	
	void waitClocks()
	{
		while (waitClocks > 0 && clocks > 0)
		{
			div.add(1);
			writeDIV();
			
			waitClocks--;
			clocks--;
		}
	}
	
	void writeDIV()
	{
		GB.cpu.mmu.write(0xFF04, div.subByte(1));
	}
	
	void readTIMA()
	{
		tima.set(GB.cpu.memory[0xFF05].get());
	}
	
	void writeTIMA()
	{
		GB.cpu.mmu.write(0xFF05, tima.get());
	}
	
	void readTMA()
	{
		tma.set(GB.cpu.memory[0xFF06].get());
	}
	
	void readTAC()
	{
		tac.set(GB.cpu.memory[0xFF07].get());
	}
	
	int TACVal()
	{
		UnsignedByte tacval = new UnsignedByte();
		
		tacval.set(tac.get() & 0x03);
		
		int retPos = 0;
		
		switch (tacval.get())
		{
			case 0:
			{
				retPos = 9;
				break;
			}
			
			case 1:
			{
				retPos = 3;
				break;
			}
			
			case 2:
			{
				retPos = 5;
				break;
			}
			
			case 3:
			{
				retPos = 7;
				break;
			}
		}
		
		return retPos;
	}
}