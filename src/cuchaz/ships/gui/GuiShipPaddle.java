package cuchaz.ships.gui;

import java.awt.Dimension;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.TextureFXManager;
import cpw.mods.fml.common.network.PacketDispatcher;
import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.ColorUtils;
import cuchaz.ships.EntityShip;
import cuchaz.ships.EntityShipBlock;
import cuchaz.ships.Ships;
import cuchaz.ships.packets.PacketPilotShip;
import cuchaz.ships.packets.PacketPilotShip.Action;

public class GuiShipPaddle extends GuiCloseable
{
	private EntityShip m_ship;
	private BlockSide m_sideFacingPlayer;
	
	public GuiShipPaddle( Container container, EntityShip ship, EntityPlayer player )
	{
		super( container );
		
		m_ship = ship;
		
		xSize = 110;
		ySize = 25;
		
		// which xz side is facing the player?
		EntityShipBlock shipBlock = ship.getShipBlockEntity();
		
		// get a vector from the block to the player
		double dx = player.posX - shipBlock.posX;
		double dz = player.posZ - shipBlock.posZ;
		
		// UNDONE: rotate into ship coords
		
		// find the side whose normal vector best matches the vector to the player
		double maxDot = Double.NEGATIVE_INFINITY;
		m_sideFacingPlayer = null;
		for( BlockSide side : BlockSide.xzSides() )
		{
			double dot = side.getDx()*dx + side.getDz()*dz;
			
			if( dot > maxDot )
			{
				maxDot = dot;
				m_sideFacingPlayer = side;
			}
		}
	}
	
	@Override
	public void initGui( )
	{
		// show this GUI near the bottom so it doesn't block much of the screen
		guiLeft = ( width - xSize )/2;
		guiTop = height - ySize - 48;
		
		// try to let the player look around while in this gui
		allowUserInput = true;
		mc.inGameHasFocus = true;
        mc.mouseHelper.grabMouseCursor();
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
		fontRenderer.drawString( Keyboard.getKeyName( keyForward ), 11, 8, textColor );
		fontRenderer.drawString( Keyboard.getKeyName( keyBack ), 46, 8, textColor );
		fontRenderer.drawString( Keyboard.getKeyName( keyLeft ), 61, 8, textColor );
		fontRenderer.drawString( Keyboard.getKeyName( keyRight ), 95, 8, textColor );
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer( float f, int i, int j )
	{
		GL11.glColor4f( 1.0f, 1.0f, 1.0f, 1.0f );
		
		// load the texture
		final String TextureName = Ships.TexturesPath + "/gui/shipPaddle.png";
		mc.renderEngine.bindTexture( TextureName );
		Dimension dim = TextureFXManager.instance().getTextureDimensions( TextureName );
        double umax = (double)xSize/dim.width;
        double vmax = (double)ySize/dim.height;
		
		Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV( (double)( guiLeft ),         (double)( guiTop + ySize ), (double)zLevel, 0,    vmax );
        tessellator.addVertexWithUV( (double)( guiLeft + xSize ), (double)( guiTop + ySize ), (double)zLevel, umax, vmax );
        tessellator.addVertexWithUV( (double)( guiLeft + xSize ), (double)( guiTop ),         (double)zLevel, umax, 0 );
        tessellator.addVertexWithUV( (double)( guiLeft ),         (double)( guiTop ),         (double)zLevel, 0,    0 );
        tessellator.draw();
	}
	
	@Override
	public void drawDefaultBackground( )
	{
		// do nothing, so we don't draw the dark filter over the world
	}
	
	@Override
	public void updateScreen( )
	{
		// get the action, if any
		Action action = null;
		if( Keyboard.isKeyDown( mc.gameSettings.keyBindForward.keyCode ) )
		{
			action = Action.Forward;
		}
		else if( Keyboard.isKeyDown( mc.gameSettings.keyBindBack.keyCode ) )
		{
			action = Action.Backward;
		}
		// UNDONE: handle rotation!!
		
		if( action != null )
		{
			// send a packet to the server
			PacketPilotShip packet = new PacketPilotShip( m_ship.entityId, action, m_sideFacingPlayer );
			PacketDispatcher.sendPacketToServer( packet.getCustomPacket() );
			
			// and apply locally
			action.pilotShip( m_ship, m_sideFacingPlayer );
		}
	}
	
	@Override
	protected void mouseClicked( int x, int y, int button )
	{
		// NOTE: button 1 is the RMB
		if( button == 1 )
		{
			close();
		}
	}
}
