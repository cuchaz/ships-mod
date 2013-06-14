package cuchaz.ships;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;

@Mod( modid="cuchaz.ships", name="Ships", version="0.1" )
@NetworkMod( clientSideRequired=true, serverSideRequired=true )
public class Ships
{
	@Instance( "cuchaz.ships" )
	public static Ships m_instance;
	
	@PreInit
	public void preInit( FMLPreInitializationEvent event )
	{
		// nothing to do
	}
	
	@Init
	public void load( FMLInitializationEvent event )
	{
		// nothing to do
	}
	
	@PostInit
	public void postInit( FMLPostInitializationEvent event )
	{
		// nothing to do
	}
}