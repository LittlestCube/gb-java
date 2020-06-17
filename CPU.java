import littlecube.unsigned.*;
import littlecube.bitutil.*;

import java.util.Random;

public class CPU
{
	UnsignedByte opcode;
	UnsignedByte arg1;
	UnsignedByte arg2;
	
	UnsignedByte ime;
	
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
	
	final int A = 7;
	final int B = 0;
	final int C = 1;
	final int D = 2;
	final int E = 3;
	final int H = 4;
	final int L = 5;
	final int R_HL = 6;
	
	final int BC = 0;
	final int DE = 1;
	final int HL = 2;
	
	final int NZ = 0;
	final int Z = 1;
	final int NCA = 2;
	final int CA = 3;
	final int NE = 4;
	final int HC = 5;
	
	final int IE = 0xFFFF;
	final int IF = 0xFF0F;
	
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
		cc = new UnsignedByte[6];
		rp = new UnsignedShort[3];
		
		for (int i = 0; i < memory.length; i++)
		{
			Random rand = new Random();
			
			memory[i] = new UnsignedByte(rand.nextInt());
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
		
		rp[BC].craftShort(r[B].b, r[C].b);
		rp[DE].craftShort(r[D].b, r[E].b);
		rp[HL].craftShort(r[H].b, r[L].b);
		
		r[R_HL].set(memory[rp[HL].get()]);
		
		t = 0;
		m = 0;
		
		cc[NE].set(0);
		cc[HC].set(0);
		
		x.setBit(1, opcode.getBit(7));
		x.setBit(0, opcode.getBit(6));
		
		y.setBit(2, opcode.getBit(5));
		y.setBit(1, opcode.getBit(4));
		y.setBit(0, opcode.getBit(3));
		
		z.setBit(2, opcode.getBit(2));
		z.setBit(1, opcode.getBit(1));
		z.setBit(0, opcode.getBit(0));
		
		p.setBit(1, opcode.getBit(5));
		p.setBit(0, opcode.getBit(4));
		
		q.setBit(0, opcode.getBit(3));
		
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
					
					case 1:
					{
						switch (q.get())
						{
							case 0:			// LD rp[p], nn
							{
								rp[p.get()].set(nn.get());
								
								t += 12;
								m += 3;
								pc.add(3);
								break;
							}
							
							case 1:			// ADD HL, rp[p]
							{
								flags("HC CA", 0, rp[HL].get(), rp[p.get()].get());
								
								rp[2].add(rp[p.get()].get());
								
								r[H].set(BitUtil.subByte(rp[HL].get(), 1));
								r[L].set(BitUtil.subByte(rp[HL].get(), 0));
								
								t += 8;
								m += 2;
								pc.add(1);
								break;
							}
						}
						
						break;
					}
					
					case 2:
					{
						switch (q.get())
						{
							case 0:			// LD (rp[p]), A (this is insanely hard to read if I ever come back here; if p < 2 then memory[rp[p]] gets loaded into. otherwise, we have to load into either memory[HL++] or memory[HL--] (p == 2 or p == 3))
							{
								memory[rp[(p.get() == 3) ? p.get() - 1 : p.get()].get()].set(r[A]);
								
								rp[HL].add((p.get() < 2) ? 0 : ((p.get() == 2) ? 1 : -1));
								
								t += 8;
								m += 2;
								pc.add(1);
								break;
							}
							
							case 1:			// LD A, (rp[p]) (same thing, but other way around)
							{
								r[A].set(memory[rp[(p.get() == 3) ? p.get() - 1 : p.get()].get()]);
								
								rp[HL].add((p.get() < 2) ? 0 : ((p.get() == 2) ? 1 : -1));
								
								t += 8;
								m += 2;
								pc.add(1);
								break;
							}
						}
						
						break;
					}
					
					case 3:
					{
						switch (q.get())
						{
							case 0:			// INC rp[p]
							{
								rp[p.get()].add(1);
								
								t += 8;
								m += 2;
								pc.add(1);
								break;
							}
							
							case 1:			// DEC rp[p]
							{
								rp[p.get()].sub(1);
								
								t += 8;
								m += 2;
								pc.add(1);
								break;
							}
						}
						
						break;
					}
					
					case 4:					// INC r[y]
					{
						flags("Z HC", 0, r[y.get()].get(), 1);
						
						if (y.get() == R_HL)
						{
							memory[rp[HL].get()].add(1);
							
							t += 12;
							m += 3;
						}
						
						else
						{
							r[y.get()].add(1);
							
							t += 4;
							m += 1;
						}
						
						pc.add(1);
						break;
					}
					
					case 5:					// DEC r[y]
					{
						flags("Z HC", 1, r[y.get()].get(), 1);
						
						if (y.get() == R_HL)
						{
							memory[rp[HL].get()].sub(1);
							
							t += 12;
							t += 3;
						}
						
						else
						{
							r[y.get()].sub(1);
							
							t += 4;
							m += 1;
						}
						
						pc.add(1);
						break;
					}
					
					case 6:					// LD r[y], n
					{
						if (y.get() == R_HL)
						{
							memory[rp[HL].get()].set(n);
							
							t += 12;
							t += 3;
						}
						
						else
						{
							r[y.get()].set(n);
							
							t += 8;
							m += 2;
						}
						
						pc.add(2);
						break;
					}
					
					case 7:
					{
						switch (y.get())
						{
							case 0:			// RLCA
							{
								setCA(r[A].getBit(7));
								
								r[A].set(r[A].get() << 1);
								r[A].setBit(0, cc[CA].get());
								
								t += 4;
								m += 1;
								pc.add(1);
								break;
							}
							
							case 1:			// RRCA
							{
								setCA(r[A].getBit(0));
								
								r[A].set(r[A].get() >> 1);
								r[A].setBit(7, cc[CA].get());
								
								t += 4;
								m += 1;
								pc.add(1);
								break;
							}
							
							case 2:			// RLA
							{
								int prevbit7 = r[A].getBit(7);
								
								r[A].left(1);
								r[A].setBit(0, prevbit7);
								
								setCA(prevbit7);
								
								t += 4;
								m += 1;
								pc.add(1);
								break;
							}
							
							case 3:			// RRA
							{
								int prevbit0 = r[A].getBit(0);
								
								r[A].right(1);
								r[A].setBit(7, prevbit0);
								
								setCA(prevbit0);
								
								t += 4;
								m += 1;
								pc.add(1);
								break;
							}
							
							case 4:			// DAA
							{
								if (cc[NE].get() == 0)
								{
									if (cc[CA].get() == 1 || r[A].get() > 0x99)
									{
										r[A].add(0x60);
										setCA(1);
									}
									
									if (cc[HC].get() == 1 || (r[A].get() & 0x0F) > 0x09)
									{
										r[A].add(0x6);
									}
								}
								
								else
								{
									if (cc[CA].get() == 1)
									{
										r[A].sub(0x60);
									}
									
									if (cc[HC].get() == 1)
									{
										r[A].sub(0x6);
									}
								}
								
								cc[Z].set(r[A].get() == 0 ? 1 : 0);
								cc[HC].set(0);
								
								t += 4;
								m += 1;
								pc.add(1);
								break;
							}
							
							case 5:			// CPL
							{
								r[A].comp();
								
								cc[NE].set(1);
								cc[HC].set(1);
								
								t += 4;
								m += 1;
								pc.add(1);
								break;
							}
							
							case 6:			// SCF
							{
								cc[NE].set(0);
								cc[HC].set(0);
								setCA(1);
								
								t += 4;
								m += 1;
								pc.add(1);
								break;
							}
							
							case 7:			// CCF
							{
								cc[NE].set(0);
								cc[HC].set(0);
								setCA(cc[CA].get() ^ 1);
								
								t += 4;
								m += 1;
								pc.add(1);
								break;
							}
						}
					}
				}
				
