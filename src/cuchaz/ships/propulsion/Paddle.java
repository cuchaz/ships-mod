package cuchaz.ships.propulsion;

public class Paddle extends PropulsionMethod
{
	public Paddle( )
	{
		super( "Paddle", "Paddles", null );
	}
	
	@Override
	public double getThrust( )
	{
		// the paddle always has constant thrust
		return 10;
	}
}
