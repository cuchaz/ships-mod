package cuchaz.ships;

import java.util.TreeMap;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import cuchaz.modsShared.BlockArray;
import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.Envelopes;

public class ShipPhysics
{
	private static final double AccelerationGravity = 0.01;
	
	private static class DisplacementEntry
	{
		public int numBlocksAtSurface;
		public int numBlocksUnderwater;
		
		public DisplacementEntry( )
		{
			numBlocksAtSurface = 0;
			numBlocksUnderwater = 0;
		}
	}
	
	private ShipWorld m_blocks;
	private TreeMap<Integer,DisplacementEntry> m_displacement;
	private double m_shipMass;
	private double m_equilibriumWaterHeight;
	
	public ShipPhysics( ShipWorld blocks )
	{
		m_blocks = blocks;
		
		// get all the watertight blocks
		TreeSet<ChunkCoordinates> watertightBlocks = new TreeSet<ChunkCoordinates>();
		for( ChunkCoordinates coords : m_blocks.coords() )
		{
			if( MaterialProperties.isWatertight( getBlock( coords ) ) )
			{
				watertightBlocks.add( coords );
			}
		}
		
		int minY = m_blocks.getBoundingBox().minY;
		int maxY = m_blocks.getBoundingBox().maxY;
		
		// initialize displacement
		m_displacement = new TreeMap<Integer,DisplacementEntry>();
		for( int y=minY; y<=maxY+1; y++ )
		{
			m_displacement.put( y, new DisplacementEntry() );
		}
		
		// compute displacement for the ship blocks
		for( ChunkCoordinates coords : watertightBlocks )
		{
			for( int y=maxY+1; y>=coords.posY; y-- )
			{
				DisplacementEntry entry = m_displacement.get( y );
				if( y == coords.posY )
				{
					entry.numBlocksAtSurface++;
				}
				else
				{
					entry.numBlocksUnderwater++;
				}
			}
		}
		
		// update displacement for trapped air blocks
		for( int y=minY; y<=maxY+1; y++ )
		{
			DisplacementEntry entry = m_displacement.get( y );
			for( ChunkCoordinates coords : m_blocks.getGeometry().getTrappedAir( y ) )
			{
				if( y == coords.posY )
				{
					entry.numBlocksAtSurface++;
				}
				else
				{
					entry.numBlocksUnderwater++;
				}
			}
		}
		
		// compute the total mass
		m_shipMass = 0.0;
		for( ChunkCoordinates coords : m_blocks.coords() )
		{
			m_shipMass += MaterialProperties.getMass( getBlock( coords ) );
		}
		
		m_equilibriumWaterHeight = computeEquilibriumWaterHeight();
		
		/* TEMP: tell me the displacement blocks
		for( int y=minY; y<=maxY+1; y++ )
		{
			DisplacementEntry entry = m_displacement.get( y );
			System.out.println( String.format( "Blocks at %d: %d,%d", y, entry.numBlocksAtSurface, entry.numBlocksUnderwater ) );
		}
		*/
	}
	
	public Vec3 getCenterOfMass( )
	{
		Vec3 com = Vec3.createVectorHelper( 0, 0, 0 );
		double totalMass = 0.0;
		for( ChunkCoordinates coords : m_blocks.coords() )
		{
			double mass = MaterialProperties.getMass( getBlock( coords ) );
			totalMass += mass;
			com.xCoord += mass*( coords.posX + 0.5 );
			com.yCoord += mass*( coords.posY + 0.5 );
			com.zCoord += mass*( coords.posZ + 0.5 );
		}
		com.xCoord /= totalMass;
		com.yCoord /= totalMass;
		com.zCoord /= totalMass;
		return com;
	}
	
	public double getNetUpForce( double waterHeight )
	{
		// the net up force is the difference of the weight and the buoyancy
		return ( getDisplacedWaterMass( waterHeight ) - m_shipMass )*AccelerationGravity;
	}
	
	public double getDisplacedWaterMass( double waterHeight )
	{
		// get the surface block level
		int surfaceLevel = MathHelper.floor_double( waterHeight );
		DisplacementEntry entry = m_displacement.get( surfaceLevel );
		
		// compute the mass of the displaced water
		double displacedWaterMass = 0;
		if( entry != null )
		{
			double surfaceFraction = getBlockFractionSubmerged( surfaceLevel, waterHeight );
			displacedWaterMass = ( (double)entry.numBlocksUnderwater + (double)entry.numBlocksAtSurface*surfaceFraction )*getWaterBlockMass();
		}
		
		return displacedWaterMass;
	}
	
	public Double getEquilibriumWaterHeight( )
	{
		return m_equilibriumWaterHeight;
	}
	
	public double getDragCoefficient( double waterHeight, double motionX, double motionY, double motionZ, BlockSide side, Envelopes envelopes )
	{
		final double AirDragRate = 0.01;
		final double WaterDragRate = 0.5;
		
		// how fast are we going?
		double speed = Math.sqrt( motionX*motionX + motionY*motionY + motionZ*motionZ );
		
		// divide the leading envelope into air vs water
		double airSurfaceArea = 0;
		double waterSurfaceArea = 0;
		BlockArray leadingEnvelope = envelopes.getEnvelope( side );
		for( ChunkCoordinates coords : leadingEnvelope )
		{
			double fractionSubmerged = side.getFractionSubmerged( coords.posY, waterHeight );
			waterSurfaceArea += fractionSubmerged;
			airSurfaceArea += 1 - fractionSubmerged;
		}
		
		// compute the drag coefficient
		return logisticFunction( speed, AirDragRate*airSurfaceArea + WaterDragRate*waterSurfaceArea );
	}
	
	private Double computeEquilibriumWaterHeight( )
	{
		// travel up each layer until we find the one that displaces too much water
		int minY = m_blocks.getBoundingBox().minY;
		int maxY = m_blocks.getBoundingBox().maxY;
		for( int y=minY; y<=maxY+1; y++ )
		{
			// assume water completely submerges this layer
			DisplacementEntry entry = m_displacement.get( y );
			double displacedWaterMass = ( entry.numBlocksUnderwater + entry.numBlocksAtSurface )*getWaterBlockMass();
			
			// did we displace too much water?
			if( displacedWaterMass > m_shipMass )
			{
				// good, the water height is in this block level
				
				// now solve for the water height
				return y + ( m_shipMass - entry.numBlocksUnderwater*getWaterBlockMass() )/entry.numBlocksAtSurface/getWaterBlockMass();
			}
		}
		
		// The ship will sink!
		return null;
	}
	
	private double getBlockFractionSubmerged( int y, double waterHeight )
	{
		// can use any NSEW side
		return BlockSide.North.getFractionSubmerged( y, waterHeight );
	}
	
	private Block getBlock( ChunkCoordinates coords )
	{
		return Block.blocksList[m_blocks.getBlockId( coords.posX, coords.posY, coords.posZ )];
	}
	
	private double getWaterBlockMass( )
	{
		// UNDONE: use the surface height make the mass increase with depth
		return MaterialProperties.getMass( Block.waterStill );
	}
	
	private double logisticFunction( double x, double rate )
	{
		return 2.0/( 1.0 + Math.exp( -rate*x - 0.1 ) ) - 1;
	}
}
