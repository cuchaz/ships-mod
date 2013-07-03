package cuchaz.ships;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.BoxCorner;
import cuchaz.modsShared.CircleRange;
import cuchaz.modsShared.CompareReal;
import cuchaz.modsShared.RotatedBB;

public class EntityShip extends Entity
{
	// data watcher IDs. Entity uses [0,1]. We can use [2,31]
	private static final int WatcherIdBlocks = 2;
	private static final int WatcherIdShipType = 3;
	private static final int WatcherIdWaterHeight = 4;
	
	private static final double RiderEpsilon = 0.2;
	
	public float motionYaw;
	
	private ShipWorld m_blocks;
	private TreeMap<ChunkCoordinates,EntityShipBlock> m_blockEntities;
	private EntityShipBlock[] m_blockEntitiesArray;
	private ShipPhysics m_physics;
	private double m_shipBlockX;
	private double m_shipBlockY;
	private double m_shipBlockZ;
	private int m_pilotActions;
	private BlockSide m_sideShipForward;
	private boolean m_hasInfoFromServer;
	private double m_xFromServer;
	private double m_yFromServer;
	private double m_zFromServer;
	private float m_yawFromServer;
	private float m_pitchFromServer;
	
	public EntityShip( World world )
	{
		super( world );
		yOffset = 0.0f;
		motionX = 0.0;
		motionY = 0.0;
		motionZ = 0.0;
		motionYaw = 0.0f;
		
		m_blocks = null;
		m_blockEntities = null;
		m_blockEntitiesArray = null;
		m_physics = null;
		m_shipBlockX = 0;
		m_shipBlockY = 0;
		m_shipBlockZ = 0;
		m_pilotActions = 0;
		m_sideShipForward = null;
		m_hasInfoFromServer = false;
		m_xFromServer = 0;
		m_yFromServer = 0;
		m_zFromServer = 0;
		m_yawFromServer = 0;
		m_pitchFromServer = 0;
	}
	
	@Override
	protected void entityInit( )
	{
		// this gets called inside super.Entity( World )
		// it seems to be used to init the data watcher
		
		// allocate a slot for the block data
		dataWatcher.addObject( WatcherIdBlocks, "" );
		dataWatcher.addObject( WatcherIdShipType, 0 );
		dataWatcher.addObject( WatcherIdWaterHeight, -1 );
	}
	
	public void setBlocks( ShipWorld blocks )
	{
		// reset the motion again. For some reason, the client entity gets bogus velocities from somewhere...
		motionX = 0.0;
		motionY = 0.0;
		motionZ = 0.0;
		motionYaw = 0.0f;
		
		m_blocks = blocks;
		blocks.setShip( this );
		m_physics = new ShipPhysics( m_blocks );
		
		// TEMP: put the ship back
		if( false )
		{
			posX = 141;
			posY = 64;
			posZ = 282;
		}
		
		// get the ship center of mass so we can convert between ship/block spaces
		Vec3 centerOfMass = m_physics.getCenterOfMass();
		m_shipBlockX = -centerOfMass.xCoord;
		m_shipBlockY = -centerOfMass.yCoord;
		m_shipBlockZ = -centerOfMass.zCoord;
		
		// save the data into the data watcher so it gets sync'd to the client
		dataWatcher.updateObject( WatcherIdBlocks, m_blocks.getDataString() );
		
		// build the sub entities
		m_blockEntities = new TreeMap<ChunkCoordinates, EntityShipBlock>();
		for( ChunkCoordinates block : m_blocks.coords() )
		{
			EntityShipBlock entityBlock = new EntityShipBlock( worldObj, this, block );
			m_blockEntities.put( block, entityBlock );
		}
		
		// flatten to an array
		m_blockEntitiesArray = new EntityShipBlock[m_blockEntities.size()];
		m_blockEntities.values().toArray( m_blockEntitiesArray );
		
		computeBoundingBox( boundingBox, posX, posY, posZ, rotationYaw );
		
		// TEMP
		System.out.println( String.format(
			"%s EntityShip initialized at (%.2f,%.2f,%.2f) + (%.4f,%.4f,%.4f)",
			worldObj.isRemote ? "CLIENT" : "SERVER",
			posX, posY, posZ,
			motionX, motionY, motionZ
		) );
	}
	
	@Override
	public void setDead( )
	{
		super.setDead();
		
		// TEMP
		System.out.println( ( worldObj.isRemote ? "CLIENT" : "SERVER" ) + " EntityShip died!" );
	}
	
