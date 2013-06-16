package cuchaz.ships;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;

@Mod( modid="cuchaz.ships", name="Ships", version="0.1" )
@NetworkMod( clientSideRequired=true, serverSideRequired=true )
public class Ships
{
	@Instance( "cuchaz.ships" )
	public static Ships instance;
	
	public static final String TexturesPath = "/mods/ships/textures/";
	
	// (apparently the most robust id picking strategy is almost complete randomness)
	// item registration:
	
	// block registration: use ids [3170-3190]
	public BlockShip BlockShip = new BlockShip( 3170 );
	
	@PreInit
	public void preInit( FMLPreInitializationEvent event )
	{
		// nothing to do
	}
	
	@Init
	public void load( FMLInitializationEvent event )
	{
		loadThings();
		loadLanguage();
		loadRecipes();
		
		// set the ship renderer
		RenderManager.instance.entityRenderMap.put( EntityShip.class, new RenderShip() );
		
		// GUI hooks
		NetworkRegistry.instance().registerGuiHandler( this, new IGuiHandler( )
		{
			@Override
			public Object getServerGuiElement( int id, EntityPlayer player, World world, int x, int y, int z )
			{
				return Gui.values()[id].getContainer( player, x, y, z );
			}
			
			@Override
			public Object getClientGuiElement( int id, EntityPlayer player, World world, int x, int y, int z )
			{
				return Gui.values()[id].getGui( player, x, y, z );
			}
		} );
	}
	
	private void loadThings( )
	{
		GameRegistry.registerBlock( BlockShip, "blockShip" );
	}

	private void loadLanguage( )
	{
		// block names
		LanguageRegistry.addName( BlockShip, "Ship" );
		
		// gui strings
		for( GuiString string : GuiString.values() )
		{
			LanguageRegistry.instance().addStringLocalization( string.getKey(), string.getUnlocalizedText() );
		}
	}

	private void loadRecipes( )
	{
		ItemStack ironStack = new ItemStack( Item.ingotIron );
		ItemStack redstoneStack = new ItemStack( Item.redstone );
		
		// ship
		GameRegistry.addRecipe(
			new ItemStack( BlockShip ),
			"xxx", "xyx", "xxx",
			'x', ironStack,
			'y', redstoneStack
		);
	}

	@PostInit
	public void postInit( FMLPostInitializationEvent event )
	{
		// nothing to do
	}
}