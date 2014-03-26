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
import java.util.Arrays;
import java.util.logging.Logger;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import cpw.mods.fml.client.FMLFileResourcePack;
import cpw.mods.fml.client.FMLFolderResourcePack;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.FMLNetworkHandler;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.FMLHacker;
import cuchaz.ships.blocks.BlockAirWall;
import cuchaz.ships.blocks.BlockHelm;
import cuchaz.ships.blocks.BlockShip;
import cuchaz.ships.gui.Gui;
import cuchaz.ships.gui.GuiString;
import cuchaz.ships.items.ItemListOfSupporters;
import cuchaz.ships.items.ItemMagicBucket;
import cuchaz.ships.items.ItemMagicShipLevitator;
import cuchaz.ships.items.ItemPaddle;
import cuchaz.ships.items.ItemShipClipboard;
import cuchaz.ships.items.ItemShipEraser;
import cuchaz.ships.items.ItemShipPlaque;
import cuchaz.ships.items.ItemSupporterPlaque;
import cuchaz.ships.items.SupporterPlaqueType;
import cuchaz.ships.packets.PacketChangedBlocks;
import cuchaz.ships.packets.PacketEraseShip;
import cuchaz.ships.packets.PacketHandler;
import cuchaz.ships.packets.PacketLaunchShip;
import cuchaz.ships.packets.PacketPasteShip;
import cuchaz.ships.packets.PacketPilotShip;
import cuchaz.ships.packets.PacketRequestShipBlocks;
import cuchaz.ships.packets.PacketShipBlockEvent;
import cuchaz.ships.packets.PacketShipBlocks;
import cuchaz.ships.packets.PacketShipLaunched;
import cuchaz.ships.packets.PacketShipPlaque;
import cuchaz.ships.packets.PacketUnlaunchShip;
import cuchaz.ships.render.RenderShip;
import cuchaz.ships.render.RenderShipPlaque;
import cuchaz.ships.render.RenderSupporterPlaque;
import cuchaz.ships.render.TileEntityHelmRenderer;

@NetworkMod(
	// NOTE: 16-character limit for channel names
	channels = { PacketLaunchShip.Channel, PacketShipLaunched.Channel, PacketUnlaunchShip.Channel,
		PacketRequestShipBlocks.Channel, PacketShipBlocks.Channel, PacketPilotShip.Channel,
		PacketShipBlockEvent.Channel, PacketChangedBlocks.Channel, PacketPasteShip.Channel,
		PacketEraseShip.Channel, PacketShipPlaque.Channel },
	packetHandler = PacketHandler.class,
	clientSideRequired = true, // clients without ship mod should not connect to a ships mod server
	serverSideRequired = false // clients with ships mod should connect to a non-ships mod server
)
public class Ships extends DummyModContainer
{
	public static Ships instance = null;
	public static EnhancedLogger logger = new EnhancedLogger( Logger.getLogger( "cuchaz.ships" ) );
	
	// materials
	public static final Material m_materialAirWall = new MaterialAirWall( MapColor.airColor );
	
	// (apparently the most robust id picking strategy is almost complete randomness)
	// item registration: use ids [7321-7325]
	public static final ItemPaddle m_itemPaddle = new ItemPaddle( 7321 );
	public static final ItemMagicBucket m_itemMagicBucket = new ItemMagicBucket( 7322 );
	public static final ItemMagicShipLevitator m_itemMagicShipLevitator = new ItemMagicShipLevitator( 7323 );
	public static final ItemShipClipboard m_itemShipClipboard = new ItemShipClipboard( 7324 );
	public static final ItemListOfSupporters m_itemListOfSupporters = new ItemListOfSupporters( 7325 );
	public static final ItemSupporterPlaque m_itemSupporterPlaque = new ItemSupporterPlaque( 7326 );
	public static final ItemShipEraser m_itemShipEraser = new ItemShipEraser( 7327 );
	public static final ItemShipPlaque m_itemShipPlaque = new ItemShipPlaque( 7328 );
	
	// block registration: use ids [3170-3190]
	public static final BlockShip m_blockShip = new BlockShip( 3170 );
	public static final BlockAirWall m_blockAirWall = new BlockAirWall( 3171 );
	public static final BlockHelm m_blockHelm = new BlockHelm( 3712 );
	
