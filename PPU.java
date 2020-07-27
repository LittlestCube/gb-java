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
	JFrame frame;
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
	
	final int w = 160;
	final int h = 144;
	
	final int scale = 4;
	
	public PPU()
	{
		init();
	}
	
	public void init()
	{
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
		
		byte[] off = new byte[w * h];
		
		for (int i = 0; i < off.length; i++)
		{
			off[i] = Palette.OFF;
		}
		
		display = getDisplay(off);
		
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
	
	BufferedImage getDisplay(byte[] gfx)
	{
		BufferedImage screen = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
		
		Palette palette = new Palette(0xFF47);
		
		for (int x = 0; x < w; x++)
		{
			for (int y = 0; y < h; y++)
			{
				for (int i = 0; i < scale; i++)
				{
					for (int j = 0; j < scale; j++)
					{
						screen.setRGB(x * scale + i, y * scale + j, palette.color(gfx[x + (y * w)]));
					}
				}
			}
		}
		
		return screen;
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
			
			UnsignedByte subBGP = new UnsignedByte();
			
			if (color < 4)
			{
				subBGP.setBit(0, palRegister.getBit(color * 2));
				subBGP.setBit(1, palRegister.getBit((color * 2) + 1));
			}
			
			else if (color == OFF)
			{
				subBGP.set(OFF);
			}
			
			else
			{
				subBGP.set(-1);
			}
			
			return trueColors[subBGP.get()].getRGB();
		}
	}
	
	private class Tile
	{
		final static int LEN = 16;
		
		int index;
		
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
			tileData = new UnsignedByte[LEN];
		}
		
		void setIndex(int newIndex)
		{
			index = newIndex;
		}
		
		void updateData()
		{
			for (int i = 0; i < LEN; i++)
			{
				tileData[i] = GB.cpu.mmu.read(index + i);
			}
		}
	}
	
	private class TileSet
	{
		final static int LEN = 384;
		
		final static int VRAM = 0x8000;
		
		Palette palette;
		
		Tile tiles[];
		
		private TileSet()
		{
			tiles = new Tile[LEN];
			
			for (int i = 0; i < tiles.length; i++)
			{
				tiles[i].setIndex(VRAM + (i * 16));
				
				System.out.println(VRAM + (i * 16));
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
}