	@Override
	protected void readEntityFromNBT( NBTTagCompound nbt )
	{
		setShipType( ShipType.values()[nbt.getInteger( "shipType" )] );
		setWaterHeight( nbt.getInteger( "waterHeight" ) );
		setBlocks( new ShipWorld( worldObj, nbt.getByteArray( "blocks" ) ) );
	}
	
	@Override
	protected void writeEntityToNBT( NBTTagCompound nbt )
	{
		nbt.setInteger( "shipType", getShipType().ordinal() );
		nbt.setInteger( "waterHeight", getWaterHeight() );
		nbt.setByteArray( "blocks", m_blocks.getData() );
	}
	
	public ShipWorld getBlocks( )
	{
		return m_blocks;
	}
	
	public ShipType getShipType( )
	{
		return ShipType.values()[dataWatcher.getWatchableObjectInt( WatcherIdShipType )];
	}
	public void setShipType( ShipType val )
	{
		dataWatcher.updateObject( WatcherIdShipType, val.ordinal() );
	}
	
	public int getWaterHeight( )
	{
		return dataWatcher.getWatchableObjectInt( WatcherIdWaterHeight );
	}
	public void setWaterHeight( int val )
	{
		dataWatcher.updateObject( WatcherIdWaterHeight, val );
	}
	
	@Override
	public Entity[] getParts()
    {
        return m_blockEntitiesArray;
    }
	
	public EntityShipBlock getBlockEntity( ChunkCoordinates coords )
	{
		return m_blockEntities.get( coords );
	}
	
	public EntityShipBlock getShipBlockEntity( )
	{
		return m_blockEntities.get( new ChunkCoordinates( 0, 0, 0 ) );
	}
	
	@Override
	public boolean canBeCollidedWith()
    {
        return false;
    }
	
	@Override
	public void setPosition( double x, double y, double z )
	{
		posX = x;
        posY = y;
        posZ = z;
		computeBoundingBox( boundingBox, posX, posY, posZ, rotationYaw );
		
		if( m_blockEntitiesArray != null )
		{
			// update blocks
			for( EntityShipBlock block : m_blockEntitiesArray )
			{
				block.updatePositionAndRotationFromShip( this );
			}
		}
	}
	
	@Override
	public void setPositionAndRotation2( double x, double y, double z, float yaw, float pitch, int alwaysThree )
	{
		// NOTE: this function should really be called onGetUpdatedPositionFromServer()
		
		// just save the info and we'll deal with it on the next update tick
		m_hasInfoFromServer = true;
		m_xFromServer = x;
		m_yFromServer = y;
		m_zFromServer = z;
		m_yawFromServer = yaw;
		m_pitchFromServer = pitch;
	}
	
	@Override
	public void onUpdate( )
	{
		// on the client, see if the blocks loaded yet
		if( worldObj.isRemote )
		{
			if( m_blocks == null )
			{
				// do we have blocks from the data watcher?
				String blockData = dataWatcher.getWatchableObjectString( WatcherIdBlocks );
				if( blockData != null && blockData.length() > 0 )
				{
					// then load the blocks
					setBlocks( new ShipWorld( worldObj, blockData ) );
				}
			}
			
			// UNDONE: find a way to tell if blocks were changed
		}
		
		// don't do any updating until we get blocks
		if( m_blocks == null )
		{
			return;
		}
		
		// simulate the vertical forces
		double upForce = m_physics.getNetUpForce( getWaterHeight() - posY );
		motionY += upForce;
		
		// handle thrust
		applyThrust();
		
		// UNDONE: update this to be rotation-aware!
		//adjustMotionBecauseOfBlockCollisions();
		
		double dx = motionX;
		double dy = motionY;
		double dz = motionZ;
		float dYaw = motionYaw;
		
		// did we get an updated position from the server?
		if( m_hasInfoFromServer )
		{
			// position deltas are easy
			dx += m_xFromServer - posX;
			dy += m_yFromServer - posY;
			dz += m_zFromServer - posZ;
			
			// we need fancy math to get the correct rotation delta
			double yawRadClient = CircleRange.mapMinusPiToPi( Math.toRadians( rotationYaw ) );
			double yawRadServer = CircleRange.mapMinusPiToPi( Math.toRadians( m_yawFromServer ) );
			double yawDelta = CircleRange.newByShortSegment( yawRadClient, yawRadServer ).getLength();
			
			// was the rotation delta actually positive?
			if( !CompareReal.eq( CircleRange.mapMinusPiToPi( yawRadClient + yawDelta ), yawRadServer ) )
			{
				// nope. it's a negative delta
				yawDelta = -yawDelta;
			}
			yawDelta = Math.toDegrees( yawDelta );
			
			// TEMP
			System.out.println( String.format( "Update from server dYaw=%.1f", yawDelta ) );
			
			dYaw += yawDelta;
			
			// just apply the pitch directly
			rotationPitch = m_pitchFromServer;
			
			m_hasInfoFromServer = false;
		}
		
		// did we even move?
		if( dx == 0.0 && dy == 0.0 && dz == 0.0 && dYaw == 0.0f )
		{
			return;
		}
		
		List<Entity> riders = getRiders();
		
		// apply motion
		setRotation(
			rotationYaw + dYaw,
			rotationPitch
		);
		setPosition(
			posX + dx,
			posY + dy,
			posZ + dz
		);
		
		// UNDONE BUGBUG riders slide off on turns... float precision problem?
		
		// update nearby entities
		moveRiders( riders, dx, dy, dz, dYaw );
		// UNDONE: update this!
		//moveCollidingEntities( dx, dy, dz );
		
		// reduce the velocity for next time
		applyDrag();
	}
	
