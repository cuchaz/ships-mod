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
package cuchaz.ships.gui;

import java.util.List;

import cuchaz.ships.ContainerShip;
import cuchaz.ships.Supporters;

public class GuiListOfSupporters extends GuiShip
{
	private List<String> m_supporters;
	
	public GuiListOfSupporters( ContainerShip container )
	{
		super( container );
		
		// get the supporters
		m_supporters = Supporters.getSortedNames();
		
		// remove my name from the list
		if( m_supporters.get( 0 ).equalsIgnoreCase( "cuchaz" ) )
		{
			m_supporters.remove( 0 );
		}
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer( int mouseX, int mouseY )
	{
		drawHeaderText( GuiString.ListOfSupporters.getLocalizedText(), 0 );
		for( int i=0; i<m_supporters.size(); i++ )
		{
			drawText( m_supporters.get( i ), i + 1 );
		}
		
		// UNDONE: will have to do pagination when we get more supporters...
	}
}
