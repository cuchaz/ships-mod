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
package cuchaz.ships.items;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.Environment;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.BlockUtils.BlockExplorer;
import cuchaz.modsShared.blocks.BlockUtils.UpdateRules;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.ShipLauncher;
import cuchaz.ships.ShipType;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.Ships;
import cuchaz.ships.config.BlockProperties;
import cuchaz.ships.gui.GuiString;
import cuchaz.ships.packets.PacketEraseShip;

public class ItemShipEraser extends Item {
	
	public ItemShipEraser() {
		maxStackSize = 1;
		setCreativeTab(CreativeTabs.tabTools);
		setUnlocalizedName("cuchaz.ships.shipEraser");
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister iconRegister) {
		itemIcon = iconRegister.registerIcon("ships:shipEraser");
	}
	
	@Override
	public boolean onItemUseFirst(ItemStack itemStack, EntityPlayer player, final World world, int blockX, int blockY, int blockZ, int side, float hitX, float hitY, float hitZ) {
		// NOTE: this only happens on the client
		
		if (world.getBlock(blockX, blockY, blockZ) == Ships.m_blockShip) {
			return eraseShip(world, player, blockX, blockY, blockZ);
		} else {
			message(player, GuiString.EraserUsage);
			return false;
		}
	}
	
	public static boolean eraseShip(final World world, EntityPlayer player, int blockX, int blockY, int blockZ) {
		// make sure we're in creative mode
		if (!player.capabilities.isCreativeMode) {
			message(player, GuiString.OnlyCreative);
			return false;
		}
		
		// get the ship type from the block
		ShipType shipType = Ships.m_blockShip.getShipType(world, blockX, blockY, blockZ);
		
		// find all the blocks connected to the ship block
		BlockSet blocks = BlockUtils.searchForBlocks(blockX, blockY, blockZ, shipType.getMaxNumBlocks(), new BlockExplorer() {
			
			@Override
			public boolean shouldExploreBlock(Coords coords) {
				return !BlockProperties.isSeparator(world.getBlock(coords.x, coords.y, coords.z));
			}
		}, ShipLauncher.ShipBlockNeighbors);
		
		// did we find too many blocks?
		if (blocks == null) {
			message(player, GuiString.NoShipWasFoundHere);
			return false;
		}
		
		if (Environment.isClient()) {
			Ships.net.getDispatch().sendToServer(new PacketEraseShip(blockX, blockY, blockZ));
		} else {
			// also add the ship block
			Coords shipCoords = new Coords(blockX, blockY, blockZ);
			blocks.add(shipCoords);
			
			// remove the ship
			ShipLauncher.removeShipFromWorld(world, new ShipWorld(world, shipCoords, blocks), shipCoords, UpdateRules.UpdateClients);
		}
		return true;
	}
	
	private static void message(EntityPlayer player, GuiString text, Object... args) {
		if (Environment.isClient()) {
			player.addChatMessage(new ChatComponentTranslation(text.getLocalizedText(), args));
		}
	}
}
