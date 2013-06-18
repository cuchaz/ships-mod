package cuchaz.ships;

import java.util.TreeMap;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

public class EntityShip extends Entity
{
	private ShipBlocks m_blocks;
	private TreeMap<ChunkCoordinates,EntityShipBlock> m_blockEntities;
	private EntityShipBlock[] m_blockEntitiesArray;
	
	public EntityShip( World world, ShipBlocks blocks )
	{
		super( world );
		
		m_blocks = blocks;
		
		// build the sub entities
		m_blockEntities = new TreeMap<ChunkCoordinates, EntityShipBlock>();
		for( ChunkCoordinates block : m_blocks.blocks() )
		{
			EntityShipBlock entityBlock = new EntityShipBlock( world, this, block );
			m_blockEntities.put( block, entityBlock );
		}
		
		// flatten to an array
		m_blockEntitiesArray = new EntityShipBlock[m_blockEntities.size()];
		m_blockEntities.values().toArray( m_blockEntitiesArray );
		
		// init defaults
		motionX = 0.0;
		motionY = 0.0;
		motionZ = 0.0;
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		yOffset = 0.0f;
		computeBoundingBox();
	}
	
	public ShipBlocks getBlocks( )
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
		// TODO Auto-generated method stub
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
	protected void readEntityFromNBT( NBTTagCompound nbttagcompound )
	{
		// TODO Auto-generated method stub
	}

	@Override
	protected void writeEntityToNBT( NBTTagCompound nbttagcompound )
	{
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onUpdate( )
	{
		super.onUpdate();
		
		/* make the ship fall down
		this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.motionY -= 0.01;
        //this.noClip = this.pushOutOfBlocks(this.posX, (this.boundingBox.minY + this.boundingBox.maxY) / 2.0D, this.posZ);
        this.moveEntity(this.motionX, this.motionY, this.motionZ);
        boolean flag = (int)this.prevPosX != (int)this.posX || (int)this.prevPosY != (int)this.posY || (int)this.prevPosZ != (int)this.posZ;
		*/
		
		// here's how to despawn
		//setDead();
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
