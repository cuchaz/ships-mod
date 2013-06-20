package cuchaz.ships.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.world.World;
import cuchaz.ships.ContainerShip;
import cuchaz.ships.ShipBuilder;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.Ships;

public enum Gui
{
	BuildShip
	{
		@Override
		public Container getContainer( EntityPlayer player, World world, int x, int y, int z )
		{
			return new ContainerShip();
		}
		
		@Override
		public GuiContainer getGui( EntityPlayer player, World world, int x, int y, int z )
		{
			return new GuiShipBuild( new ContainerShip(), ShipBuilder.newFromWorld( world, x, y, z ) );
		}
	},
	UnbuildShip
	{
		@Override
		public Container getContainer( EntityPlayer player, World world, int x, int y, int z )
		{
			return new ContainerShip();
		}
		
		@Override
		public GuiContainer getGui( EntityPlayer player, World world, int x, int y, int z )
		{
			return new GuiShipUnbuild( new ContainerShip(), ShipBuilder.newFromShip( ((ShipWorld)world).getShip() ) );
		}
	};
	
	public void open( EntityPlayer player, World world, int x, int y, int z )
	{
		player.openGui( Ships.instance, ordinal(), world, x, y, z );
	}
	
	public abstract Container getContainer( EntityPlayer player, World world, int x, int y, int z );
	public abstract GuiContainer getGui( EntityPlayer player, World world, int x, int y, int z );
}
