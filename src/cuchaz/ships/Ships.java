package cuchaz.ships;

import net.minecraft.client.renderer.entity.RenderManager;
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
	
	// (apparently the most robust id picking strategy is almost complete randomness)
	// item registration: use ids [7308,7319]
	// block registration: use ids [3170-3190]
	
	@PreInit
	public void preInit( FMLPreInitializationEvent event )
	{
		// nothing to do
	}
	
	@Init
	public void load( FMLInitializationEvent event )
	{
		// set the ship renderer
		RenderManager.instance.entityRenderMap.put( EntityShip.class, new RenderShip() );
	}
	
	@PostInit
	public void postInit( FMLPostInitializationEvent event )
	{
		// nothing to do
	}
}