package cuchaz.ships.gui;

import static cuchaz.ships.gui.GuiSettings.LeftMargin;
import static cuchaz.ships.gui.GuiSettings.LineSpacing;
import static cuchaz.ships.gui.GuiSettings.TopMargin;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cuchaz.modsShared.ColorUtils;

public abstract class GuiShip extends GuiCloseable
{
	public final int TextColor = ColorUtils.getGrey( 64 );
	public final int YesColor = ColorUtils.getColor( 0, 160, 0 );
	public final int NoColor = ColorUtils.getColor( 160, 0, 0 );
	
	private static final ResourceLocation BackgroundTexture = new ResourceLocation( "ships", "/textures/gui/ship.png" );
	
	public GuiShip( Container container )
	{
		super( container );
		
		xSize = 176;
		ySize = 166;
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer( float renderPartialTicks, int mouseX, int mouseY )
	{
		GL11.glColor4f( 1.0f, 1.0f, 1.0f, 1.0f );
		
		// this call loads the texture. The deobfuscation mappings haven't picked this one up yet in 1.6.1
		this.mc.func_110434_K().func_110577_a( BackgroundTexture );
		
		drawTexturedModalRect( guiLeft, guiTop, 0, 0, xSize, ySize );
	}
	
	protected void drawText( String text, int lineNum )
	{
		fontRenderer.drawString( text, LeftMargin, getLineY( lineNum ), TextColor );
	}
	
	protected void drawLabelValueText( String labelText, String valueText, int lineNum )
	{
		// draw the label
		fontRenderer.drawString( labelText + ":", LeftMargin, getLineY( lineNum ), TextColor );
		
		// draw the value
		int valueWidth = fontRenderer.getStringWidth( valueText );
		fontRenderer.drawString( valueText, xSize - LeftMargin - valueWidth, getLineY( lineNum ), TextColor );
	}
	
	protected String getYesNoText( boolean flag )
	{
		return flag ? GuiString.Yes.getLocalizedText() : GuiString.No.getLocalizedText();
	}
	
	protected void drawYesNoText( String labelText, boolean isYes, int lineNum )
	{
		// draw the label
		fontRenderer.drawString( labelText + ":", LeftMargin, getLineY( lineNum ), TextColor );
		
		// draw the value
		String valueText = isYes ? "Yes" : "No";
		int valueColor = isYes ? YesColor : NoColor;
		int valueWidth = fontRenderer.getStringWidth( valueText );
		fontRenderer.drawString( valueText, xSize - LeftMargin - valueWidth, getLineY( lineNum ), valueColor );
	}
	
	protected int getLineY( int lineNum )
	{
		return TopMargin + ( fontRenderer.FONT_HEIGHT + LineSpacing )*lineNum;
	}
}
