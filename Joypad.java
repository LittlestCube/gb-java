import littlecube.unsigned.*;

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

public class Joypad implements KeyListener
{
	UnsignedByte keys;
	
	UnsignedByte tempkeys;
	UnsignedByte directions;
	UnsignedByte buttons;
	
	boolean trueKeys[];
	
	Joypad()
	{
		keys = new UnsignedByte(0xFF);
		
		tempkeys = new UnsignedByte();
		directions = new UnsignedByte();
		buttons = new UnsignedByte();
		
		trueKeys = new boolean[8];
		
		for (int i = 0; i < trueKeys.length; i++)
		{
			trueKeys[i] = false;
		}
		
		setRegister();
	}
	
	public void keyPressed(KeyEvent e)
	{
		// directions (bit 4 == 0)
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_DOWN:
			{
				trueKeys[3] = true;
				
				break;
			}
			
			case KeyEvent.VK_UP:
			{
				trueKeys[2] = true;
				
				break;
			}
			
			case KeyEvent.VK_LEFT:
			{
				trueKeys[1] = true;
				
				break;
			}
			
			case KeyEvent.VK_RIGHT:
			{
				trueKeys[0] = true;
				
				break;
			}
		}
		
		// buttons (bit 5 == 0)
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_ENTER:
			{
				trueKeys[7] = true;
				
				break;
			}
			
			case KeyEvent.VK_SHIFT:
			{
				trueKeys[6] = true;
				
				break;
			}
			
			case KeyEvent.VK_X:
			{
				trueKeys[5] = true;
				
				break;
			}
			
			case KeyEvent.VK_Z:
			{
				trueKeys[4] = true;
				
				break;
			}
		}
	}
	
	public void keyReleased(KeyEvent e)
	{
		// directions (bit 4 == 0)
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_DOWN:
			{
				trueKeys[3] = false;
				
				break;
			}
			
			case KeyEvent.VK_UP:
			{
				trueKeys[2] = false;
				
				break;
			}
			
			case KeyEvent.VK_LEFT:
			{
				trueKeys[1] = false;
				
				break;
			}
			
			case KeyEvent.VK_RIGHT:
			{
				trueKeys[0] = false;
				
				break;
			}
		}
		
		// buttons (bit 5 == 0)
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_ENTER:
			{
				trueKeys[7] = false;
				
				break;
			}
			
			case KeyEvent.VK_SHIFT:
			{
				trueKeys[6] = false;
				
				break;
			}
			
			case KeyEvent.VK_X:
			{
				trueKeys[5] = false;
				
				break;
			}
			
			case KeyEvent.VK_Z:
			{
				trueKeys[4] = false;
				
				break;
			}
		}
	}
	
	public void keyTyped(KeyEvent e) {}			// get the compiler to stop nagging
	
	
	
	void joypadInterrupt()
	{
		GB.cpu.memory[CPU.IF].setBit(4, 1);
	}
	
	
	
	void status()
	{
		clearInputs();
		
		if (GB.cpu.memory[0xFF00].getBit(4) == 0)
		{
			directions();
		}
		
		if (GB.cpu.memory[0xFF00].getBit(5) == 0)
		{
			buttons();
		}
		
		tempkeys.or(directions);
		tempkeys.or(buttons);
		
		tempkeys.comp();
		
		keys.setBits(0, 3, tempkeys.get());
		
		for (int i = 0; i < 6; i++)
		{
			if (keys.getBit(i) == 0 && GB.cpu.memory[0xFF00].getBit(i) == 1)
			{
				joypadInterrupt();
			}
		}
		
		setRegister();
	}
	
	
	
	void directions()
	{
		for (int i = 0; i < 4; i++)
		{
			directions.setBit(i, trueKeys[i] ? 1 : 0);
		}
	}
	
	void buttons()
	{
		for (int i = 0; i < 4; i++)
		{
			buttons.setBit(i, trueKeys[i + 4] ? 1 : 0);
		}
	}
	
	void clearInputs()
	{
		tempkeys.set(0);
		directions.set(0);
		buttons.set(0);
		keys.set(0);
	}
	
	
	
	void getRegister()
	{
		keys.setBits(0, 3, GB.cpu.memory[0xFF00].get());
	}
	
	void setRegister()
	{
		GB.cpu.memory[0xFF00].setBits(0, 3, keys.get());
	}
}