package cuchaz.ships;

import java.io.File;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

public class DetatchedWorld extends World
{
	public DetatchedWorld( World realWorld, String worldName )
	{
		// none of these values have to actually work, but we just need to get past the World constructor
		super(
			new ISaveHandler( )
			{
				@Override
				public WorldInfo loadWorldInfo( )
				{
					return null;
				}

				@Override
				public void checkSessionLock( )
				throws MinecraftException
				{
				}

				@Override
				public IChunkLoader getChunkLoader( WorldProvider worldprovider )
				{
					return null;
				}

				@Override
				public void saveWorldInfoWithPlayer( WorldInfo worldinfo, NBTTagCompound nbttagcompound )
				{
				}

				@Override
				public void saveWorldInfo( WorldInfo worldinfo )
				{
				}

				@Override
				public IPlayerFileData getSaveHandler( )
				{
					return null;
				}

				@Override
				public void flush( )
				{
				}

				@Override
				public File getMapFileFromName( String s )
				{
					return null;
				}

				@Override
				public String getWorldDirectoryName( )
				{
					return null;
				}
			},
			worldName,
			new WorldSettings( realWorld.getWorldInfo() ),
			realWorld.provider,
			MinecraftServer.getServer().theProfiler,
			realWorld.getWorldLogAgent()
		);
		
		// world constructors try to take over the existing world
		// so let the real world take back over
		realWorld.provider.registerWorld( realWorld );
		
		isRemote = realWorld.isRemote;
	}
	
	@Override
	protected IChunkProvider createChunkProvider( )
	{
		return null;
	}
	
	@Override
	public Entity getEntityByID( int i )
	{
		return null;
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public int getLightBrightnessForSkyBlocks( int x, int y, int z, int minBlockBrightness )
	{
		return 0;
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public float getBrightness( int x, int y, int z, int minLight )
	{
		// apparently only used for tripwires and piston extensions. ie, we don't care
		return 0;
	}
	
	@Override
	@SideOnly( Side.CLIENT )
	public float getLightBrightness( int x, int y, int z )
	{
		// how bright is this block intrinsically? (eg fluids)
		// returns [0-1] where 1 is the most bright
		
		// fluids aren't part of the boat. ie, we don't care
		return 0;
	}
}
