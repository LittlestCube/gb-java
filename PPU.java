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
	TileSet tileset;
	Palette BGP;
	
	JFrame frame;
	JFrame tileFrame;
	JFrame debugFrame;
	JFrame ramFrame;
	JMenuBar bar;
	JMenu file;
	JMenu debug;
	JMenuItem open;
	JMenuItem debugItem;
	JMenuItem ram;
	JMenuItem pause;
	JMenuItem sleep;
	
	static JTextArea debugText;
	static JTextArea ramText;
	
	JScrollPane scroll;
	
	JFileChooser fc;
	
	BufferedImage display;
	
	String rompath = "";
	
	final static int w = 160;
	final static int h = 144;
	
	final static int scale = 4;
	
	public PPU()
	{
		init();
	}
	
	public void init()
	{
		BGP = new Palette(0xFF47);
		
		fc = new JFileChooser();
		
		frame = new JFrame("GrumpBoy");
		
		open = new JMenuItem("Open");
		debugItem = new JMenuItem("Debugger");
		ram = new JMenuItem("RAM View");
		pause = new JMenuItem("Pause");
		sleep = new JMenuItem("Sleep Value");
		file = new JMenu("File");
		debug = new JMenu("Debug");
		bar = new JMenuBar();
		
		open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		debugItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));
		ram.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
		pause.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK));
		
		int off[] = new int[w * h];
		
		for (int i = 0; i < off.length; i++)
		{
			off[i] = BGP.color(Palette.OFF);
		}
		
		display = PixelOps.getDisplay(off);
		
		tileset = new TileSet();
		
		tileset.setPalettes(BGP);
		
		tileWindow();
		
		JLabel item = new JLabel(new ImageIcon(display));
		frame.add(item);
		
		file.add(open);
		debug.add(debugItem);
		debug.add(ram);
		debug.addSeparator();
		debug.add(pause);
		debug.add(sleep);
		bar.add(file);
		bar.add(debug);
		frame.setJMenuBar(bar);
		
		pause.setEnabled(false);
		
		open.addActionListener(this);
		debugItem.addActionListener(this);
		ram.addActionListener(this);
		pause.addActionListener(this);
		sleep.addActionListener(this);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.pack();
		frame.setVisible(true);
	}
	
	void tileWindow()
	{
		tileFrame = new JFrame();
		
		tileFrame.setVisible(false);
		
		updateTileWindow();
		
		tileFrame.pack();
		tileFrame.setVisible(true);
	}
	
	void updateTileWindow()
	{
		JLabel item = new JLabel(new ImageIcon(PixelOps.getTileDisplay(tileset)));
		tileFrame.add(item);
		
		tileFrame.repaint();
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
			
			ramFrame.setVisible(true);
			
			GB.ram = true;
			GB.cpu.ram();
		}
		
		else
		{
			ramFrame.requestFocus();
		}
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
			
			if (fci == JFileChooser.APPROVE_OPTION) {
				rompath = fc.getSelectedFile().getAbsolutePath();
				
				pause.setEnabled(true);
				
				GB.reset();
				GB.cpu.loadGame(rompath);
				GB.cpu.loadBIOS();
				
				frame.setTitle("GrumpBoy - " + getROMTitle());
			}
		}
		
		if (src == debugItem)
		{
			debugWindow();
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
		
		if (src == ram)
		{
			ramWindow();
		}
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
	}
	
	private class Tile
	{
		final static int LEN = 16;
		
		final static int VRAM = 0x8000;
		
		int index;
		
		Palette palette;
		
		UnsignedByte tileData[];
		
		private Tile()
		{
			index = -1;
			
			init();
		}
		
		private Tile(int newIndex)
		{
			setIndex(newIndex);
			
			init();
		}
		
		void init()
		{
			tileData = new UnsignedByte[LEN * 4];
			
			for (int i = 0; i < tileData.length; i++)
			{
				tileData[i] = new UnsignedByte();
			}
		}
		
		void setPalette(Palette newPalette)
		{
			palette = newPalette;
		}
		
		void setIndex(int newIndex)
		{
			index = newIndex;
		}
		
		void updateData()
		{
			UnsignedByte pixel = new UnsignedByte();
			
			for (int i = 0; i < 16; i++)
			{
				for (int j = 0; j < 4; j++)
				{
					pixel.setBit(0, GB.cpu.mmu.read(VRAM + i).getBit(j * 2));
					pixel.setBit(1, GB.cpu.mmu.read(VRAM + i).getBit((j * 2) + 1));
					
					tileData[j + (i * 4)].set(pixel);
				}
			}
		}
	}
	
	private class TileSet
	{
		final static int LEN = 384;
		
		Palette palette;
		
		Tile tiles[];
		
		private TileSet()
		{
			tiles = new Tile[LEN];
			
			initTiles();
			
			updateTiles();
		}
		
		void setPalettes(Palette newPalette)
		{
			for (Tile tile : tiles)
			{
				tile.setPalette(newPalette);
			}
		}
		
		void initTiles()
		{
			for (int i = 0; i < tiles.length; i++)
			{
				tiles[i] = new Tile();
				
				tiles[i].setIndex(Tile.VRAM + (i * 0x10));
			}
		}
		
		void updateTiles()
		{
			for (Tile tile : tiles)
			{
				tile.updateData();
			}
		}
	}
	
	
	
	private static abstract class PixelOps
	{
		static BufferedImage getDisplay(int gfx[])
		{
			BufferedImage screen = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
			
			setDisplay(screen, gfx);
			
			return screen;
		}
		
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
							screen.setRGB(x * scale + i, y * scale + j, gfx[x + (y * width)]);
						}
					}
				}
			}
		}
		
		static BufferedImage getTileDisplay(TileSet tileset)
		{
			BufferedImage screen = new BufferedImage(tileset.LEN / 6, tileset.LEN, BufferedImage.TYPE_INT_RGB);
			
			int gfx[] = new int[tileset.LEN * 384];
			
			tileset.updateTiles();
			
			drawTile(tileset.tiles[0], gfx, 0);
			
			for (int x = 0; x < 8; x++)
			{
				for (int y = 0; y < 48; y++)
				{
					
				}
			}
			
			setDisplay(screen, gfx);
			
			return screen;
		}
		
		static int[] drawTile(Tile tile, int fb[], int offset)
		{
			tile.palette.updatePalette();
			
			tile.updateData();
			
			for (int y = 0; y < 8; y++)
			{
				for (int x = 0; y < 8; y++)
				{
					
				}
			}
			
			return fb;
		}
	}
	
	private class BGMap
	{
		
	}
}