package cuchaz.ships;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
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
	
	public static enum Gui
	{
		Ship
		{
			@Override
			public Container getContainer( EntityPlayer player, int x, int y, int z )
			{
				return new ContainerShip();
			}
			
			@Override
			public GuiContainer getGui( EntityPlayer player, int x, int y, int z )
			{
				return new GuiShip( new ContainerShip() );
			}
		};
		
		public void open( EntityPlayer player, int x, int y, int z )
		{
			player.openGui( instance, ordinal(), player.worldObj, x, y, z );
		}
		
		public abstract Container getContainer( EntityPlayer player, int x, int y, int z );
		public abstract GuiContainer getGui( EntityPlayer player, int x, int y, int z );
	}
	
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
		GameRegistry.registerBlock( BlockShip, "blockShip" );
		
		LanguageRegistry.addName( BlockShip, "Ship" );
		
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
		
		ItemStack ironStack = new ItemStack( Item.ingotIron );
		ItemStack redstoneStack = new ItemStack( Item.redstone );
		
		// crafting recipes
		
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