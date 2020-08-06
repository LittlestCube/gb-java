import littlecube.unsigned.*;
import littlecube.bitutil.*;

import java.util.List;

import java.io.File;

public class GB
{
	static boolean useBIOS;
	
	static boolean debug;
	static boolean ram;
	static boolean tile;
	static boolean map;
	
	static boolean loopDone;
	
	static CPU cpu;
	static PPU ppu;
	static Timer timer;
	
	static int millisleeps;
	
	public static void main(String args[]) throws InterruptedException
	{
		try
		{
			millisleeps = 0;
			
			useBIOS = false;
			debug = false;
			ram = false;
			tile = false;
			map = false;
			
			loopDone = true;
			
			cpu = new CPU();
			
			cpu.initMMU();
			
			timer = new Timer();
			
			ppu = new PPU();
			
			while (true)
			{
				Thread.sleep(0);
				
				if (cpu.run)
				{
					//ppu.scaleSet.setEnabled(false);
					
					ppu.sr.turnOnDisplay();
					
					cpu.memory[0xFF44].set(0x90);
					
					while (cpu.run)
					{
						loopDone = false;
						
						if (cpu.clockm % 1000 == 0 && GB.tile)
						{
							GB.ppu.updateTileWindow();
						}
						
						if (cpu.clockm % 1000 == 0 && GB.map)
						{
							GB.ppu.updateBGMWindow();
						}
						
						if (millisleeps != 0)
						{
							Thread.sleep(millisleeps);
						}
						
						cpu.cycle();
						
						ppu.sr.clock(cpu.t);
						
						if (cpu.mmu.dmaOffset != -1)
						{
							cpu.mmu.dmaClock(cpu.t);
						}
						
						timer.clock(cpu.t);
						
						loopDone = true;
					}
				}
			}
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void reset()
	{
		cpu.init();
		cpu.initMMU();
		ppu.sr.init();
		timer.init();
	}
}