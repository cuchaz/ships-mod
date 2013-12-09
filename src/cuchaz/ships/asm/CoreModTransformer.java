/*******************************************************************************
 * Copyright (c) 2013 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.asm;

import java.util.Arrays;
import java.util.List;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class CoreModTransformer implements IClassTransformer
{
	@Override
	public byte[] transform( String name, String transformedName, byte[] classData )
	{
		if( classData == null )
		{
			throw new Error( "Received no class data for " + name + "!" );
		}
		
		// don't transform some important stuff
		List<String> privilegedPackages = Arrays.asList( "cuchaz.ships.", "net.minecraftforge.", "cpw." );
		for( String privilegedPackage : privilegedPackages )
		{
			if( name.startsWith( privilegedPackage ) )
			{
				return classData;
			}
		}
		
		// do we know about the obfuscation state yet?
		if( CoreModPlugin.isObfuscatedEnvironment == null )
		{
			return classData;
		}
		
		ClassWriter writer = new ClassWriter( ClassWriter.COMPUTE_MAXS );
		
		// set up the adapter chain
		ClassVisitor tailAdapter = writer;
		tailAdapter = new TileEntityInventoryAdapter( Opcodes.ASM4, tailAdapter, CoreModPlugin.isObfuscatedEnvironment );
		tailAdapter = new EntityMoveAdapter( Opcodes.ASM4, tailAdapter, CoreModPlugin.isObfuscatedEnvironment );
		tailAdapter = new WorldAdapter( Opcodes.ASM4, tailAdapter, CoreModPlugin.isObfuscatedEnvironment );
		
		// run the transformations
		new ClassReader( classData ).accept( tailAdapter, 0 );
		return writer.toByteArray();
	}
}
