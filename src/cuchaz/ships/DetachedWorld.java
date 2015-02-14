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
package cuchaz.ships;

import java.io.File;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class DetachedWorld extends World {
	
	private static class SaveHandler implements ISaveHandler {
		
		@Override
		public WorldInfo loadWorldInfo() {
			return null;
		}
		
		@Override
		public void checkSessionLock() throws MinecraftException {
		}
		
		@Override
		public IChunkLoader getChunkLoader(WorldProvider worldprovider) {
			return null;
		}
		
		@Override
		public void saveWorldInfoWithPlayer(WorldInfo worldinfo, NBTTagCompound nbttagcompound) {
		}
		
		@Override
		public void saveWorldInfo(WorldInfo worldinfo) {
		}
		
		@Override
		public IPlayerFileData getSaveHandler() {
			return null;
		}
		
		@Override
		public void flush() {
		}
		
		@Override
		public File getMapFileFromName(String s) {
			return null;
		}
		
		@Override
		public String getWorldDirectoryName() {
			return null;
		}
		
		@Override
		public File getWorldDirectory() {
			return null;
		}
	}
	
	public DetachedWorld(World realWorld, String worldName) {
		// none of these values have to actually work, but we just need to get past the World constructor
		super(new SaveHandler(), worldName, new WorldSettings(realWorld.getWorldInfo()), realWorld.provider, new Profiler());
		
		// world constructors try to take over the existing world
		// so let the real world take back over
		realWorld.provider.registerWorld(realWorld);
		
		isRemote = realWorld.isRemote;
	}
	
	@Override
	protected IChunkProvider createChunkProvider() {
		return null;
	}
	
	@Override
	public Entity getEntityByID(int i) {
		return null;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public int getLightBrightnessForSkyBlocks(int x, int y, int z, int minBlockBrightness) {
		return 0;
	}
	
	@Override
	public float getLightBrightness(int x, int y, int z) {
		// how bright is this block intrinsically? (eg fluids)
		// returns [0-1] where 1 is the most bright
		
		// fluids aren't part of the boat. ie, we don't care
		return 0;
	}
	
	@Override
	public BiomeGenBase getBiomeGenForCoords(int x, int z) {
		// detached worlds don't have biomes
		return null;
	}
	
	@Override
	public BiomeGenBase getBiomeGenForCoordsBody(int x, int z) {
		// detached worlds don't have biomes
		return null;
	}
	
	@Override
	public boolean doChunksNearChunkExist(int blockX, int blockY, int blockZ, int dist) {
		return true;
	}
	
	@Override
	protected boolean chunkExists(int chunkX, int chunkZ) {
		return true;
	}
	
	@Override
	public Chunk getChunkFromChunkCoords(int chunkX, int chunkZ) {
		// detatched worlds don't have chunks
		return null;
	}
	
	@Override
	public String getProviderName() {
		return "Detatched world";
	}
	
	@Override
	public boolean isBlockNormalCubeDefault(int blockX, int blockY, int blockZ, boolean defaultValue) {
		return false;
	}
	
	@Override
	protected int func_152379_p() {
		// TODO Auto-generated method stub
		return 0;
	}
}
