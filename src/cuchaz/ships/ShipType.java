package cuchaz.ships;

public enum ShipType
{
	// NOTE: players that move too fast get kicked
	// Player's can't move more than 10 blocks per client->server update
	Raft( 16, true, 0.3 );
	
	private int m_maxNumBlocks;
	private boolean m_isPaddleable;
	private double m_maxSpeed;
	
	private ShipType( int maxNumBlocks, boolean isPaddleable, double maxSpeed )
	{
		m_maxNumBlocks = maxNumBlocks;
		m_isPaddleable = isPaddleable;
		m_maxSpeed = maxSpeed;
	}
	
	public int getMaxNumBlocks( )
	{
		return m_maxNumBlocks;
	}
	
	public boolean isPaddleable( )
	{
		return m_isPaddleable;
	}
	
	public double getMaxSpeed( )
	{
		return m_maxSpeed;
	}
}
