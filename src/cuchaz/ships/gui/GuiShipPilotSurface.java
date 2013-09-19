package cuchaz.ships.gui;

import java.util.Arrays;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.CircleRange;
import cuchaz.modsShared.ColorUtils;
import cuchaz.ships.EntityShip;
import cuchaz.ships.PilotAction;

public class GuiShipPilotSurface extends GuiShipPilot
{
	private static final ResourceLocation BackgroundTexture = new ResourceLocation( "ships", "/textures/gui/shipPilotSurface.png" );
	private static final int TextureWidth = 256;
	private static final int TextureHeight = 64;
	private static final int CompassHeight = 12;
	private static final int CompassY = 52;
	private static final int CompassFrameX = 156;
	private static final int CompassFrameY = 6;
	private static final int CompassFrameWidth = 93;
	private static final int CompassNorthOffset = 5;
	private static final int CompassMarkerX = 201;
	private static final int CompassMarkerY = 31;
	private static final int CompassMarkerWidth = 3;
	private static final int CompassMarkerHeight = 4;
	private static final int ThrottleHeight = 7; // UNDONE: check these!
	private static final int ThrottleX = 9;
	private static final int ThrottleY = 31;
	
	public GuiShipPilotSurface( Container container, EntityShip ship, EntityPlayer player )
	{
		super(
			container,
			ship,
			player,
			Arrays.asList( PilotAction.ThrottleUp, PilotAction.ThrottleDown, PilotAction.Left, PilotAction.Right ),
			ForwardSideMethod.ByHelm
		);
		
		xSize = 256;
		ySize = 25;
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer( int mouseX, int mouseY )
	{
		int keyForward = mc.gameSettings.keyBindForward.keyCode;
		int keyBack = mc.gameSettings.keyBindBack.keyCode;
		int keyLeft = mc.gameSettings.keyBindLeft.keyCode;
		int keyRight = mc.gameSettings.keyBindRight.keyCode;
		
		// draw the key binds
		int textColor = ColorUtils.getGrey( 64 );
		final int TextOffset = 44;
		fontRenderer.drawString( Keyboard.getKeyName( keyForward ), TextOffset + 11, 8, textColor );
		fontRenderer.drawString( Keyboard.getKeyName( keyBack ), TextOffset + 46, 8, textColor );
		fontRenderer.drawString( Keyboard.getKeyName( keyLeft ), TextOffset + 61, 8, textColor );
		fontRenderer.drawString( Keyboard.getKeyName( keyRight ), TextOffset + 95, 8, textColor );
		
		loadTexture();
		
		// determine the compass offset
		double compassOffset = (double)getForwardSide().getXZOffset()/4
			- CircleRange.mapZeroTo360( getShip().rotationYaw )/360.0f
			+ (double)CompassNorthOffset/TextureWidth
			- (double)CompassFrameWidth/2/TextureWidth;
		
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		double z = zLevel;
		
		// draw the compass
		double umin = compassOffset;
		double umax = compassOffset + (double)CompassFrameWidth/TextureWidth;
		double vmin = (double)CompassY/TextureHeight;
		double vmax = 1;
		double x = CompassFrameX;
		double y = CompassFrameY;
		tessellator.addVertexWithUV( x,                     y + CompassHeight, z, umin, vmax );
		tessellator.addVertexWithUV( x + CompassFrameWidth, y + CompassHeight, z, umax, vmax );
		tessellator.addVertexWithUV( x + CompassFrameWidth, y,                 z, umax, vmin );
		tessellator.addVertexWithUV( x,                     y,                 z, umin, vmin );
		
		// draw the compass marker
		umin = (double)CompassMarkerX/TextureWidth;
		umax = umin + (double)CompassMarkerWidth/TextureWidth;
		vmin = (double)CompassMarkerY/TextureHeight;
		vmax = vmin + (double)CompassMarkerHeight/TextureHeight;
		x = CompassFrameX + CompassFrameWidth/2 - CompassMarkerWidth/2;
		y = CompassFrameY - 2;
		tessellator.addVertexWithUV( x,                      y + CompassMarkerHeight, z, umin, vmax );
		tessellator.addVertexWithUV( x + CompassMarkerWidth, y + CompassMarkerHeight, z, umax, vmax );
		tessellator.addVertexWithUV( x + CompassMarkerWidth, y,                       z, umax, vmin );
		tessellator.addVertexWithUV( x,                      y,                       z, umin, vmin );
		
		// UNDONE: draw the throttle
		if( getShip().linearThrottle > 0 )
		{
		}
		else if( getShip().linearThrottle < 0 )
		{
		}
		
		tessellator.draw();
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer( float f, int i, int j )
	{
		loadTexture();
		
        double umax = (double)xSize/TextureWidth;
        double vmax = (double)ySize/TextureHeight;
		
		Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV( (double)( guiLeft ),         (double)( guiTop + ySize ), (double)zLevel, 0,    vmax );
        tessellator.addVertexWithUV( (double)( guiLeft + xSize ), (double)( guiTop + ySize ), (double)zLevel, umax, vmax );
        tessellator.addVertexWithUV( (double)( guiLeft + xSize ), (double)( guiTop ),         (double)zLevel, umax, 0 );
        tessellator.addVertexWithUV( (double)( guiLeft ),         (double)( guiTop ),         (double)zLevel, 0,    0 );
        tessellator.draw();
	}
	
	private void loadTexture( )
	{
		GL11.glColor4f( 1.0f, 1.0f, 1.0f, 1.0f );
		
		// load the texture
		// this call loads the texture. The deobfuscation mappings haven't picked this one up yet in 1.6.1
		this.mc.func_110434_K().func_110577_a( BackgroundTexture );
	}
}
