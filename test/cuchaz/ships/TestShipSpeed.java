/*******************************************************************************
 * Copyright (c) 2013 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships;

import org.junit.Test;

import cuchaz.modsShared.Envelopes;
import cuchaz.modsShared.Util;
import cuchaz.ships.propulsion.Propulsion;

public class TestShipSpeed
{
	// yeah, I know these aren't really unit tests
	
	@Test
	public void testLongShip5x3Sail( )
	throws Exception
	{
		printSpeeds( "H4sIAAAAAAAAAH3OWw6CQAxA0bYJf8RNyMpYiStWkZcKAjOxhMnkhv5M56QvEakkxPqPe0gL8XB7gD3BGrAXWAvWgfVgA9gI9gb7gH3BJrAZ7Ae2gK25eVr4f/E37r1G32tSS/pTUzA76Q1zL7e95oiyjrP8FoVbFOblpmAG8+IOgx0GO3JTMMttA4jxGjfUAgAA" );
	}
	
	@Test
	public void testRaft( )
	throws Exception
	{
	}
	
	private void printSpeeds( final String encodedBlocks )
	throws Exception
	{
		final String methodName = Thread.currentThread().getStackTrace()[2].getMethodName().substring( 4 );
		new MinecraftRunner( )
		{
			@Override
			public void onRun( )
			throws Exception
			{
				BlocksStorage shipBlocks = new BlocksStorage();
				shipBlocks.readFromString( encodedBlocks );
				ShipPhysics physics = new ShipPhysics( shipBlocks );
				Propulsion propulsion = new Propulsion( shipBlocks );
				Envelopes envelopes = shipBlocks.getGeometry().getEnvelopes();
				
				double topLinearSpeed = physics.getTopLinearSpeed( propulsion, envelopes );
				double topAngularSpeed = physics.getTopAngularSpeed( propulsion, envelopes );
				
				System.out.println( methodName + ", " + shipBlocks.getNumBlocks() + " blocks:" );
				System.out.println( "\t" + propulsion.dumpMethods() );
				System.out.println( String.format( "\tMass: %.2f Kg", physics.getMass() ) );
				System.out.println( String.format( "\tThrust: %.2f N", propulsion.getTotalThrust( 0 ) ) );
				System.out.println( String.format( "\tLinear speed: %.2f m/s", topLinearSpeed*Util.TicksPerSecond ) );
				System.out.println( String.format( "\tAngular speed: %.2f deg/s", topAngularSpeed*Util.TicksPerSecond ) );
			}
		}.run( encodedBlocks, methodName );
	}
}
