/*******************************************************************************
 * Copyright (c) 2013 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.gui;

import static cuchaz.ships.gui.GuiSettings.LeftMargin;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.inventory.Container;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL20;

import cuchaz.modsShared.BlockArray;
import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.BlockUtils;
import cuchaz.modsShared.BlockUtils.BlockConditionValidator;
import cuchaz.modsShared.BlockUtils.Neighbors;
import cuchaz.modsShared.ColorUtils;
import cuchaz.modsShared.Util;
import cuchaz.ships.MaterialProperties;
import cuchaz.ships.ShipLauncher;
import cuchaz.ships.Ships;
import cuchaz.ships.propulsion.Propulsion;
import cuchaz.ships.render.RenderShip2D;
import cuchaz.ships.render.ShaderLoader;

public class GuiShipPropulsion extends GuiShip
{
	private static final ResourceLocation DesaturationShader = new ResourceLocation( "ships", "shaders/desaturate.frag" );
	
	private ShipLauncher m_shipLauncher;
	private Propulsion m_propulsion;
	private BlockArray m_shipEnvelope;
	private BlockArray m_helmEnvelope;
	private BlockArray m_propulsionEnvelope;
	private int m_desaturationProgramId;
	private double m_topLinearSpeed;
	private float m_topAngularSpeed;
	private String m_propulsionMethodsDescription;
	
	public GuiShipPropulsion( Container container, final World world, int helmX, int helmY, int helmZ )
	{
		super( container );
		
		// defaults
		m_shipLauncher = null;
		m_shipEnvelope = null;
		m_helmEnvelope = null;
		m_propulsionEnvelope = null;
		m_topLinearSpeed = 0;
		m_topAngularSpeed = 0f;
		m_desaturationProgramId = 0;
		
		// this should be the helm
		assert( world.getBlockId( helmX, helmY, helmZ ) == Ships.m_blockHelm.blockID );
		ChunkCoordinates helmCoords = new ChunkCoordinates( helmX, helmY, helmZ );
		
		// find the ship block
		ChunkCoordinates shipBlockCoords = BlockUtils.searchForBlock(
			helmX, helmY, helmZ,
			10000,
			new BlockConditionValidator( )
			{
				@Override
				public boolean isValid( ChunkCoordinates coords )
				{
					return !MaterialProperties.isSeparatorBlock( Block.blocksList[world.getBlockId( coords.posX, coords.posY, coords.posZ )] );
				}
				
				@Override
				public boolean isConditionMet( ChunkCoordinates coords )
				{
					return world.getBlockId( coords.posX, coords.posY, coords.posZ ) == Ships.m_blockShip.blockID;
				}
			},
			Neighbors.Edges
		);
		if( shipBlockCoords == null )
		{
			// can't find a ship block
			return;
		}
		
		// get the ship info from the launcher
		m_shipLauncher = new ShipLauncher( world, shipBlockCoords.posX, shipBlockCoords.posY, shipBlockCoords.posZ );
		if( m_shipLauncher.getShipWorld() == null )
		{
			// there's no valid ship
			return;
		}
		
		if( m_shipLauncher.getShipType().isPaddleable() )
		{
			// paddleable ships cannot support a helm
			return;
		}
		
		m_shipEnvelope = m_shipLauncher.getShipEnvelope( BlockSide.Top );
		
		// compute an envelope for the helm
		helmCoords.posX -= shipBlockCoords.posX;
		helmCoords.posY -= shipBlockCoords.posY;
		helmCoords.posZ -= shipBlockCoords.posZ;
		m_helmEnvelope = m_shipEnvelope.newEmptyCopy();
		m_helmEnvelope.setBlock( helmCoords.posX, helmCoords.posZ, helmCoords );
		
		// get the propulsion
		m_propulsion = new Propulsion( m_shipLauncher.getShipWorld() );
		m_propulsionEnvelope = m_propulsion.getEnevelope();
		
		// create our shader
		try
		{
			m_desaturationProgramId = ShaderLoader.createProgram( ShaderLoader.load( DesaturationShader ) );
		}
		catch( IOException ex )
		{
			// UNDONE: log the exception
			ex.printStackTrace();
		}
		
		// compute the propulsion properties
		m_topLinearSpeed = m_shipLauncher.getShipPhysics().getTopLinearSpeed( m_propulsion, m_shipLauncher.getShipWorld().getGeometry().getEnvelopes() );
		m_topAngularSpeed = m_shipLauncher.getShipPhysics().getTopAngularSpeed( m_propulsion );
		
		// build the description string
		StringBuilder buf = new StringBuilder();
		buf.append( "Found " );
		String delimiter = "";
		for( Propulsion.MethodCount count : m_propulsion.methodCounts() )
		{
			buf.append( delimiter );
			buf.append( count.toString() );
			delimiter = ", ";
		}
		if( delimiter == "" )
		{
			buf.append( "no propulsion methods!" );
		}
		m_propulsionMethodsDescription = buf.toString();
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer( int mouseX, int mouseY )
	{
		drawHeaderText( GuiString.ShipPropulsion.getLocalizedText(), 0 );
		
		if( m_shipLauncher == null )
		{
			drawText( GuiString.NoShipBlock.getLocalizedText(), 1 );
		}
		else if( m_propulsion == null )
		{
			drawText( GuiString.InvalidShip.getLocalizedText(), 1 );
		}
		else
		{
			// list the specs
			drawLabelValueText( "Ship Mass", String.format( "%.1f Kg", m_shipLauncher.getShipPhysics().getMass() ), 1 );
			drawLabelValueText( "Thrust", String.format( "%.1f N", m_propulsion.getTotalThrust() ), 2 );
			drawLabelValueText( "Top Speed", String.format( "%.1f m/s", m_topLinearSpeed*Util.TicksPerSecond ), 3 );
			drawLabelValueText( "Turning Speed", String.format( "%.1f deg/sec", m_topAngularSpeed ), 4 );
			
			drawPropulsion();
			
			// list the propulsion types
			drawText( m_propulsionMethodsDescription, 12 );
		}
	}
	
	private void drawPropulsion( )
	{
		int x = LeftMargin;
		int y = getLineY( 6 );
		int width = xSize - LeftMargin*2;
		int height = 64;
		
		RenderShip2D.drawWater( x, y, zLevel, width, height );
		
		if( m_shipEnvelope.getHeight() > m_shipEnvelope.getWidth() )
		{
			// rotate the envelopes so the long axis is across the GUI width
			m_shipEnvelope = BlockArray.Rotation.Ccw90.rotate( m_shipEnvelope );
			m_helmEnvelope = BlockArray.Rotation.Ccw90.rotate( m_helmEnvelope );
			m_propulsionEnvelope = BlockArray.Rotation.Ccw90.rotate( m_propulsionEnvelope );
		}
		
		// draw a desaturated ship
		RenderShip2D.drawShipAsColor(
			m_shipEnvelope,
			ColorUtils.getGrey( 64 ),
			x, y, zLevel, width, height
		);
		GL20.glUseProgram( m_desaturationProgramId );
		RenderShip2D.drawShip(
			m_shipEnvelope,
			m_shipLauncher.getShipWorld(),
			x, y, zLevel, width, height
		);
		GL20.glUseProgram( 0 );
		
		// draw the propulsion blocks and the helm at full saturation
		RenderShip2D.drawShip(
			m_propulsionEnvelope,
			m_shipLauncher.getShipWorld(),
			x, y, zLevel, width, height
		);
		RenderShip2D.drawShip(
			m_helmEnvelope,
			m_shipLauncher.getShipWorld(),
			x, y, zLevel, width, height
		);
	}
}
