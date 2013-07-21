package cuchaz.ships;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
			posX = 136;
			posY = 63.4;
			posZ = 274;
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
		updateBlockEntityPositions();
		
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
		updateBlockEntityPositions();
	}
	
	private void updateBlockEntityPositions( )
	{
		if( m_blockEntitiesArray == null )
		{
			return;
		}
		
		Vec3 p = Vec3.createVectorHelper( 0, 0, 0 );
		
		for( EntityShipBlock block : m_blockEntitiesArray )
		{
			// compute the block position
			block.getBlockPosition( p );
			blocksToShip( p );
			shipToWorld( p );
			
			block.prevPosX = block.posX;
			block.prevPosY = block.posY;
			block.prevPosZ = block.posZ;
			block.prevRotationYaw = block.rotationYaw;
			block.prevRotationPitch = block.rotationPitch;
			
			block.setPositionAndRotation(
				p.xCoord,
				p.yCoord,
				p.zCoord,
				rotationYaw,
				rotationPitch
			);
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
		}
		
		// don't do any updating until we get blocks
		if( m_blocks == null )
		{
			return;
		}
		
		// TEMP
		if( !worldObj.isRemote )
		{
			System.out.println( String.format( "%s START ship pos: %.1f, ship motion: %8f",
				worldObj.isRemote ? "CLIENT" : "SERVER",
				posY,
				motionY
			) );
		}
		
		/* TEMP
		
		double waterHeight = shipToBlocksY( worldToShipY( getWaterHeight() ) );
		motionY += m_physics.getNetUpForce( waterHeight );
		
		adjustMotionDueToThrust();
		adjustMotionDueToBlockCollisions();
		
		// TEMP: oscillate so we can test entity collision
		if( false )
		{
			motionZ += ( 270 - posZ )/50.0;
		}
		
		// simulate the weight/buoyancy forces
		// TEMP
		if( !worldObj.isRemote )
		{
			System.out.println( String.format( "%s ship pos: %.1f, ship motion: %8f, water in world: %d, water in ship: %.1f, water in blocks: %.1f",
				worldObj.isRemote ? "CLIENT" : "SERVER",
				posY,
				motionY,
				getWaterHeight(),
				worldToShipY( getWaterHeight() ),
				shipToBlocksY( worldToShipY( getWaterHeight() ) )
			) );
		}
		
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
			
			dYaw += yawDelta;
			
			// just apply the pitch directly
			rotationPitch = m_pitchFromServer;
			
			m_hasInfoFromServer = false;
		}
		
		// did we even move a noticeable amount?
		final double Epsilon = 1e-4;
		if( Math.abs( dx ) >= Epsilon || Math.abs( dy ) >= Epsilon || Math.abs( dz ) >= Epsilon || Math.abs( dYaw ) >= Epsilon )
		{
			List<Entity> riders = getRiders();
			
			// apply motion
			prevPosX = posX;
			prevPosY = posY;
			prevPosZ = posZ;
			prevRotationYaw = rotationYaw;
			prevRotationPitch = rotationPitch;
			setRotation(
				rotationYaw + dYaw,
				rotationPitch
			);
			setPosition(
				posX + dx,
				posY + dy,
				posZ + dz
			);
			
			moveRiders( riders, dx, dy, dz, dYaw );
			//moveCollidingEntities( riders );
		}
		
		// did the ship sink?
		if( isSunk( waterHeight ) )
		{
			// TEMP
			System.out.println( String.format( "%s Ship Sunk!",
				worldObj.isRemote ? "CLIENT" : "SERVER"
			) );
			
			// unlaunch the ship at the bottom of the ocean
			ShipUnlauncher unlauncher = new ShipUnlauncher( this );
			unlauncher.snapToNearestDirection();
			unlauncher.unlaunch();
		}
		
		// reduce the velocity for next time
		adjustMotionDueToDrag();
		
		*/
		
		// TEMP
		if( !worldObj.isRemote )
		{
			System.out.println( String.format( "%s STOP  ship pos: %.1f, ship motion: %8f",
				worldObj.isRemote ? "CLIENT" : "SERVER",
				posY,
				motionY
			) );
		}
	}
	
	private boolean isSunk( double waterHeight )
	{
		// is the ship completely underwater?
		boolean isFalling = motionY < 0;
		boolean isUnderwater = waterHeight > m_blocks.getMax().posY + 1;
		
		// TEMP
		if( !worldObj.isRemote )
		{
			System.out.println( String.format( "%s isSunk() motionY = %.2f, onGround = %b, isUnderwater = %b",
				worldObj.isRemote ? "CLIENT" : "SERVER",
				motionY,
				onGround,
				isUnderwater
			) );
		}
		
		return isFalling && this.onGround && isUnderwater;
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
	
	public void blocksToShip( Vec3 v )
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
	
	private void adjustMotionDueToThrust( )
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
	
	private void adjustMotionDueToDrag( )
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
	
	private void adjustMotionDueToBlockCollisions( )
	{
		// UNDONE: we can probably optimize this quite a bit
		
		// where would we move to?
		AxisAlignedBB nextBox = AxisAlignedBB.getBoundingBox( 0, 0, 0, 0, 0, 0 );
		computeBoundingBox( nextBox, posX + motionX, posY + motionY, posZ + motionZ, rotationYaw + motionYaw );
		
		// do a range query to get colliding world blocks
		List<AxisAlignedBB> nearbyWorldBlocks = new ArrayList<AxisAlignedBB>();
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
                        block.addCollisionBoxesToList( worldObj, x, y, z, nextBox, nearbyWorldBlocks, this );
                    }
                }
            }
        }
        
        // no collisions?
 		if( nearbyWorldBlocks.isEmpty() )
 		{
 			return;
 		}
 		
 		// compute the scaling to avoid the collision
 		// it's ok if the world block doesn't actually collide with every nearby world block
 		// in that case, the scaling will be > 1 and will be ignored
 		double s = 1.0;
 		Vec3 p = Vec3.createVectorHelper( 0, 0, 0 );
 		AxisAlignedBB updatedShipBlock = AxisAlignedBB.getBoundingBox( 0, 0, 0, 0, 0, 0 );
 		int numCollisions = 0;
		for( EntityShipBlock blockEntity : m_blockEntitiesArray )
        {
			// get the next position of the ship block
			blockEntity.getBlockPosition( p );
			// HACKHACK: temporarily adjust the ship's yaw to fool shipToWorld()
			rotationYaw += motionYaw;
			blocksToShip( p );
			shipToWorld( p );
			p.xCoord += motionX;
			p.yCoord += motionY;
			p.zCoord += motionZ;
			EntityShipBlock.computeBoundingBox( updatedShipBlock, p.xCoord, p.yCoord, p.zCoord, rotationYaw );
			rotationYaw -= motionYaw;
			
			for( AxisAlignedBB worldBlock : nearbyWorldBlocks )
			{
				// would the boxes actually collide?
				if( !worldBlock.intersectsWith( updatedShipBlock ) )
				{
					continue;
				}
				
				numCollisions++;
				s = Math.min( s, getScalingToAvoidCollision( motionX, motionY, motionZ, blockEntity.getBoundingBox(), worldBlock ) );
			}
        }
		
		// avoid the collision
		motionX *= s;
		motionY *= s;
		motionZ *= s;
		
		// update rotation too
		if( numCollisions > 0 )
		{
			motionYaw = 0;
		}
	}
	
	private double getScalingToAvoidCollision( double dx, double dy, double dz, AxisAlignedBB shipBox, AxisAlignedBB externalBox )
	{
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
		
		double s = Math.max( sx, Math.max( sy, sz ) );
		
		// if the scaling we get is below zero, there was no real collision to worry about
		if( s < 0 )
		{
			return 1.0;
		}
		return s;
	}
	
	public List<Entity> getRiders( )
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
		// also remove entities that are floating or moving upwards (e.g. jumping)
		Iterator<Entity> iter = entities.iterator();
		while( iter.hasNext() )
		{
			Entity entity = iter.next();
			if( entity == this || entity.motionY >= 0 || !isEntityCloseEnoughToRide( entity ) )
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
			// apply rotation of position relative to the ship center
			p.xCoord = rider.posX + dx;
			p.yCoord = rider.posY + dy;
			p.zCoord = rider.posZ + dz;
			worldToShip( p );
			float yawRad = (float)Math.toRadians( dYaw );
			float cos = MathHelper.cos( yawRad );
			float sin = MathHelper.sin( yawRad );
			double x = p.xCoord*cos + p.zCoord*sin;
			double z = -p.xCoord*sin + p.zCoord*cos;
			p.xCoord = x;
			p.zCoord = z;
			shipToWorld( p );
			dx += ( p.xCoord - dx ) - rider.posX;
			dz += ( p.zCoord - dz ) - rider.posZ;
			
			// adjust the translation to snap riders to the surface of the ship
			double riderY = shipToBlocksY( worldToShipY( rider.boundingBox.minY ) );
			int targetY = (int)( riderY + 0.5 );
			dy += targetY - riderY;
			
			// apply the transformation
			rider.rotationYaw -= dYaw;
			rider.moveEntity( dx, dy, dz );
			rider.onGround = true;
		}
	}
	
	private void moveCollidingEntities( List<Entity> riders )
	{
		@SuppressWarnings( "unchecked" )
		List<Entity> entities = worldObj.getEntitiesWithinAABB( Entity.class, boundingBox );
		
		for( Entity entity : entities )
		{
			// don't collide with self
			if( entity == this )
			{
				continue;
			}
			
			// is this entity is a rider?
			// UNDONE: could use more efficient check
			boolean isRider = riders.contains( entity );
			
			moveCollidingEntity( entity, isRider );
		}
	}
	
	private void moveCollidingEntity( Entity entity, boolean isRider )
	{
		// UNDONE: collisions isn't quite perfect yet.
		// The displacements aren't quite the right size for the observed z-overlap!
		
		// find the displacement that moves the entity out of the way of the blocks
		double maxDisplacement = 0.0;
		double maxDx = 0;
		double maxDy = 0;
		double maxDz = 0;
		
		// UNDONE: optimize this range query. Do something smarter than brute force
		for( EntityShipBlock blockEntity : m_blockEntitiesArray )
		{
			// is this block actually colliding with the entity?
			if( !blockEntity.getBoundingBox().intersectsWith( entity.boundingBox ) )
			{
				continue;
			}
			
			// get the actual motion vector of the block accounting for rotation
			double dxBlock = blockEntity.posX - blockEntity.prevPosX;
			double dyBlock = blockEntity.posY - blockEntity.prevPosY;
			double dzBlock = blockEntity.posZ - blockEntity.prevPosZ;
			
			// is the ship block actually moving towards the entity?
			Vec3 toEntity = Vec3.createVectorHelper(
				entity.posX - blockEntity.posX,
				entity.posY - blockEntity.posY,
				entity.posZ - blockEntity.posZ
			);
			Vec3 motion = Vec3.createVectorHelper( dxBlock, dyBlock, dzBlock );
			boolean isMovingTowardsEntity = toEntity.dotProduct( motion ) > 0.0;
			if( !isMovingTowardsEntity )
			{
				continue;
			}
			
			double scaling = getScalingToPushBox( dxBlock, dyBlock, dzBlock, blockEntity.getBoundingBox(), entity.boundingBox );
			
			// calculate the displacement
			double displacement = scaling*Math.sqrt( dxBlock*dxBlock + dyBlock*dyBlock + dzBlock*dzBlock );
			
			if( displacement > maxDisplacement )
			{
				maxDisplacement = displacement;
				maxDx = scaling*dxBlock;
				maxDy = scaling*dyBlock;
				maxDz = scaling*dzBlock;
			}
			
			// TEMP
			System.out.println( String.format(
				"%s entity %s displcement for block: %.4f (%.2f,%.2f,%.2f), z overlap: %.4f",
				worldObj.isRemote ? "CLIENT" : "SERVER",
				entity.getClass().getSimpleName(), displacement,
				scaling*dxBlock, scaling*dyBlock, scaling*dzBlock,
				Math.min( blockEntity.getBoundingBox().maxZ - entity.boundingBox.minZ, entity.boundingBox.maxZ - blockEntity.getBoundingBox().minZ )
			) );
		}
		
		// don't update y-positions for riders
		if( isRider )
		{
			maxDy = 0;
		}
		
		// move the entity out of the way of the blocks
		entity.setPosition(
			entity.posX + maxDx,
			entity.posY + maxDy,
			entity.posZ + maxDz
		);
		// UNDONE: change to moveEntity() and get rid of rider snap?
	}
	
	private double getScalingToPushBox( double dx, double dy, double dz, AxisAlignedBB shipBox, AxisAlignedBB externalBox )
	{
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
		
		// clamp scalings to <= 1
		return Math.min( 1, Math.max( sx, Math.max( sy, sz ) ) );
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
	
	public void setPilotActions( int actions, BlockSide sideShipForward )
	{
		m_pilotActions = actions;
		m_sideShipForward = sideShipForward;
	}
}
