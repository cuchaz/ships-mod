package cuchaz.ships;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import cuchaz.modsShared.RotatedBB;

public class ShipCollider
{
	private static class Collision
	{
		public ChunkCoordinates coords;
		public AxisAlignedBB box;
		
		public Collision( ChunkCoordinates coords, AxisAlignedBB box )
		{
			this.coords = coords;
			this.box = box;
		}
	}
	
	
	private EntityShip m_ship;
	
	// DEBUG
	public List<ChunkCoordinates> m_highlightedCoords = new ArrayList<ChunkCoordinates>();
	public AxisAlignedBB m_queryBox = AxisAlignedBB.getBoundingBox( 0, 0, 0, 0, 0, 0 );
	
	public ShipCollider( EntityShip ship )
	{
		m_ship = ship;
	}
	
	public TreeSet<MovingObjectPosition> getBlocksPlayerIsLookingAt( EntityPlayer player )
	{
		// get the player look line segment
		Vec3 eyePos = player.worldObj.getWorldVec3Pool().getVecFromPool(
			player.posX,
			player.posY + player.ySize - player.yOffset + player.getEyeHeight(),
			player.posZ
		);
        
		final double toRadians = Math.PI / 180.0;
		float pitch = (float)( player.rotationPitch * toRadians );
		float yaw = (float)( player.rotationYaw * toRadians );
		float cosYaw = MathHelper.cos( -yaw - (float)Math.PI );
		float sinYaw = MathHelper.sin( -yaw - (float)Math.PI );
		float cosPitch = MathHelper.cos( -pitch );
		float sinPitch = MathHelper.sin( -pitch );
		double dist = 5.0;
		
		Vec3 targetPos = eyePos.addVector(
			sinYaw * -cosPitch * dist,
			sinPitch * dist,
			cosYaw * -cosPitch * dist
		);
		
		// convert the positions into blocks space
		m_ship.worldToShip( eyePos );
		m_ship.worldToShip( targetPos );
		m_ship.shipToBlocks( eyePos );
		m_ship.shipToBlocks( targetPos );
		
		// find out what blocks this line segment intersects
		return lineSegmentQuery( eyePos, targetPos );
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
		
		List<Collision> collisions = getEntityCollisions( oldPos, newPos, entity );
		
		// TEMP: render the boxes
		if( entity instanceof EntityPlayer )
		{
			synchronized( m_highlightedCoords )
			{
				m_highlightedCoords.clear();
				for( Collision collision : collisions )
				{
					m_highlightedCoords.add( collision.coords );
				}
			}
		}
	}
	
	public AxisAlignedBB getBlockBoxInBlockSpace( ChunkCoordinates coords )
	{
		Block block = Block.blocksList[m_ship.getBlocks().getBlockId( coords )];
		block.setBlockBoundsBasedOnState( m_ship.getBlocks(), coords.posX, coords.posY, coords.posZ );
		return AxisAlignedBB.getBoundingBox(
			block.getBlockBoundsMinX() + coords.posX,
			block.getBlockBoundsMinY() + coords.posY,
			block.getBlockBoundsMinZ() + coords.posZ,
			block.getBlockBoundsMaxX() + coords.posX,
			block.getBlockBoundsMaxY() + coords.posY,
			block.getBlockBoundsMaxZ() + coords.posZ
		);
	}
	
	public RotatedBB getBlockBoxInWorldSpace( ChunkCoordinates coords )
	{
		return m_ship.blocksToWorld( getBlockBoxInBlockSpace( coords ) );
	}
	
	private List<Collision> getEntityCollisions( Vec3 oldPos, Vec3 newPos, Entity entity )
	{
		// make a box that contains the entire entity trajectory
		// remember, the y pos is wonky
		// box.minY = posY - yOffset + ySize
		// box.maxY = box.minY + height
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
			Math.min( oldPos.xCoord, newPos.xCoord ),
			Math.min( oldPos.yCoord, newPos.yCoord ),
			Math.min( oldPos.zCoord, newPos.zCoord ),
			Math.max( oldPos.xCoord, newPos.xCoord ),
			Math.max( oldPos.yCoord, newPos.yCoord ),
			Math.max( oldPos.zCoord, newPos.zCoord )
		);
		double hdx = ( entity.boundingBox.maxX - entity.boundingBox.minX )/2;
		double hdz = ( entity.boundingBox.maxZ - entity.boundingBox.minZ )/2;
		box.minX -= hdx;
		box.maxX += hdx;
		box.minY += entity.ySize - entity.yOffset;
		box.maxY += entity.ySize - entity.yOffset + entity.height;
		box.minZ -= hdz;
		box.maxZ += hdz;
		
