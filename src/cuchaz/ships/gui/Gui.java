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

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.ContainerShip;
import cuchaz.ships.EntityShip;
import cuchaz.ships.EntityShipPlaque;
import cuchaz.ships.ShipLauncher;
import cuchaz.ships.ShipLocator;
import cuchaz.ships.Ships;

public enum Gui {
	BuildShip {
		
		@Override
		@SideOnly(Side.CLIENT)
		public GuiContainer getGui(EntityPlayer player, World world, int x, int y, int z) {
			return new GuiShipLaunch(new ContainerShip(), new ShipLauncher(world, new Coords(x, y, z)));
		}
	},
	UnbuildShip {
		
		@Override
		@SideOnly(Side.CLIENT)
		public GuiContainer getGuiOnShip(EntityPlayer player, EntityShip ship) {
			return new GuiShipUnlaunch(new ContainerShip(), ship);
		}
	},
	PaddleShip {
		
		@Override
		@SideOnly(Side.CLIENT)
		public GuiContainer getGuiOnShip(EntityPlayer player, EntityShip ship) {
			return new GuiShipPilotPaddle(new ContainerShip(), ship, player);
		}
	},
	PilotSurfaceShip {
		
		@Override
		@SideOnly(Side.CLIENT)
		public GuiContainer getGuiOnShip(EntityPlayer player, EntityShip ship) {
			return new GuiShipPilotSurface(new ContainerShip(), ship, player);
		}
	},
	ShipPropulsion {
		
		@Override
		@SideOnly(Side.CLIENT)
		public GuiContainer getGui(EntityPlayer player, World world, int x, int y, int z) {
			return new GuiShipPropulsion(new ContainerShip(), world, x, y, z);
		}
	},
	ListOfSupporters {
		
		@Override
		@SideOnly(Side.CLIENT)
		public GuiContainer getGui(EntityPlayer player, World world, int x, int y, int z) {
			return new GuiListOfSupporters(new ContainerShip());
		}
	},
	ShipPlaque {
		
		@Override
		@SideOnly(Side.CLIENT)
		public GuiContainer getGui(EntityPlayer player, World world, int x, int y, int z) {
			// get the ship plaque entity from the block (entity id is assigned to x)
			int entityId = x;
			Entity entity = world.getEntityByID(entityId);
			if (entity instanceof EntityShipPlaque) {
				return new GuiShipPlaque(new ContainerShip(), (EntityShipPlaque)entity);
			}
			return null;
		}
	};
	
	public void open(EntityPlayer player, World world, int x, int y, int z) {
		player.openGui(Ships.instance, ordinal(), world, x, y, z);
	}
	
	public Container getContainer(EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerShip();
	}
	
	@SideOnly(Side.CLIENT)
	public GuiContainer getGui(EntityPlayer player, World world, int x, int y, int z) {
		// NOTE: world is always the real world, never the ship world
		EntityShip ship = ShipLocator.getFromPlayerLook(player);
		if (ship == null) {
			Ships.logger.warning("Unable to locate ship!");
			return null;
		}
		return getGuiOnShip(player, ship);
	}
	
	@SideOnly(Side.CLIENT)
	public GuiContainer getGuiOnShip(EntityPlayer player, EntityShip ship) {
		return null;
	}
}
