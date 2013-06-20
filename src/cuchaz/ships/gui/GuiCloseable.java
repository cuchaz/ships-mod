package cuchaz.ships.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;

public abstract class GuiCloseable extends GuiContainer
{
	public GuiCloseable( Container container )
	{
		super( container );
	}
	
	public void close( )
	{
		mc.thePlayer.closeScreen();
		mc.setIngameFocus();
	}
}