	public void worldToShip( Vec3 v )
	{
		double x = worldToShipX( v.xCoord, v.zCoord );
		double y = worldToShipY( v.yCoord );
		double z = worldToShipZ( v.xCoord, v.zCoord );
		
		v.xCoord = x;
		v.yCoord = y;
		v.zCoord = z;
	}
	
	public double worldToShipX( double x, double z )
	{
		float yawRad = (float)Math.toRadians( rotationYaw );
		double cos = MathHelper.cos( yawRad );
		double sin = MathHelper.sin( yawRad );
		return ( x - posX )*cos - ( z - posZ )*sin;
	}
	
	public double worldToShipY( double y )
	{
		return y - posY;
	}
	
	public double worldToShipZ( double x, double z )
	{
		float yawRad = (float)Math.toRadians( rotationYaw );
		double cos = MathHelper.cos( yawRad );
		double sin = MathHelper.sin( yawRad );
		return ( x - posX )*sin + ( z - posZ )*cos;
	}
	
	public void shipToWorld( Vec3 v )
	{
		double x = shipToWorldX( v.xCoord, v.zCoord );
		double y = shipToWorldY( v.yCoord );
		double z = shipToWorldZ( v.xCoord, v.zCoord );
		
		v.xCoord = x;
		v.yCoord = y;
		v.zCoord = z;
	}
	
	public double shipToWorldX( double x, double z )
	{
		float yawRad = (float)Math.toRadians( rotationYaw );
		double cos = MathHelper.cos( yawRad );
		double sin = MathHelper.sin( yawRad );
		return x*cos + z*sin + posX;
	}
	
	public double shipToWorldY( double y )
	{
		return y + posY;
	}
	
	public double shipToWorldZ( double x, double z )
	{
		float yawRad = (float)Math.toRadians( rotationYaw );
		double cos = MathHelper.cos( yawRad );
		double sin = MathHelper.sin( yawRad );
		return -x*sin + z*cos + posZ;
	}
	
	public void shipToBlocks( Vec3 v )
	{
		v.xCoord = shipToBlocksX( v.xCoord );
		v.yCoord = shipToBlocksY( v.yCoord );
		v.zCoord = shipToBlocksZ( v.zCoord );
	}
	
	public double shipToBlocksX( double x )
	{
		return x - m_shipBlockX;
	}
	
	public double shipToBlocksY( double y )
	{
		return y - m_shipBlockY;
	}
	
	public double shipToBlocksZ( double z )
	{
		return z - m_shipBlockZ;
	}
	
	public void blockstoShip( Vec3 v )
	{
		v.xCoord = blocksToShipX( v.xCoord );
		v.yCoord = blocksToShipY( v.yCoord );
		v.zCoord = blocksToShipZ( v.zCoord );
	}
	
	public double blocksToShipX( double x )
	{
		return x + m_shipBlockX;
	}
	
	public double blocksToShipY( double y )
	{
		return y + m_shipBlockY;
	}
	
