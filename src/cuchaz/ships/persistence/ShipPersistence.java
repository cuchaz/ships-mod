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

import java.util.TreeMap;

import net.minecraft.nbt.NBTTagCompound;
import cuchaz.ships.EntityShip;

public enum ShipPersistence
{
	V1( 1 )
	{
		@Override
		public void read( EntityShip ship, NBTTagCompound nbt )
		throws UnrecognizedPersistenceVersion
		{
			ship.setShipWorld( ShipWorldPersistence.readAnyVersion( ship.worldObj, nbt.getByteArray( "blocks" ) ) );
		}
		
		@Override
		public void write( EntityShip ship, NBTTagCompound nbt )
		{
			nbt.setByteArray( "blocks", ShipWorldPersistence.writeNewestVersion( ship.getShipWorld() ) );
		}
	};
	
	private static TreeMap<Integer,ShipPersistence> m_versions;
	
	static
	{
		m_versions = new TreeMap<Integer,ShipPersistence>();
		for( ShipPersistence persistence : values() )
		{
			m_versions.put( persistence.m_version, persistence );
		}
	}
	
	private int m_version;
	
	private ShipPersistence( int version )
	{
		m_version = version;
	}
	
	public abstract void read( EntityShip ship, NBTTagCompound nbt ) throws UnrecognizedPersistenceVersion;
	public abstract void write( EntityShip ship, NBTTagCompound nbt );
	
	private static ShipPersistence get( int version )
	{
		return m_versions.get( version );
	}
	
	private static ShipPersistence getNewestVersion( )
	{
		return m_versions.lastEntry().getValue();
	}
	
	public static void readAnyVersion( EntityShip ship, NBTTagCompound nbt )
	throws UnrecognizedPersistenceVersion
	{
		int version = nbt.getByte( "version" );
		ShipPersistence persistence = get( version );
		if( persistence == null )
		{
			throw new UnrecognizedPersistenceVersion( version );
		}
		
		persistence.read( ship, nbt );
	}
	
	public static void writeNewestVersion( EntityShip ship, NBTTagCompound nbt )
	{
		ShipPersistence persistence = getNewestVersion();
		nbt.setByte( "version", (byte)persistence.m_version );
		persistence.write( ship, nbt );
	}
}
