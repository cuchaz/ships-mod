package cuchaz.ships;

import cuchaz.modsShared.BlockSide;

public enum PilotAction
{
	Forward
	{
		@Override
		public void adjustShipVelocity( EntityShip ship, BlockSide forwardShipFace )
		{
			ship.motionX += forwardShipFace.getDx()*Acceleration;
			ship.motionY += forwardShipFace.getDy()*Acceleration;
			ship.motionZ += forwardShipFace.getDz()*Acceleration;
		}
	},
	Backward
	{
		@Override
		public void adjustShipVelocity( EntityShip ship, BlockSide forwardShipFace )
		{
			ship.motionX -= forwardShipFace.getDx()*Acceleration;
			ship.motionY -= forwardShipFace.getDy()*Acceleration;
			ship.motionZ -= forwardShipFace.getDz()*Acceleration;
		}
	},
	Left
	{
		@Override
		public void adjustShipVelocity( EntityShip ship, BlockSide forwardShipFace )
		{
			// UNDONE
		}
	},
	Right
	{
		@Override
		public void adjustShipVelocity( EntityShip ship, BlockSide forwardShipFace )
		{
			// UNDONE
		}
	};
	
	// TEMP: acceleration should come from thrusters (modified by mass)!
	private static double Acceleration = 0.01;
	
	public abstract void adjustShipVelocity( EntityShip ship, BlockSide forwardShipFace );
}