	public double blocksToShipZ( double z )
	{
		return z + m_shipBlockZ;
	}
	
	public RotatedBB worldToBlocks( AxisAlignedBB box )
	{
		// transform the box center into block space
		Vec3 center = Vec3.createVectorHelper(
			( box.minX + box.maxX )/2,
			( box.minY + box.maxY )/2,
			( box.minZ + box.maxZ )/2
		);
		worldToShip( center );
		shipToBlocks( center );
		
		// build a box of the same dimensions here in blocks space
		double dxh = ( box.maxX - box.minX )/2;
		double dyh = ( box.maxY - box.minY )/2;
		double dzh = ( box.maxZ - box.minZ )/2;
		box = AxisAlignedBB.getBoundingBox(
			center.xCoord - dxh, center.yCoord - dyh, center.zCoord - dzh,
			center.xCoord + dxh, center.yCoord + dyh, center.zCoord + dzh
		);
		
		return new RotatedBB( box, -rotationYaw );
	}
	
	private void applyThrust( )
	{
		if( m_pilotActions == 0 )
		{
			// just coast
		}
		else
		{
			// full ahead, captain!!
			PilotAction.applyToShip( this, m_pilotActions, m_sideShipForward );
			
			// need to be careful though about ship speed. Players that move too fast get kicked
			// impose the max speed
			double speed = Math.sqrt( motionX*motionX + motionY*motionY + motionZ*motionZ );
			double maxSpeed = getShipType().getMaxLinearSpeed();
			if( speed > maxSpeed )
			{
				double fixFactor = maxSpeed/speed;
				motionX *= fixFactor;
				motionY *= fixFactor;
				motionZ *= fixFactor;
			}
			
			// apply max rotational speed too
			if( Math.abs( motionYaw ) > getShipType().getMaxRotationalSpeed() )
			{
				motionYaw = Math.signum( motionYaw )*getShipType().getMaxRotationalSpeed();
			}
		}
	}
	
	private void applyDrag( )
	{
		// UNDONE: drag based on mass, submerged surface area??
		final double LinearDrag = 0.01;
		final float RotationalDrag = 0.5f;
		
		// apply the position drag
		double dragLength = Math.sqrt( LinearDrag*LinearDrag*3 );
		double length = Math.sqrt( motionX*motionX + motionY*motionY + motionZ*motionZ );
		if( length > dragLength )
		{
			motionX -= motionX/length*LinearDrag;
			motionY -= motionY/length*LinearDrag;
			motionZ -= motionZ/length*LinearDrag;
		}
		else
		{
			motionX = 0;
			motionY = 0;
			motionZ = 0;
		}
		
		// apply rotational drag
		if( (float)Math.abs( motionYaw ) > RotationalDrag )
		{
			motionYaw -= Math.signum( motionYaw )*RotationalDrag;
		}
		else
		{
			motionYaw = 0;
		}
	}
	
