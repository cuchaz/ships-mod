package cuchaz.ships;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAccessor;
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
		dy = applyBackoff( dy, originalDy );
		entityBox.offset( 0, dy, 0 );
		
		double dx = originalDx;
		for( PossibleCollision collision : possibleCollisions )
		{
			dx = collision.box.calculateXOffset( entityBox, dx );
		}
		dx = applyBackoff( dx, originalDx );
		entityBox.offset( dx, 0, 0 );
		
		double dz = originalDz;
		for( PossibleCollision collision : possibleCollisions )
		{
			dz = collision.box.calculateZOffset( entityBox, dz );
		}
		dz = applyBackoff( dz, originalDz );
		entityBox.offset( 0, 0, dz );
		
		// translate back into world coordinates
		newPos.xCoord = ( entityBox.minX + entityBox.maxX )/2;
		newPos.yCoord = entityBox.minY + entity.yOffset - entity.ySize;
		newPos.zCoord = ( entityBox.minZ + entityBox.maxZ )/2;
		
		// TEMP
		if( entity instanceof EntityClientPlayerMP && !m_ship.worldObj.isRemote )
		{
			System.out.println( String.format( "entity %s: (%.4f,%.4f,%.4f) world=(%.4f,%.4f,%.4f) od=(%.4f,%.4f,%.4f) d=(%.4f,%.4f,%.4f)",
				entity.getClass().getSimpleName(),
				oldPos.xCoord, oldPos.yCoord + entity.ySize - entity.yOffset, oldPos.zCoord,
				oldX, oldY + entity.ySize - entity.yOffset, oldZ,
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
		
		// update fall state. Sadly, we can't just call this:
		//entity.updateFallState( dy, entity.onGround );
		// so we're going have to do it using package injection
		EntityAccessor.updateFallState( entity, dy, entity.onGround );
		
		// NEXTTIME: we keep falling through the bottom of the ship when:
		// we're flying and crouching to move down. While crouching, the ship is solid
		// when crouching stops, the player pops down ~0.2 and falls through
		// so far, signs point to the application of gravity without collision testing... somewhere...
		
		// can test by swimming up into the bottom of the ship
		// every tick, the player is magically moved back down (due to gravity?) without a call to moveEntity()
		// we need to find out how this position is changing...
		// maybe put a strack dump on changes to posY somehow? (ASM?)
		
		// another problem... the y velicity (due to gravity?) keeps decreasing
		// even when we're "standing" on the ship (ie onGround = true)
		
		// TEMP
		if( entity instanceof EntityClientPlayerMP && !m_ship.worldObj.isRemote )
		{
			newPos.xCoord = entity.posX;
			newPos.yCoord = entity.posY;
			newPos.zCoord = entity.posZ;
			m_ship.worldToShip( newPos );
			m_ship.shipToBlocks( newPos );
			
			System.out.println( String.format( "entity %s: (%.4f,%.4f,%.4f) world=(%.4f,%.4f,%.4f) onGround=%b fallDist=%.2f",
				entity.getClass().getSimpleName(),
				newPos.xCoord, newPos.yCoord + entity.ySize - entity.yOffset, newPos.zCoord,
				entity.posX, entity.posY + entity.ySize - entity.yOffset, entity.posZ,
				entity.onGround,
				entity.fallDistance
			) );
		}
	}
	
	private double applyBackoff( double d, double originalD )
	{
		// what is backoff and why do we need it?
		// the entity/world collision system doesn't use backoff...
		
		// Due to roundoff errors in world/blocks coordinate conversions,
		// sometimes collision calculations place entity JUST INSIDE an obstacle
		// Even though an intifintessimal translation would put the entity correctly outside of the obstacle,
		// once inside the obstacle, the entity can continue movement unimpeded.
		
		// either we have to detect (and prevent) when an entity is already colliding with an obstacle
		// (which might be preferable, but then players could get "stuck" in geometry),
		// or we have to place the entity so it is JUST OUTSIDE the obstacle after a collision,
		// instead of exactly on the boundary
		
		// distance in game coords, but small enough not to notice
		final double Backoff = 0.001;
		
		if( d != originalD )
		{
			// apply the backoff in the opposite direction of the delta
			if( d > 0 )
			{
				return d - Backoff;
			}
			else
			{
				return d + Backoff;
			}
		}
		return d;
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
