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

import net.minecraft.nbt.NBTTagCompound;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipWorld;

public enum ShipPersistence
{
	// the order matters. The newest should always be last
	V1( 1 )
	{
		@Override
		public void read( EntityShip ship, NBTTagCompound nbt )
		{
			ship.setShipWorld( new ShipWorld( ship.worldObj, nbt.getByteArray( "blocks" ) ) );
		}
		
		@Override
		public void write( EntityShip ship, NBTTagCompound nbt )
		{
			nbt.setByteArray( "blocks", ship.getShipWorld().getData() );
		}
	};
	
	private int m_version;
	
	public int getVersion( )
	{
		return m_version;
	}
	
	private ShipPersistence( int version )
	{
		m_version = version;
	}
	
	public abstract void read( EntityShip ship, NBTTagCompound nbt );
	public abstract void write( EntityShip ship, NBTTagCompound nbt );
	
	public static ShipPersistence get( int version )
	{
		if( version >= 0 && version < values().length )
		{
			return values()[version];
		}
		return null;
	}

	public static ShipPersistence getNewestVersion( )
	{
		return values()[ values().length - 1 ];
	}
}
