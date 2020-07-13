import littlecube.unsigned.*;
import littlecube.bitutil.*;

import java.util.List;

import java.io.File;

public class GB
{
	static boolean debug;
	static boolean ram;
	
	static CPU cpu;
	static PPU ppu;
	
	static int millisleeps;
	
	public static void main(String args[]) throws InterruptedException
	{
		millisleeps = 0;
		
		debug = false;
		ram = false;
		
		cpu = new CPU();
		
		ppu = new PPU();
		
		while (true)
		{
			Thread.sleep(0);
			
			if (cpu.run)
			{
				while (cpu.run)
				{
					if (cpu.mmu.read(0xFF02).get() == 0xFF)
					{
						System.out.print((char) cpu.memory[0xFF01].get());
						
						cpu.mmu.write(0xFF02, 0x7E);
					}
					
					if (cpu.pc.get() == 0x100)
					{
						cpu.replaceBIOS();
					}
					
					cpu.memory[0xFF44].set(0x90);						// PPU substitute
					
					Thread.sleep(millisleeps);
					
					cpu.cycle();
				}
			}
		}
	}
}