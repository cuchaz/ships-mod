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

import java.io.FileNotFoundException;
import java.util.Arrays;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import org.apache.logging.log4j.LogManager;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.net.PacketRegistry;
import cuchaz.ships.blocks.BlockAirRoof;
import cuchaz.ships.blocks.BlockAirWall;
import cuchaz.ships.blocks.BlockBerth;
import cuchaz.ships.blocks.BlockHelm;
import cuchaz.ships.blocks.BlockProjector;
import cuchaz.ships.blocks.BlockShip;
import cuchaz.ships.config.BlockProperties;
import cuchaz.ships.gui.Gui;
import cuchaz.ships.items.ItemBerth;
import cuchaz.ships.items.ItemMagicBucket;
import cuchaz.ships.items.ItemMagicShipLevitator;
import cuchaz.ships.items.ItemPaddle;
import cuchaz.ships.items.ItemProjector;
import cuchaz.ships.items.ItemShipBlock;
import cuchaz.ships.items.ItemShipClipboard;
import cuchaz.ships.items.ItemShipEraser;
import cuchaz.ships.packets.PacketBlockPropertiesOverrides;
import cuchaz.ships.packets.PacketChangedBlocks;
import cuchaz.ships.packets.PacketEraseShip;
import cuchaz.ships.packets.PacketLaunchShip;
import cuchaz.ships.packets.PacketPasteShip;
import cuchaz.ships.packets.PacketPilotShip;
import cuchaz.ships.packets.PacketPlaceProjector;
import cuchaz.ships.packets.PacketPlayerSleepInBerth;
import cuchaz.ships.packets.PacketRequestShipBlocks;
import cuchaz.ships.packets.PacketShipBlockEvent;
import cuchaz.ships.packets.PacketShipBlocks;
import cuchaz.ships.packets.PacketShipLaunched;
import cuchaz.ships.packets.PacketUnlaunchShip;
import cuchaz.ships.render.RenderShip;
import cuchaz.ships.render.TileEntityHelmRenderer;
import cuchaz.ships.render.TileEntityProjectorRenderer;

@Mod(
	modid = Ships.Id,
	name = "Ships Mod",
	version = Ships.Version,
	dependencies = "required-after:Forge@[10.13.2.1230,);required-after:cuchaz.ships.core;required-after:cuchaz.cuchazinteractive",
	acceptedMinecraftVersions = "[1.7.10,)"
)
public class Ships {
	
	public static final String Id = "cuchaz.ships";
	public static final String Version = "1.7.10-1.0.3";
	public static final String VersionUrl = "http://www.cuchazinteractive.com/ships/versions.json";
	
	@Mod.Instance(Id)
	public static Ships instance = null;
	
	public static EnhancedLogger logger = new EnhancedLogger(LogManager.getLogger(Id));
	public static PacketRegistry net = null;
	
	// materials
	public static final Material m_materialAirWall = new MaterialAirWall(MapColor.airColor);
	
	// blocks
	public static final BlockShip m_blockShip = new BlockShip();
	public static final BlockAirWall m_blockAirWall = new BlockAirWall();
	public static final BlockHelm m_blockHelm = new BlockHelm();
	public static final BlockBerth m_blockBerth = new BlockBerth();
	public static final BlockAirWall m_blockAirRoof = new BlockAirRoof();
	public static final BlockProjector m_blockProjector = new BlockProjector();
	
	// items
	public static final ItemPaddle m_itemPaddle = new ItemPaddle();
	public static final ItemMagicBucket m_itemMagicBucket = new ItemMagicBucket();
	public static final ItemMagicShipLevitator m_itemMagicShipLevitator = new ItemMagicShipLevitator();
	public static final ItemShipClipboard m_itemShipClipboard = new ItemShipClipboard();
	public static final ItemShipEraser m_itemShipEraser = new ItemShipEraser();
	public static final ItemBerth m_itemBerth = new ItemBerth();
	public static ItemProjector m_itemProjector = null;
	
