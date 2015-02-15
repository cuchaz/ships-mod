/*******************************************************************************
 * Copyright (c) 2013 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.EntityShip;
import cuchaz.ships.ShipLauncher;
import cuchaz.ships.ShipLocator;
import cuchaz.ships.Ships;

public enum Gui {
	BuildShip {
		
		@Override
		@SideOnly(Side.CLIENT)
		public GuiScreen getGui(EntityPlayer player, World world, int x, int y, int z) {
			return new GuiShipLaunch(new ShipLauncher(world, new Coords(x, y, z)));
		}
	},
	UnbuildShip {
		
		@Override
		@SideOnly(Side.CLIENT)
		public GuiScreen getGuiOnShip(EntityPlayer player, EntityShip ship) {
			return new GuiShipUnlaunch(ship);
		}
	},
	PaddleShip {
		
		@Override
		@SideOnly(Side.CLIENT)
		public GuiScreen getGuiOnShip(EntityPlayer player, EntityShip ship) {
			return new GuiShipPilotPaddle(ship, player);
		}
	},
	PilotSurfaceShip {
		
		@Override
		@SideOnly(Side.CLIENT)
		public GuiScreen getGuiOnShip(EntityPlayer player, EntityShip ship) {
			return new GuiShipPilotSurface(ship, player);
		}
	},
	ShipPropulsion {
		
		@Override
		@SideOnly(Side.CLIENT)
		public GuiScreen getGui(EntityPlayer player, World world, int x, int y, int z) {
			return new GuiShipPropulsion(world, x, y, z);
		}
	};
	
	public void open(EntityPlayer player, World world, int x, int y, int z) {
		player.openGui(Ships.instance, ordinal(), world, x, y, z);
	}
	
	@SideOnly(Side.CLIENT)
	public GuiScreen getGui(EntityPlayer player, World world, int x, int y, int z) {
		// NOTE: world is always the real world, never the ship world
		EntityShip ship = ShipLocator.getFromPlayerLook(player);
		if (ship == null) {
			Ships.logger.warning("Unable to locate ship!");
			return null;
		}
		return getGuiOnShip(player, ship);
	}
	
	@SideOnly(Side.CLIENT)
	public GuiScreen getGuiOnShip(EntityPlayer player, EntityShip ship) {
		return null;
	}
	
	public static final IGuiHandler Handler = new IGuiHandler() {

		@Override
		public Container getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
			return null;
		}
		
		@Override
		public GuiScreen getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
			return values()[id].getGui(player, world, x, y, z);
		}
	};
}
