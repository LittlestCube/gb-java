import littlecube.unsigned.*;
import littlecube.bitutil.*;

import java.util.List;

import java.io.File;

public class GB
{
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
				Thread.sleep(0);
				
				if (cpu.run)
				{
					while (cpu.run)
					{
						if (cpu.pc.get() > 0x100 && tile)
						{
							ppu.updateTileWindow();
						}
						
						if (cpu.pc.get() > 0x100 && map)
						{
							ppu.updateBGMWindow();
						}
						
						// (temporary code) print results of blargg's test ROMs
						if (cpu.mmu.read(0xFF02).get() == 0xFF)
						{
							System.out.print((char) cpu.memory[0xFF01].get());
							
							cpu.mmu.write(0xFF02, 0x00);
						}
						
						if (cpu.pc.get() == 0x100)
						{
							cpu.replaceBIOS();
						}
						
						cpu.memory[0xFF44].set(0x90);						// PPU substitute
						
						Thread.sleep(millisleeps);
						
						cpu.cycle();
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