package cuchaz.ships;

import java.util.TreeMap;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

public class EntityShip extends Entity
{
	// data watcher IDs. Entity uses [0,1]. We can use [2,31]
	private static final int WatcherIdBlocks = 2;
	
	private ShipWorld m_blocks;
	private TreeMap<ChunkCoordinates,EntityShipBlock> m_blockEntities;
	private EntityShipBlock[] m_blockEntitiesArray;
	
	public EntityShip( World world )
	{
		super( world );
		yOffset = 0.0f;
		
		m_blocks = null;
		m_blockEntities = null;
		m_blockEntitiesArray = null;
	}
	
	public void setBlocks( ShipWorld blocks )
	{
		m_blocks = blocks;
		blocks.setShip( this );
		
		// save the block data into the data watcher so it gets sync'd to the client
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
		System.out.println( ( worldObj.isRemote ? "CLIENT" : "SERVER" ) + " EntityShip initialized!" );
		System.out.println( "\tShip spawned at (" + posX + "," + posY + "," + posZ + ") " + ( isDead ? "Dead" : "Alive" ) + " " + ( addedToChunk ? "Added" : "Detatched" ) );
		System.out.println( String.format(
			"\tblock bounding box [%.2f,%.2f] [%.2f,%.2f] [%.2f,%.2f]",
			m_blockEntitiesArray[0].boundingBox.minX, m_blockEntitiesArray[0].boundingBox.maxX,
			m_blockEntitiesArray[0].boundingBox.minY, m_blockEntitiesArray[0].boundingBox.maxY,
			m_blockEntitiesArray[0].boundingBox.minZ, m_blockEntitiesArray[0].boundingBox.maxZ
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
		setBlocks( new ShipWorld( worldObj, nbt.getByteArray( "blocks" ) ) );
	}
	
	@Override
	protected void writeEntityToNBT( NBTTagCompound nbt )
	{
		// only need to save ship blocks
		nbt.setByteArray( "blocks", m_blocks.getData() );
		
		// TEMP
		System.out.println( "Wrote NBT!" );
	}
	
	public ShipWorld getBlocks( )
	{
		return m_blocks;
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
	protected void entityInit( )
	{
		// this gets called inside super.Entity( World )
		// it seems to be used to init the data watcher
		
		// allocate a slot for the block data
		dataWatcher.addObject( WatcherIdBlocks, "" );
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
		
		// TEMP
		if( worldObj.isRemote )
		{
			if( m_blocks == null )
			{
				// do we have blocks from the data watcher?
				String blockData = dataWatcher.getWatchableObjectString( WatcherIdBlocks );
				if( blockData != null && blockData.length() > 0 )
				{
					setBlocks( new ShipWorld( worldObj, blockData ) );
				}
			}
			
			// UNDONE: find a way to tell if blocks were changed
		}
		
		/* make the ship fall down
		this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.motionY -= 0.01;
        //this.noClip = this.pushOutOfBlocks(this.posX, (this.boundingBox.minY + this.boundingBox.maxY) / 2.0D, this.posZ);
        this.moveEntity(this.motionX, this.motionY, this.motionZ);
        boolean flag = (int)this.prevPosX != (int)this.posX || (int)this.prevPosY != (int)this.posY || (int)this.prevPosZ != (int)this.posZ;
		*/
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
