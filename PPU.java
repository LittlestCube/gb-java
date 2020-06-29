// yeah, yeah, this file is called PPU.java, but it really handles all graphics

import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.ImageIcon;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.KeyStroke;

import javax.swing.filechooser.*;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import java.awt.image.BufferedImage;

public class PPU implements ActionListener
{
	JFrame frame;
	JFrame debugFrame;
	JMenuBar bar;
	JMenu file;
	JMenu debug;
	JMenuItem open;
	JMenuItem debugItem;
	
	JTextArea debugText;
	
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
		file = new JMenu("File");
		debug = new JMenu("Debug");
		bar = new JMenuBar();
		
		open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		
		display = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB);
		JLabel item = new JLabel(new ImageIcon(display));
		frame.add(item);
		
		file.add(open);
		debug.add(debugItem);
		bar.add(file);
		bar.add(debug);
		frame.setJMenuBar(bar);
		
		open.addActionListener(this);
		debugItem.addActionListener(this);
		
		frame.pack();
		frame.setVisible(true);
	}
	
	void debugWindow()
	{
		debugText = new JTextArea(30, 20);
		debugFrame = new JFrame("Debugger");
		debugFrame.add(debugText);
		debugFrame.pack();
		GB.debug = true;
		debugFrame.setVisible(true);
		GB.cpu.debug();
	}
	
	public void actionPerformed(ActionEvent e)
	{
		Object src = e.getSource();
		
		if (src == open)
		{
			SwingUtilities.updateComponentTreeUI(fc);
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int fci = fc.showOpenDialog(null);
			
			if (fci == JFileChooser.APPROVE_OPTION) {
				rompath = fc.getSelectedFile().getAbsolutePath();
				
				try
				{
					GB.cpu.loadGame(rompath);
				}
				
				catch (Exception exc)
				{
					System.out.println("E: Couldn't read file.");
					exc.printStackTrace();
				}
			}
		}
		
		if (src == debugItem)
		{
			debugWindow();
		}
	}
}