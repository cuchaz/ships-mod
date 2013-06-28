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
import net.minecraft.world.World;

public class EntityShip extends Entity
{
	// data watcher IDs. Entity uses [0,1]. We can use [2,31]
	private static final int WatcherIdBlocks = 2;
	private static final int WatcherIdShipType = 3;
	private static final int WatcherIdWaterHeight = 4;
	
	private ShipWorld m_blocks;
	private TreeMap<ChunkCoordinates,EntityShipBlock> m_blockEntities;
	private EntityShipBlock[] m_blockEntitiesArray;
	private ShipPhysics m_physics;
	
	private transient AxisAlignedBB m_nextBox;
	
	public EntityShip( World world )
	{
		super( world );
		yOffset = 0.0f;
		motionX = 0.0;
		motionY = 0.0;
		motionZ = 0.0;
		
		m_blocks = null;
		m_blockEntities = null;
		m_blockEntitiesArray = null;
		m_physics = null;
		
		m_nextBox = AxisAlignedBB.getBoundingBox( 0, 0, 0, 0, 0, 0 );
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
		
		/*
		if( !worldObj.isRemote )
		{
			// TEMP: set the position
			posX = 138;
			posY = 63.12;
			posZ = 274;
		}
		*/
		
		m_blocks = blocks;
		blocks.setShip( this );
		m_physics = new ShipPhysics( m_blocks );
		
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
		
		computeBoundingBox( boundingBox, posX, posY, posZ );
		
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
		computeBoundingBox( boundingBox, posX, posY, posZ );
		
		if( m_blockEntitiesArray != null )
		{
			// update blocks
			for( EntityShipBlock block : m_blockEntitiesArray )
			{
				block.setPosition(
					x + block.coords.posX,
					y + block.coords.posY,
					z + block.coords.posZ
				);
			}
		}
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
		
		// dampen the velocity
		final double DampeningFactor = 0.9;
		motionX *= DampeningFactor;
		motionY *= DampeningFactor;
		motionZ *= DampeningFactor;
		
		// UNDONE: when the up force is small, maybe just put the ship at the equilibrium position?
		
		// get rid of infinitesimal motion
		final double Threshold = 0.01;
		if( Math.abs( motionX ) < Threshold )
		{
			motionX = 0;
		}
		if( Math.abs( motionY ) < Threshold )
		{
			motionY = 0;
		}
		if( Math.abs( motionZ ) < Threshold )
		{
			motionZ = 0;
		}
		
		// did we even move?
		if( motionX == 0.0 && motionY == 0.0 && motionZ == 0.0 )
		{
			return;
		}
		
		// where would we move to?
		computeBoundingBox( m_nextBox, posX + motionX, posY + motionY, posZ + motionZ );
		
		adjustMotionBecauseOfBlockCollisions();
		
		List<Entity> riders = getRiders();
		
		// apply motion
		setPosition(
			posX + motionX,
			posY + motionY,
			posZ + motionZ
		);
		
		// move riders
		for( Entity rider : riders )
		{
			rider.setPosition(
				rider.posX + motionX,
				rider.posY + motionY,
				rider.posZ + motionZ
			);
			
			// snap riders to the surface if they're really close
			// UNDONE: maybe after proper collisions, we can get rid of this
			double riderY = rider.posY - rider.yOffset - posY;
			int targetY = (int)( riderY + 0.5 );
			if( Math.abs( riderY - targetY ) < 0.1 && rider.motionY <= 0 )
			{
				rider.setPosition( rider.posX, targetY + posY + rider.yOffset, rider.posZ );
			}
		}
		
		// UNDONE: move entities we collided with
	}
	
