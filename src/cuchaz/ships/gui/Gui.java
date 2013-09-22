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
		public GuiContainer getGui( EntityPlayer player, World world, int x, int y, int z )
		{
			return new GuiShipLaunch( new ContainerShip(), new ShipLauncher( world, x, y, z ) );
		}
	},
	UnbuildShip
	{
		@Override
		public GuiContainer getGuiOnShip( EntityPlayer player, EntityShip ship )
		{
			return new GuiShipUnlaunch( new ContainerShip(), ship );
		}
	},
	PaddleShip
	{
		@Override
		public GuiContainer getGuiOnShip( EntityPlayer player, EntityShip ship )
		{
			return new GuiShipPilotPaddle( new ContainerShip(), ship, player );
		}
	},
	PilotSurfaceShip
	{
		@Override
		public GuiContainer getGuiOnShip( EntityPlayer player, EntityShip ship )
		{
			return new GuiShipPilotSurface( new ContainerShip(), ship, player );
		}
	},
	ShipPropulsion
	{
		@Override
		public GuiContainer getGui( EntityPlayer player, World world, int x, int y, int z )
		{
			return new GuiShipPropulsion( new ContainerShip(), world, x, y, z );
		}
	};
	
	public void open( EntityPlayer player, World world, int x, int y, int z )
	{
		player.openGui( Ships.instance, ordinal(), world, x, y, z );
	}
	
	public Container getContainer( EntityPlayer player, World world, int x, int y, int z )
	{
		return new ContainerShip();
	}
	
	public GuiContainer getGui( EntityPlayer player, World world, int x, int y, int z )
	{
		// NOTE: world is always the real world, never the ship world
		EntityShip ship = ShipLocator.getFromPlayerLook( player );
		if( ship == null )
		{
			System.out.println( "Unable to locate ship!" );
			return null;
		}
		return getGuiOnShip( player, ship );
	}
	
	public GuiContainer getGuiOnShip( EntityPlayer player, EntityShip ship )
	{
		return null;
	}
}
