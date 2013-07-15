package cuchaz.ships.gui;

import static cuchaz.ships.gui.GuiSettings.LeftMargin;
import static cuchaz.ships.gui.GuiSettings.TopMargin;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.ResourceLocation;
import net.minecraft.inventory.Container;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Icon;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.network.PacketDispatcher;
import cuchaz.modsShared.BlockArray;
import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.ColorUtils;
import cuchaz.ships.ShipLauncher;
import cuchaz.ships.ShipLauncher.LaunchFlag;
import cuchaz.ships.packets.PacketLaunchShip;

public class GuiShipLaunch extends GuiShip
{
	private static final ResourceLocation ShipTexture = TextureMap.field_110575_b;
	
	private ShipLauncher m_shipLauncher;
	private GuiButton m_buttonLaunchShip;
	
	public GuiShipLaunch( Container container, ShipLauncher shipLauncher )
	{
		super( container );
		
		m_shipLauncher = shipLauncher;
		
		m_buttonLaunchShip = null;
	}
	
	@Override
	@SuppressWarnings( "unchecked" )
	public void initGui( )
	{
		super.initGui();
		
		// add the buttons
		m_buttonLaunchShip = new GuiButton( 
			0, guiLeft + LeftMargin,
			guiTop + ySize - TopMargin - 20,
			80,
			20,
			GuiString.ShipLaunch.getLocalizedText()
		);
		m_buttonLaunchShip.enabled = m_shipLauncher.isLaunchable();
		buttonList.add( m_buttonLaunchShip );
	}
	
