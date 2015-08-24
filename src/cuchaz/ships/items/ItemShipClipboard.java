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

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.Environment;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.BlockUtils.BlockExplorer;
import cuchaz.modsShared.blocks.BoundingBoxInt;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.ShipClipboard;
import cuchaz.ships.ShipLauncher;
import cuchaz.ships.ShipType;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.Ships;
import cuchaz.ships.config.BlockProperties;
import cuchaz.ships.gui.GuiString;
import cuchaz.ships.packets.PacketPasteShip;
import cuchaz.ships.persistence.PersistenceException;

public class ItemShipClipboard extends Item {
	
	public ItemShipClipboard() {
		maxStackSize = 1;
		setCreativeTab(CreativeTabs.tabTools);
		setUnlocalizedName("cuchaz.ships.shipClipboard");
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister iconRegister) {
		itemIcon = iconRegister.registerIcon("ships:shipClipboard");
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack itemStack, World world, EntityPlayer player) {
		// client only
		if (Environment.isServer()) {
			return itemStack;
		}
		
		// find out where we're aiming
		final boolean IntersectWater = true;
		MovingObjectPosition movingobjectposition = getMovingObjectPositionFromPlayer(world, player, IntersectWater);
		if (movingobjectposition == null || movingobjectposition.typeOfHit != MovingObjectType.BLOCK) {
			return itemStack;
		}
		int x = movingobjectposition.blockX;
		int y = movingobjectposition.blockY;
		int z = movingobjectposition.blockZ;
		
		// did we use the item on a water block?
		if (BlockProperties.isWater(world.getBlock(x, y, z))) {
			pasteShip(world, player, x, y, z);
		} else {
			message(player, GuiString.ClipboardUsage);
		}
		
		return itemStack;
	}
	
	@Override
	public boolean onItemUseFirst(ItemStack itemStack, EntityPlayer player, final World world, int blockX, int blockY, int blockZ, int side, float hitX, float hitY, float hitZ) {
		// only on the client
		if (Environment.isServer()) {
			return false;
		}
		
		if (world.getBlock(blockX, blockY, blockZ) == Ships.m_blockShip) {
			return copyShip(world, player, blockX, blockY, blockZ);
		}
		return false;
	}
	
	private boolean copyShip(final World world, EntityPlayer player, int blockX, int blockY, int blockZ) {
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
		
		// also add the ship block
		Coords shipCoords = new Coords(blockX, blockY, blockZ);
		blocks.add(shipCoords);
		
		// save the ship to the clipboard
		ShipClipboard.saveShipWorld(new ShipWorld(world, shipCoords, blocks));
		
		message(player, GuiString.CopiedShip);
		return true;
	}
	
	private boolean pasteShip(World world, EntityPlayer player, int blockX, int blockY, int blockZ) {
		// make sure we're in creative mode
		if (!player.capabilities.isCreativeMode) {
			message(player, GuiString.OnlyCreative);
			return false;
		}
		
		try {
			// get the ship
			String encodedBlocks = ShipClipboard.getBlocks();
			if (encodedBlocks == null) {
				message(player, GuiString.NoShipOnClipboard);
				return false;
			}
			ShipWorld shipWorld = ShipClipboard.createShipWorld(world, encodedBlocks);
			
			// how big is the ship?
			BoundingBoxInt shipBox = shipWorld.getBoundingBox();
			BoundingBoxInt box = new BoundingBoxInt(shipBox);
			int dx = box.getDx();
			int dy = box.getDy();
			int dz = box.getDz();
			
			// look for a place to put the ship
			box.minY = blockY + 1;
			box.maxY = blockY + dy;
			for (int x = 0; x < dx; x++) {
				box.minX = blockX - x;
				box.maxX = blockX + dx - 1;
				for (int z = 0; z < dz; z++) {
					box.minZ = blockZ - z;
					box.maxZ = blockZ + dz - 1;
					if (isBoxAndShellEmpty(world, box)) {
						// compute the translation
						int tx = box.minX - shipBox.minX;
						int ty = box.minY - shipBox.minY;
						int tz = box.minZ - shipBox.minZ;
						
						// send the ship to the server for reconstruction
						Ships.net.getDispatch().sendToServer(new PacketPasteShip(encodedBlocks, tx, ty, tz));
						message(player, GuiString.PastedShip);
						return true;
					}
				}
			}
			
			message(player, GuiString.NoRoomToPasteShip, dx, dy, dz);
			return false;
		} catch (PersistenceException ex) {
			Ships.logger.error(ex, "Could not reconstruct ship from data");
			message(player, GuiString.ShipDataCorrupted);
			return false;
		}
	}
	
	private boolean isBoxAndShellEmpty(World world, BoundingBoxInt box) {
		// check each block in the box, and also a shell of size 1 around the box
		for (int x = box.minX - 1; x <= box.maxX + 1; x++) {
			for (int y = box.minY - 1; y <= box.maxY + 1; y++) {
				for (int z = box.minZ - 1; z <= box.maxZ + 1; z++) {
					Block block = world.getBlock(x, y, z);
					if (block != Blocks.air && !BlockProperties.isWater(block)) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private void message(EntityPlayer player, GuiString text, Object... args) {
		if (Environment.isClient()) {
			player.addChatMessage(new ChatComponentTranslation(text.getLocalizedText(), args));
		}
	}
}
