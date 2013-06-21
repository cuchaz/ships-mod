package cuchaz.ships;

import net.minecraft.block.material.Material;
import net.minecraft.util.ChunkCoordinates;

public class ShipPhysics
{
	private static final double AccelerationGravity = 0.01;
	
	private ShipWorld m_blocks;
	
	public ShipPhysics( ShipWorld blocks )
	{
		m_blocks = blocks;
	}
	
	public double getNetUpForce( double waterHeight )
	{
		double upForce = 0.0;
		for( ChunkCoordinates coords : m_blocks.coords() )
		{
			// UNDONE: handle trapped air blocks!
			upForce += -getBlockWeight( coords ) + getBlockBuoyancyForce( coords, waterHeight );
		}
		return upForce;
	}
	
	private double getBlockWeight( ChunkCoordinates coords )
	{
		// the downward force on the block is its weight
		return MaterialProperties.getMass( getBlockMaterial( coords ) )*AccelerationGravity;
	}
	
	private double getBlockBuoyancyForce( ChunkCoordinates coords, double waterHeight )
	{
		if( MaterialProperties.isWatertight( getBlockMaterial( coords ) ) )
		{
			// the upward force is the weight of the fluid that is displaced by the block
			double fractionSubmerged = getBlockFractionSubmerged( coords, waterHeight );
			double bottom = getBlockBottom( coords );
			double waterTopHeight = bottom + fractionSubmerged;
			double waterWeight = getWaterMass( bottom, waterTopHeight )*AccelerationGravity;
			return waterWeight;
		}
		return 0.0;
	}
	
	private double getBlockFractionSubmerged( ChunkCoordinates coords, double waterHeight )
	{
		double bottom = getBlockBottom( coords );
		double top = getBlockTop( coords );
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
	
	private double getBlockBottom( ChunkCoordinates coords )
	{
		return coords.posY;
	}
	
	private double getBlockTop( ChunkCoordinates coords )
	{
		return coords.posY + 1.0;
	}
	
	private Material getBlockMaterial( ChunkCoordinates coords )
	{
		return m_blocks.getBlockMaterial( coords.posX, coords.posY, coords.posZ );
	}
	
	private double getWaterMass( double bottomHeight, double topHeight )
	{
		// UNDONE: use the surface height make the mass increase with depth
		return MaterialProperties.getMass( Material.water )*( topHeight - bottomHeight );
	}
}