	// entity registration
	public static final int EntityShipId = 174;
	public static final int EntitySupporterPlaqueId = 175;
	public static final int EntityShipPlaqueId = 176;
	
	private File m_source;
	
	public Ships( )
	{
		super( new ModMetadata() );
		ModMetadata meta = getMetadata();
		meta.modId = "cuchaz.ships";
		meta.name = "Ships Mod";
		meta.version = "0.8 BETA";
		meta.authorList = Arrays.asList( new String[] { "Cuchaz" } );
		meta.description = "Build sailable ships out of blocks.";
		meta.url = "";
		
		m_source = FMLHacker.getModSource( getClass() );
		
		// make sure instance semantics are being preserved in core mod land
		if( instance != null )
		{
			throw new Error( "An instance of ships was already active!" );
		}
		instance = this;
	}
	
	@Override
	public boolean registerBus( EventBus bus, LoadController controller )
	{
		bus.register( this );
		return true;
	}
	
	@Override
	public Object getMod( )
	{
		return this;
	}
	
	@Override
    public boolean isNetworkMod( )
    {
        return true;
    }
	
	@Override
	public boolean isImmutable( )
	{
		return false;
	}
	
	@Override
	public File getSource( )
	{
		return m_source;
	}
	
	@Override
	public Class<?> getCustomResourcePackClass( )
	{
		if( getSource().isDirectory() )
		{
			return FMLFolderResourcePack.class;
		}
		else
		{
			return FMLFileResourcePack.class;
		}
	}
	
	@Subscribe
	public void construct( FMLConstructionEvent event )
	{
		// the event dispatcher swallows exceptions, so report them here
		try
		{
			// this is where the magic happens
			FMLHacker.unwrapModContainer( this );
			
			// add our container to the ASM data table
			event.getASMHarvestedData().addContainer( this );
	        
			// register for network support
			FMLNetworkHandler.instance().registerNetworkMod( this, getClass(), event.getASMHarvestedData() );
		}
		catch( RuntimeException ex )
		{
			Ships.logger.warning( ex, "Unable to construct mod container!" );
		}
	}
	
