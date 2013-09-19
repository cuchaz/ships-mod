package cuchaz.ships;

public enum ShipType
{
	Raft( 1024, true );
	
	private int m_maxNumBlocks;
	private boolean m_isPaddleable;
	
	private ShipType( int maxNumBlocks, boolean isPaddleable )
	{
		m_maxNumBlocks = maxNumBlocks;
		m_isPaddleable = isPaddleable;
	}
	
	public int getMaxNumBlocks( )
	{
		return m_maxNumBlocks;
	}
	
	public boolean isPaddleable( )
	{
		return m_isPaddleable;
	}
}
