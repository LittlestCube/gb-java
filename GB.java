import littlecube.unsigned.*;
import littlecube.bitutil.*;

import java.util.List;

import java.io.File;

public class GB
{
	static boolean debug;
	
	static CPU cpu;
	static PPU ppu;
	
	public static void main(String args[])
	{
		debug = false;
		
		ppu = new PPU();
		
		cpu = new CPU();
		
		while (true)
		{
			System.out.println();
			
			if (cpu.run)
			{
				while (true)
				{
					cpu.cycle();
				}
			}
		}
	}
}