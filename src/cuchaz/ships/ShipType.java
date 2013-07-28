package cuchaz.ships;

public enum ShipType
{
	Raft( 64, true, 3.0f );
	
	private int m_maxNumBlocks;
	private boolean m_isPaddleable;
	private float m_maxRotationalSpeed;
	
	private ShipType( int maxNumBlocks, boolean isPaddleable, float maxRotationalSpeed )
	{
		m_maxNumBlocks = maxNumBlocks;
		m_isPaddleable = isPaddleable;
		m_maxRotationalSpeed = maxRotationalSpeed;
	}
	
	public int getMaxNumBlocks( )
	{
		return m_maxNumBlocks;
	}
	
	public boolean isPaddleable( )
	{
		return m_isPaddleable;
	}
	
	public float getMaxRotationalSpeed( )
	{
		return m_maxRotationalSpeed;
	}
}
