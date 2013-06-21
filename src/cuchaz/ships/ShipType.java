package cuchaz.ships;

public enum ShipType
{
	Small( 16 );
	
	private int m_maxNumBlocks;
	
	private ShipType( int maxNumBlocks )
	{
		m_maxNumBlocks = maxNumBlocks;
	}
	
	public int getMaxNumBlocks( )
	{
		return m_maxNumBlocks;
	}
}