	private void adjustMotionBecauseOfBlockCollisions( )
	{
		// UNDONE: we can probably optimize the piss out of this function
		// especially by getting rid of the intermediate data structures
		
		// UNDONE: check for changes in rotation too...
		
		// where would we move to?
		AxisAlignedBB nextBox = AxisAlignedBB.getBoundingBox( 0, 0, 0, 0, 0, 0 );
		computeBoundingBox( nextBox, posX + motionX, posY + motionY, posZ + motionZ, rotationYaw + motionYaw );
		
		// do a range query to get colliding world blocks
		List<AxisAlignedBB> collidingWorldBlocks = new ArrayList<AxisAlignedBB>();
        int minX = MathHelper.floor_double( nextBox.minX );
        int maxX = MathHelper.floor_double( nextBox.maxX );
        int minY = MathHelper.floor_double( nextBox.minY );
        int maxY = MathHelper.floor_double( nextBox.maxY );
        int minZ = MathHelper.floor_double( nextBox.minZ );
        int maxZ = MathHelper.floor_double( nextBox.maxZ );
        for( int x=minX; x<=maxX; x++ )
        {
            for( int z=minZ; z<maxZ; z++ )
            {
                for( int y=minY; y<=maxY; y++ )
                {
                    Block block = Block.blocksList[worldObj.getBlockId( x, y, z )];
                    if( block != null )
                    {
                        block.addCollisionBoxesToList( worldObj, x, y, z, nextBox, collidingWorldBlocks, this );
                    }
                }
            }
        }
        
		// find out which ship blocks collide with each world block
		// NOTE: the map should hash on instance for AxisAlignedBB
		Map<AxisAlignedBB,List<ChunkCoordinates>> collisions = new HashMap<AxisAlignedBB,List<ChunkCoordinates>>();
		for( AxisAlignedBB worldBlock : collidingWorldBlocks )
		{
			List<ChunkCoordinates> collidingShipBlocks = getCollidingShipBlocks( worldBlock );
			if( !collidingShipBlocks.isEmpty() )
			{
				collisions.put( worldBlock, collidingShipBlocks );
			}
		}
		
		// no collisions?
		if( collisions.isEmpty() )
		{
			return;
		}
		
		// TEMP: tell us what collided
		System.out.println( String.format(
			"%s colliding with %d blocks!",
			worldObj.isRemote ? "CLIENT" : "SERVER",
			collisions.size()
		) );
		
		// find a scaling of the motion vector that prevents the collision
		// hint: it's between 0 and 1
		double s = 1.0;
		for( Map.Entry<AxisAlignedBB,List<ChunkCoordinates>> entry : collisions.entrySet() )
		{
			AxisAlignedBB worldBlock = entry.getKey();
			List<ChunkCoordinates> shipBlocks = entry.getValue();
			for( ChunkCoordinates coords : shipBlocks )
			{
				AxisAlignedBB shipBlock = m_blockEntities.get( coords ).getBoundingBox();
				s = Math.min( s, getScalingToAvoidCollision( motionX, motionY, motionZ, shipBlock, worldBlock ) );
			}
		}
		
		// avoid the collision
		motionX *= s;
		motionY *= s;
		motionZ *= s;
	}
	
	private List<ChunkCoordinates> getCollidingShipBlocks( AxisAlignedBB worldBlock )
	{
		// do a range query in ship space (at the next position)
		List<ChunkCoordinates> collidingShipBlocks = new ArrayList<ChunkCoordinates>();
		int minX = MathHelper.floor_double( worldBlock.minX - ( posX + motionX ) );
        int maxX = MathHelper.floor_double( worldBlock.maxX - ( posX + motionX ) );
        int minY = MathHelper.floor_double( worldBlock.minY - ( posY + motionY ) );
        int maxY = MathHelper.floor_double( worldBlock.maxY - ( posY + motionY ) );
        int minZ = MathHelper.floor_double( worldBlock.minZ - ( posZ + motionZ ) );
        int maxZ = MathHelper.floor_double( worldBlock.maxZ - ( posZ + motionZ ) );
        ChunkCoordinates coords = new ChunkCoordinates( 0, 0, 0 );
        for( coords.posX=minX; coords.posX<=maxX; coords.posX++ )
        {
            for( coords.posZ=minZ; coords.posZ<maxZ; coords.posZ++ )
            {
                for( coords.posY=minY; coords.posY<=maxY; coords.posY++ )
                {
                	if( getBlockEntity( coords ) != null )
                	{
                		collidingShipBlocks.add( new ChunkCoordinates( coords ) );
                	}
                }
            }
        }
        return collidingShipBlocks;
	}
	
	private double getScalingToAvoidCollision( double dx, double dy, double dz, AxisAlignedBB shipBox, AxisAlignedBB externalBox )
	{
		// UNDONE: merge with other scaling func?
		double sx = 0;
		if( dx > 0 )
		{
			sx = ( externalBox.minX - shipBox.maxX )/dx;
		}
		else if( dx < 0 )
		{
			sx = ( externalBox.maxX - shipBox.minX )/dx;
		}
		
		double sy = 0;
		if( dy > 0 )
		{
			sy = ( externalBox.minY - shipBox.maxY )/dy;
		}
		else if( dy < 0 )
		{
			sy = ( externalBox.maxY - shipBox.minY )/dy;
		}
		
		double sz = 0;
		if( dz > 0 )
		{
			sz = ( externalBox.minZ - shipBox.maxZ )/dz;
		}
		else if( dz < 0 )
		{
			sz = ( externalBox.maxZ - shipBox.minZ )/dz;
		}
		
		return Math.max( sx, Math.max( sy, sz ) );
	}
	
