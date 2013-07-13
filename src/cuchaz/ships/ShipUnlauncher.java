package cuchaz.ships;

import java.util.List;
import java.util.TreeMap;

import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;

public class ShipUnlauncher
{
	public static enum UnlaunchFlag
	{
		TouchingOnlySeparatorBlocks
		{
			@Override
			public boolean computeValue( ShipUnlauncher unlauncher )
			{
				return false;
			}
		};
		
		public abstract boolean computeValue( ShipUnlauncher unlauncher );
	}
	
	private EntityShip m_ship;
	private List<Boolean> m_unlaunchFlags;
	private TreeMap<ChunkCoordinates,ChunkCoordinates> m_correspondence;
	
	public ShipUnlauncher( EntityShip ship )
	{
		m_ship = ship;
		
		// compute the block correspondence
		m_correspondence = new TreeMap<ChunkCoordinates,ChunkCoordinates>();
		
		//MaterialProperties.isSeparatorBlock( Block.blocksList[world.getBlockId( x, y, z )] );
		
		// compute the unlaunch flags
		for( UnlaunchFlag flag : UnlaunchFlag.values() )
		{
			setUnlaunchFlag( flag, flag.computeValue( this ) );
		}
	}
	
	public boolean getUnlaunchFlag( UnlaunchFlag flag )
	{
		return m_unlaunchFlags.get( flag.ordinal() );
	}
	private void setUnlaunchFlag( UnlaunchFlag flag, boolean val )
	{
		m_unlaunchFlags.set( flag.ordinal(), val );
	}
	
	public boolean isUnlaunchable( )
	{
		boolean isValid = true;
		for( UnlaunchFlag flag : UnlaunchFlag.values() )
		{
			isValid = isValid && getUnlaunchFlag( flag );
		}
		return isValid;
	}
	
	public void unlaunch( )
	{
		m_ship.setDead();
		
		// UNDONE: use the block correspondence
		
		// restore all the blocks
		m_ship.getBlocks().restoreToWorld(
			m_ship.worldObj,
			MathHelper.floor_double( m_ship.posX ),
			MathHelper.floor_double( m_ship.posY ),
			MathHelper.floor_double( m_ship.posZ )
		);
	}
	
	private void computeUnlaunchFlags( )
	{
		// TODO Auto-generated method stub
		
	}
}
