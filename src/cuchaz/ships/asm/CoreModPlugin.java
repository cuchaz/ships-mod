package cuchaz.ships.asm;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

@TransformerExclusions( { "cuchaz.ships.asm" } )
public class CoreModPlugin implements IFMLLoadingPlugin
{
	@Override
	public String[] getLibraryRequestClass( )
	{
		// to download libraries if we need them (apache commons?)
		return null;
	}
	
	@Override
	public String[] getASMTransformerClass( )
	{
		return new String[] { "cuchaz.ships.asm.TileEntityTransformer" };
	}
	
	@Override
	public String getModContainerClass( )
	{
		return "cuchaz.ships.Ships";
	}
	
	@Override
	public String getSetupClass( )
	{
		// UNDONE: implement this if we want to get classloader/deobfuscation data
		return null;
	}
	
	@Override
	public void injectData( Map<String,Object> data )
	{
		// do nothing for now
		// data has mcLocation, coremodList, runtimeDeobfuscationEnabled
	}
}
