package cuchaz.ships;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cuchaz.modsShared.BlockArray;
import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.BlockUtils;
import cuchaz.modsShared.BoundingBoxInt;
import cuchaz.modsShared.Envelopes;

public class ShipLauncher
{
	public static enum LaunchFlag
	{
		RightNumberOfBlocks
		{
			@Override
			public boolean computeValue( ShipLauncher launcher )
			{
				return launcher.m_blocks != null
					&& !launcher.m_blocks.isEmpty()
					&& launcher.m_blocks.size() <= launcher.m_shipType.getMaxNumBlocks();
			}
		},
		HasWaterBelow
		{
			@Override
			public boolean computeValue( ShipLauncher launcher )
			{
				if( launcher.m_blocks == null )
				{
					return false;
				}
				
				for( ChunkCoordinates coords : launcher.m_envelopes.getEnvelope( BlockSide.Bottom ) )
				{
					if( !launcher.hasWaterBelow( coords ) )
					{
						return false;
					}
				}
				return true;
			}
		},
		HasAirAbove
		{
			@Override
			public boolean computeValue( ShipLauncher launcher )
			{
				if( launcher.m_blocks == null )
				{
					return false;
				}
				
				for( ChunkCoordinates coords : launcher.m_envelopes.getEnvelope( BlockSide.Top ) )
				{
					if( launcher.m_world.getBlockMaterial( coords.posX, coords.posY + 1, coords.posZ ) == Material.air )
					{
						return true;
					}
				}
				return false;
			}
		},
		FoundWaterHeight
		{
			@Override
			public boolean computeValue( ShipLauncher launcher )
			{
				if( launcher.m_blocks == null )
				{
					return false;
				}
				
				return launcher.computeWaterHeight() != -1;
			}
		},
		WillItFloat
		{
			@Override
			public boolean computeValue( ShipLauncher launcher )
			{
				if( launcher.m_blocks == null )
				{
					return false;
				}
				
				return launcher.m_equilibriumWaterHeight - launcher.m_y < launcher.m_shipWorld.getMax().posY + 1;
			}
		};

		public abstract boolean computeValue( ShipLauncher launcher );
	}
	
	private World m_world;
	private int m_x;
	private int m_y;
	private int m_z;
	private ShipType m_shipType;
	private List<ChunkCoordinates> m_blocks; // NOTE: blocks are in world coordinates
	private List<Boolean> m_launchFlags;
	private Envelopes m_envelopes;
	private ShipWorld m_shipWorld;
	private ShipPhysics m_shipPhysics;
	private double m_equilibriumWaterHeight;
	
	public ShipLauncher( final World world, int x, int y, int z )
	{
		m_world = world;
		m_x = x;
		m_y = y;
		m_z = z;
		
		// get the ship type from the block
		m_shipType = Ships.instance.BlockShip.getShipType( world, x, y, z );
		
		// find all the blocks connected to the ship block
		m_blocks = BlockUtils.searchForBlocks(
			x, y, z,
			m_shipType.getMaxNumBlocks(),
			new BlockUtils.BlockValidator( )
			{
				@Override
				public boolean isValid( ChunkCoordinates coords )
				{
					return !MaterialProperties.isSeparatorBlock( Block.blocksList[world.getBlockId( coords.posX, coords.posY, coords.posZ )] );
				}
			}
		);
		
		if( m_blocks != null )
		{
			// also add the ship block
			m_blocks.add( new ChunkCoordinates( m_x, m_y, m_z ) );
			
			m_envelopes = new Envelopes( m_blocks );
			m_shipWorld = new ShipWorld( m_world, new ChunkCoordinates( m_x, m_y, m_z ), m_blocks );
			m_shipPhysics = new ShipPhysics( m_shipWorld );
			m_equilibriumWaterHeight = m_shipPhysics.getEquilibriumWaterHeight();
		}
		else
		{
			m_envelopes = null;
			m_shipWorld = null;
			m_shipPhysics = null;
			m_equilibriumWaterHeight = Double.NaN;
		}
		
		// compute the launch flags
		m_launchFlags = new ArrayList<Boolean>();
		for( LaunchFlag flag : LaunchFlag.values() )
		{
			m_launchFlags.add( flag.computeValue( this ) );
		}
	}
	
	public int getX( )
	{
		return m_x;
	}
	
	public int getY( )
	{
		return m_y;
	}
	
	public int getZ( )
	{
		return m_z;
	}
	
	public ShipType getShipType( )
	{
		return m_shipType;
	}
	