	// entities
	public static final int EntityShipId = 0;
	
	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		
		ModMetadata meta = event.getModMetadata();
		meta.autogenerated = false;
		meta.description = "Build sailable ships out of blocks.";
		meta.url = "http://www.cuchazinteractive.com/ships";
		meta.credits = "Created by Cuchaz";
		meta.updateUrl = meta.url;
		meta.authorList = Arrays.asList("Cuchaz");
	}
	
	@Mod.EventHandler
	public void load(FMLInitializationEvent event) {
		
		// the event dispatcher swallows exceptions, so report them here
		try {
			// check version
			FMLInterModComms.sendRuntimeMessage(Id, "VersionChecker", "addVersionCheck", VersionUrl);
			
			// register for network support
			net = new PacketRegistry(Id);
			net.register(new PacketLaunchShip());
			net.register(new PacketShipLaunched());
			net.register(new PacketUnlaunchShip());
			net.register(new PacketRequestShipBlocks());
			net.register(new PacketShipBlocks());
			net.register(new PacketPilotShip());
			net.register(new PacketShipBlockEvent());
			net.register(new PacketChangedBlocks());
			net.register(new PacketPasteShip());
			net.register(new PacketEraseShip());
			net.register(new PacketPlayerSleepInBerth());
			net.register(new PacketBlockPropertiesOverrides());
			net.register(new PacketPlaceProjector());
			
			// register for forge events
			MinecraftForge.EVENT_BUS.register(this);
			
			loadThings();
			loadRecipes();
			
			if (event.getSide().isClient()) {
				// load client things if needed
				loadClient();
				
				// NOTE: the "loadClient" method gets removed from the server bytecode,
				// but as long as this if block is never run on the server,
				// this missing method reference won't cause an exception
			}
			
			// GUI hooks
			NetworkRegistry.INSTANCE.registerGuiHandler(this, Gui.Handler);
			
		} catch (Throwable ex) {
			Ships.logger.warning(ex, "Exception occurred while loading mod.");
		}
	}
	
	@EventHandler
	public void serverLoad(FMLServerStartingEvent event) {
		
		// register our commands
		event.registerServerCommand(new CommandShips());
		
		try {
			// load the block properties
			BlockProperties.readConfigFile();
		} catch (FileNotFoundException ex) {
			logger.warning("Unable to read block properties", ex);
		}
	}
	
	@SideOnly(Side.CLIENT)
	private void loadClient() {
		
		// set renderers
		RenderShip shipRenderer = new RenderShip();
		RenderingRegistry.registerEntityRenderingHandler(EntityShip.class, shipRenderer);
		
		// set tile entity renderers
		registerTileEntityRenderer(TileEntityHelm.class, new TileEntityHelmRenderer());
		registerTileEntityRenderer(TileEntityProjector.class, new TileEntityProjectorRenderer(shipRenderer));
	}
	
	@SideOnly(Side.CLIENT)
	@SuppressWarnings("unchecked")
	private void registerTileEntityRenderer(Class<? extends TileEntity> c, TileEntitySpecialRenderer renderer) {
		TileEntityRendererDispatcher.instance.mapSpecialRenderers.put(c, renderer);
		renderer.func_147497_a(TileEntityRendererDispatcher.instance);
	}
	
	private void loadThings() {
		
		// blocks
		// sadly, these names look stupid in their id-name format:
		// cuchaz.ships:blockShip
		// but it's too late to change them now. =P
		GameRegistry.registerBlock(m_blockShip, ItemShipBlock.class, "blockShip");
		GameRegistry.registerBlock(m_blockAirWall, "blockAirWall");
		GameRegistry.registerBlock(m_blockHelm, "blockHelm");
		GameRegistry.registerBlock(m_blockBerth, "blockBerth");
		GameRegistry.registerBlock(m_blockAirRoof, "blockAirRoof");
		GameRegistry.registerBlock(m_blockProjector, ItemProjector.class, "blockProjector");
		
		// items
		GameRegistry.registerItem(m_itemPaddle, "paddle");
		GameRegistry.registerItem(m_itemMagicBucket, "magicBucket");
		GameRegistry.registerItem(m_itemMagicShipLevitator, "magicShipLevitator");
		GameRegistry.registerItem(m_itemShipClipboard, "shipClipboard");
		GameRegistry.registerItem(m_itemShipEraser, "shipEraser");
		GameRegistry.registerItem(m_itemBerth, "berth");
		
		// we can't register these items directly, so get them after registration
		m_itemProjector = (ItemProjector)Item.getItemFromBlock(m_blockProjector);
		
		// entities
		EntityRegistry.registerModEntity(EntityShip.class, "Ship", EntityShipId, this, 256, 10, true);
		
		// tile entities
		GameRegistry.registerTileEntity(TileEntityHelm.class, "helm");
		GameRegistry.registerTileEntity(TileEntityProjector.class, "projector");
	}
	
	private void loadRecipes() {
		ShipType.registerRecipes();
		
		ItemStack stickStack = new ItemStack(Items.stick);
		ItemStack goldStack = new ItemStack(Items.gold_ingot);
		ItemStack ironStack = new ItemStack(Items.iron_ingot);
		ItemStack paperStack = new ItemStack(Items.paper);
		ItemStack glassStack = new ItemStack(Blocks.glass);
		ItemStack lapisStack = new ItemStack(Items.dye, 1, 4);
		ItemStack boatStack = new ItemStack(Items.boat);
		
		// paddle
		GameRegistry.addRecipe(new ItemStack(m_itemPaddle), 
			" xx",
			" xx",
			"x  ",
			'x', stickStack
		);
		
		// magic bucket
		GameRegistry.addRecipe(new ItemStack(m_itemMagicBucket),
			"   ",
			"x x",
			" x ",
			'x', goldStack
		);
		
		// helm
		GameRegistry.addRecipe(new ItemStack(m_blockHelm),
			" x ",
			"x x",
			"yxy",
			'x', stickStack,
			'y', ironStack
		);
		
		// berth
		for (int i = 0; i < 16; i++) {
			GameRegistry.addRecipe(new ItemStack(m_itemBerth),
				"   ",
				"xxx",
				"yzy",
				'x', new ItemStack(Blocks.wool, 1, i),
				'y', stickStack,
				'z', goldStack
			);
		}
		
		// ship clipboard
		GameRegistry.addRecipe(new ItemStack(m_itemShipClipboard),
			"xxx",
			"xxx",
			"yzy",
			'x', paperStack,
			'y', stickStack,
			'z', ShipType.Tiny.newItemStack()
		);
		
		// ship projector
		GameRegistry.addRecipe(new ItemStack(m_blockProjector),
			" x ",
			"yzy",
			" w ",
			'x', glassStack,
			'y', ironStack,
			'z', lapisStack,
			'w', boatStack
		);
	}
	
	@SubscribeEvent
	public void onEntityJoin(EntityJoinWorldEvent event) {
		if (event.world.isRemote) {
			// ignore on client
			return;
		}
		
		// is this a player?
		EntityPlayerMP player = null;
		if (event.entity instanceof EntityPlayerMP) {
			player = (EntityPlayerMP)event.entity;
		}
		if (player == null) {
			return;
		}
		
		// send block overrides to the client
		net.getDispatch().sendTo(new PacketBlockPropertiesOverrides(BlockProperties.getOverrides()), player);
	}
}
