package cuchaz.ships.propulsion;

import java.util.Set;

import net.minecraft.util.ChunkCoordinates;
import cuchaz.modsShared.BlockSide;
import cuchaz.ships.ShipWorld;

public class Sail extends PropulsionMethod
{
	private int m_numExposedBlocks;
	
	protected Sail( ShipWorld world, Set<ChunkCoordinates> coords, BlockSide frontDirection )
	{
		super( "Sail", "Sails", coords );
		
		m_numExposedBlocks = getNumExposedBlocks( world, coords, frontDirection );
	}
	
	@Override
	public double getThrust( )
	{
		return 1.0*m_numExposedBlocks;
	}
	
	public boolean isValid( )
	{
		// if the number of exposed blocks is at least 50% of the sail, it's a good sail
		return m_numExposedBlocks >= getCoords().size()/2;
	}
	
	private int getNumExposedBlocks( ShipWorld world, Set<ChunkCoordinates> blockCoords, BlockSide frontDirection )
	{
		int numExposedBlocks = 0;
		ChunkCoordinates neighborCoords = new ChunkCoordinates();
		BlockSide backDirection = frontDirection.getOppositeSide();
		for( ChunkCoordinates coords : blockCoords )
		{
			neighborCoords.set(
				coords.posX + frontDirection.getDx(),
				coords.posY + frontDirection.getDy(),
				coords.posZ + frontDirection.getDz()
			);
			int frontId = world.getBlockId( neighborCoords );
			neighborCoords.set(
				coords.posX + backDirection.getDx(),
				coords.posY + backDirection.getDy(),
				coords.posZ + backDirection.getDz()
			);
			int backId = world.getBlockId( neighborCoords );
			
			if( frontId == 0 && backId == 0 )
			{
				numExposedBlocks++;
			}
		}
		return numExposedBlocks;
	}
}
