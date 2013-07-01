package cuchaz.ships;

import net.minecraft.client.settings.GameSettings;

import org.lwjgl.input.Keyboard;

import cuchaz.modsShared.BlockSide;

public enum PilotAction
{
	Forward
	{
		@Override
		public void applyToShip( EntityShip ship, BlockSide forwardShipFace )
		{
			ship.motionX += forwardShipFace.getDx()*LinearAcceleration;
			ship.motionY += forwardShipFace.getDy()*LinearAcceleration;
			ship.motionZ += forwardShipFace.getDz()*LinearAcceleration;
		}
	},
	Backward
	{
		@Override
		public void applyToShip( EntityShip ship, BlockSide forwardShipFace )
		{
			ship.motionX -= forwardShipFace.getDx()*LinearAcceleration;
			ship.motionY -= forwardShipFace.getDy()*LinearAcceleration;
			ship.motionZ -= forwardShipFace.getDz()*LinearAcceleration;
		}
	},
	Left
	{
		@Override
		public void applyToShip( EntityShip ship, BlockSide forwardShipFace )
		{
			ship.motionYaw += RotationalAcceleration;
		}
	},
	Right
	{
		@Override
		public void applyToShip( EntityShip ship, BlockSide forwardShipFace )
		{
			ship.motionYaw -= RotationalAcceleration;
		}
	};
	
	// TEMP: acceleration should come from thrusters (modified by mass)!
	private static double LinearAcceleration = 0.02;
	private static float RotationalAcceleration = 1.0f;
	
	private int m_keyCode;
	
	private PilotAction( )
	{
		m_keyCode = -1;
	}
	
	public static void setActionCodes( GameSettings settings )
	{
		Forward.m_keyCode = settings.keyBindForward.keyCode;
		Backward.m_keyCode = settings.keyBindBack.keyCode;
		Left.m_keyCode = settings.keyBindLeft.keyCode;
		Right.m_keyCode = settings.keyBindRight.keyCode;
	}
	
	public static int getActiveActions( GameSettings settings )
	{
		// roll up the actions into a bit vector
		int actions = 0;
		for( PilotAction action : values() )
		{
			if( Keyboard.isKeyDown( action.m_keyCode ) )
			{
				actions |= 1 << action.ordinal();
			}
		}
		return actions;
	}
	
	public static void applyToShip( EntityShip ship, int actions, BlockSide sideShipForward )
	{
		for( PilotAction action : values() )
		{
			if( action.isActive( actions ) )
			{
				action.applyToShip( ship, sideShipForward );
			}
		}
	}
	
	public boolean isActive( int actions )
	{
		return ( ( actions >> ordinal() ) & 0x1 ) == 1;
	}
	
	protected abstract void applyToShip( EntityShip ship, BlockSide forwardShipFace );
}
