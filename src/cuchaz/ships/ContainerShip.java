package cuchaz.ships;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class ContainerShip extends Container
{
	public ContainerShip( )
	{
		// add slots here if we need
		
		// NOTE: this class isn't really used, but it's needed by Forge's GUI API
	}
	
	@Override
	public boolean canInteractWith( EntityPlayer entityplayer )
	{
		return true;
	}
}