				break;
			}
			
			case 1:
			{
				if (z.get() == 6 && y.get() == 6)		// HALT
				{
					
				}
				
				else									// LD r[y], r[z]
				{
					r[y.get()].set(r[z.get()]);
				}
				
				t += 4;
				m += 1;
				pc.add(1);
				break;
			}
		}
		
		clockt += t;
		clockm += m;
	}
	
	void flags(String flags, int newNe, int fnum, int snum)
	{
		if (newNe == 0)
		{
			if (flags.contains("Z"))
			{
				boolean zero = (fnum + snum) % 256 == 0;
				
				cc[Z].set(zero ? 1 : 0);
				cc[NZ].set(zero ? 0 : 1);
			}
			
			if (flags.contains("CA"))
			{
				boolean carry = fnum + snum > 255;
				
				cc[CA].set(carry ? 1 : 0);
				cc[NCA].set(carry ? 0 : 1);
			}
			
			if (flags.contains("HC"))
			{
				boolean hcarry = (fnum & 0x0F) + (snum & 0x0F) > 0x0F;
				
				cc[HC].set(hcarry ? 1 : 0);
			}
		}
		
		else if (newNe == 1)
		{
			if (flags.contains("Z"))
			{
				boolean zero = fnum - snum == 0;
				
				cc[Z].set(zero ? 1 : 0);
				cc[NZ].set(zero ? 0 : 1);
			}
			
			if (flags.contains("CA"))
			{
				boolean carry = fnum - snum < 0;
				
				setCA(carry ? 1 : 0);
			}
			
			if (flags.contains("HC"))
			{
				boolean hcarry = (fnum & 0x0F) + (snum & 0x0F) > 0x0F;
				
				cc[HC].set(hcarry ? 1 : 0);
			}
		}
	}
	
	void setCA(int value)
	{
		cc[CA].set(value);
		cc[NCA].set(value ^ 1);
	}
}