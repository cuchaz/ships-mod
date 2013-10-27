package cuchaz.ships;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAccessor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.BoxCorner;
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
	
	private static class BlockCollisionResult
	{
		public double scaling;
		public int numCollidingBoxes;
	}
	
	
	private EntityShip m_ship;
	
	// DEBUG
	public List<ChunkCoordinates> m_highlightedCoords = new ArrayList<ChunkCoordinates>();
	public AxisAlignedBB m_queryBox = AxisAlignedBB.getBoundingBox( 0, 0, 0, 0, 0, 0 );
	
	public ShipCollider( EntityShip ship )
	{
		m_ship = ship;
	}
	
	public void computeShipBoundingBox( AxisAlignedBB box, double x, double y, double z, float yaw )
	{
		ShipWorld blocks = m_ship.getBlocks();
		if( blocks == null )
		{
			return;
		}
		
		// make an un-rotated box in world-space
		box.minX = x + m_ship.blocksToShipX( blocks.getBoundingBox().minX );
		box.minY = y + m_ship.blocksToShipY( blocks.getBoundingBox().minY );
		box.minZ = z + m_ship.blocksToShipZ( blocks.getBoundingBox().minZ );
		box.maxX = x + m_ship.blocksToShipX( blocks.getBoundingBox().maxX + 1 );
		box.maxY = y + m_ship.blocksToShipY( blocks.getBoundingBox().maxY + 1 );
		box.maxZ = z + m_ship.blocksToShipZ( blocks.getBoundingBox().maxZ + 1 );
		
		// now rotate by the yaw
		// UNDONE: optimize out the new
		RotatedBB rotatedBox = new RotatedBB( box.copy(), yaw, x, z );
		
		// compute the new xz bounds
		box.minX = Integer.MAX_VALUE;
		box.maxX = Integer.MIN_VALUE;
		box.minZ = Integer.MAX_VALUE;
		box.maxZ = Integer.MIN_VALUE;
		Vec3 p = Vec3.createVectorHelper( 0, 0, 0 );
		for( BoxCorner corner : BlockSide.Top.getCorners() )
		{
			rotatedBox.getCorner( p, corner );
			
			box.minX = Math.min( box.minX, p.xCoord );
			box.maxX = Math.max( box.maxX, p.xCoord );
			box.minZ = Math.min( box.minZ, p.zCoord );
			box.maxZ = Math.max( box.maxZ, p.zCoord );
		}
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
	
	public void onNearbyEntityMoved( double oldX, double oldY, double oldZ, double oldYSize, Entity entity )
	{
		// get boxes for the entity's original and final positions
		AxisAlignedBB oldEntityBox = AxisAlignedBB.getBoundingBox( 0, 0, 0, 0, 0, 0 );
		getEntityBoxInBlockSpace( oldEntityBox, entity, Vec3.createVectorHelper( oldX, oldY, oldZ ) );
		
		// get a box for the entity's current position
		AxisAlignedBB newEntityBox = AxisAlignedBB.getBoundingBox( 0, 0, 0, 0, 0, 0 );
		getEntityBoxInBlockSpace( newEntityBox, entity );
		
		// adjust the old box for the old ySize
		double dYSize = entity.ySize - oldYSize;
		oldEntityBox.maxY -= dYSize;
		oldEntityBox.minY -= dYSize;
		
		// TEMP
		if( entity instanceof EntityPlayer )
		{
			m_queryBox.setBB( oldEntityBox );
		}
		
		List<PossibleCollision> possibleCollisions = trajectoryQuery( oldEntityBox, newEntityBox );
		
		// get the deltas in blocks coordinates
		double originalDx = newEntityBox.minX - oldEntityBox.minX;
		double originalDy = newEntityBox.minY - oldEntityBox.minY;
		double originalDz = newEntityBox.minZ - oldEntityBox.minZ;
		
		// TEMP: render the boxes
		if( entity instanceof EntityPlayer )
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
		
		// no collisions? No changes needed
		if( possibleCollisions.isEmpty() )
		{
			return;
		}
		
		// calculate the actual collision
		// move along the manhattan path, stopping at the first collision
		// y first, then x, then z
		// different orders should give different collisions,
		// but for a small enough d vector, the difference should be un-noticeable
		double dy = originalDy;
		for( PossibleCollision collision : possibleCollisions )
		{
			dy = collision.box.calculateYOffset( oldEntityBox, dy );
		}
		dy = applyBackoff( dy, originalDy );
		oldEntityBox.offset( 0, dy, 0 );
		
		double dx = originalDx;
		for( PossibleCollision collision : possibleCollisions )
		{
			dx = collision.box.calculateXOffset( oldEntityBox, dx );
		}
		dx = applyBackoff( dx, originalDx );
		oldEntityBox.offset( dx, 0, 0 );
		
		double dz = originalDz;
		for( PossibleCollision collision : possibleCollisions )
		{
			dz = collision.box.calculateZOffset( oldEntityBox, dz );
		}
		dz = applyBackoff( dz, originalDz );
		oldEntityBox.offset( 0, 0, dz );
		
		// translate back into world coordinates
		Vec3 newPos = Vec3.createVectorHelper(
			( oldEntityBox.minX + oldEntityBox.maxX )/2,
			oldEntityBox.minY,
			( oldEntityBox.minZ + oldEntityBox.maxZ )/2
		);
		m_ship.blocksToShip( newPos );
		m_ship.shipToWorld( newPos );
		
		// update the entity properties
		entity.setPosition( newPos.xCoord, newPos.yCoord + entity.yOffset - entity.ySize, newPos.zCoord );
		entity.isCollidedHorizontally = originalDx != dx || originalDz != dz;
		entity.isCollidedVertically = originalDy != dy;
		entity.onGround = entity.isCollidedVertically && originalDy < 0;
		entity.isCollided = entity.isCollidedHorizontally || entity.isCollidedVertically;
		
		// if we collided, kill the velocity
		if( originalDx != dx )
		{
			entity.motionX = 0;
		}
		if( originalDy != dy )
		{
			entity.motionY = 0;
		}
		if( originalDz != dz )
		{
			entity.motionZ = 0;
		}
        
		// update fall state. Sadly, we can't just call this:
		//entity.updateFallState( dy, entity.onGround );
		// so we're going have to do it using package injection
		EntityAccessor.updateFallState( entity, dy, entity.onGround );
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
	
	public AxisAlignedBB getBlockWorldBoundingBox( AxisAlignedBB box, ChunkCoordinates coords )
	{
		// transform the block center into world space
		Vec3 p = Vec3.createVectorHelper( coords.posX + 0.5, coords.posY + 0.5, coords.posZ + 0.5 );
		m_ship.blocksToShip( p );
		m_ship.shipToWorld( p );
		
		// compute the halfwidth of the bounding box
		float yawRad = (float)Math.toRadians( m_ship.rotationYaw );
		double cos = MathHelper.cos( yawRad );
		double sin = MathHelper.sin( yawRad );
		double halfSize =  Math.max(
			Math.abs( cos - sin ),
			Math.abs( sin + cos )
		)/2;
		
		return box.setBounds(
			p.xCoord - halfSize, p.yCoord - 0.5, p.zCoord - halfSize,
			p.xCoord + halfSize, p.yCoord + 0.5, p.zCoord + halfSize
		);
	}
	
	public AxisAlignedBB getBlockWorldBoundingBox( AxisAlignedBB box, ChunkCoordinates coords, double shipX, double shipY, double shipZ, float shipYaw )
	{
		// temporarily place the ship at the new position
		double oldX = m_ship.posX;
		double oldY = m_ship.posY;
		double oldZ = m_ship.posZ;
		float oldYaw = m_ship.rotationYaw;
		
		// temporarily move the ship to the new location
		m_ship.posX = shipX;
		m_ship.posY = shipY;
		m_ship.posZ = shipZ;
		m_ship.rotationYaw = shipYaw;
		
		AxisAlignedBB blockWorldBox = getBlockWorldBoundingBox( box, coords );
		
		// restore the ship before anyone notices =P
		m_ship.posX = oldX;
		m_ship.posY = oldY;
		m_ship.posZ = oldZ;
		m_ship.rotationYaw = oldYaw;
		
		return blockWorldBox;
	}
	
	public void moveShip( double dx, double dy, double dz, float dYaw )
	{
		// compute the scaling of the delta (between 0 and 1) that avoids collisions
		double scaling = 1.0;
		int numCollidingBoxes = 0;
		BlockCollisionResult collisionResult = new BlockCollisionResult();
		for( ChunkCoordinates coords : m_ship.getBlocks().coords() )
		{
			checkBlockCollision( collisionResult, coords, dx, dy, dz, dYaw );
			if( collisionResult.scaling < 1.0 )
			{
				scaling = Math.min( scaling, collisionResult.scaling );
			}
			numCollidingBoxes += collisionResult.numCollidingBoxes;
		}
		
		// avoid the collision
		dx *= scaling;
		dy *= scaling;
		dz *= scaling;
		
		// if there are any collisions, don't try to compute the dYaw that avoids the collision
		// just kill any rotation
		if( numCollidingBoxes > 0 )
		{
			dYaw = 0;
		}
		
		// apply the new delta
		m_ship.rotationYaw += dYaw;
		m_ship.setPosition(
			m_ship.posX + dx,
			m_ship.posY + dy,
			m_ship.posZ + dz
		);
	}
	
	public List<Entity> getRiders( )
	{
		// get all nearby entities
		AxisAlignedBB checkBox = m_ship.boundingBox.copy();
		checkBox.expand( 1, 1, 1 );
		@SuppressWarnings( "unchecked" )
		List<Entity> entities = m_ship.worldObj.getEntitiesWithinAABB( Entity.class, checkBox );
		
		// remove any entities from the list not close enough to be considered riding
		// also remove entities that are floating or moving upwards (e.g. jumping)
		Iterator<Entity> iter = entities.iterator();
		while( iter.hasNext() )
		{
			Entity entity = iter.next();
			if( entity == m_ship || !isEntityAboard( entity ) )
			{
				iter.remove();
			}
		}
		return entities;
	}
	
	public boolean isEntityAboard( Entity entity )
	{
		// easy check first
		if( !entity.onGround )
		{
			return false;
		}
		
		final double RiderEpsilon = 1e-1;
		
		// get the closest block y the entity could be standing on
		double entityMinY = m_ship.shipToBlocksY( m_ship.worldToShipY( entity.boundingBox.minY ) );
		int blockY = (int)( entityMinY + 0.5 ) - 1;
		
		// get the bounding box of the entity in block space
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox( 0, 0, 0, 0, 0, 0 );
		getEntityBoxInBlockSpace( box, entity );
		
		for( ChunkCoordinates coords : m_ship.getBlocks().getGeometry().rangeQuery( box, blockY ) )
		{
			boolean isOnBlock = Math.abs( coords.posY + 1 - box.minY ) <= RiderEpsilon;
			if( isOnBlock )
			{
				return true;
			}
		}
		
		return false;
	}
	
	private void checkBlockCollision( BlockCollisionResult result, ChunkCoordinates coords, double dx, double dy, double dz, float dYaw )
	{
		// get the current world bounding box for the ship block
		AxisAlignedBB shipBlockBox = AxisAlignedBB.getBoundingBox( 0, 0, 0, 0, 0, 0 );
		getBlockWorldBoundingBox( shipBlockBox, coords );
		
		// where would the ship block move to?
		double nextX = m_ship.posX + dx;
		double nextY = m_ship.posY + dy;
		double nextZ = m_ship.posZ + dz;
		float nextYaw = m_ship.rotationYaw + dYaw;
		AxisAlignedBB nextShipBlockBox = AxisAlignedBB.getBoundingBox( 0, 0, 0, 0, 0, 0 );
 		getBlockWorldBoundingBox( nextShipBlockBox, coords, nextX, nextY, nextZ, nextYaw );
 		
 		// func_111270_a returns the bounding box of both boxes
 		AxisAlignedBB combinedBlockBox = shipBlockBox.func_111270_a( nextShipBlockBox );
 		
 		// do a range query to get colliding world blocks
		List<AxisAlignedBB> nearbyWorldBlocks = new ArrayList<AxisAlignedBB>();
        int minX = MathHelper.floor_double( combinedBlockBox.minX );
        int maxX = MathHelper.floor_double( combinedBlockBox.maxX );
        int minY = MathHelper.floor_double( combinedBlockBox.minY );
        int maxY = MathHelper.floor_double( combinedBlockBox.maxY );
        int minZ = MathHelper.floor_double( combinedBlockBox.minZ );
        int maxZ = MathHelper.floor_double( combinedBlockBox.maxZ );
        for( int x=minX; x<=maxX; x++ )
        {
            for( int z=minZ; z<=maxZ; z++ )
            {
                for( int y=minY; y<=maxY; y++ )
                {
                    Block block = Block.blocksList[m_ship.worldObj.getBlockId( x, y, z )];
                    if( block != null )
                    {
                        block.addCollisionBoxesToList( m_ship.worldObj, x, y, z, combinedBlockBox, nearbyWorldBlocks, null );
                    }
                }
            }
        }
        
        // get the scaling that avoids the collision
        result.scaling = 1;
        for( AxisAlignedBB worldBlockBox : nearbyWorldBlocks )
		{
        	result.scaling = Math.min( result.scaling, getScalingToAvoidCollision( shipBlockBox, dx, dy, dz, worldBlockBox ) );
		}
        result.numCollidingBoxes = nearbyWorldBlocks.size();
	}
	
	private double getScalingToAvoidCollision( AxisAlignedBB box, double dx, double dy, double dz, AxisAlignedBB obstacleBox )
	{
		double sx = getScalingToAvoidCollision( dx, box.minX, box.maxX, obstacleBox.minX, obstacleBox.maxX );
		double sy = getScalingToAvoidCollision( dy, box.minY, box.maxY, obstacleBox.minY, obstacleBox.maxY );
		double sz = getScalingToAvoidCollision( dz, box.minZ, box.maxZ, obstacleBox.minZ, obstacleBox.maxZ );
		return Math.min( sx, Math.min( sy, sz ) );
	}
	
	private double getScalingToAvoidCollision( double delta, double selfMin, double selfMax, double obstacleMin, double obstacleMax )
	{
		// by default, don't scale the delta
		double scaling = 1;
		
		if( delta > 0 )
		{
			double dist = obstacleMin - selfMax;
			if( dist >= 0 )
			{
				// TEMP
				System.out.println( String.format(
					"%s dist to collision: %.4f, scaling: %.4f",
					m_ship.worldObj.isRemote ? "CLIENT" : "SERVER",
					dist, dist/delta
				) );
				
				return dist/delta;
			}
		}
		else if( delta < 0 )
		{
			double dist = selfMin - obstacleMax;
			if( dist >= 0 )
			{
				// TEMP
				System.out.println( String.format(
					"%s dist to collision: %.4f, scaling: %.4f",
					m_ship.worldObj.isRemote ? "CLIENT" : "SERVER",
					dist, dist/-delta
				) );
				
				return dist/-delta;
			}
		}
		
		assert( scaling >= 0 );
		return scaling;
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
	
	private List<PossibleCollision> trajectoryQuery( AxisAlignedBB oldBox, AxisAlignedBB newBox )
	{
		// get a bounding box containing the entire entity trajectory
		AxisAlignedBB trajectoryBox = oldBox.func_111270_a( newBox );
		
		// collect the boxes for the blocks in that box
		List<PossibleCollision> collisions = new ArrayList<PossibleCollision>();
		for( ChunkCoordinates coords : m_ship.getBlocks().getGeometry().rangeQuery( trajectoryBox ) )
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
	
	private void getEntityBoxInBlockSpace( AxisAlignedBB box, Entity entity )
	{
		getEntityBoxInBlockSpace( box, entity, Vec3.createVectorHelper( entity.posX, entity.posY, entity.posZ ) );
	}
	
	private void getEntityBoxInBlockSpace( AxisAlignedBB box, Entity entity, Vec3 pos )
	{
		// copy the vector since we need to modify it
		pos = Vec3.createVectorHelper( pos.xCoord, pos.yCoord, pos.zCoord );
		
		// transform to block coords
		m_ship.worldToShip( pos );
		m_ship.shipToBlocks( pos );
		
		// set the box here
		box.setBB( entity.boundingBox );
		box.offset( -entity.posX, -entity.posY, -entity.posZ );
		box.offset( pos.xCoord, pos.yCoord, pos.zCoord );
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
