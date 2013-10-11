package cuchaz.ships;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;

public class ShipCollider
{
	private EntityShip m_ship;
	
	public ShipCollider( EntityShip ship )
	{
		m_ship = ship;
	}
	
	public void onNearbyEntityMoved( double oldX, double oldY, double oldZ, Entity entity )
	{
		Vec3 oldPos = Vec3.createVectorHelper( oldX, oldY, oldZ );
		Vec3 newPos = Vec3.createVectorHelper( entity.posX, entity.posY, entity.posZ );
		
		// translate everything into the blocks coordinates
		m_ship.worldToShip( oldPos );
		m_ship.shipToBlocks( oldPos );
		m_ship.worldToShip( newPos );
		m_ship.shipToBlocks( newPos );
		
		List<AxisAlignedBB> boxes = getBoundingBoxesNearEntity( oldPos, newPos, entity.boundingBox );
		
		// NEXTTIME: we have the nearby boxes now. Use them to move the entity like Entity.moveEntity() does.
		
	}
	
	private List<AxisAlignedBB> getBoundingBoxesNearEntity( Vec3 oldPos, Vec3 newPos, AxisAlignedBB entityBox )
	{
		// make a box that contains the entire entity trajectory
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
			Math.min( oldPos.xCoord - entityBox.minX, newPos.xCoord - entityBox.minX ),
			Math.min( oldPos.yCoord - entityBox.minY, newPos.yCoord - entityBox.minY ),
			Math.min( oldPos.zCoord - entityBox.minZ, newPos.zCoord - entityBox.minZ ),
			Math.max( oldPos.xCoord + entityBox.maxX, newPos.xCoord + entityBox.maxX ),
			Math.max( oldPos.yCoord + entityBox.maxY, newPos.yCoord + entityBox.maxY ),
			Math.max( oldPos.zCoord + entityBox.maxZ, newPos.zCoord + entityBox.maxZ )
		);
		
		// collect the boxes for the blocks in that box
		// UNDONE: optimize out the new
		List<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
		for( ChunkCoordinates coords : m_ship.getBlocks().getGeometry().rangeQuery( box ) )
		{
			Block block = Block.blocksList[m_ship.getBlocks().getBlockId( coords )];
			block.setBlockBoundsBasedOnState( m_ship.getBlocks(), coords.posX, coords.posY, coords.posZ );
			boxes.add( AxisAlignedBB.getBoundingBox(
				block.getBlockBoundsMinX(),
				block.getBlockBoundsMinY(),
				block.getBlockBoundsMinZ(),
				block.getBlockBoundsMaxX(),
				block.getBlockBoundsMaxY(),
				block.getBlockBoundsMaxZ()
			) );
		}
		return boxes;
	}
}
