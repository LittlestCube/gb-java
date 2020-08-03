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
			
			cpu = new CPU();
			
			cpu.initMMU();
			
			timer = new Timer();
			
			ppu = new PPU();
			
			while (true)
			{
				ppu.frame.repaint();
				ppu.tileFrame.repaint();
				ppu.bgmFrame.repaint();
				
				if (cpu.run)
				{
					ppu.sr.turnOnDisplay();
					
					cpu.memory[0xFF44].set(0x90);
					
					while (cpu.run)
					{
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
		timer.init();
	}
}