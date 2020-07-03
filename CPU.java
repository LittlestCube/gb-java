import littlecube.unsigned.*;
import littlecube.bitutil.*;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.io.File;
import java.io.IOException;

import java.util.Random;

public class CPU
{
	byte[] ROM;
	
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
	
	boolean biosDone;
	
	boolean stop;
	
	boolean halt;
	boolean haltbug;
	int pcForHaltBug;
	
	boolean ei;
	
	static UnsignedByte memory[];
	
	boolean run;
	
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
	
	String debugging;
	
	public CPU()
	{
		init();
		initMemory();
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
		
		biosDone = false;
		
		stop = false;
		
		halt = false;
		haltbug = false;
		pcForHaltBug = -2;
		
		ei = false;
		
		r = new UnsignedByte[9];
		cc = new UnsignedByte[6];
		rp = new UnsignedShort[4];
		rp2 = new UnsignedShort[4];
		
		run = false;
		
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
	}
	
	void initMemory()
	{
		memory = new UnsignedByte[0x10000];
		
		Random rand = new Random();
		
		for (int i = 0; i < memory.length; i++)
		{
			//memory[i] = new UnsignedByte(rand.nextInt());
			memory[i] = new UnsignedByte();
		}
		
		retainConstants();
	}
	
	void retainConstants()
	{
		r[F].setBit(0, 0);
		r[F].setBit(1, 0);
		r[F].setBit(2, 0);
		r[F].setBit(3, 0);
		
		memory[IE].setBit(5, 1);
		memory[IE].setBit(6, 1);
		memory[IE].setBit(7, 1);
		
		memory[IF].setBit(5, 1);
		memory[IF].setBit(6, 1);
		memory[IF].setBit(7, 1);
		
		memory[0xFF02].setBit(1, 1);
		memory[0xFF02].setBit(2, 1);
		memory[0xFF02].setBit(3, 1);
		memory[0xFF02].setBit(4, 1);
		memory[0xFF02].setBit(5, 1);
		memory[0xFF02].setBit(6, 1);
	}
	
	void debug()
	{
		if (GB.debug)
		{
			debugging = "";
			
			debugging += String.format("A: 0x%02X\n", r[A].get());
			debugging += String.format("B: 0x%02X\n", r[B].get());
			debugging += String.format("C: 0x%02X\n", r[C].get());
			debugging += String.format("D: 0x%02X\n", r[D].get());
			debugging += String.format("E: 0x%02X\n", r[E].get());
			debugging += String.format("H: 0x%02X\n", r[H].get());
			debugging += String.format("L: 0x%02X\n", r[L].get());
			debugging += String.format("F: 0x%02X\n", r[F].get());
			debugging += String.format("\nrp2 AF: 0x%04X\n", rp2[AF].get());
			debugging += String.format("rp2 BC: 0x%04X\n", rp2[BC].get());
			debugging += String.format("rp2 DE: 0x%04X\n", rp2[DE].get());
			debugging += String.format("rp2 HL: 0x%04X\n", rp2[HL].get());
			debugging += String.format("\nrp BC: 0x%04X\n", rp[BC].get());
			debugging += String.format("rp DE: 0x%04X\n", rp[DE].get());
			debugging += String.format("rp HL: 0x%04X\n", rp[HL].get());
			debugging += String.format("\nopcode: 0x%04X\n", opcode.get());
			debugging += String.format("\npc: 0x%04X\n", pc.get());
			debugging += String.format("\nsp: 0x%04X\n", rp[SP].get());
			
			GB.ppu.debugText.setText(debugging);
		}
	}
	
