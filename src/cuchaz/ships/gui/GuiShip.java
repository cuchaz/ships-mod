package cuchaz.ships.gui;

import net.minecraft.inventory.Container;

import org.lwjgl.opengl.GL11;

import cuchaz.ships.Ships;

public abstract class GuiShip extends GuiCloseable
{
	public GuiShip( Container container )
	{
		super( container );
		
		xSize = 166;
		ySize = 153;
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer( float renderPartialTicks, int mouseX, int mouseY )
	{
		// render the GUI background
        GL11.glColor4f( 1.0f, 1.0f, 1.0f, 1.0f );
        mc.renderEngine.bindTexture( Ships.TexturesPath + "/gui/ship.png" );
        drawTexturedModalRect( guiLeft, guiTop, 0, 0, xSize, ySize );
	}
}