	@Subscribe
	public void load( FMLInitializationEvent event )
	{
		// the event dispatcher swallows exceptions, so report them here
		try
		{
			loadThings();
			loadLanguage();
			loadRecipes();
			
			if( event.getSide().isClient() )
			{
				// load client things if needed
				loadClient();
				
				// NOTE: the "loadClient" method gets removed from the server bytecode,
				// but as long as this if block is never run on the server,
				// this missing method reference won't cause an exception
			}
			
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
		catch( Throwable ex )
		{
			Ships.logger.warning( ex, "Exception occurred while loading mod." );
		}
	}
	
	@Subscribe
	public void serverLoad( FMLServerStartingEvent event )
	{
		// register our commands
		event.registerServerCommand( new CommandShips() );
	}
	
	@SideOnly( Side.CLIENT )
	private void loadClient( )
	{
		// set renderers
		RenderingRegistry.registerEntityRenderingHandler( EntityShip.class, new RenderShip() );
		RenderingRegistry.registerEntityRenderingHandler( EntitySupporterPlaque.class, new RenderSupporterPlaque() );
		RenderingRegistry.registerEntityRenderingHandler( EntityShipPlaque.class, new RenderShipPlaque() );
		
		// set tile entity renderers
		registerTileEntityRenderer( TileEntityHelm.class, new TileEntityHelmRenderer() );
	}
	
	@SideOnly( Side.CLIENT )
	@SuppressWarnings( "unchecked" )
	private void registerTileEntityRenderer( Class<? extends TileEntity> c, TileEntitySpecialRenderer renderer )
	{
		TileEntityRenderer.instance.specialRendererMap.put( c, renderer );
		renderer.setTileEntityRenderer( TileEntityRenderer.instance );
	}

	private void loadThings( )
	{
		// blocks
		GameRegistry.registerBlock( m_blockShip, ShipItemBlock.class, "blockShip" );
		ShipType.registerBlocks();
		GameRegistry.registerBlock( m_blockAirWall, "blockAirWall" );
		GameRegistry.registerBlock( m_blockHelm, "blockHelm" );
		
		// items
		GameRegistry.registerItem( m_itemPaddle, "paddle" );
		GameRegistry.registerItem( m_itemMagicBucket, "magicBucket" );
		GameRegistry.registerItem( m_itemMagicShipLevitator, "magicShipLevitator" );
		GameRegistry.registerItem( m_itemShipClipboard, "shipClipboard" );
		GameRegistry.registerItem( m_itemListOfSupporters, "listOfSupporters" );
		GameRegistry.registerItem( m_itemSupporterPlaque, "supporterPlaque" );
		GameRegistry.registerItem( m_itemShipEraser, "shipEraser" );
		GameRegistry.registerItem( m_itemShipPlaque, "shipPlaque" );
		
		// entities
		EntityRegistry.registerGlobalEntityID( EntityShip.class, "Ship", EntityShipId );
		EntityRegistry.registerModEntity( EntityShip.class, "Ship", EntityShipId, this, 256, 10, true );
		EntityRegistry.registerGlobalEntityID( EntitySupporterPlaque.class, "Supporter Plaque", EntitySupporterPlaqueId );
		EntityRegistry.registerModEntity( EntitySupporterPlaque.class, "Supporter Plaque", EntitySupporterPlaqueId, this, 256, 10, false );
		EntityRegistry.registerGlobalEntityID( EntityShipPlaque.class, "Ship Plaque", EntityShipPlaqueId );
		EntityRegistry.registerModEntity( EntityShipPlaque.class, "Ship Plaque", EntityShipPlaqueId, this, 256, 10, false );
		
		// tile entities
		GameRegistry.registerTileEntity( TileEntityHelm.class, "helm" );
	}
	
	private void loadLanguage( )
	{
		// block names
		LanguageRegistry.addName( m_blockAirWall, "Air Wall" );
		LanguageRegistry.addName( m_blockHelm, "Helm" );
		
		// item names
		LanguageRegistry.addName( m_itemPaddle, "Paddle" );
		LanguageRegistry.addName( m_itemMagicBucket, "Magic Bucket" );
		LanguageRegistry.addName( m_itemMagicShipLevitator, "Magic Ship Levitator" );
		LanguageRegistry.addName( m_itemShipClipboard, "Ship Clipboard" );
		LanguageRegistry.addName( m_itemListOfSupporters, "Cuchaz Interactive List of Supporters" );
		LanguageRegistry.addName( m_itemShipEraser, "Ship Eraser" );
		LanguageRegistry.addName( m_itemShipPlaque, "Ship Plaque" );
		
		// gui strings
		for( GuiString string : GuiString.values() )
		{
			LanguageRegistry.instance().addStringLocalization( string.getKey(), string.getUnlocalizedText() );
		}
	}

	private void loadRecipes( )
	{
		// NOTE: the recipes for ship blocks are in the ShipType enum
		
		SupporterPlaqueType.registerRecipes();
		
		ItemStack stickStack = new ItemStack( Item.stick );
		ItemStack goldStack = new ItemStack( Item.ingotGold );
		ItemStack ironStack = new ItemStack( Item.ingotIron );
		ItemStack paperStack = new ItemStack( Item.paper );
		
		// paddle
		GameRegistry.addRecipe(
			new ItemStack( m_itemPaddle ),
			" xx", " xx", "x  ",
			'x', stickStack
		);
		
		// magic bucket
		GameRegistry.addRecipe(
			new ItemStack( m_itemMagicBucket ),
			"   ", "x x", " x ",
			'x', goldStack
		);
		
		// helm
		GameRegistry.addRecipe(
			new ItemStack( m_blockHelm ),
			" x ", "x x", "yxy",
			'x', stickStack,
			'y', ironStack
		);
		
		// list of supporters
		GameRegistry.addRecipe(
			new ItemStack( m_itemListOfSupporters ),
			"yyy", "yxy", "yyy",
			'x', ShipType.Tiny.newItemStack(),
			'y', paperStack
		);
		
		// ship plaque
		GameRegistry.addRecipe(
			new ItemStack( m_itemShipPlaque ),
			"   ", " x ", "yyy",
			'x', new ItemStack( Item.ingotIron ),
			'y', new ItemStack( Block.planks )
		);
	}
}
