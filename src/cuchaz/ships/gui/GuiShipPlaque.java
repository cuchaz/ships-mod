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

import static cuchaz.ships.gui.GuiSettings.*;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;

import org.lwjgl.input.Keyboard;

import cuchaz.ships.ContainerShip;
import cuchaz.ships.EntityShipPlaque;
import cuchaz.ships.Ships;
import cuchaz.ships.packets.PacketShipPlaque;

public class GuiShipPlaque extends GuiShip
{
	private EntityShipPlaque m_shipPlaque;
	private GuiTextField m_textName;
	private GuiButton m_buttonDone;
	
	public GuiShipPlaque( ContainerShip container, EntityShipPlaque shipPlaque )
	{
		super( container );
		
		m_shipPlaque = shipPlaque;
	}
	
	@Override
	@SuppressWarnings( "unchecked" )
	public void initGui( )
	{
		super.initGui();
		
		// add the text box
		m_textName = new GuiTextField( this.mc.fontRenderer,
			LeftMargin,
			60,
			120,
			20
		);
		m_textName.setFocused( true );
		m_textName.setText( m_shipPlaque.getName() );
		
		// add the button
		m_buttonDone = new GuiButton( 0,
			guiLeft + LeftMargin,
			guiTop + ySize - TopMargin - 20,
			80,
			20,
			GuiString.Done.getLocalizedText()
		);
		buttonList.add( m_buttonDone );
	}
	
	@Override
	protected void actionPerformed( GuiButton button )
	{
		if( button.id == m_buttonDone.id )
		{
			apply();
		}
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer( int mouseX, int mouseY )
	{
		drawHeaderText( GuiString.ShipPlaque.getLocalizedText(), 0 );
		
		m_textName.drawTextBox();
	}
	
	@Override
	protected void keyTyped( char keyChar, int keyCode )
    {
		if( keyCode == Keyboard.KEY_RETURN )
		{
			apply();
		}
		
		if( m_textName.isFocused() )
		{
			m_textName.textboxKeyTyped( keyChar, keyCode );
		}
		super.keyTyped( keyChar, keyCode );
    }
	
	private void apply( )
	{
		// save the name
		m_shipPlaque.setName( m_textName.getText() );
		
		// update the server too
		Ships.net.getDispatch().sendToServer( new PacketShipPlaque( m_shipPlaque ) );
		
		close();
	}
}
