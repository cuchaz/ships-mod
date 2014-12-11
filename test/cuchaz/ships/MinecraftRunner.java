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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.launchwrapper.LaunchClassLoader;
import cpw.mods.fml.common.Loader;

public abstract class MinecraftRunner
{
	// to run Minecraft-enabled code in a non-minecraft environment, we have to do some setup with classloaders and Forge state
	
	static
	{
		if( isMinecraftEnvironment() )
		{
			// init the FML loader
			Loader.injectData( null, null, null, null, Loader.MC_VERSION, null, null, null );
			
			// force some Minecraft classes to run their static initializers
			@SuppressWarnings( "unused" )
			Object o;
			o = Blocks.air;
			o = Items.apple;
			
			// init the mod
			o = Ships.instance;
		}
	}
	
	public void run( Object ... args )
	throws Exception
	{
		// build the classloader
		LaunchClassLoader cl = new LaunchClassLoader( new URL[] {} );
		for( String path : System.getProperty( "java.class.path" ).split( ":" ) )
		{
			cl.addURL( new File( path ).toURI().toURL() );
		}
		
		// get an instance of the runnable class
		String outerClassName = Thread.currentThread().getStackTrace()[2].getClassName();
		Class<?> classOuter = cl.loadClass( outerClassName );
		Class<?> classRunnable = cl.loadClass( getClass().getName() );
		Class<?>[] constructorArgTypes = new Class<?>[args.length + 1];
		constructorArgTypes[0] = classOuter;
		for( int i=0; i<args.length; i++ )
		{
			constructorArgTypes[i+1] = args[i].getClass();
		}
		Constructor<?> constructor = classRunnable.getDeclaredConstructor( constructorArgTypes );
		constructor.setAccessible( true );
		Object[] constructorArgs = new Object[constructorArgTypes.length];
		constructorArgs[0] = classOuter.newInstance();
		for( int i=0; i<args.length; i++ )
		{
			constructorArgs[i+1] = args[i];
		}
		Object runnableInstance = constructor.newInstance( constructorArgs );
		
		// call the method
		Method method = classRunnable.getDeclaredMethod( "onRun" );
		method.setAccessible( true );
		method.invoke( runnableInstance );
	}
	
	public abstract void onRun( ) throws Exception;
	
	private static boolean isMinecraftEnvironment( )
	{
		return TestShipSpeed.class.getClassLoader() instanceof LaunchClassLoader;
	}
}