	void ram()
	{
		if (GB.ram)
		{
			String ram = "";
			
			int memlines = memory.length / 16;
			
			for (int i = 0; i < memlines; i++)
			{
				int j = i * 16;
				
				ram += String.format("%04X: %02X %02X %02X %02X %02X %02X %02X %02X  %02X %02X %02X %02X %02X %02X %02X %02X \n", j,
				memory[j].get(), memory[j + 1].get(), memory[j + 2].get(), memory[j + 3].get(),
				memory[j + 4].get(), memory[j + 5].get(), memory[j + 6].get(), memory[j + 7].get(),
				memory[j + 8].get(), memory[j + 9].get(), memory[j + 0xA].get(), memory[j + 0xB].get(),
				memory[j + 0xC].get(), memory[j + 0xD].get(), memory[j + 0xE].get(), memory[j + 0xF].get());
			}
			
			GB.ppu.ramText.setText(ram);
			GB.ppu.ramFrame.setSize(400, 432);
		}
	}
	
	public void loadBIOS()
	{
		run = false;
		
		try
		{
			File bios = new File("dmg_bios.gb");
			
			if (bios.exists())
			{
				byte[] biosROM = Files.readAllBytes(Paths.get(bios.getAbsolutePath()));
				
				for (int i = 0; i < 0x100; i++)
				{
					memory[i].set(biosROM[i]);
				}
			}
			
			else
			{
				r[A].set(0x01);
				r[B].set(0x00);
				r[C].set(0x13);
				r[D].set(0x00);
				r[E].set(0xD8);
				r[H].set(0x01);
				r[L].set(0x4D);
				
				pc.set(0x100);
				
				rp[SP].set(0xFFFE);
			}
		}
		
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		run = true;
	}
	
	public void replaceBIOS()
	{
		if (!biosDone)
		{
			for (int i = 0; i < 0x100; i++)
			{
				memory[i].set(ROM[i]);
			}
		}
	}
	
	public void loadGame(String filepath)
	{
		try
		{
			ROM = Files.readAllBytes(Paths.get(filepath));
			
			for (int i = 0; i < ROM.length; i++)
			{
				memory[i].set(ROM[i]);
			}
		}
		
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void cycle()
	{
		if (stop)
		{
			return;
		}
		
		retainConstants();
		
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
						call(0x40 + (0x8 * i));						// CALL range 0x0040-0x0060 (if interrupt bit 0 `JP 0x0040`, if interrupt bit 1 `JP 0x0048`, etc.)
						
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
			
			d.set(memory[pc.get()]);
			n.set(memory[pc.get()]);
			nn.craftShort(memory[pc.get() + 1].b, n.b);
			
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
		
		debug();
		ram();
		
		splitOpcode(opcode);
		
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
								memory[nn.get()].set(rp[SP].subByte(1));
								memory[nn.get() + 1].set(rp[SP].subByte(0));
								
								t += 20;
								m += 5;
								pc.add(3);
								break;
							}
							
							case 2:						// STOP
							{
								System.out.println("W: STOP called");
								
								// TODO: get rid of all this and break when input detected
								stop = true;
								
								t += 4;
								m += 1;
								pc.add(2);
								break;
							}
							
							case 3:						// JR d
							{
								pc.add(2);
								
								pc.set((pc.get()) + d.b);
								
								t += 12;
								m += 3;
								break;
							}
							
							case 4:
							
							case 5:
							
							case 6:
							
							case 7:						// JR cc[y - 4], d
							{
								pc.add(2);
								
								if (cc[y.get() - 4].get() == 0)
								{
									t += 8;
									m += 2;
									break;
								}
								
								pc.set(pc.get() + d.b);
								
								t += 12;
								m += 3;
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
								
								movRP(p.get());
								
								t += 12;
								m += 3;
								pc.add(3);
								break;
							}
							
							case 1:						// ADD HL, rp[p]
							{
								flags("HC CA", 0, rp[HL].get(), rp[p.get()].get());
								
								rp[2].add(rp[p.get()].get());
								
								movRP(HL);
								
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
							case 0:						// LD (rp[p]), A; if p < 2 then memory[rp[p]] gets loaded into. otherwise, we have to load into either memory[rp[HL++]] (p == 2) or memory[rp[HL--]] (p == 3))
							{
								int trueMemP = 0;
								
								if (p.get() == BC || p.get() == DE)
								{
									trueMemP = rp[p.get()].get();
								}
								
								else
								{
									trueMemP = rp[HL].get();
									
									if (p.get() == 2)
									{
										rp[HL].add(1);
									}
									
									else if (p.get() == 3)
									{
										rp[HL].sub(1);
									}
									
									movRP(HL);
								}
								
								memory[trueMemP].set(r[A]);
								
								t += 8;
								m += 2;
								pc.add(1);
								break;
							}
							
							case 1:						// LD A, (rp[p]) (same thing, but other way around)
							{
								int trueMemP = 0;
								
								if (p.get() == BC || p.get() == DE)
								{
									trueMemP = rp[p.get()].get();
								}
								
								else
								{
									trueMemP = rp[HL].get();
									
									if (p.get() == 2)
									{
										rp[HL].add(1);
									}
									
									else if (p.get() == 3)
									{
										rp[HL].sub(1);
									}
									
									movRP(HL);
								}
								
								r[A].set(memory[trueMemP]);
								
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
								
								movRP(p.get());
								
								t += 8;
								m += 2;
								pc.add(1);
								break;
							}
							
							case 1:						// DEC rp[p]
							{
								rp[p.get()].sub(1);
								
								movRP(p.get());
								
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
							updateMEMHL();
							
							t += 8;
							m += 2;
						}
						
						r[y.get()].add(1);
						
						t += 4;
						m += 1;
						pc.add(1);
						break;
					}
					
					case 5:								// DEC r[y]
					{
						flags("Z HC", 1, r[y.get()].get(), 1);
						
						if (y.get() == R_HL)
						{
							updateMEMHL();
							
							t += 8;
							m += 2;
						}
						
						r[y.get()].sub(1);
						
						t += 4;
						m += 1;
						pc.add(1);
						break;
					}
					
					case 6:								// LD r[y], n
					{
						if (y.get() == R_HL)
						{
							updateMEMHL();
							
							t += 4;
							t += 1;
						}
						
						r[y.get()].set(n);
						
						t += 8;
						m += 2;
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
					
					if (z.get() == R_HL)
					{
						if (y.get() == R_HL)
						{
							updateMEMHL();
						}
						
						t += 4;
						m += 1;
					}
				}
				
				t += 4;
				m += 1;
				pc.add(1);
				break;
			}
			
