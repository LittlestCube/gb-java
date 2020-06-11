import littlecube.unsigned.*;
import littlecube.bitutil.*;

public class CPU
{
	UnsignedByte opcode;
	UnsignedByte arg1;
	UnsignedByte arg2;
	
	UnsignedByte x;		// the opcode's 1st octal digit (i.e. bits 7-6)
	UnsignedByte y;		// the opcode's 2nd octal digit (i.e. bits 5-3)
	UnsignedByte z;		// the opcode's 3rd octal digit (i.e. bits 2-0)
	UnsignedByte p;		// y rightshifted one position (i.e. bits 5-4)
	UnsignedByte q;		// y modulo 2 (i.e. bit 3)
	
	UnsignedByte d;		// displacement byte (8-bit signed byte)
	UnsignedByte n;		// 8-bit immediate operand (unsigned byte)
	UnsignedShort nn;	// 16-bit immediate operand (unsigned byte)
	
	UnsignedShort sp;
	UnsignedShort af;
	
	int t;
	int m;
	
	int clockt;
	int clockm;
	
	UnsignedShort pc;
	
	UnsignedByte memory[];
	
	UnsignedByte r[];
	UnsignedByte cc[];
	UnsignedShort rp[];
	
	String alu[];
	String rot[];
	
	public CPU()
	{
		init();
	}
	
	void init()
	{
		opcode = new UnsignedByte();
		
		sp = new UnsignedShort();
		af = new UnsignedShort();
		
		clockt = 0;
		clockm = 0;
		
		pc = new UnsignedShort();
		
		x = new UnsignedByte();
		y = new UnsignedByte();
		z = new UnsignedByte();
		p = new UnsignedByte();
		q = new UnsignedByte();
		
		d = new UnsignedByte();
		n = new UnsignedByte();
		nn = new UnsignedShort();
		
		memory = new UnsignedByte[0x10000];
		
		r = new UnsignedByte[8];
		cc = new UnsignedByte[4];
		rp = new UnsignedShort[3];
		
		for (int i = 0; i < memory.length; i++)
		{
			memory[i] = new UnsignedByte();
		}
		
		for (int i = 0; i < r.length; i++)
		{
			r[i] = new UnsignedByte();
		}
		
		for (int i = 0; i < cc.length; i++)
		{
			cc[i] = new UnsignedByte();
		}
		
		for (int i = 0; i < rp.length; i++)
		{
			rp[i] = new UnsignedShort();
		}
		
		alu = new String[] { "ADD A", "ADC A", "SUB", "SBC A", "AND", "XOR", "OR", "CP" };
		rot = new String[] { "RLC", "RRC", "RL", "RR", "SLA", "SRA", "SWAP", "SRL" };
	}
	
	public void cycle()
	{
		opcode.set(memory[pc.get()]);
		d.set(memory[pc.get() + 1]);
		n.set(memory[pc.get() + 1]);
		nn.set((memory[pc.get() + 2].get() << 8) | (memory[pc.get() + 1].get()));
		
		t = 0;
		m = 0;
		
		x.setBit(BitUtil.bit(opcode.get(), 7), 1);
		x.setBit(BitUtil.bit(opcode.get(), 6), 0);
		
		y.setBit(BitUtil.bit(opcode.get(), 5), 2);
		y.setBit(BitUtil.bit(opcode.get(), 4), 1);
		y.setBit(BitUtil.bit(opcode.get(), 3), 0);
		
		z.setBit(BitUtil.bit(opcode.get(), 2), 2);
		z.setBit(BitUtil.bit(opcode.get(), 1), 1);
		z.setBit(BitUtil.bit(opcode.get(), 0), 0);
		
		p.setBit(BitUtil.bit(opcode.get(), 5), 1);
		p.setBit(BitUtil.bit(opcode.get(), 4), 0);
		
		q.setBit(BitUtil.bit(opcode.get(), 3), 0);
		
		switch (x.get())
		{
			case 0:
			{
				switch (z.get())
				{
					case 0:
					{
						switch (y.get())
						{
							case 0:			// NOP
							{
								t += 4;
								m += 1;
								pc.add(1);
								break;
							}
							
							case 1:			// LD (nn), SP
							{
								memory[nn.get()].set(BitUtil.subByte(sp.get(), 0));
								memory[nn.get() + 1].set(BitUtil.subByte(sp.get(), 1));
								
								t += 20;
								m += 5;
								pc.add(3);
								break;
							}
							
							case 2:			// STOP
							{
								while (true)
								{
									// TODO: break when input detected
									System.out.println("I: this boy called STOP");
									break;
								}
								
								t += 4;
								m += 1;
								pc.add(2);
								break;
							}
							
							case 3:			// JR d
							{
								pc.set((pc.get() + 1) + d.b);
								
								t += 12;
								m += 3;
								break;
							}
							
							default:		// JR cc[y - 4], d
							{
								if (cc[y.get() - 4].get() == 1)
								{
									t += 12;
									m += 3;
									pc.set((pc.get() + 1) + d.b);
								}
								
								else
								{
									t += 8;
									m += 2;
									pc.set(pc.get() + 1);
								}
								break;
							}
						}
						
						break;
					}
				}
				
				break;
			}
		}
		
		clockt += t;
		clockm += m;
	}
}