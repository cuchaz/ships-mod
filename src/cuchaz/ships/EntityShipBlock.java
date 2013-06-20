package cuchaz.ships;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
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
		setSize( 1.0f, 1.0f );
		setPosition(
			coords.posX + m_ship.posX,
			coords.posY + m_ship.posY,
			coords.posZ + m_ship.posZ
		);
		yOffset = 0.0f;
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
	
	@Override
	public boolean interact( EntityPlayer player )
	{
		// activate the block
		MovingObjectPosition pos = getPlayerMovingObjectPosition( player );
		if( pos == null )
		{
			// this shouldn't happen...
			System.err.println( "EntityShipBlock.interact(): No hit!" );
			
			return false;
		}
		
		return getBlock().onBlockActivated(
			m_ship.getBlocks(),
			coords.posX, coords.posY, coords.posZ,
			player,
			pos.sideHit,
			(float)pos.hitVec.xCoord, (float)pos.hitVec.yCoord, (float)pos.hitVec.zCoord
		);
	}
	
	public MovingObjectPosition getPlayerMovingObjectPosition( EntityPlayer player )
	{
		Vec3 eyeVec = worldObj.getWorldVec3Pool().getVecFromPool(
			player.posX - m_ship.posX,
			player.posY + player.getEyeHeight() - m_ship.posY,
			player.posZ - m_ship.posZ
		);
        
		final double toRadians = Math.PI / 180.0;
		float pitch = (float)( player.rotationPitch * toRadians );
		float yaw = (float)( player.rotationYaw * toRadians );
		float cosYaw = MathHelper.cos( -yaw - (float)Math.PI );
		float sinYaw = MathHelper.sin( -yaw - (float)Math.PI );
		float cosPitch = MathHelper.cos( -pitch );
		float sinPitch = MathHelper.sin( -pitch );
		double dist = 5.0;
		
		Vec3 toVec = eyeVec.addVector(
			sinYaw * -cosPitch * dist,
			sinPitch * dist,
			cosYaw * -cosPitch * dist
		);
		
		return collisionRayTrace( eyeVec, toVec );
	}
	
	public MovingObjectPosition collisionRayTrace( Vec3 start, Vec3 stop )
	{
		return getBlock().collisionRayTrace( m_ship.getBlocks(), coords.posX, coords.posY, coords.posZ, start, stop );
	}
	
	private Block getBlock( )
	{
		return Block.blocksList[m_ship.getBlocks().getBlockId( coords )];
	}
}
