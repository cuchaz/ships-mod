/*******************************************************************************
 * Copyright (c) 2013 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
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
	public final int HeaderColor = ColorUtils.getColor( 50, 99, 145 );
	public final int HeaderBevelColor = ColorUtils.getColor( 213, 223, 239 );
	public final int YesColor = ColorUtils.getColor( 0, 160, 0 );
	public final int NoColor = ColorUtils.getColor( 160, 0, 0 );
	
	private static final ResourceLocation BackgroundTexture = new ResourceLocation( "ships", "textures/gui/ship.png" );
	
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
		mc.getTextureManager().bindTexture( BackgroundTexture );
		drawTexturedModalRect( guiLeft, guiTop, 0, 0, xSize, ySize );
	}
	
	protected void drawHeaderText( String text, int lineNum )
	{
		int x1 = LeftMargin;
		int x2 = xSize - LeftMargin;
		int y1 = getLineY( lineNum );
		int y2 = y1 + fontRenderer.FONT_HEIGHT;
		drawHorizontalLine( x1 - 1, x2 - 1, y2 - 2, HeaderBevelColor );
		fontRenderer.drawString( text, x1 - 1, y1 - 1, HeaderBevelColor );
		drawHorizontalLine( x1, x2, y2 - 1, HeaderColor );
		fontRenderer.drawString( text, x1, y1, HeaderColor );
	}
	
	protected void drawText( String text, int lineNum )
	{
		drawText( text, lineNum, TextColor );
	}
	
	protected void drawText( String text, int lineNum, int color )
	{
		fontRenderer.drawString( text, LeftMargin, getLineY( lineNum ), color );
	}
	
	protected void drawWrappedText( String text, int lineNum, int width )
	{
		fontRenderer.drawSplitString( text, LeftMargin, getLineY( lineNum ), width, TextColor );
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
		drawYesNoText(
			labelText,
			isYes ? GuiString.Yes.getLocalizedText() : GuiString.No.getLocalizedText(),
			isYes,
			lineNum
		);
	}
	
	protected void drawYesNoText( String labelText, String valueText, boolean isYes, int lineNum )
	{
		// draw the label
		fontRenderer.drawString( labelText + ":", LeftMargin, getLineY( lineNum ), TextColor );
		
		// draw the value
		int valueColor = isYes ? YesColor : NoColor;
		int valueWidth = fontRenderer.getStringWidth( valueText );
		fontRenderer.drawString( valueText, xSize - LeftMargin - valueWidth, getLineY( lineNum ), valueColor );
	}
	
	protected int getLineY( int lineNum )
	{
		return TopMargin + ( fontRenderer.FONT_HEIGHT + LineSpacing )*lineNum;
	}
}
