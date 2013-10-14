package cuchaz.ships;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityClientPlayerMP;
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
	private static class PossibleCollision
	{
		public ChunkCoordinates coords;
		public AxisAlignedBB box;
		
		public PossibleCollision( ChunkCoordinates coords, AxisAlignedBB box )
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
		double originalDx = newPos.xCoord - oldPos.xCoord;
		double originalDy = newPos.yCoord - oldPos.yCoord;
		double originalDz = newPos.zCoord - oldPos.zCoord;
		
		List<PossibleCollision> possibleCollisions = getEntityPossibleCollisions( oldPos, newPos, entity );
		
		// TEMP: render the boxes
		if( entity instanceof EntityClientPlayerMP )
		{
			synchronized( m_highlightedCoords )
			{
				m_highlightedCoords.clear();
				for( PossibleCollision collision : possibleCollisions )
				{
					m_highlightedCoords.add( collision.coords );
				}
			}
		}
		
		// TEMP
		if( entity instanceof EntityClientPlayerMP && !m_ship.worldObj.isRemote )
		{
			System.out.println( String.format( "collisions for %s: %d",
				entity.getClass().getSimpleName(),
				possibleCollisions.size()
			) );
		}
		
		// no collisions? No changes needed
		if( possibleCollisions.isEmpty() )
		{
			return;
		}
		
		// make a box for the entity in block space
		double hw = entity.width/2;
		AxisAlignedBB entityBox = AxisAlignedBB.getBoundingBox(
			oldPos.xCoord - hw,
			oldPos.yCoord + entity.ySize - entity.yOffset,
			oldPos.zCoord - hw,
			oldPos.xCoord + hw,
			oldPos.yCoord + entity.ySize - entity.yOffset + entity.height,
			oldPos.zCoord + hw
		);
		
		// calculate the actual collision
		// move along the manhattan path, stopping at the first collision
		// y first, then x, then z
		// different orders should give different collisions,
		// but for a small enough d vector, the difference should be un-noticeable
		double dy = originalDy;
		for( PossibleCollision collision : possibleCollisions )
		{
			dy = collision.box.calculateYOffset( entityBox, dy );
		}
		entityBox.offset( 0, dy, 0 );
		
		double dx = originalDx;
		for( PossibleCollision collision : possibleCollisions )
		{
			dx = collision.box.calculateXOffset( entityBox, dx );
		}
		entityBox.offset( dx, 0, 0 );
		
		double dz = originalDz;
		for( PossibleCollision collision : possibleCollisions )
		{
			dz = collision.box.calculateZOffset( entityBox, dz );
		}
		entityBox.offset( 0, 0, dz );
		
		// NEXTTIME: roundoff error from the coordinate transformation is screwing up the collisions!
		// we need to make a version of AxisAlignedBB.calculateYOffset() that takes an epsilon
		// wait... that won't work by itself... the roundoff error will let up keep creeping through the epsilon
		// we need a way to go backwards a tiny bit... maybe?
		
		// translate back into world coordinates
		newPos.xCoord = ( entityBox.minX + entityBox.maxX )/2;
		newPos.yCoord = entityBox.minY + entity.yOffset - entity.ySize;
		newPos.zCoord = ( entityBox.minZ + entityBox.maxZ )/2;
		
		// TEMP
		if( entity instanceof EntityClientPlayerMP && !m_ship.worldObj.isRemote )
		{
			System.out.println( String.format( "entity %s: (%.4f,%.4f,%.12f)->(%.4f,%.4f,%.12f) od=(%.4f,%.4f,%.12f) d=(%.4f,%.4f,%.12f)",
				entity.getClass().getSimpleName(),
				oldPos.xCoord, oldPos.yCoord, oldPos.zCoord,
				newPos.xCoord, newPos.yCoord, newPos.zCoord,
				originalDx, originalDy, originalDz,
				dx, dy, dz
			) );
		}
		
		m_ship.blocksToShip( newPos );
		m_ship.shipToWorld( newPos );
		
		// update the entity properties
		entity.setPosition( newPos.xCoord, newPos.yCoord, newPos.zCoord );
		entity.isCollidedHorizontally = originalDx != dx || originalDz != dz;
		entity.isCollidedVertically = originalDy != dy;
		entity.onGround = entity.isCollidedVertically && originalDy < 0;
		entity.isCollided = entity.isCollidedHorizontally || entity.isCollidedVertically;
		
		// UNDONE: find out how to apply fall damage
		//entity.updateFallState( dy, entity.onGround );
		
		// TEMP
		if( entity instanceof EntityClientPlayerMP && !m_ship.worldObj.isRemote )
		{
			newPos.xCoord = entity.posX;
			newPos.yCoord = entity.posY;
			newPos.zCoord = entity.posZ;
			m_ship.worldToShip( newPos );
			m_ship.shipToBlocks( newPos );
			
			System.out.println( String.format( "entity %s pos: (%.4f,%.4f,%.12f)",
				entity.getClass().getSimpleName(),
				newPos.xCoord, newPos.yCoord, newPos.zCoord
			) );
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
	
	private List<PossibleCollision> getEntityPossibleCollisions( Vec3 oldPos, Vec3 newPos, Entity entity )
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
		double hw = entity.width/2;
		box.minX -= hw;
		box.maxX += hw;
		box.minY += entity.ySize - entity.yOffset;
		box.maxY += entity.ySize - entity.yOffset + entity.height;
		box.minZ -= hw;
		box.maxZ += hw;
		
		// TEMP
		if( entity instanceof EntityPlayer )
		{
			m_queryBox.setBB( box );
		}
		
		// collect the boxes for the blocks in that box
		// UNDONE: optimize out the new
		List<PossibleCollision> collisions = new ArrayList<PossibleCollision>();
		for( ChunkCoordinates coords : m_ship.getBlocks().getGeometry().rangeQuery( box ) )
		{
			Block block = Block.blocksList[m_ship.getBlocks().getBlockId( coords )];
			block.setBlockBoundsBasedOnState( m_ship.getBlocks(), coords.posX, coords.posY, coords.posZ );
			collisions.add(
				new PossibleCollision( coords, AxisAlignedBB.getBoundingBox(
					block.getBlockBoundsMinX() + coords.posX,
					block.getBlockBoundsMinY() + coords.posY,
					block.getBlockBoundsMinZ() + coords.posZ,
					block.getBlockBoundsMaxX() + coords.posX,
					block.getBlockBoundsMaxY() + coords.posY,
					block.getBlockBoundsMaxZ() + coords.posZ
				) )
			);
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