	private List<Entity> getRiders( )
	{
		final double Expand = 0.5;
		AxisAlignedBB checkBox = AxisAlignedBB.getAABBPool().getAABB(
			boundingBox.minX - Expand,
			boundingBox.minY - Expand,
			boundingBox.minZ - Expand,
			boundingBox.maxX + Expand,
			boundingBox.maxY + Expand,
			boundingBox.maxZ + Expand
		);
		@SuppressWarnings( "unchecked" )
		List<Entity> entities = worldObj.getEntitiesWithinAABB( Entity.class, checkBox );
		
		// remove any entities from the list not close enough to be considered riding
		Iterator<Entity> iter = entities.iterator();
		while( iter.hasNext() )
		{
			Entity entity = iter.next();
			if( entity == this || !isEntityCloseEnoughToRide( entity ) )
			{
				iter.remove();
			}
		}
		
		return entities;
	}
	
	public boolean isEntityCloseEnoughToRide( Entity entity )
	{
		// get the closest block y the entity could be standing on
		int y = (int)( shipToBlocksY( worldToShipY( entity.boundingBox.minY ) ) + 0.5 ) - 1;
		
		// convert the entity box into block coordinates
		RotatedBB box = worldToBlocks( entity.boundingBox );
		
		for( ChunkCoordinates coords : m_blocks.xzRangeQuery( y, box ) )
		{
			if( isBoxCloseEnoughToRide( box, coords ) )
			{
				return true;
			}
		}
		
		return false;
	}
	
	private boolean isBoxCloseEnoughToRide( RotatedBB box, ChunkCoordinates coords )
	{
		// is the entity close enough to the top of the block?
		double yBlockTop = coords.posY + 1;
		return Math.abs( yBlockTop - box.getMinY() ) <= RiderEpsilon;
	}
	
	private void moveRiders( List<Entity> riders, double dx, double dy, double dz, float dYaw )
	{
		Vec3 p = Vec3.createVectorHelper( 0, 0, 0 );
		
		// move riders
		for( Entity rider : riders )
		{
			// apply rotation of position
			p.xCoord = rider.posX;
			p.yCoord = rider.posY;
			p.zCoord = rider.posZ;
			worldToShip( p );
			float yawRad = (float)Math.toRadians( dYaw );
			float cos = MathHelper.cos( yawRad );
			float sin = MathHelper.sin( yawRad );
			double x = p.xCoord*cos + p.zCoord*sin;
			double z = -p.xCoord*sin + p.zCoord*cos;
			p.xCoord = x;
			p.zCoord = z;
			shipToWorld( p );
			
			// apply translation
			p.xCoord += dx;
			p.yCoord += dy;
			p.zCoord += dz;
			
			rider.setPositionAndRotation(
				p.xCoord, p.yCoord, p.zCoord,
				rider.rotationYaw - dYaw,
				rider.rotationPitch
			);
			
			// snap riders to the surface of the ship
			double riderY = shipToBlocksY( worldToShipY( rider.boundingBox.minY ) );
			int targetY = (int)( riderY + 0.5 );
			double correction = targetY - riderY;
			if( correction != 0.0 && Math.abs( correction ) < RiderEpsilon && rider.motionY <= 0 )
			{
				rider.moveEntity( 0, correction, 0 );
				rider.onGround = true;
			}
		}
	}
	
