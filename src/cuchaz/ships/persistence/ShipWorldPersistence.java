/*******************************************************************************
 * Copyright (c) 2014 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.persistence;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.TreeMap;

import cuchaz.ships.ShipWorld;

public enum ShipWorldPersistence
{
	V1( 1 )
	{
		@Override
		public ShipWorld read( InputStream in )
		{
			return null;
		}
		
		@Override
		public void write( ShipWorld shipWorld, OutputStream out )
		{
		}
	};
	
	private static TreeMap<Integer,ShipWorldPersistence> m_versions;
	
	static
	{
		m_versions = new TreeMap<Integer,ShipWorldPersistence>();
		for( ShipWorldPersistence persistence : values() )
		{
			m_versions.put( persistence.getVersion(), persistence );
		}
	}
	
	private int m_version;
	
	public int getVersion( )
	{
		return m_version;
	}
	
	private ShipWorldPersistence( int version )
	{
		m_version = version;
	}
	
	public abstract ShipWorld read( InputStream in );
	public abstract void write( ShipWorld shipWorld, OutputStream out );
	
	public static ShipWorldPersistence get( int version )
	{
		return m_versions.get( version );
	}
	
	public static ShipWorldPersistence getNewestVersion( )
	{
		return m_versions.lastEntry().getValue();
	}
}
