import littlecube.unsigned.*;
import littlecube.bitutil.*;

public class GB
{
	static CPU cpu;
	
	public static void main(String args[])
	{
		cpu = new CPU();
		
		while (true)
		{
			cpu.cycle();
		}
	}
}