	private void moveCollidingEntities( double dx, double dy, double dz )
	{
		@SuppressWarnings( "unchecked" )
		List<Entity> entities = worldObj.getEntitiesWithinAABB( Entity.class, boundingBox );
		
		List<ChunkCoordinates> collidingBlocks = new ArrayList<ChunkCoordinates>();
		
		for( Entity entity : entities )
		{
			collidingBlocks.clear();
			
			// don't collide with self
			if( entity == this )
			{
				continue;
			}
			
			// do a range query to get all the blocks that are colliding with the entity
			AxisAlignedBB entityBox = entity.boundingBox;
			for( int x=MathHelper.floor_double( entityBox.minX - posX ); x<=MathHelper.floor_double( entityBox.maxX - posX ); x++ )
			{
				for( int y=MathHelper.floor_double( entityBox.minY - posY ); y<=MathHelper.floor_double( entityBox.maxY - posY ); y++ )
				{
					for( int z=MathHelper.floor_double( entityBox.minZ - posZ ); z<=MathHelper.floor_double( entityBox.maxZ - posZ ); z++ )
					{
						if( m_blocks.getBlockId( x, y, z ) != 0 )
						{
							collidingBlocks.add( new ChunkCoordinates( x, y, z ) );
						}
					}
				}
			}
			
			if( collidingBlocks.isEmpty() )
			{
				continue;
			}
			
			// TEMP
			System.out.println( String.format(
				"%s entity %s collides with %d blocks",
				worldObj.isRemote ? "CLIENT" : "SERVER",
				entity.getClass().getSimpleName(), collidingBlocks.size()
			) );
			
			// find the scaling of dx,dy,dz that moves the entity out of the way of the blocks
			double maxScaling = 0.0;
			for( ChunkCoordinates coords : collidingBlocks )
			{
				EntityShipBlock blockEntity = m_blockEntities.get( coords );
				if( blockEntity == null )
				{
					continue;
				}
				
				// is the ship block actually moving towards the entity?
				Vec3 toEntity = Vec3.createVectorHelper(
					entity.posX - blockEntity.posX,
					entity.posY - blockEntity.posY,
					entity.posZ - blockEntity.posZ
				);
				Vec3 motion = Vec3.createVectorHelper( dx, dy, dz );
				boolean isMovingTowardsEntity = toEntity.dotProduct( motion ) > 0.0;
				if( !isMovingTowardsEntity )
				{
					// TEMP
					System.out.println( String.format(
						"%s block moving away",
						worldObj.isRemote ? "CLIENT" : "SERVER"
					) );
					
					continue;
				}
				
				AxisAlignedBB blockBox = blockEntity.getBoundingBox();
				double scaling = getScalingToPushBox( dx, dy, dz, blockBox, entityBox );
				
				// TEMP
				System.out.println( String.format(
					"%s entity %s scaling for block is %.4f",
					worldObj.isRemote ? "CLIENT" : "SERVER",
					entity.getClass().getSimpleName(), scaling
				) );
				
				maxScaling = Math.max( maxScaling, scaling );
			}
			
			// TEMP
			System.out.println( String.format(
				"%s moving entity %s: %.4f (dist=%.2f)",
				worldObj.isRemote ? "CLIENT" : "SERVER",
				entity.getClass().getSimpleName(), maxScaling,
				maxScaling*Math.sqrt( dx*dx + dy*dy + dz*dz )
			) );
			
			// move the entity out of the way of the blocks
			entity.setPosition(
				entity.posX + dx*maxScaling,
				entity.posY + dy*maxScaling,
				entity.posZ + dz*maxScaling
			);
		}
	}
	
	private double getScalingToPushBox( double dx, double dy, double dz, AxisAlignedBB shipBox, AxisAlignedBB externalBox )
	{
		// UNDONE: merge with other scaling func?
		double sx = 0;
		if( dx > 0 )
		{
			sx = ( shipBox.maxX - externalBox.minX )/dx;
		}
		else if( dx < 0 )
		{
			sx = ( shipBox.minX - externalBox.maxX )/dx;
		}
		
		double sy = 0;
		if( dy > 0 )
		{
			sy = ( shipBox.maxY - externalBox.minY )/dy;
		}
		else if( dy < 0 )
		{
			sy = ( shipBox.minY - externalBox.maxY )/dy;
		}
		
		double sz = 0;
		if( dz > 0 )
		{
			sz = ( shipBox.maxZ - externalBox.minZ )/dz;
		}
		else if( dz < 0 )
		{
			sz = ( shipBox.minZ - externalBox.maxZ )/dz;
		}
		
		return Math.max( sx, Math.max( sy, sz ) );
	}
	
	private void computeBoundingBox( AxisAlignedBB box, double x, double y, double z, float yaw )
	{
		if( m_blocks == null )
		{
			return;
		}
		
		// make an un-rotated box in world-space
		ChunkCoordinates min = m_blocks.getMin();
		box.minX = x + blocksToShipX( min.posX );
		box.minY = y + blocksToShipY( min.posY );
		box.minZ = z + blocksToShipZ( min.posZ );
		ChunkCoordinates max = m_blocks.getMax();
		box.maxX = x + blocksToShipX( max.posX + 1 );
		box.maxY = y + blocksToShipY( max.posY + 1 );
		box.maxZ = z + blocksToShipZ( max.posZ + 1 );
		
		// now rotate by the yaw
		// UNDONE: optimize out the new
		RotatedBB rotatedBox = new RotatedBB( box.copy(), yaw, posX, posZ );
		
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
	
	public void setPilotActions( int actions, BlockSide sideShipForward )
	{
		m_pilotActions = actions;
		m_sideShipForward = sideShipForward;
	}
}
