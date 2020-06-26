import littlecube.unsigned.*;
import littlecube.bitutil.*;

import java.util.Random;

public class CPU
{
	UnsignedByte opcode;
	
	boolean ime;
	
	UnsignedByte x;		// the opcode's 1st octal digit (i.e. bits 7-6)
	UnsignedByte y;		// the opcode's 2nd octal digit (i.e. bits 5-3)
	UnsignedByte z;		// the opcode's 3rd octal digit (i.e. bits 2-0)
	UnsignedByte p;		// y rightshifted one position (i.e. bits 5-4)
	UnsignedByte q;		// y modulo 2 (i.e. bit 3)
	
	UnsignedByte d;		// displacement byte (8-bit signed byte)
	UnsignedByte n;		// 8-bit immediate operand (unsigned byte)
	UnsignedShort nn;	// 16-bit immediate operand (unsigned byte)
	
	int t;
	int m;
	
	int clockt;
	int clockm;
	
	UnsignedShort pc;
	
	boolean halt;
	boolean haltbug;
	int pcForHaltBug;
	
	boolean ei;
	
	UnsignedByte memory[];
	
	final int IE = 0xFFFF;
	final int IF = 0xFF0F;
	
	UnsignedByte r[];
	
	final int A = 7;
	final int B = 0;
	final int C = 1;
	final int D = 2;
	final int E = 3;
	final int H = 4;
	final int L = 5;
	final int R_HL = 6;
	final int F = 8;
	
	UnsignedByte cc[];
	
	final int NZ = 0;
	final int Z = 1;
	final int NCA = 2;
	final int CA = 3;
	final int NE = 4;
	final int HC = 5;
	
	// for use with the flags() method
	final int AND = 2;
	final int OR = 3;
	final int XOR = 4;
	final int CP = 5;
	
	UnsignedShort rp[];
	
	final int BC = 0;
	final int DE = 1;
	final int HL = 2;
	
	final int SP = 3;
	
	UnsignedShort rp2[];
	
	final int AF = 3;
	
	UnsignedShort stack[];
	
	String alu[];
	String rot[];
	
	public CPU()
	{
		init();
	}
	
	void init()
	{
		opcode = new UnsignedByte();
		
		ime = false;
		
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
		
		halt = false;
		haltbug = false;
		pcForHaltBug = -2;
		
		ei = false;
		
		r = new UnsignedByte[9];
		cc = new UnsignedByte[6];
		rp = new UnsignedShort[4];
		rp2 = new UnsignedShort[4];
		
		stack = new UnsignedShort[0x10000];
		
		for (int i = 0; i < memory.length; i++)
		{
			Random rand = new Random();
			
			memory[i] = new UnsignedByte(rand.nextInt());
		}
		
		memory[IE].setBit(5, 1);
		memory[IE].setBit(6, 1);
		memory[IE].setBit(7, 1);
		
		memory[IF].setBit(5, 1);
		memory[IF].setBit(6, 1);
		memory[IF].setBit(7, 1);
		
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
		
		for (int i = 0; i < rp2.length; i++)
		{
			rp2[i] = new UnsignedShort();
		}
		
		for (int i = 0; i < stack.length; i++)
		{
			stack[i] = new UnsignedShort();
		}
		
		alu = new String[] { "ADD A", "ADC A", "SUB", "SBC A", "AND", "XOR", "OR", "CP" };
		rot = new String[] { "RLC", "RRC", "RL", "RR", "SLA", "SRA", "SWAP", "SRL" };
	}
	
