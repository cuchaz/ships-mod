/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.persistence;

public class UnrecognizedPersistenceVersion extends Exception
{
	private static final long serialVersionUID = 4074487319960658175L;
	
	private int m_version;
	
	public UnrecognizedPersistenceVersion( int version )
	{
		super( String.format( "Version %d was not recognized!", version ) );
		m_version = version;
	}
	
	public int getVersion( )
	{
		return m_version;
	}
}
