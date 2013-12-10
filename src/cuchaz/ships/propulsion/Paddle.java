/*******************************************************************************
 * Copyright (c) 2013 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.propulsion;



public class Paddle extends PropulsionMethod
{
	public Paddle( )
	{
		super( "Paddle", "Paddles" );
	}
	
	@Override
	public double getThrust( double speed )
	{
		// the paddle always has constant thrust
		return 0.4;
	}
}
