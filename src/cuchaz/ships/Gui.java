package cuchaz.ships;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public enum Gui
{
	Ship
	{
		@Override
		public Container getContainer( EntityPlayer player, int x, int y, int z )
		{
			return new ContainerShip();
		}
		
		@Override
		public GuiContainer getGui( EntityPlayer player, int x, int y, int z )
		{
			return new GuiShip( new ContainerShip(), new ShipBuilder( player.worldObj, x, y, z ) );
		}
	};
	
	public void open( EntityPlayer player, int x, int y, int z )
	{
		player.openGui( Ships.instance, ordinal(), player.worldObj, x, y, z );
	}
	
	public abstract Container getContainer( EntityPlayer player, int x, int y, int z );
	public abstract GuiContainer getGui( EntityPlayer player, int x, int y, int z );
}
