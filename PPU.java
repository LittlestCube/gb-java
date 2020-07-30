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
	final static int scale = 2;
	
	int bgm[] = new int[256 * 256];
	
	static FIFO fifo;
	
	static TileSelector tileselector;
	
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
	JMenuItem sleep;
	
	static BufferedImage tileDisplay;
	static JLabel tileItem;
	
	static BufferedImage bgmDisplay;
	static JLabel bgmItem;
	
	static JTextArea debugText;
	static JTextArea ramText;
	
	JScrollPane scroll;
	
	JFileChooser fc;
	
	String rompath = "";
	
	public PPU()
	{
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
		
		tileselector = new TileSelector();
		
		fifo = new FIFO();
		
		fifo.turnOffDisplay();
		
		JLabel item = new JLabel(new ImageIcon(fifo.display));
		frame.add(item);
		
		file.add(open);
		debug.add(debugItem);
		debug.add(ram);
		debug.add(tile);
		debug.add(map);
		debug.addSeparator();
		debug.add(pause);
		debug.add(sleep);
		options.add(bios);
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
		PixelOps.getTileDisplay(tileDisplay, tileselector);
		
		tileFrame.repaint();
	}
	
	void bgmWindow()
	{
		if (!GB.map)
		{
			if (bgmDisplay == null)
			{
				bgmDisplay = new BufferedImage(256 * scale, 256 * scale, BufferedImage.TYPE_INT_RGB);
				
				bgmItem = new JLabel(new ImageIcon(bgmDisplay));
				bgmFrame.add(bgmItem);
			}
			
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
		PixelOps.getBGMDisplay(bgmDisplay, tileselector);
		
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
	
	
	
	// Let the torment begin...
	
	class FIFO
	{
		final static int w = 160;
		final static int h = 144;
		
		int gfx[] = new int[w * h];
		
		int buffer[] = new int[16];
		
		BufferedImage display;
		
		private FIFO()
		{
			display = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
		}
		
		void turnOffDisplay()
		{
			fillDisplay(Palette.OFF);
		}
		
		void turnOnDisplay()
		{
			fillDisplay(0);
		}
		
		void fillDisplay(int gbcolor)
		{
			int fillbytes[] = new int[w * h];
			
			for (int i = 0; i < fillbytes.length; i++)
			{
				fillbytes[i] = tileselector.BGP.trueColor(gbcolor);
			}
			
			PixelOps.setDisplay(display, fillbytes);
			
			updateMainFrame();
		}
	}
	
	
	
	// Tiles, tiles, tiles...
	
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
				
				tiles[i].setIndex(Tile.VRAM + (i * 0x10));
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
		
		int[] tileData(int tileNo)
		{
			updatePalettes();
			
			int index = tileset.tiles[tileNo].index;
			
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
					for (int i = 0; i < scale; i++)
					{
						for (int j = 0; j < scale; j++)
						{
							screen.setRGB((x * scale) + i, (y * scale) + j, gfx[x + (y * width)]);
						}
					}
				}
			}
		}
		
		static BufferedImage getTileDisplay(BufferedImage screen, TileSelector ts)
		{
			int gfx[] = new int[16 * 8 * 24 * 8];
			
			for (int y = 0; y < 24; y++)
			{
				for (int x = 0; x < 16; x++)
				{
					drawTile((x + (y * 16)), gfx, screen.getWidth() / scale, ((x * 8) + ((y * 8) * 128)));
				}
			}
			
			setDisplay(screen, gfx);
			
			return screen;
		}
		
		static BufferedImage getBGMDisplay(BufferedImage screen, TileSelector ts)
		{
			int gfx[] = new int[256 * 256];
			
			for (int y = 0; y < 32; y++)
			{
				for (int x = 0; x < 32; x++)
				{
					drawTile(GB.cpu.memory[TileSelector.MAP1 + (x + (y * 32))].get(), gfx, screen.getWidth() / scale, ((x * 8) + ((y * 8) * 256)));
				}
			}
			
			setDisplay(screen, gfx);
			
			return screen;
		}
		
		static int[] drawTile(int tileNo, int fb[], int widthOfDisplay, int offset)
		{
			for (int y = 0; y < 8; y++)
			{
				for (int x = 0; x < 8; x++)
				{
					fb[offset + (x + (y * widthOfDisplay))] = tileselector.BGP.color(tileselector.tileData(tileNo)[x + (y * 8)]);
				}
			}
			
			return fb;
		}
	}
}