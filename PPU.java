// yeah, yeah, this file is called PPU.java, but it really handles all graphics

import littlecube.unsigned.*;

import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.ImageIcon;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.KeyStroke;

import javax.swing.filechooser.*;

import java.awt.Color;

import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import java.awt.image.BufferedImage;

public class PPU implements ActionListener
{
	final static int w = 160;
	final static int h = 144;
	
	static int scale;
	
	static ScanlineRenderer sr;
	
	static TileSelector tileselector;
	
	static BufferedImage mainDisplay;
	static BufferedImage tileDisplay;
	static BufferedImage bgmDisplay;
	
	JFrame frame;
	JFrame tileFrame;
	JFrame bgmFrame;
	JFrame debugFrame;
	JFrame ramFrame;
	
	JMenuBar bar;
	JMenu file;
	JMenu options;
	JMenu debug;
	JMenuItem open;
	JCheckBoxMenuItem bios;
	JMenuItem debugItem;
	JMenuItem ram;
	JMenuItem tile;
	JMenuItem map;
	JMenuItem pause;
	JMenuItem scaleSet;
	JMenuItem sleep;
	
	static JLabel tileItem;
	
	static JLabel bgmItem;
	
	static JTextArea debugText;
	static JTextArea ramText;
	
	JScrollPane scroll;
	
	JFileChooser fc;
	
	String rompath = "";
	
	public PPU()
	{
		scale = 5;
		
		init();
	}
	
