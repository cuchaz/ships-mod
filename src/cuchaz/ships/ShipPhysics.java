package cuchaz.ships;

import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.material.Material;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;

public class ShipPhysics
{
	private static final double AccelerationGravity = 0.01;
	
	private ShipWorld m_blocks;
	private TreeMap<Integer,Integer> m_numDisplacementBlocks;
	private double m_shipMass;
	
	public ShipPhysics( ShipWorld blocks )
	{
		m_blocks = blocks;
		
		// count the number of watertight blocks
		m_numDisplacementBlocks = new TreeMap<Integer,Integer>();
		for( ChunkCoordinates coords : m_blocks.coords() )
		{
			// UNDONE: get trapped air blocks too
			
			// skip non-watertight blocks
			if( !MaterialProperties.isWatertight( getBlockMaterial( coords ) ) )
			{
				continue;
			}
			
			if( m_numDisplacementBlocks.containsKey( coords.posY ) )
			{
				m_numDisplacementBlocks.put( coords.posY, m_numDisplacementBlocks.get( coords.posY ) + 1 );
			}
			else
			{
				m_numDisplacementBlocks.put( coords.posY, 1 );
			}
		}
		
		// compute the total mass
		m_shipMass = 0.0;
		for( ChunkCoordinates coords : m_blocks.coords() )
		{
			m_shipMass += MaterialProperties.getMass( getBlockMaterial( coords ) );
		}
	}
	
	public Vec3 getCenterOfMass( )
	{
		Vec3 com = Vec3.createVectorHelper( 0, 0, 0 );
		double totalMass = 0.0;
		for( ChunkCoordinates coords : m_blocks.coords() )
		{
			double mass = MaterialProperties.getMass( getBlockMaterial( coords ) );
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
		double displacedWaterMass = 0.0;
		for( Map.Entry<Integer,Integer> entry : m_numDisplacementBlocks.entrySet() )
		{
			int y = entry.getKey();
			int numBlocks = entry.getValue();
			
			displacedWaterMass += getBlockFractionSubmerged( y, waterHeight )*numBlocks*getWaterMass( y, waterHeight );
		}
		
		double shipWeight = m_shipMass*AccelerationGravity;
		double displacedWaterWeight = displacedWaterMass*AccelerationGravity;
		return displacedWaterWeight - shipWeight;
	}
	
	public double getEquilibriumWaterHeight( )
	{
		// travel up each later until we find the one that displaces too much water
		double displacedWaterMassSoFar = 0.0;
		for( Map.Entry<Integer,Integer> entry : m_numDisplacementBlocks.entrySet() )
		{
			int y = entry.getKey();
			int numBlocks = entry.getValue();
			
			// assume the water completely submerges this layer
			double waterHeight = y + 1;
			double displacedWaterMassThisLevel = numBlocks*getWaterMass( y, waterHeight );
			
			// did we displace too much water?
			if( displacedWaterMassSoFar + displacedWaterMassThisLevel > m_shipMass )
			{
				// good, the water height is in this block level
				
				// now solve for the water height
				return y + ( m_shipMass - displacedWaterMassSoFar )/numBlocks/getWaterMass( y, waterHeight );
			}
			else
			{
				// try the next later on the next iteration
				displacedWaterMassSoFar += displacedWaterMassThisLevel;
			}
		}
		
		// there aren't enough blocks! The ship will sink!
		return Double.NaN;
	}
	
	private double getBlockFractionSubmerged( int y, double waterHeight )
	{
		double bottom = y;
		double top = y + 1;
		if( top <= waterHeight )
		{
			return 1.0;
		}
		else if( bottom >= waterHeight )
		{
			return 0;
		}
		else
		{
			return waterHeight - bottom;
		}
	}
	
	private Material getBlockMaterial( ChunkCoordinates coords )
	{
		return m_blocks.getBlockMaterial( coords.posX, coords.posY, coords.posZ );
	}
	
	private double getWaterMass( int y, double waterHeight )
	{
		// UNDONE: use the surface height make the mass increase with depth
		return MaterialProperties.getMass( Material.water );
	}
}
