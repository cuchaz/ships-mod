package cuchaz.ships;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class EntityShip extends Entity
{
	private ShipBlocks m_blocks;
	
	public EntityShip( World world, ShipBlocks blocks )
	{
		super( world );
		
		m_blocks = blocks;
		
		// init defaults
		motionX = 0.0;
        motionY = 0.0;
        motionZ = 0.0;
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        yOffset = 0.0f;
        setSize( 1.0f, 1.0f );
	}
	
	public ShipBlocks getBlocks( )
	{
		return m_blocks;
	}
	
	@Override
	protected void entityInit( )
	{
		// TODO Auto-generated method stub
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
	public void onEntityUpdate( )
	{
		// UNDONE: implement me!
	}
}
