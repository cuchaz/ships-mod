package cuchaz.ships.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.world.World;
import cuchaz.ships.ContainerShip;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipLauncher;
import cuchaz.ships.ShipLocator;
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
			return new GuiShipLaunch( new ContainerShip(), new ShipLauncher( world, x, y, z ) );
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
			EntityShip ship = ShipLocator.getFromPlayerLocation( world, player );
			if( ship == null )
			{
				return null;
			}
			return new GuiShipUnlaunch( new ContainerShip(), ship );
		}
	},
	PaddleShip
	{
		@Override
		public Container getContainer( EntityPlayer player, World world, int x, int y, int z )
		{
			return new ContainerShip();
		}
		
		@Override
		public GuiContainer getGui( EntityPlayer player, World world, int x, int y, int z )
		{
			EntityShip ship = ShipLocator.getFromPlayerLocation( world, player );
			if( ship == null )
			{
				return null;
			}
			return new GuiShipPaddle( new ContainerShip(), ship, player );
		}
	};
	
	public void open( EntityPlayer player, World world, int x, int y, int z )
	{
		player.openGui( Ships.instance, ordinal(), world, x, y, z );
	}
	
	public abstract Container getContainer( EntityPlayer player, World world, int x, int y, int z );
	public abstract GuiContainer getGui( EntityPlayer player, World world, int x, int y, int z );
}
