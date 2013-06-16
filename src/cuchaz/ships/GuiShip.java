package cuchaz.ships;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

public class GuiShip extends GuiContainer
{
	public GuiShip( Container container )
	{
		super( container );
		xSize = 166;
		ySize = 153;
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer( int mouseX, int mouseY )
	{
		int color = 4210752;
		fontRenderer.drawString( "Ship", 8, 6, color );
        fontRenderer.drawString( StatCollector.translateToLocal( "container.inventory" ), 8, ySize - 96 + 2, color );
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer( float renderPartialTicks, int mouseX, int mouseY )
	{
		// render the GUI background
        GL11.glColor4f( 1.0f, 1.0f, 1.0f, 1.0f );
        mc.renderEngine.bindTexture( Ships.TexturesPath + "/gui/ship.png" );
        int x = ( width - xSize )/2;
        int y = ( height - ySize )/2;
        drawTexturedModalRect( x, y, 0, 0, xSize, ySize );
	}
}
