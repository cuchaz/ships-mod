package cuchaz.ships;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

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
		
		// TEMP: set the position
		posX = 138;
		
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
		
		computeBoundingBox();
		
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
		computeBoundingBox();
		
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
		super.onUpdate();
		
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
		
		List<Entity> riders = getRiders();
		
		// simulate the vertical forces
		double upForce = m_physics.getNetUpForce( getWaterHeight() - posY );
		motionY += upForce;
		
		// dampen the velocity
		final double DampeningFactor = 0.9;
		//motionX *= DampeningFactor;
		motionY *= DampeningFactor;
		//motionZ *= DampeningFactor;
		
		// TEMP: oscillate in the X direction to test riding
		motionX += ( 140 - posX )/50.0;
		
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
		
		// UNDONE: when the up force is small, maybe just put the ship at the equilibrium position
		
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
			double riderY = rider.posY - rider.yOffset - posY;
			int targetY = (int)( riderY + 0.5 );
			if( Math.abs( riderY - targetY ) < 0.1 && rider.motionY <= 0 )
			{
				rider.setPosition( rider.posX, targetY + posY + rider.yOffset, rider.posZ );
			}
		}
		
		// UNDONE: check for collisions
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
	
	private boolean isEntityCloseEnoughToRide( Entity entity )
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

	private void computeBoundingBox( )
	{
		if( m_blocks == null )
		{
			return;
		}
		
		ChunkCoordinates min = m_blocks.getMin();
		boundingBox.minX = posX + (float)min.posX;
		boundingBox.minY = posY + (float)min.posY;
		boundingBox.minZ = posZ + (float)min.posZ;
		ChunkCoordinates max = m_blocks.getMax();
		boundingBox.maxX = posX + (float)max.posX + 1;
		boundingBox.maxY = posY + (float)max.posY + 1;
		boundingBox.maxZ = posZ + (float)max.posZ + 1;
	}
}