	public void cycle()
	{
		if (memory[IE].get() != 0xE0 && memory[IF].get() != 0xE0 && halt)
		{
			halt = false;
			
			t += 4;
			m += 1;
		}
		
		if (ime)
		{
			if (memory[IE].get() != 0xE0 && memory[IF].get() != 0xE0)
			{
				for (int i = 0; i < 5; i++)
				{
					if (memory[IE].getBit(i) == 1 && memory[IF].getBit(i) == 1)
					{
						stack[rp[SP].get()].set(pc);
						rp[SP].add(1);
						
						pc.set(0x40 + (0x8 * i));			// JMP to range 0x0040-0x0060 (if interrupt bit 0 `JP 0x0040`, if interrupt bit 1 `JP 0x0048`, etc.)
						
						memory[IF].setBit(i, 0);
						
						ime = false;
						
						t += 20;
						m += 5;
					}
				}
			}
		}
		
		if (halt)
		{
			return;
		}
		
		if (ei)
		{
			ime = true;
			ei = false;
		}
		
		opcode.set(memory[pc.get()]);
		
		if (haltbug)
		{
			d.set(memory[pc.get()]);
			n.set(memory[pc.get()]);
			nn.craftShort(memory[pc.get() + 1].b, n.b);
			haltbug = false;
			pcForHaltBug = pc.get();
		}
		
		else if (pcForHaltBug == pc.get() - 1)
		{
			opcode.set(memory[pc.get() - 1]);
			pcForHaltBug = -2;
		}
		
		else
		{
			d.set(memory[pc.get() + 1]);
			n.set(memory[pc.get() + 1]);
			nn.craftShort(memory[pc.get() + 2].b, memory[pc.get() + 1].b);
			pcForHaltBug = -2;
		}
		
		r[F].set(BitUtil.craftByte(cc[Z].get(), cc[NE].get(), cc[HC].get(), cc[C].get(), 0, 0, 0, 0));
		
		rp[BC].craftShort(r[B].b, r[C].b);
		rp[DE].craftShort(r[D].b, r[E].b);
		rp[HL].craftShort(r[H].b, r[L].b);
		
		rp2[BC].craftShort(r[B].b, r[C].b);
		rp2[DE].craftShort(r[D].b, r[E].b);
		rp2[HL].craftShort(r[H].b, r[L].b);
		rp2[AF].craftShort(r[A].b, r[F].b);
		
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
							case 0:						// NOP
							{
								t += 4;
								m += 1;
								pc.add(1);
								break;
							}
							
							case 1:						// LD (nn), SP
							{
								memory[nn.get()].set(BitUtil.subByte(rp[SP].get(), 0));
								memory[nn.get() + 1].set(BitUtil.subByte(rp[SP].get(), 1));
								
								t += 20;
								m += 5;
								pc.add(3);
								break;
							}
							
							case 2:						// STOP
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
							
							case 3:						// JR d
							{
								pc.set((pc.get() + 1) + d.b);
								
								t += 12;
								m += 3;
								break;
							}
							
							default:					// JR cc[y - 4], d
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
							case 0:						// LD rp[p], nn
							{
								rp[p.get()].set(nn.get());
								
								t += 12;
								m += 3;
								pc.add(3);
								break;
							}
							
							case 1:						// ADD HL, rp[p]
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
							case 0:						// LD (rp[p]), A
							{							// (this is insanely hard to read if I ever come back here; if p < 2 then memory[rp[p]] gets loaded into. otherwise, we have to load into either memory[rp[HL++]] (p == 2) or memory[rp[HL--]] (p == 3))
								memory[rp[(p.get() == 3) ? HL : p.get()].get()].set(r[A]);
								
								rp[HL].add((p.get() < 2) ? 0 : ((p.get() == 2) ? 1 : -1));
								
								t += 8;
								m += 2;
								pc.add(1);
								break;
							}
							
							case 1:						// LD A, (rp[p]) (same thing, but other way around)
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
							case 0:						// INC rp[p]
							{
								rp[p.get()].add(1);
								
								t += 8;
								m += 2;
								pc.add(1);
								break;
							}
							
							case 1:						// DEC rp[p]
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
					
					case 4:								// INC r[y]
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
					
					case 5:								// DEC r[y]
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
					
					case 6:								// LD r[y], n
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
							case 0:						// RLCA
							{
								setCA(r[A].getBit(7));
								
								r[A].set(r[A].get() << 1);
								r[A].setBit(0, cc[CA].get());
								
								t += 4;
								m += 1;
								pc.add(1);
								break;
							}
							
							case 1:						// RRCA
							{
								setCA(r[A].getBit(0));
								
								r[A].set(r[A].get() >> 1);
								r[A].setBit(7, cc[CA].get());
								
								t += 4;
								m += 1;
								pc.add(1);
								break;
							}
							
							case 2:						// RLA
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
							
							case 3:						// RRA
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
							
							case 4:						// DAA
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
							
							case 5:						// CPL
							{
								r[A].comp();
								
								cc[NE].set(1);
								cc[HC].set(1);
								
								t += 4;
								m += 1;
								pc.add(1);
								break;
							}
							
							case 6:						// SCF
							{
								cc[NE].set(0);
								cc[HC].set(0);
								setCA(1);
								
								t += 4;
								m += 1;
								pc.add(1);
								break;
							}
							
							case 7:						// CCF
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
					if (ime)
					{
						halt = true;
					}
					
					else
					{
						if (memory[IE].get() == 0xE0 || memory[IF].get() == 0xE0)
						{
							halt = true;
						}
						
						else
						{
							haltbug = true;
						}
					}
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
			
			case 2:
			{
				alu(y.get(), r[z.get()].get());
				
				if (z.get() == R_HL)
				{
					t += 4;
					m += 1;
				}
				
				t += 4;
				m += 1;
				pc.add(1);
				break;
			}
			
			case 3:
			{
				switch (z.get())
				{
					case 0:
					{
						switch (y.get())
						{
							case 0:
							
							case 1:
							
							case 2:
							
							case 3:						// RET cc[y]
							{
								if (cc[y.get()].get() == 0)
								{
									t += 8;
									m += 2;
									pc.add(1);
									break;
								}
								
								rp[SP].sub(1);
								pc.set(stack[rp[SP].get()]);
								stack[rp[SP].get()].set(0);
								
								t += 20;
								m += 5;
								break;
							}
							
							case 4:						// LD (0xFF00 + n), A
							{
								memory[0xFF00 + n.get()].set(r[A]);
								
								t += 12;
								m += 3;
								pc.add(2);
								break;
							}
							
							case 5:						// ADD SP, d
							{
								int prevSP = rp[SP].get();
								rp[SP].add(d.b);
								
								setZ(0);
								flags("HC C", 0, prevSP, d.b);
								
								t += 16;
								m += 4;
								pc.add(2);
								break;
							}
							
							case 6:						// LD A, (0xFF00 + n)
							{
								r[A].set(memory[0xFF00 + n.get()]);
								
								t += 12;
								m += 3;
								pc.add(2);
								break;
							}
							
							case 7:						// LD HL, SP + d
							{
								rp[HL].set(rp[SP].get() + d.b);
								r[R_HL].set(memory[rp[HL].get()]);
								
								setZ(0);
								flags("HC C", 0, rp[SP].get(), d.b);
								
								t += 12;
								m += 3;
								pc.add(2);
								break;
							}
						}
						
						break;
					}
					
					case 1:
					{
						switch (q.get())
						{
							case 0:						// POP rp2[p]
							{
								rp2[p.get()].set(stack[rp[SP].get()]);
								
								
								t += 12;
								m += 3;
								pc.add(1);
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
		
		memory[rp[HL].get()].set(r[R_HL]);
	}
	
	void alu(int index, int operand)
	{
		int prevA = r[A].get();
		
		switch (index)
		{
			case 0:										// ADD A
			{
				r[A].add(operand);
				flags("Z CA HC", 0, prevA, operand);
				
				break;
			}
			
			case 1:										// ADC A
			{
				r[A].add(operand + cc[C].get());
				flags("Z CA HC", 0, prevA, operand + cc[C].get());
				
				break;
			}
			
			case 2:										// SUB A
			{
				r[A].sub(operand);
				flags("Z CA HC", 1, prevA, operand);
				
				break;
			}
			
			case 3:										// SBC A
			{
				r[A].sub(operand + cc[C].get());
				flags("Z CA HC", 1, prevA, operand + cc[C].get());
				
				break;
			}
			
			case 4:										// AND A
			{
				r[A].and(operand);
				flags("Z CA HC", 2, prevA, operand);
				
				break;
			}
			
			case 5:										// OR A
			{
				r[A].or(operand);
				flags("Z CA HC", 3, prevA, operand);
				
				break;
			}
			
			case 6:										// XOR A
			{
				r[A].xor(operand);
				flags("Z CA HC", 4, prevA, operand);
				
				break;
			}
			
			case 7:										// CP A
			{
				flags("Z CA HC", 5, prevA, operand);
				
				break;
			}
		}
	}
	
	void flags(String flags, int operation, int fnum, int snum)
	{
		switch (operation)
		{
			case 0:										// ADD flags
				cc[NE].set(0);
				
				if (flags.contains("Z"))
				{
					boolean zero = (fnum + snum) % 256 == 0;
					
					setZ(zero ? 1 : 0);
				}
				
				if (flags.contains("CA"))
				{
					boolean carry = fnum + snum > 255;
					
					setCA(carry ? 1 : 0);
				}
				
				if (flags.contains("HC"))
				{
					boolean hcarry = (fnum & 0x0F) + (snum & 0x0F) > 0x0F;
					
					cc[HC].set(hcarry ? 1 : 0);
				}
				
				break;
			
			case 5:										// CP flags (same as SUB flags)
			
			case 1:										// SUB flags
			{
				cc[NE].set(1);
				
				if (flags.contains("Z"))
				{
					boolean zero = fnum - snum == 0;
					
					setZ(zero ? 1 : 0);
				}
				
				if (flags.contains("CA"))
				{
					boolean carry = snum < fnum;
					
					setCA(carry ? 1 : 0);
				}
				
				if (flags.contains("HC"))
				{
					boolean hcarry = (snum & 0x0F) > (fnum & 0x0F);
					
					cc[HC].set(hcarry ? 1 : 0);
				}
				
				break;
			}
			
			case 2:										// AND flags
			{
				cc[NE].set(0);
				
				if (flags.contains("Z"))
				{
					boolean zero = (fnum & snum) == 0;
					
					setZ(zero ? 1 : 0);
				}
				
				if (flags.contains("CA"))
				{
					setCA(0);
				}
				
				if (flags.contains("HC"))
				{
					cc[HC].set(1);
				}
				
				break;
			}
			
			case 3:										// OR flags (same as XOR flags)
			
			case 4:										// XOR flags
			{
				cc[NE].set(0);
				
				if (flags.contains("Z"))
				{
					boolean zero = (fnum & snum) == 0;
					
					setZ(zero ? 1 : 0);
				}
				
				if (flags.contains("CA"))
				{
					setCA(0);
				}
				
				if (flags.contains("HC"))
				{
					cc[HC].set(0);
				}
				
				break;
			}
		}
	}
	
	void setZ(int value)
	{
		cc[Z].set(value);
		cc[NZ].set(value ^ 1);
	}
	
	void setCA(int value)
	{
		cc[CA].set(value);
		cc[NCA].set(value ^ 1);
	}
}