			case 2:										// alu[y] r[z]
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
								
								ret();
								
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
								int prevSP = rp[SP].get();
								
								rp[SP].add(d.b);
								
								rp[HL].set(rp[SP].get());
								r[R_HL].set(memory[rp[HL].get()]);
								
								movRP(HL);
								
								setZ(0);
								flags("HC C", 0, prevSP, d.b);
								
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
								rp2[p.get()].set(pop());
								
								movRP2(p.get());
								
								t += 12;
								m += 3;
								pc.add(1);
								break;
							}
							
							case 1:
							{
								switch (p.get())
								{
									case 0:				// RET
									{
										ret();
										
										t += 16;
										m += 4;
										break;
									}
									
									case 1:				// RETI
									{
										ei = true;
										
										ret();
										
										t += 16;
										m += 4;
										break;
									}
									
									case 2:				// JP HL
									{
										pc.set(rp[HL]);
										
										t += 4;
										m += 1;
										break;
									}
									
									case 3:				// LD SP, HL
									{
										rp[SP].set(rp[HL]);
										
										t += 8;
										m += 2;
										pc.add(1);
										break;
									}
								}
								
								break;
							}
						}
						
						break;
					}
					
					case 2:
					{
						switch (y.get())
						{
							case 0:
							
							case 1:
							
							case 2:
							
							case 3:						// JP cc[y], nn
							{
								if (cc[y.get()].get() == 0)
								{
									t += 12;
									m += 3;
									pc.add(3);
									break;
								}
								
								pc.set(nn.get());
								
								t += 16;
								m += 4;
								break;
							}
							
							case 4:						// LD (0xFF00 + C), A
							{
								memory[0xFF00 + r[C].get()].set(r[A]);
								
								t += 8;
								m += 2;
								pc.add(1);
								break;
							}
							
							case 5:						// LD (nn), A
							{
								memory[nn.get()].set(r[A]);
								
								t += 16;
								m += 4;
								pc.add(3);
								break;
							}
							
							case 6:						// LD A, (0xFF00 + C)
							{
								r[A].set(memory[0xFF00 + r[C].get()]);
								
								t += 8;
								m += 2;
								pc.add(1);
								break;
							}
							
							case 7:						// LD A, (nn)
							{
								r[A].set(memory[nn.get()]);
								
								t += 8;
								m += 2;
								pc.add(3);
								break;
							}
						}
						
						break;
					}
					
					case 3:
					{
						switch (y.get())
						{
							case 0:						// JP nn
							{
								pc.set(nn.get());
								
								t += 16;
								m += 4;
								break;
							}
							
							case 1:									// CB-prefixed
							{
								splitOpcode(n);
								
								switch (x.get())
								{
									case 0:				// rot[y] r[z]
									{
										rot(y.get(), z.get());
										
										if (z.get() == R_HL)
										{
											updateMEMHL();
											
											t += 8;
											m += 2;
										}
										
										t += 8;
										m += 2;
										pc.add(2);
										break;
									}
									
									case 1:				// BIT y, r[z]
									{
										flags("Z", 0, r[z.get()].getBit(y.get()), 0);
										cc[HC].set(1);
										
										if (z.get() == R_HL)
										{
											t += 4;
											m += 1;
										}
										
										t += 8;
										m += 2;
										pc.add(2);
										break;
									}
									
									case 2:				// RES y, r[z]
									{
										r[z.get()].setBit(y.get(), 0);
										
										if (z.get() == R_HL)
										{
											updateMEMHL();
											
											t += 8;
											m += 2;
										}
										
										t += 8;
										m += 2;
										pc.add(2);
										break;
									}
									
									case 3:				// SET y, r[z]
									{
										r[z.get()].setBit(y.get(), 1);
										
										if (z.get() == R_HL)
										{
											updateMEMHL();
											
											t += 8;
											m += 2;
										}
										
										t += 8;
										m += 2;
										pc.add(2);
										break;
									}
								}
								
								break;
							}
							
							case 6:						// DI
							{
								ime = false;
								
								t += 4;
								m += 1;
								pc.add(1);
								break;
							}
							
							case 7:						// EI
							{
								ei = true;
								
								t += 4;
								m += 1;
								pc.add(1);
								break;
							}
						}
						
						break;
					}
					
					case 4:
					{
						switch (y.get())
						{
							case 0:
							
							case 1:
							
							case 2:
							
							case 3:						// CALL cc[y], nn
							{
								pc.add(3);
								
								if (cc[y.get()].get() == 0)
								{
									t += 12;
									m += 3;
									break;
								}
								
								call(nn.get());
								
								t += 24;
								m += 6;
								break;
							}
						}
						
						break;
					}
					
					case 5:
					{
						switch (q.get())
						{
							case 0:						// PUSH rp2[p]
							{
								push(rp2[p.get()].get());
								
								t += 16;
								m += 4;
								pc.add(1);
								break;
							}
							
							case 1:
							{
								if (p.get() == 0)		// CALL nn
								{
									pc.add(3);
									
									call(nn.get());
									
									t += 24;
									m += 6;
								}
								
								break;
							}
						}
						
						break;
					}
					
					case 6:								// alu[y] n
					{
						alu(y.get(), n.get());
						
						t += 8;
						m += 2;
						pc.add(2);
						break;
					}
					
					case 7:								// RST y*8
					{
						call(y.get() * 8);
						
						t += 16;
						m += 4;
						pc.add(1);
						break;
					}
				}
				
				break;
			}
		}
		
		clockt += t;
		clockm += m;
		
		if (t == 0)
		{
			System.out.printf("E: Unrecognized opcode 0x%02X at pc 0x%02X\n", opcode.get(), pc.get());
			pc.add(1);
		}
	}
	
	void call(int address)
	{
		rp[SP].sub(2);
		
		memory[rp[SP].get()].set(pc.subByte(0));
		memory[rp[SP].get() + 1].set(pc.subByte(1));
		
		pc.set(address);
	}
	
	void ret()
	{
		pc.setByte(0, memory[rp[SP].get()].get());
		pc.setByte(1, memory[rp[SP].get() + 1].get());
		
		rp[SP].add(2);
	}
	
	void push(int value)
	{
		rp[SP].sub(2);
		
		memory[rp[SP].get()].set(BitUtil.subByte(0, value));
		memory[rp[SP].get() + 1].set(BitUtil.subByte(1, value));
	}
	
	short pop()
	{
		short stackVal = BitUtil.craftShort(memory[rp[SP].get() + 1].get(), memory[rp[SP].get()].get());
		
		memory[rp[SP].get()].set(0x00);
		memory[rp[SP].get() + 1].set(0x00);
		
		rp[SP].add(2);
		
		return stackVal;
	}
	
	void splitOpcode(UnsignedByte op)
	{
		x.setBit(1, op.getBit(7));
		x.setBit(0, op.getBit(6));
		
		y.setBit(2, op.getBit(5));
		y.setBit(1, op.getBit(4));
		y.setBit(0, op.getBit(3));
		
		z.setBit(2, op.getBit(2));
		z.setBit(1, op.getBit(1));
		z.setBit(0, op.getBit(0));
		
		p.setBit(1, op.getBit(5));
		p.setBit(0, op.getBit(4));
		
		q.setBit(0, op.getBit(3));
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
				flags("Z CA HC", 6, prevA, operand);
				
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
				flags("Z CA HC", 7, prevA, operand + cc[C].get());
				
				break;
			}
			
			case 4:										// AND A
			{
				r[A].and(operand);
				flags("Z CA HC", 2, prevA, operand);
				
				break;
			}
			
			case 5:										// XOR A
			{
				r[A].xor(operand);
				flags("Z CA HC", 3, prevA, operand);
				
				break;
			}
			
			case 6:										// OR A
			{
				r[A].or(operand);
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
	
	void rot(int index, int reg)
	{
		switch (index)
		{
			case 0:										// RLC
			{
				int prevBit7 = r[reg].getBit(7);
				
				r[reg].left(1);
				r[reg].setBit(0, prevBit7);
				
				setCA(prevBit7);
				flags("Z", 0, r[reg].get(), 0);
				cc[HC].set(0);
				
				break;
			}
			
			case 1:										// RRC
			{
				int prevBit0 = r[reg].getBit(0);
				
				r[reg].right(1);
				r[reg].setBit(7, prevBit0);
				
				setCA(prevBit0);
				flags("Z", 0, r[reg].get(), 0);
				cc[HC].set(0);
				
				break;
			}
			
			case 2:										// RL
			{
				int prevBit7 = r[reg].getBit(7);
				
				r[reg].left(1);
				r[reg].setBit(0, cc[CA].get());
				
				setCA(prevBit7);
				flags("Z", 0, r[reg].get(), 0);
				cc[HC].set(0);
				
				break;
			}
			
			case 3:										// RR
			{
				int prevBit0 = r[reg].getBit(0);
				
				r[reg].right(1);
				r[reg].setBit(7, cc[CA].get());
				
				setCA(prevBit0);
				flags("Z", 0, r[reg].get(), 0);
				cc[HC].set(0);
				
				break;
			}
			
			case 4:										// SLA
			{
				int prevBit7 = r[reg].getBit(7);
				int prevBit0 = r[reg].getBit(0);
				
				r[reg].left(1);
				r[reg].setBit(0, prevBit0);
				
				setCA(prevBit7);
				flags("Z", 0, r[reg].get(), 0);
				cc[HC].set(0);
				
				break;
			}
			
			case 5:										// SRA
			{
				int prevBit7 = r[reg].getBit(7);
				int prevBit0 = r[reg].getBit(0);
				
				r[reg].right(1);
				r[reg].setBit(7, prevBit7);
				
				setCA(prevBit0);
				flags("Z", 0, r[reg].get(), 0);
				cc[HC].set(0);
				
				break;
			}
			
			case 6:										// SWAP
			{
				UnsignedByte prevReg = new UnsignedByte(r[reg].b);
				
				r[reg].setBit(0, prevReg.getBit(4));
				r[reg].setBit(1, prevReg.getBit(5));
				r[reg].setBit(2, prevReg.getBit(6));
				r[reg].setBit(3, prevReg.getBit(7));
				
				r[reg].setBit(4, prevReg.getBit(0));
				r[reg].setBit(5, prevReg.getBit(1));
				r[reg].setBit(6, prevReg.getBit(2));
				r[reg].setBit(7, prevReg.getBit(3));
				
				flags("Z", 0, r[reg].get(), 0);
				cc[HC].set(0);
				setCA(0);
				
				break;
			}
			
			case 7:										// SRL
			{
				int prevBit0 = r[reg].getBit(0);
				
				r[reg].right(1);
				r[reg].setBit(7, prevBit0);
				
				setCA(prevBit0);
				flags("Z", 0, r[reg].get(), 0);
				cc[HC].set(0);
				
				break;
			}
		}
	}
	
	void movRP(int index)
	{
		if (index == SP)
		{
			return;
		}
		
		r[(index * 2)].set(rp[index].subByte(1));
		r[(index * 2) + 1].set(rp[index].subByte(0));
	}
	
	void movRP2(int index)
	{
		if (index == AF)
		{
			r[A].set(rp2[AF].subByte(1));
			r[F].set(rp2[AF].subByte(0));
			
			setZ(r[F].getBit(7));
			cc[NE].set(r[F].getBit(6));
			cc[HC].set(r[F].getBit(5));
			setCA(r[F].getBit(4));
		}
		
		else
		{
			r[(index * 2)].set(rp2[index].subByte(1));
			r[(index * 2) + 1].set(rp2[index].subByte(0));
		}
	}
	
	void updateMEMHL()
	{
		memory[rp[HL].get()].set(r[R_HL]);
	}
	
	void flags(String flags, int operation, int fnum, int snum)
	{
		switch (operation)
		{
			case 0:										// ADD flags
			{
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
					boolean hcarry = ((fnum & 0xF) + (snum & 0xF)) > 0xF;
					
					cc[HC].set(hcarry ? 1 : 0);
				}
				
				break;
			}
			
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
					boolean carry = snum > fnum;
					
					setCA(carry ? 1 : 0);
				}
				
				if (flags.contains("HC"))
				{
					boolean hcarry = (snum & 0xF) > (fnum & 0xF);
					
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
			
			case 3:										// XOR flags
			{
				cc[NE].set(0);
				
				if (flags.contains("Z"))
				{
					boolean zero = (fnum ^ snum) == 0;
					
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
			
			case 4:										// OR flags
			{
				cc[NE].set(0);
				
				if (flags.contains("Z"))
				{
					boolean zero = (fnum | snum) == 0;
					
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
			
			case 5:										// CP flags
			{
				cc[NE].set(1);
				
				if (flags.contains("Z"))
				{
					boolean zero = fnum - snum == 0;
					
					setZ(zero ? 1 : 0);
				}
				
				if (flags.contains("CA"))
				{
					boolean carry = snum > fnum;
					
					setCA(carry ? 1 : 0);
				}
				
				if (flags.contains("HC"))
				{
					boolean hcarry = (snum & 0xF) > (fnum & 0xF);
					
					cc[HC].set(hcarry ? 1 : 0);
				}
				
				break;
			}
			
			case 6:										// ADC flags
			{
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
					boolean hcarry = (fnum & 0xF) + (snum & 0xF) + cc[C].get() > 0x0F;
					
					cc[HC].set(hcarry ? 1 : 0);
				}
				
				break;
			}
			
			case 7:										// SBC flags
			{
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
					boolean hcarry = (snum & 0x0F) > ((fnum & 0x0F) - cc[C].get());
					
					cc[HC].set(hcarry ? 1 : 0);
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