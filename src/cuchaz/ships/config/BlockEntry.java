package cuchaz.ships.config;

public class BlockEntry
{
	public double mass;
	public double displacement;
	public boolean isWatertight;
	public boolean isSeparator;
	
	public BlockEntry( double mass, double displacement, boolean isWatertight, boolean isSeparator )
	{
		this.mass = mass;
		this.displacement = displacement;
		this.isWatertight = isWatertight;
		this.isSeparator = isSeparator;
	}
}