	private void adjustMotionBecauseOfBlockCollisions( )
	{
		// UNDONE: we can probably optimize the piss out of this function
		// especially by getting rid of the intermediate data structures
		
		// do a range query to get colliding world blocks
		List<AxisAlignedBB> collidingWorldBlocks = new ArrayList<AxisAlignedBB>();
        int minX = MathHelper.floor_double( m_nextBox.minX );
        int maxX = MathHelper.floor_double( m_nextBox.maxX );
        int minY = MathHelper.floor_double( m_nextBox.minY );
        int maxY = MathHelper.floor_double( m_nextBox.maxY );
        int minZ = MathHelper.floor_double( m_nextBox.minZ );
        int maxZ = MathHelper.floor_double( m_nextBox.maxZ );
        for( int x=minX; x<=maxX; x++ )
        {
            for( int z=minZ; z<maxZ; z++ )
            {
                for( int y=minY; y<=maxY; y++ )
                {
                    Block block = Block.blocksList[worldObj.getBlockId( x, y, z )];
                    if( block != null )
                    {
                        block.addCollisionBoxesToList( worldObj, x, y, z, m_nextBox, collidingWorldBlocks, this );
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
				s = Math.min( s, getScalingToAvoidCollision( shipBlock, worldBlock ) );
			}
		}
		
		// TEMP: tell us what collided
		System.out.println( String.format(
			"%s avoiding collision: %.4f",
			worldObj.isRemote ? "CLIENT" : "SERVER",
			s
		) );
		
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
	
	private double getScalingToAvoidCollision( AxisAlignedBB shipBlock, AxisAlignedBB worldBlock )
	{
		double sx = 0;
		if( motionX > 0 )
		{
			sx = ( worldBlock.minX - shipBlock.maxX )/motionX;
		}
		else if( motionX < 0 )
		{
			sx = ( worldBlock.maxX - shipBlock.minX )/motionX;
		}
		
		double sy = 0;
		if( motionY > 0 )
		{
			sy = ( worldBlock.minY - shipBlock.maxY )/motionY;
		}
		else if( motionY < 0 )
		{
			sy = ( worldBlock.maxY - shipBlock.minY )/motionY;
		}
		
		double sz = 0;
		if( motionZ > 0 )
		{
			sz = ( worldBlock.minZ - shipBlock.maxZ )/motionZ;
		}
		else if( motionZ < 0 )
		{
			sz = ( worldBlock.maxZ - shipBlock.minZ )/motionZ;
		}
		
		// TEMP
		System.out.println( String.format(
			"%s computing scaling: d=(%.4f,%.4f,%.4f) s=(%.4f,%.4f,%.4f)",
			worldObj.isRemote ? "CLIENT" : "SERVER",
			motionX, motionY, motionZ,
			sx, sy, sz
		) );
		
		return Math.max( sx, Math.max( sy, sz ) );
	}
	
	private List<Entity> getRiders( )
	{
		final double Expand = 0.1;
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
		int y = (int)( entity.posY - entity.yOffset - posY + 0.5 ) - 1;
		
		// now, do a range query to get all the blocks that are at the entity's bottom bounding box face
		AxisAlignedBB box = entity.boundingBox;
		for( int x=MathHelper.floor_double( box.minX - posX ); x<=MathHelper.floor_double( box.maxX - posX ); x++ )
		{
			for( int z=MathHelper.floor_double( box.minZ - posZ ); z<=MathHelper.floor_double( box.maxZ - posZ ); z++ )
			{
				if( isEntityCloseEnoughToRide( entity, x, y, z ) )
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	private boolean isEntityCloseEnoughToRide( Entity entity, int x, int y, int z )
	{
		// is there a block here?
		if( m_blocks.getBlockId( x, y, z ) == 0 )
		{
			return false;
		}
		
		// is the entity close enough to the top of the block?
		double yBlockTop = y + 1;
		double yEntityBottom = entity.boundingBox.minY - posY;
		
		final double Epsilon = 0.2;
		return Math.abs( yBlockTop - yEntityBottom ) <= Epsilon;
	}
	
	private void computeBoundingBox( AxisAlignedBB box, double x, double y, double z )
	{
		if( m_blocks == null )
		{
			return;
		}
		
		ChunkCoordinates min = m_blocks.getMin();
		box.minX = x + (float)min.posX;
		box.minY = y + (float)min.posY;
		box.minZ = z + (float)min.posZ;
		ChunkCoordinates max = m_blocks.getMax();
		box.maxX = x + (float)max.posX + 1;
		box.maxY = y + (float)max.posY + 1;
		box.maxZ = z + (float)max.posZ + 1;
	}
	
	public void moveByPilot( int dx, int dy, int dz )
	{
		// UNDONE: acceleration should come from thrusters
		// also, do mass calculation
		double acceleration = 0.1;
		
		// apply acceleration
		motionX += dx*acceleration;
		motionY += dy*acceleration;
		motionZ += dz*acceleration;
		
		// impose the max speed
		double speed = Math.sqrt( motionX*motionX + motionY*motionY + motionZ*motionZ );
		double maxSpeed = getShipType().getMaxSpeed();
		if( speed > maxSpeed )
		{
			double fixFactor = maxSpeed/speed;
			motionX *= fixFactor;
			motionY *= fixFactor;
			motionZ *= fixFactor;
		}
		
		// UNDONE: motion too fast drops the player off the raft!!
	}
}
