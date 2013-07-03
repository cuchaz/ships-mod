package cuchaz.ships;

public enum ShipType
{
	// NOTE: players that move too fast get kicked
	// Player's can't move more than 10 blocks per client->server update
	Raft( 16, true, 0.2, 5.0f);
	
	private int m_maxNumBlocks;
	private boolean m_isPaddleable;
	private double m_maxLinearSpeed;
	private float m_maxRotationalSpeed;
	
	private ShipType( int maxNumBlocks, boolean isPaddleable, double maxLinearSpeed, float maxRotationalSpeed )
	{
		m_maxNumBlocks = maxNumBlocks;
		m_isPaddleable = isPaddleable;
		m_maxLinearSpeed = maxLinearSpeed;
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
	
	public double getMaxLinearSpeed( )
	{
		return m_maxLinearSpeed;
	}
	
	public float getMaxRotationalSpeed( )
	{
		return m_maxRotationalSpeed;
	}
}
