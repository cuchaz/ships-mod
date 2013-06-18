package cuchaz.ships;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

public class EntityShipBlock extends Entity
{
	private EntityShip m_ship;
	public ChunkCoordinates coords;
	
	public EntityShipBlock( World world, EntityShip ship, ChunkCoordinates coords )
	{
		super( world );
		
		m_ship = ship;
		this.coords = coords;
		
		// init defaults
		motionX = 0.0;
		motionY = 0.0;
		motionZ = 0.0;
		posX = coords.posX + m_ship.posX;
		posY = coords.posY + m_ship.posY;
		posZ = coords.posZ + m_ship.posZ;
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		yOffset = 0.0f;
		setSize( 1.0f, 1.0f );
	}
	
	@Override
	protected void entityInit( )
	{
	}
	
	@Override
	protected void readEntityFromNBT( NBTTagCompound nbttagcompound )
	{
	}
	
	@Override
	protected void writeEntityToNBT( NBTTagCompound nbttagcompound )
	{
	}
	
	@Override
	public boolean canBeCollidedWith()
    {
        return true;
    }
	
	@Override
	public void setPosition( double x, double y, double z )
	{
		posX = x;
        posY = y;
        posZ = z;
		boundingBox.setBounds( x, y, z, x+1, y+1, z+1 );
	}
		
	@Override
	public AxisAlignedBB getBoundingBox( )
	{
		return boundingBox;
	}
	
	@Override
	public AxisAlignedBB getCollisionBox( Entity other )
	{
		return boundingBox;
	}
}