	@Override
	protected void actionPerformed( GuiButton button )
	{
		if( button.id == m_buttonLaunchShip.id )
		{
			// tell the server to spawn a ship
			PacketLaunchShip packet = new PacketLaunchShip( m_shipLauncher.getX(), m_shipLauncher.getY(), m_shipLauncher.getZ() );
			PacketDispatcher.sendPacketToServer( packet.getCustomPacket() );
			close();
		}
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer( int mouseX, int mouseY )
	{
		int textColor = ColorUtils.getGrey( 64 );
		
		drawText( GuiString.ShipConstruction.getLocalizedText(), 0, textColor );
		
		String valueText;
		
		// right number of blocks
		if( m_shipLauncher.getLaunchFlag( LaunchFlag.RightNumberOfBlocks ) )
		{
			valueText = String.format( "%d / %d",
				m_shipLauncher.getNumBlocks(),
				m_shipLauncher.getShipType().getMaxNumBlocks()
			);
		}
		else
		{
			valueText = GuiString.ShipTooLarge.getLocalizedText();
		}
		drawText( String.format( "%s: %s", GuiString.ShipNumBlocks.getLocalizedText(), valueText ), 1, textColor );
		
		// draw the launch flags
		drawYesNoText(
			GuiString.ShipInOrAboveWater.getLocalizedText(),
			m_shipLauncher.getLaunchFlag( LaunchFlag.HasWaterBelow ),
			2
		);
		drawYesNoText(
			GuiString.ShipHasAirAbove.getLocalizedText(),
			m_shipLauncher.getLaunchFlag( LaunchFlag.HasAirAbove ),
			3
		);
		drawYesNoText(
			GuiString.ShipFoundWaterHeight.getLocalizedText(),
			m_shipLauncher.getLaunchFlag( LaunchFlag.FoundWaterHeight ),
			4
		);
		drawYesNoText(
			GuiString.ShipWillItFloat.getLocalizedText(),
			m_shipLauncher.getLaunchFlag( LaunchFlag.WillItFloat ),
			5
		);
		
		// draw the ship and show the water height
		final int ShipHeight = 64;
		BlockSide shipSide = m_shipLauncher.getShipSide();
		if( shipSide != null )
		{
			drawShipSide( shipSide, LeftMargin, getLineY( 6 ), xSize - LeftMargin*2, ShipHeight );
		}
	}
	
	private void drawShipSide( BlockSide side, int x, int y, int maxWidth, int maxHeight )
	{
		BlockArray envelope = m_shipLauncher.getShipEnvelope( side );
		
		// compute the block size
		double blockSize = Math.min(
			(double)maxWidth/(double)envelope.getWidth(),
			(double)maxHeight/(double)envelope.getHeight()
		);
		
		// shink the block size so we have some border
		blockSize *= 0.8;
		
		double shipWidth = (double)envelope.getWidth()*blockSize;
		double shipHeight = (double)envelope.getHeight()*blockSize;
		
		// compute the water height
		double waterHeight = m_shipLauncher.getEquilibriumWaterHeight();
		
		double waterRectHeight = maxHeight;
		if( !Double.isNaN( waterHeight ) )
		{
			waterRectHeight = Math.min( maxHeight, ( waterHeight - envelope.getVMin() )*blockSize + ( maxHeight - shipHeight )/2.0 );
		}
		
		// draw the water rect
		final int WaterColor = ColorUtils.getColor( 43, 99, 225 );
		drawColoredBlock( x, y + maxHeight - waterRectHeight, maxWidth, waterRectHeight, WaterColor );
		
		// draw the ship blocks
		// this call loads the texture. The deobfuscation mappings haven't picked this one up yet in 1.6.1
		this.mc.func_110434_K().func_110577_a( ShipTexture );
		for( int u=envelope.getUMin(); u<=envelope.getUMax(); u++ )
		{
			for( int v=envelope.getVMin(); v<=envelope.getVMax(); v++ )
			{
				ChunkCoordinates coords = envelope.getBlock( u, v );
				if( coords == null )
				{
					continue;
				}
				
				// get the block texture
				Block block = Block.blocksList[m_shipLauncher.getWorld().getBlockId( coords.posX, coords.posY, coords.posZ )];
				int meta = m_shipLauncher.getWorld().getBlockMetadata( coords.posX, coords.posY, coords.posZ );
				Icon icon = block.getBlockTexture( m_shipLauncher.getWorld(), coords.posX, coords.posY, coords.posZ, meta );
				
				// draw a block right on the GUI
				drawScaledBlock(
					( maxWidth - shipWidth )/2.0 + x + ( envelope.toZeroBasedU( u ) )*blockSize,
					( maxHeight - shipHeight )/2.0 + y + ( envelope.getHeight() - envelope.toZeroBasedV( v ) - 1 )*blockSize,
					blockSize,
					blockSize,
					icon
				);
			}
		}
	}
	
	private void drawColoredBlock( double x, double y, double width, double height, int color )
	{
		double z = (double)zLevel;
		
        GL11.glDisable( GL11.GL_TEXTURE_2D );
        GL11.glColor4f(
			ColorUtils.getRedf( color ),
			ColorUtils.getGreenf( color ),
			ColorUtils.getBluef( color ),
			ColorUtils.getAlphaf( color )
		);
        
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		
		// upper left corner, then clockwise
		tessellator.addVertex( x + 0, y + height, z );
		tessellator.addVertex( x + width, y + height, z );
		tessellator.addVertex( x + width, y + 0, z );
		tessellator.addVertex( x + 0, y + 0, z );
		
		tessellator.draw();
		
		GL11.glEnable( GL11.GL_TEXTURE_2D );
	}
	
	private void drawScaledBlock( double x, double y, double width, double height, Icon icon )
	{
		// get the texture u/v for this icon
		double minU = (double)icon.getInterpolatedU( 0.0 );
		double maxU = (double)icon.getInterpolatedU( 16.0 );
		double minV = (double)icon.getInterpolatedV( 0.0 );
		double maxV = (double)icon.getInterpolatedV( 16.0 );
		
		double z = (double)zLevel;
		
		GL11.glColor4f( 1.0f, 1.0f, 1.0f, 1.0f );
		
		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		
		// upper left corner, then clockwise
		tessellator.addVertexWithUV( x + 0, y + height, z, minU, maxV );
		tessellator.addVertexWithUV( x + width, y + height, z, maxU, maxV );
		tessellator.addVertexWithUV( x + width, y + 0, z, maxU, minV );
		tessellator.addVertexWithUV( x + 0, y + 0, z, minU, minV );
		
		tessellator.draw();
	}
}
