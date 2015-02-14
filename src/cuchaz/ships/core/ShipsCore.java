package cuchaz.ships.core;

import java.util.Arrays;

import com.google.common.eventbus.EventBus;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cuchaz.ships.Ships;


public class ShipsCore extends DummyModContainer {
	
	public ShipsCore() {
		super(new ModMetadata());
		
		ModMetadata meta = getMetadata();
		meta.modId = "cuchaz.ships.core";
		meta.name = "Ships Mod Core";
		meta.version = Ships.Version;
		meta.authorList = Arrays.asList("Cuchaz");
		meta.description = "Build sailable ships out of blocks.";
		meta.url = "http://www.cuchazinteractive.com/ships";
		meta.credits = "Created by Cuchaz";
	}
	
	@Override
	public boolean registerBus(EventBus bus, LoadController controller) {
		// need to register to be active
		bus.register(this);
		return true;
	}
}
