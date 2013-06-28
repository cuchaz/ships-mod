package cuchaz.ships;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import cpw.mods.fml.client.registry.RenderingRegistry;
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
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cuchaz.ships.gui.Gui;
import cuchaz.ships.gui.GuiString;
import cuchaz.ships.packets.PacketBuildShip;
import cuchaz.ships.packets.PacketHandler;
import cuchaz.ships.packets.PacketPilotShip;
import cuchaz.ships.packets.PacketUnbuildShip;

@Mod( modid="cuchaz.ships", name="Ships", version="0.1" )
@NetworkMod(
	clientSideRequired = true,
	serverSideRequired = true,
	// NOTE: 20-character limit for channel names
	channels = { PacketBuildShip.Channel, PacketUnbuildShip.Channel, PacketPilotShip.Channel },
	packetHandler = PacketHandler.class
)

public class Ships
{
	@Instance( "cuchaz.ships" )
	public static Ships instance;
	
	public static final String TexturesPath = "/mods/ships/textures/";
	
	// (apparently the most robust id picking strategy is almost complete randomness)
	// item registration: use ids [7321-7325]
	public static final ItemPaddle ItemPaddle = new ItemPaddle( 7321 );
	
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
		
		// set renderers
		RenderingRegistry.registerEntityRenderingHandler( EntityShip.class, new RenderShip() );
		
		// GUI hooks
		NetworkRegistry.instance().registerGuiHandler( this, new IGuiHandler( )
		{
			@Override
			public Object getServerGuiElement( int id, EntityPlayer player, World world, int x, int y, int z )
			{
				return Gui.values()[id].getContainer( player, world, x, y, z );
			}
			
			@Override
			public Object getClientGuiElement( int id, EntityPlayer player, World world, int x, int y, int z )
			{
				return Gui.values()[id].getGui( player, world, x, y, z );
			}
		} );
	}
	
	private void loadThings( )
	{
		// blocks
		GameRegistry.registerBlock( BlockShip, "blockShip" );
		
		// items
		GameRegistry.registerItem( ItemPaddle, "paddle" );
		
		// entities
		final int EntityShipId = 174;
		EntityRegistry.registerGlobalEntityID( EntityShip.class, "Ship", EntityShipId );
		EntityRegistry.registerModEntity( EntityShip.class, "Ship", EntityShipId, instance, 256, 10, true );
	}
	
	private void loadLanguage( )
	{
		// block names
		LanguageRegistry.addName( BlockShip, "Ship" );
		
		// item names
		LanguageRegistry.addName( ItemPaddle, "Paddle" );
		
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
		ItemStack stickStack = new ItemStack( Item.stick );
		
		// ship
		GameRegistry.addRecipe(
			new ItemStack( BlockShip ),
			"xxx", "xyx", "xxx",
			'x', ironStack,
			'y', redstoneStack
		);
		
		// paddle
		GameRegistry.addRecipe(
			new ItemStack( ItemPaddle, 1 ),
			" xx", " xx", "x  ",
			'x', stickStack
		);
	}

	@PostInit
	public void postInit( FMLPostInitializationEvent event )
	{
		// nothing to do
	}
}