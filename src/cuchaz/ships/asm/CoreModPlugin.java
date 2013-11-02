package cuchaz.ships.asm;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.Name;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

@Name( "cuchaz.ships.core" )
@MCVersion( "1.6.4" )
@TransformerExclusions( { "cuchaz.ships.asm" } )
public class CoreModPlugin implements IFMLLoadingPlugin
{
	public static Boolean isObfuscatedEnvironment = null;
	
	@Override
	public String[] getLibraryRequestClass( )
	{
		// to download libraries if we need them (apache commons?)
		return null;
	}
	
	@Override
	public String[] getASMTransformerClass( )
	{
		return new String[] { "cuchaz.ships.asm.CoreModTransformer" };
	}
	
	@Override
	public String getModContainerClass( )
	{
		return "cuchaz.ships.Ships";
	}
	
	@Override
	public String getSetupClass( )
	{
		// implement this if we want to get launcher classloader directly
		return null;
	}
	
	@Override
	public void injectData( Map<String,Object> data )
	{
		// data keys: mcLocation, coremodList, runtimeDeobfuscationEnabled, coremodLocation
		
		// are we running in an obfuscated environment?
		isObfuscatedEnvironment = (Boolean)data.get( "runtimeDeobfuscationEnabled" );
	}
}
