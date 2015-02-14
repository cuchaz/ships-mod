/*******************************************************************************
 * Copyright (c) 2014 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityHanging;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cuchaz.modsShared.blocks.BlockMap;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.persistence.BlockStoragePersistence;
import cuchaz.ships.persistence.PersistenceException;
import cuchaz.ships.persistence.ShipWorldPersistence;

public class ShipClipboard {
	
	public static void saveShipWorld(ShipWorld shipWorld) {
		StringSelection selection = new StringSelection(ShipWorldPersistence.writeNewestVersionToString(shipWorld));
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
	}
	
	public static String getBlocks() {
		try {
			String encodedBlocks = null;
			// the stupid freaking system clipboard is collosally slow
			// getData() can take up to 1s to finish and there doesn't seem to be anything I can do about it...
			// the game hangs, it looks stupid, and we're stuck with it for now
			long startTime = System.currentTimeMillis();
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
				encodedBlocks = (String)clipboard.getData(DataFlavor.stringFlavor);
			}
			Ships.logger.info("Clipboard access took %.2f s", (System.currentTimeMillis() - startTime) / 1000.0f);
			return encodedBlocks;
		} catch (UnsupportedFlavorException ex) {
			return null;
		} catch (IOException ex) {
			return null;
		}
	}
	
	public static ShipWorld createShipWorld(World world, String encodedBlocks) throws PersistenceException {
		// decode the ship: older versions just save blocks, newer versions save the whole ship world
		try {
			return ShipWorldPersistence.readAnyVersion(world, encodedBlocks);
		} catch (PersistenceException shipWorldException) {
			try {
				// it's probably not a ship world, try reading just the blocks
				BlocksStorage storage = BlockStoragePersistence.readAnyVersion(encodedBlocks);
				return new ShipWorld(world, storage, new BlockMap<TileEntity>(), new BlockMap<EntityHanging>(), 0);
			} catch (PersistenceException blockStorageException) {
				// doesn't look like it's a ship world or a block storage... just re-throw the first exception
				throw shipWorldException;
			}
		}
	}
	
	public static void restoreShip(World world, String encodedBlocks, Coords translation) throws PersistenceException {
		// create the ship world
		ShipWorld shipWorld = createShipWorld(world, encodedBlocks);
		
		// compute the block correspondence
		BlockMap<Coords> correspondence = new BlockMap<Coords>();
		for (Coords coords : shipWorld.coords()) {
			// translate to the world
			Coords worldCoords = new Coords(coords);
			worldCoords.x += translation.x;
			worldCoords.y += translation.y;
			worldCoords.z += translation.z;
			
			correspondence.put(coords, worldCoords);
		}
		
		// TODO: move into persistence/loading
		// if there are unrecognized blocks, just replace them with wood planks
		boolean foundUnknownBlocks = false;
		for (Coords coords : shipWorld.coords()) {
			Block block = shipWorld.getBlock(coords);
			if (block == null) {
				foundUnknownBlocks = true;
				BlockStorage storage = shipWorld.getBlockStorage(coords);
				storage.block = Blocks.planks;
				storage.meta = 0;
			}
		}
		if (foundUnknownBlocks) {
			Ships.logger.warning("Unknown blocks found in ship! They're probably mod blocks from an uninstalled mod. Replacing with wood planks.");
		}
		
		// update the world
		shipWorld.restoreToWorld(world, correspondence, shipWorld.getBoundingBox().minY - 1);
	}
}