	public void init()
	{
		fc = new JFileChooser();
		
		frame = new JFrame("GrumpBoy");
		tileFrame = new JFrame("Tile View");
		bgmFrame = new JFrame("Map View");
		
		open = new JMenuItem("Open");
		bios = new JCheckBoxMenuItem("Use BIOS");
		debugItem = new JMenuItem("Debugger");
		ram = new JMenuItem("RAM View");
		tile = new JMenuItem("Tile View");
		map = new JMenuItem("Map View");
		pause = new JMenuItem("Pause");
		scaleSet = new JMenuItem("Set Scale");
		sleep = new JMenuItem("Sleep Value");
		file = new JMenu("File");
		options = new JMenu("Options");
		debug = new JMenu("Debug");
		bar = new JMenuBar();
		
		open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		debugItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));
		ram.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
		tile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK));
		map.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.CTRL_MASK));
		pause.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK));
		
		resetMainDisplay();
		
		bgmDisplay = new BufferedImage(256 * scale, 256 * scale, BufferedImage.TYPE_INT_RGB);
		
		bgmItem = new JLabel(new ImageIcon(bgmDisplay));
		bgmFrame.add(bgmItem);
		
		tileselector = new TileSelector();
		
		sr = new ScanlineRenderer();
		
		sr.turnOffDisplay();
		
		file.add(open);
		
		debug.add(debugItem);
		debug.add(ram);
		debug.add(tile);
		debug.add(map);
		
		debug.addSeparator();
		
		debug.add(pause);
		debug.add(sleep);
		
		options.add(bios);
		
		options.addSeparator();
		
		options.add(scaleSet);
		
		bar.add(file);
		bar.add(options);
		bar.add(debug);
		
		frame.setJMenuBar(bar);
		
		pause.setEnabled(false);
		
		open.addActionListener(this);
		bios.addActionListener(this);
		debugItem.addActionListener(this);
		ram.addActionListener(this);
		tile.addActionListener(this);
		map.addActionListener(this);
		pause.addActionListener(this);
		scaleSet.addActionListener(this);
		sleep.addActionListener(this);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.pack();
		frame.setResizable(false);
		frame.setVisible(true);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		Object src = e.getSource();
		
		if (src == open)
		{
			GB.cpu.run = false;
			
			SwingUtilities.updateComponentTreeUI(fc);
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int fci = fc.showOpenDialog(null);
			
			if (fci == JFileChooser.APPROVE_OPTION)
			{
				rompath = fc.getSelectedFile().getAbsolutePath();
				
				pause.setEnabled(true);
				
				GB.reset();
				GB.cpu.loadGame(rompath);
				GB.cpu.loadBIOS();
				
				frame.setTitle("GrumpBoy - " + getROMTitle());
			}
		}
		
		if (src == bios)
		{
			GB.useBIOS = bios.getState();
		}
		
		if (src == debugItem)
		{
			debugWindow();
		}
		
		if (src == ram)
		{
			ramWindow();
		}
		
		if (src == tile)
		{
			tileWindow();
		}
		
		if (src == map)
		{
			bgmWindow();
		}
		
		if (src == pause)
		{
			GB.cpu.run ^= true;
		}
		
		if (src == scaleSet)
		{
			String input = JOptionPane.showInputDialog(frame, "What scale should the display be?", scale);
			
			int newScale = 0;
			
			if (input == null)
			{
				newScale = scale;
			}
			
			else
			{
				newScale = Integer.parseInt(input);
			}
			
			scale = newScale;
			
			resetMainDisplay();
			
			frame.pack();
		}
		
		if (src == sleep)
		{
			String input = JOptionPane.showInputDialog(frame, "How many milliseconds between cycles?", GB.millisleeps);
			
			int newsleeps = 0;
			
			if (input == null)
			{
				newsleeps = GB.millisleeps;
			}
			
			else
			{
				newsleeps = Integer.parseInt(input);
			}
			
			GB.millisleeps = newsleeps;
		}
	}
	
	void resetMainDisplay()
	{
		frame.getContentPane().removeAll();
		
		mainDisplay = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
		
		JLabel item = new JLabel(new ImageIcon(mainDisplay));
		frame.add(item);
	}
	
	void updateMainFrame()
	{
		frame.repaint();
	}
	
	void debugWindow()
	{
		if (!GB.debug)
		{
			debugText = new JTextArea(30, 20);
			debugText.setEnabled(false);
			
			debugFrame = new JFrame("Debugger");
			
			debugFrame.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					GB.debug = false;
					debugFrame.dispose();
				}
			});
			
			debugFrame.add(debugText);
			debugFrame.pack();
			
			debugFrame.setLocationRelativeTo(null);
			debugFrame.setResizable(false);
			
			debugFrame.setVisible(true);
			
			GB.debug = true;
			GB.cpu.debug();
		}
		
		else
		{
			debugFrame.requestFocus();
		}
	}
	
	void ramWindow()
	{
		if (!GB.ram)
		{
			ramText = new JTextArea(1, 23);
			ramText.setEnabled(false);
			
			scroll = new JScrollPane(ramText);
			
			ramFrame = new JFrame("Memory View");
			ramFrame.add(scroll);
			ramFrame.pack();
			
			ramFrame.setLocationRelativeTo(null);
			
			ramFrame.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					GB.ram = false;
					ramFrame.dispose();
				}
			});
			
			ramFrame.setResizable(false);
			
			ramFrame.setVisible(true);
			
			GB.ram = true;
			GB.cpu.ram();
		}
		
		else
		{
			ramFrame.requestFocus();
		}
	}
	
	void tileWindow()
	{
		if (!GB.tile)
		{
			if (tileDisplay == null)
			{
				tileDisplay = new BufferedImage(16 * 8 * scale, 24 * 8 * scale, BufferedImage.TYPE_INT_RGB);
				
				tileItem = new JLabel(new ImageIcon(tileDisplay));
				tileFrame.add(tileItem);
			}
			
			updateTileWindow();
			
			tileFrame.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					GB.tile = false;
					tileFrame.dispose();
				}
			});
			
			tileFrame.pack();
			tileFrame.setResizable(false);
			tileFrame.setVisible(true);
			
			GB.tile = true;
		}
		
		else
		{
			tileFrame.requestFocus();
		}
	}
	
	void updateTileWindow()
	{
		TileMap.updateDisplay();
		
		PixelOps.setDisplay(tileDisplay, TileMap.tilemap);
		
		tileFrame.repaint();
	}
	
	void bgmWindow()
	{
		if (!GB.map)
		{
			updateBGMWindow();
			
			bgmFrame.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					GB.map = false;
					bgmFrame.dispose();
				}
			});
			
			bgmItem = new JLabel(new ImageIcon(bgmDisplay));
			bgmFrame.add(bgmItem);
			
			bgmFrame.pack();
			bgmFrame.setResizable(false);
			bgmFrame.setVisible(true);
			
			GB.map = true;
		}
		
		else
		{
			bgmFrame.requestFocus();
		}
	}
	
	void updateBGMWindow()
	{
		BGMap.updateDisplay();
		
		PixelOps.setDisplay(bgmDisplay, BGMap.bgmap);
		
		bgmFrame.repaint();
	}
	
	String getROMTitle()
	{
		String title = "";
		
		for (int i = 0x134; i <= 0x143; i++)
		{
			title += (char) GB.cpu.memory[i].get();
		}
		
		return title;
	}
	
	
	
	class ScanlineRenderer
	{
		final int frameLength = 15;
		
		UnsignedByte lcdc;
		
		UnsignedByte stat;
		
		UnsignedByte lyc;
		
		UnsignedByte ly;
		
		UnsignedByte scx;
		
		UnsignedByte scy;
		
		UnsignedByte wx;
		
		UnsignedByte wy;
		
		int clocks;
		
		int currClocks;
		
		int lx;
		
		int internalWLine;
		
		int gfx[];
		
		long frameStartTime;
		long remainderTime;
		
		boolean incWLine;
		
		private ScanlineRenderer()
		{
			gfx = new int[w * h];
			
			init();
		}
		
		void init()
		{
			lcdc = new UnsignedByte();
			
			stat = new UnsignedByte();
			
			lyc = new UnsignedByte();
			
			ly = new UnsignedByte();
			writeLY();
			
			scx = new UnsignedByte();
			scy = new UnsignedByte();
			
			wx = new UnsignedByte();
			wy = new UnsignedByte();
			
			GB.cpu.memory[0xFF43].set(scx);
			GB.cpu.memory[0xFF42].set(scy);
			
			lx = 0;
			
			internalWLine = 0;
			
			frameStartTime = 0;
			remainderTime = 0;
			
			incWLine = false;
			
			setMode(2);
			
			currClocks = -1;
			
			GB.cpu.mmu.lockRange(0xFE00, 0xFE9F);
		}
		
		void getRegisters()
		{
			lcdc.set(GB.cpu.memory[0xFF40].get());
			
			stat.set(GB.cpu.memory[0xFF41].get());
			
			lyc.set(GB.cpu.memory[0xFF45].get());
			
			scx.set(GB.cpu.memory[0xFF43].get());
			
			scy.set(GB.cpu.memory[0xFF42].get());
			
			wx.set(GB.cpu.memory[0xFF4B].get());
			
			wy.set(GB.cpu.memory[0xFF4A].get());
		}
		
		void writeLY()
		{
			if (ly.get() == lyc.get())
			{
				stat.setBit(2, 1);
				
				if (stat.getBit(6) == 1)
				{
					GB.cpu.memory[CPU.IF].setBit(1, 1);
				}
			}
			
			else
			{
				stat.setBit(2, 0);
			}
			
			GB.cpu.memory[0xFF44].set(ly);
		}
		
		void clock(int newClocks)
		{
			clocks += newClocks;
			
			getRegisters();
			
			while (clocks > 0)
			{
				currClocks++;
				clocks--;
				
				switch (getMode())
				{
					case 0:
					{
						if (currClocks % 456 == 0)
						{
							if (ly.get() >= 143)
							{
								GB.cpu.memory[CPU.IF].setBit(0, 1);
								
								setMode(1);
							}
							
							else if (ly.get() < 143)
							{
								if (stat.getBit(3) == 1)
								{
									GB.cpu.memory[CPU.IF].setBit(1, 1);
								}
								
								if (incWLine)
								{
									internalWLine++;
									
									incWLine = false;
								}
								
								setMode(2);
							}
							
							ly.add(1);
							writeLY();
						}
						
						break;
					}
					
					case 1:
					{
						if (currClocks % 456 == 0 && currClocks != 70224)
						{
							ly.add(1);
							writeLY();
						}
						
						else if (currClocks == 70224)
						{
							PixelOps.setDisplay(mainDisplay, gfx);
							
							updateMainFrame();
							
							ly.set(0);
							writeLY();
							
							internalWLine = 0;
							
							currClocks = -1;
							
							setMode(2);
							
							try
							{
								if ((remainderTime = System.currentTimeMillis() - frameStartTime) < frameLength)
								{
									Thread.sleep(frameLength - remainderTime);
								}
								
								frameStartTime = System.currentTimeMillis();
							}
							
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}
						
						break;
					}
					
					case 2:
					{
						// TODO: add OAM search stuffs
						
						if (currClocks % 456 == 80)
						{
							setMode(3);
							
							GB.cpu.mmu.unlockRange(0x8000, 0x9FFF);		// mode 0 unlocks VRAM
							GB.cpu.mmu.unlockRange(0xFE00, 0xFE9F);		// and OAM
						}
						
						else if (currClocks % 456 > 80)
						{
							System.out.println("currClocks % 456 > 80");
						}
						
						break;
					}
					
					case 3:
					{
						nextPixel();
						
						lx++;
						
						if (lx == 160)
						{
							setMode(0);
							
							lx = 0;
						}
						
						break;
					}
				}
			}
		}
		
		void nextPixel()
		{
			try
			{
				int possiblePixel = 0;
				
				if (lcdc.getBit(0) == 1)
				{
					if (lcdc.getBit(5) == 1 && lx >= (wx.get() - 7) && ly.get() >= wy.get())
					{
						incWLine = true;
						
						possiblePixel = getWinPixel();
					}
					
					else
					{
						possiblePixel = getBGPixel();
					}
				}
				
				else
				{
					possiblePixel = tileselector.BGP.color(0);
				}
				
				gfx[lx + (ly.get() * w)] = possiblePixel;
			}
			
			catch (Exception e)
			{
				e.printStackTrace();
				
				System.out.println("lx: " + lx + "\nly: " + ly.get() + "\nscx: " + scx.get() + "\nscy: " + scy.get());
				System.exit(0);
			}
		}
		
		int getBGPixel()
		{
			int tile[] = tileselector.BGMapTileData(((lx + scx.get()) / 8) + (((ly.get() + scy.get()) / 8) * 32));
			
			return tileselector.BGP.color(tile[(((lx + scx.get()) % 8) + (((ly.get() + scy.get()) % 8) * 8))]);
		}
		
		int getWinPixel()
		{
			int tile[] = tileselector.WinMapTileData((((lx - (wx.get() - 7))) / 8) + ((internalWLine / 8) * 32));
			
			return tileselector.BGP.color(tile[(((lx - (wx.get() - 7)) % 8) + (((internalWLine % 8) * 8)))]);
		}
		
		void turnOffDisplay()
		{
			fillDisplay(Palette.OFF);
		}
		
		void turnOnDisplay()
		{
			fillDisplay(0);
			
			setMode(2);
			
			getRegisters();
			
			lx = 0;
			
			ly.set(0);
		}
		
		void fillDisplay(int gbcolor)
		{
			int fillbytes[] = new int[w * h];
			
			for (int i = 0; i < fillbytes.length; i++)
			{
				fillbytes[i] = tileselector.BGP.trueColor(gbcolor);
			}
			
			PixelOps.setDisplay(mainDisplay, fillbytes);
			
			updateMainFrame();
		}
		
		int getMode()
		{
			return stat.subBits(0, 1);
		}
		
		void setMode(int newMode)
		{
			stat.setBits(0, 1, newMode);
			
			for (int i = 0; i < 3; i++)
			{
				if (stat.getBit(i + 3) == 1 && getMode() == i)
				{
					GB.cpu.memory[CPU.IF].setBit(1, 1);
				}
			}
			
			GB.cpu.memory[0xFF41].set(stat);
		}
	}
	
	
	
	private static abstract class TileMap
	{
		static int tilemap[] = new int[16 * 8 * 24 * 8];
		
		static BufferedImage display = new BufferedImage(16 * 8 * scale, 24 * 8 * scale, BufferedImage.TYPE_INT_RGB);
		
		static void updateDisplay()
		{
			Thread thread = new Thread()
			{
				public void run()
				{
					int gfx[] = new int[16 * 8 * 24 * 8];
					
					for (int y = 0; y < 24; y++)
					{
						for (int x = 0; x < 16; x++)
						{
							PixelOps.drawBGTile((x + (y * 16)), ((x * 8) + ((y * 8) * 128)), display.getWidth() / scale, tilemap);
						}
					}
				}
			};
			
			thread.start();
		}
	}
	
	private static abstract class BGMap
	{
		static int bgmap[] = new int[256 * 256];
		
		static void updateDisplay()
		{
			Thread thread = new Thread()
			{
				public void run()
				{
					for (int y = 0; y < 32; y++)
					{
						for (int x = 0; x < 32; x++)
						{
							int mapOffset;
							
							if (sr.lcdc.getBit(3) == 0)
							{
								mapOffset = TileSelector.MAP1;
							}
							
							else
							{
								mapOffset = TileSelector.MAP2;
							}
							
							PixelOps.drawBGTile(GB.cpu.memory[mapOffset + (x + (y * 32))].get(), ((x * 8) + ((y * 8) * 256)), bgmDisplay.getWidth() / scale, bgmap);
						}
					}
				}
			};
			
			thread.start();
		}
	}
	
	private static abstract class WinMap
	{
		static int winmap[] = new int[256 * 256];
		
		static void updateDisplay()
		{
			Thread thread = new Thread()
			{
				public void run()
				{
					for (int y = 0; y < 32; y++)
					{
						for (int x = 0; x < 32; x++)
						{
							int mapOffset = tileselector.getWinMapOffset();
							
							PixelOps.drawBGTile(GB.cpu.memory[mapOffset + (x + (y * 32))].get(), ((x * 8) + ((y * 8) * 256)), bgmDisplay.getWidth() / scale, winmap);
						}
					}
				}
			};
			
			thread.start();
		}
	}
	
	
	
	private class Palette
	{
		final Color trueColors[] = { new Color(155, 188, 15),
									 new Color(139, 172, 15),
									 new Color(48, 98, 48),
									 new Color(15, 56, 15),
									 new Color(131, 118, 38) };
		
		final static int OFF = 4;
		
		UnsignedByte palRegister;
		
		int palIndex;
		
		private Palette()
		{
			setRegister(-1);
			
			init();
		}
		
		private Palette(int offset)
		{
			setRegister(offset);
			
			init();
			
			updatePalette();
		}
		
		private void init()
		{
			palRegister = new UnsignedByte();
		}
		
		void setRegister(int offset)
		{
			palIndex = offset;
		}
		
		void updatePalette()
		{
			palRegister.set(GB.cpu.mmu.read(palIndex));
		}
		
		int color(int color)
		{
			updatePalette();
			
			UnsignedByte subPal = new UnsignedByte();
			
			if (color < 4)
			{
				subPal.setBit(0, palRegister.getBit(color * 2));
				subPal.setBit(1, palRegister.getBit((color * 2) + 1));
			}
			
			else if (color == OFF)
			{
				subPal.set(OFF);
			}
			
			else
			{
				subPal.set(-1);
			}
			
			return trueColors[subPal.get()].getRGB();
		}
		
		int trueColor(int color)
		{
			return trueColors[color].getRGB();
		}
	}
	
	private class Tile
	{
		final static int LEN = 16;
		
		final static int VRAM = 0x8000;
		
		int index;
		
		private Tile()
		{
			setIndex(-1);
		}
		
		private Tile(int newIndex)
		{
			setIndex(newIndex);
		}
		
		void setIndex(int newIndex)
		{
			index = newIndex;
		}
	}
	
	private class TileSet
	{
		final static int LEN = 384;
		
		Tile tiles[];
		
		private TileSet()
		{
			tiles = new Tile[LEN];
			
			initTiles();
		}
		
		void initTiles()
		{
			for (int i = 0; i < tiles.length; i++)
			{
				tiles[i] = new Tile();
				
				tiles[i].setIndex(Tile.VRAM + (i * Tile.LEN));
			}
		}
	}
	
	private class TileSelector
	{
		final static int MAP1 = 0x9800;
		final static int MAP2 = 0x9C00;
		
		Palette BGP;
		Palette OBP1;
		Palette OBP2;
		
		TileSet tileset;
		
		private TileSelector()
		{
			tileset = new TileSet();
			
			initPalettes();
		}
		
		void initPalettes()
		{
			BGP = new Palette();
			OBP1 = new Palette();
			OBP2 = new Palette();
			
			BGP.setRegister(0xFF47);
			OBP1.setRegister(0xFF48);
			OBP2.setRegister(0xFF49);
			
			updatePalettes();
		}
		
		void updatePalettes()
		{
			BGP.updatePalette();
			OBP1.updatePalette();
			OBP2.updatePalette();
		}
		
		int[] BGMapTileData(int indexNo)
		{
			return BGTileData(GB.cpu.memory[getBGMapOffset() + indexNo].get());
		}
		
		int[] WinMapTileData(int indexNo)
		{
			return BGTileData(GB.cpu.memory[getWinMapOffset() + indexNo].get());
		}
		
		int[] BGTileData(int tileNo)
		{
			updatePalettes();
			
			int tileOffset = 0;
			
			if (sr.lcdc.getBit(4) == 0)
			{
				tileNo = (byte) tileNo;
				tileOffset = 256;
			}
			
			int index = tileset.tiles[tileOffset + tileNo].index;
			
			int tileData[] = new int[64];
			
			UnsignedByte pixel = new UnsignedByte();
			
			UnsignedShort tempData[] = new UnsignedShort[8];
			
			for (int i = 0; i < tempData.length; i++)
			{
				tempData[i] = new UnsignedShort();
			}
			
			for (int i = 0; i < 8; i++)
			{
				tempData[i].setByte(0, GB.cpu.memory[index + (i * 2) + 1].get());
				tempData[i].setByte(1, GB.cpu.memory[index + (i * 2)].get());
			}
			
			for (int i = 0; i < 8; i++)
			{
				for (int j = 7; j >= 0; j--)
				{
					pixel.setBit(0, tempData[i].getBit((j) + 8));
					pixel.setBit(1, tempData[i].getBit(j));
					
					tileData[Math.abs(j - 7) + (i * 8)] = pixel.get();
				}
			}
			
			return tileData;
		}
		
		int getBGMapOffset()
		{
			int mapOffset = 0;
			
			if (sr.lcdc.getBit(3) == 0)
			{
				mapOffset = TileSelector.MAP1;
			}
			
			else
			{
				mapOffset = TileSelector.MAP2;
			}
			
			return mapOffset;
		}
		
		int getWinMapOffset()
		{
			int mapOffset = 0;
			
			if (sr.lcdc.getBit(6) == 0)
			{
				mapOffset = TileSelector.MAP1;
			}
			
			else
			{
				mapOffset = TileSelector.MAP2;
			}
			
			return mapOffset;
		}
	}
	
	
	
	private static abstract class PixelOps
	{
		static void setDisplay(BufferedImage screen, int gfx[])
		{
			int width = ((screen.getWidth()) / scale);
			int height = ((screen.getHeight()) / scale);
			
			for (int x = 0; x < width; x++)
			{
				for (int y = 0; y < height; y++)
				{
					drawPixel(x, y, screen, gfx[x + (y * width)]);
				}
			}
		}
		
		static void drawPixel(int x, int y, BufferedImage screen, int color)
		{
			int width = ((screen.getWidth()) / scale);
			
			for (int i = 0; i < scale; i++)
			{
				for (int j = 0; j < scale; j++)
				{
					screen.setRGB((x * scale) + i, (y * scale) + j, color);
				}
			}
		}
		
		static void drawBGTile(int tileNo, int offset, int width, int fb[])
		{
			for (int y = 0; y < 8; y++)
			{
				for (int x = 0; x < 8; x++)
				{
					fb[offset + (x + (y * width))] = tileselector.BGP.color(tileselector.BGTileData(tileNo)[x + (y * 8)]);
				}
			}
		}
	}
}