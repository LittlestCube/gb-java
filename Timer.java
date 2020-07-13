import littlecube.bitutil.*;
import littlecube.unsigned.*;

public class Timer
{
	static UnsignedShort div;
	static UnsignedByte tima;
	static UnsignedByte tma;
	static UnsignedByte tac;
	
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
	}
	
	public void clock(int clocks)
	{
		div.add(clocks);
	}
	
	void updateDiv()
	{
		GB.cpu.mmu.write(0xFF04, div.subByte(1));
	}
}