		// TEMP
		if( entity instanceof EntityPlayer )
		{
			System.out.println( String.format( "%s old: (%.2f,%.2f,%.2f), new: (%.2f,%.2f,%.2f), check box: [%.2f,%.2f]x[%.2f,%.2f]x[%.2f,%.2f]",
				m_ship.worldObj.isRemote ? "CLIENT" : "SERVER",
				oldPos.xCoord, oldPos.yCoord, oldPos.zCoord,
				newPos.xCoord, newPos.yCoord, newPos.zCoord,
				box.minX, box.maxX, box.minY, box.maxY, box.minZ, box.maxZ
			) );
			
			m_queryBox.setBB( box );
		}
		
		// collect the boxes for the blocks in that box
		// UNDONE: optimize out the new
		List<Collision> collisions = new ArrayList<Collision>();
		for( ChunkCoordinates coords : m_ship.getBlocks().getGeometry().rangeQuery( box ) )
		{
			Block block = Block.blocksList[m_ship.getBlocks().getBlockId( coords )];
			block.setBlockBoundsBasedOnState( m_ship.getBlocks(), coords.posX, coords.posY, coords.posZ );
			collisions.add(
				new Collision( coords, AxisAlignedBB.getBoundingBox(
					block.getBlockBoundsMinX() + coords.posX,
					block.getBlockBoundsMinY() + coords.posY,
					block.getBlockBoundsMinZ() + coords.posZ,
					block.getBlockBoundsMaxX() + coords.posX,
					block.getBlockBoundsMaxY() + coords.posY,
					block.getBlockBoundsMaxZ() + coords.posZ
				) )
			);
		}
		
		// TEMP
		if( entity instanceof EntityPlayer )
		{
			System.out.println( String.format( "%s collisions: %d",
				m_ship.worldObj.isRemote ? "CLIENT" : "SERVER",
				collisions.size()
			) );
		}
		
		return collisions;
	}
	
	private TreeSet<MovingObjectPosition> lineSegmentQuery( final Vec3 from, Vec3 to )
	{
		// do a range query using the bounding box of the line segment
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
			Math.min( from.xCoord, to.xCoord ),
			Math.min( from.yCoord, to.yCoord ),
			Math.min( from.zCoord, to.zCoord ),
			Math.max( from.xCoord, to.xCoord ),
			Math.max( from.yCoord, to.yCoord ),
			Math.max( from.zCoord, to.zCoord )
		);
		List<ChunkCoordinates> nearbyBlocks = m_ship.getBlocks().getGeometry().rangeQuery( box );
		
		// sort the boxes by their line/box intersection distance to the "from" point
		// throw out boxes that don't actually intersect the line segment
		TreeSet<MovingObjectPosition> sortedIntersections = new TreeSet<MovingObjectPosition>( new Comparator<MovingObjectPosition>( )
		{
			@Override
			public int compare( MovingObjectPosition a, MovingObjectPosition b )
			{
				// return the point closer to the "from" point
				return Double.compare( a.hitVec.distanceTo( from ), b.hitVec.distanceTo( from ) );
			}
		} );
		for( ChunkCoordinates coords : nearbyBlocks )
		{
			// get the intersection point with the line segment
			Block block = Block.blocksList[m_ship.getBlocks().getBlockId( coords )];
			MovingObjectPosition intersection = block.collisionRayTrace( m_ship.getBlocks(), coords.posX, coords.posY, coords.posZ, from, to );
			if( intersection != null )
			{
				sortedIntersections.add( intersection );
			}
		}
		return sortedIntersections;
	}
}
