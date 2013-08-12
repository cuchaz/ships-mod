package cuchaz.ships.asm;

import java.util.Arrays;

import com.google.common.eventbus.EventBus;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;

public class CoreModContainer extends DummyModContainer
{
	public CoreModContainer( )
	{
		super( new ModMetadata() );
		ModMetadata meta = getMetadata();
		meta.modId = "cuchaz.ships.core";
		meta.name = "Ships (core)";
		meta.version = "0.1";
		meta.authorList = Arrays.asList( new String[] { "Cuchaz" } );
		meta.description = "Core extensions for Ships mod";
		meta.url = "";
	}
	
	@Override
	public boolean registerBus( EventBus bus, LoadController controller )
	{
		bus.register( this );
		return true;
	}
}