	public World getWorld( )
	{
		return m_world;
	}
	
	public int getNumBlocks( )
	{
		// don't count the ship block towards the size quota
		return m_blocks.size() - 1;
	}
	
	public boolean getLaunchFlag( LaunchFlag flag )
	{
		return m_launchFlags.get( flag.ordinal() );
	}
	
	public boolean isLaunchable( )
	{
		boolean isValid = true;
		for( LaunchFlag flag : LaunchFlag.values() )
		{
			isValid = isValid && getLaunchFlag( flag );
		}
		return isValid;
	}
	
	public BlockSide getShipSide( )
	{
		if( m_envelopes == null )
		{
			return null;
		}
		
		// return the widest side of north,west
		if( m_envelopes.getBoundingBox().getDx() > m_envelopes.getBoundingBox().getDz() )
		{
			return BlockSide.West;
		}
		return BlockSide.North;
	}
	
	public BlockSide getShipFront( )
	{
		// return the thinnest side of north,west
		if( m_envelopes.getBoundingBox().getDx() > m_envelopes.getBoundingBox().getDz() )
		{
			return BlockSide.North;
		}
		return BlockSide.West;
	}
	
	public BoundingBoxInt getShipBoundingBox( )
	{
		return m_envelopes.getBoundingBox();
	}
	
	public BlockArray getShipEnvelope( BlockSide side )
	{
		return m_envelopes.getEnvelope( side );
	}
	
	public double getEquilibriumWaterHeight( )
	{
		// convert to world coordinates
		return m_equilibriumWaterHeight + m_y;
	}
	
	public EntityShip launch( )
	{
		int waterHeight = computeWaterHeight();
		
		ShipWorld blocks = new ShipWorld( m_world, new ChunkCoordinates( m_x, m_y, m_z ), m_blocks );
		Vec3 centerOfMass = new ShipPhysics( blocks ).getCenterOfMass();
		
		// spawn a ship entity
		EntityShip ship = new EntityShip( m_world );
		ship.setPositionAndRotation(
			m_x + centerOfMass.xCoord,
			m_y + centerOfMass.yCoord,
			m_z + centerOfMass.zCoord,
			0, 0
		);
		ship.setShipType( m_shipType );
		ship.setWaterHeight( waterHeight );
		ship.setBlocks( blocks );
		
		if( !m_world.spawnEntityInWorld( ship ) )
		{
			return null;
		}
		
		// remove all the blocks from the world
		for( ChunkCoordinates coords : m_blocks )
		{
			if( coords.posY >= waterHeight )
			{
				m_world.setBlockToAir( coords.posX, coords.posY, coords.posZ );
			}
			else
			{
				m_world.setBlock( coords.posX, coords.posY, coords.posZ, Block.waterStill.blockID );
			}
		}
		m_world.setBlockToAir( m_x, m_y, m_z );
		
		return ship;
	}
	
	private int computeWaterHeight( )
	{
		// for each column in the ship or outside it
		for( int x=m_envelopes.getBoundingBox().minX-1; x<=m_envelopes.getBoundingBox().maxX+1; x++ )
		{
			for( int z=m_envelopes.getBoundingBox().minZ-1; z<=m_envelopes.getBoundingBox().maxZ+1; z++ )
			{
				int waterHeight = computeWaterHeight( x, z );
				if( waterHeight != -1 )
				{
					return waterHeight;
				}
			}
		}
		return -1;
	}

	private int computeWaterHeight( int x, int z )
	{
		// start at the top of the box
		int y = m_envelopes.getBoundingBox().maxY+1;
		
		// drop until we hit air
		boolean foundAir = false;
		for( ; y>=0; y-- )
		{
			if( m_world.getBlockMaterial( x, y, z ) == Material.air )
			{
				foundAir = true;
				break;
			}
		}
		
		if( !foundAir )
		{
			return -1;
		}
		
		// keep dropping until we hit water
		for( ; y>=0; y-- )
		{
			if( m_world.getBlockMaterial( x, y, z ).isLiquid() )
			{
				return y + 1;
			}
		}
		
		return -1;
	}
	
	private boolean hasWaterBelow( ChunkCoordinates coords )
	{
		// drop down until we hit something other than air
		for( int y=coords.posY-1; y>=0; y-- )
		{
			Material material = m_world.getBlockMaterial( coords.posX, y, coords.posZ );
			
			if( material == Material.air )
			{
				continue;
			}
			
			// if it's water, we win!! =D
			return material.isLiquid();
		}
		
		// ran out of blocks to check. =(
		return false;
	}
}
