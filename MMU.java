import littlecube.bitutil.*;
import littlecube.unsigned.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MMU
{
	List<Integer> lockedRanges;
	
	boolean dma;
	
	int dmaWaitClocks;
	int dmaClocks;
	int dmaOffset;
	int dmaInt;
	
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
		
		lockedRanges = new ArrayList<Integer>();
		
		dma = false;
		
		dmaWaitClocks = -1;
		dmaClocks = -1;
		dmaOffset = -1;
		dmaInt = -1;
		
		initValues();
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
	
	public UnsignedByte read(int offset)
	{
		UnsignedByte value = readNoLock(offset);
		
		if (checkLock(offset))
		{
			value.set(0xFF);
		}
		
		return value;
	}
	
	public UnsignedByte readNoLock(int offset)
	{
		UnsignedByte value = new UnsignedByte();
		
		value.set(GB.cpu.memory[offset]);
		
		readEffects(offset, value);
		
		return value;
	}
	
	public void write(int offset, int value)
	{
		writeEffects(offset, value);
		
		if (!checkLock(offset))
		{
			GB.cpu.memory[offset].set(value);
		}
	}
	
	public void writeBit(int offset, int position, int value)
	{
		UnsignedByte ub = new UnsignedByte();
		
		ub.set(read(offset));
		
		ub.setBit(position, value);
		
		write(offset, ub.get());
	}
	
	void readEffects(int offset, UnsignedByte value)
	{
		if (offset >= 0xE000 && offset <= 0xFDFF)
		{
			value.set(GB.cpu.memory[offset - 0x2000]);
		}
		
		switch (offset)
		{
			case 0xFF00:
			{
				value.setBit(7, 1);
				value.setBit(6, 1);
				break;
			}
			
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
	}
	
	void writeEffects(int offset, int value)
	{
		if (offset >= 0x0000 && offset < 0x8000)
		{
			value = GB.cpu.memory[offset].get();
		}
		
		if (offset >= 0x8000 && offset < 0x9800 && GB.tile)
		{
			GB.ppu.updateTileWindow();
		}
		
		if (offset >= 0x9800 && offset < 0xA000 && GB.map)
		{
			GB.ppu.updateBGMWindow();
		}
		
		switch (offset)
		{
			case 0xFF00:
			{
				//GB.joypad.status();
				
				break;
			}
			
			case 0xFF04:
			{
				value = 0;
				break;
			}
			
			case 0xFF44:
			{
				value = GB.cpu.memory[0xFF44].get();
				break;
			}
			
			case 0xFF46:
			{
				if (value <= 0xF1)
				{
					dma(value * 0x100);
				}
				
				break;
			}
			
			case 0xFF50:
			{
				GB.cpu.replaceBIOS();
				break;
			}
		}
	}
	
	void dma(int offset)
	{
		dma = true;
		
		dmaWaitClocks = 4;
		dmaClocks = 0;
		dmaOffset = offset;
		dmaInt = 0;
	}
	
	void dmaClock(int newClocks)
	{
		dmaClocks += newClocks / 4;
		
		while (dmaInt < 0xA0 && dmaClocks > 0)
		{
			if (dmaWaitClocks > 0)
			{
				dmaWaitClocks--;
			}
			
			else
			{
				GB.cpu.memory[0xFE00 + dmaInt].set(GB.cpu.mmu.readNoLock(dmaOffset + dmaInt));
				
				dmaInt++;
			}
		}
		
		if (dmaInt == 0x9F)
		{
			dmaClocks = -1;
			dmaOffset = -1;
			dmaInt = -1;
			
			unlockRange(0x0000, 0xFF7F);
			unlockRange(0xFFFF);
		}
	}
	
	boolean checkLock(int offset)
	{
		boolean locked = false;
		
		if (lockedRanges.size() > 0)
		{
			for (int i = 0; (i < lockedRanges.size() / 2) && (!locked); i++)
			{
				if ((offset >= lockedRanges.get(i * 2)) && (offset <= lockedRanges.get((i * 2) + 1)))
				{
					locked = true;
				}
			}
		}
		
		return locked;
	}
	
	void lockRange(int from, int to)
	{
		lockedRanges.add(from);
		lockedRanges.add(to);
	}
	
	void lockRange(int offset)
	{
		lockRange(offset, offset);
	}
	
	void unlockRange(int from, int to)
	{
		boolean exists = false;
		
		for (int i = 0; (i < lockedRanges.size() / 2) && (!exists); i++)
		{
			if ((from == lockedRanges.get(i * 2)) && (to == lockedRanges.get((i * 2) + 1)))
			{
				lockedRanges.remove(i * 2);
				lockedRanges.remove(i * 2);								// the second item gets bumped backwards, so we call the same index
				
				exists = true;
			}
		}
	}
	
	void unlockRange(int offset)
	{
		boolean exists = false;
		
		for (int i = 0; (i < lockedRanges.size() / 2) && (!exists); i++)
		{
			if ((offset == lockedRanges.get(i * 2)) && (offset == lockedRanges.get((i * 2) + 1)))
			{
				lockedRanges.remove(i * 2);
				lockedRanges.remove(i * 2);
				
				exists = false;
			}
		}
	}
	
	void unlockAll()
	{
		lockedRanges.clear();
	}
}