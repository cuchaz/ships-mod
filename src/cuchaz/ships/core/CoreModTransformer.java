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
package cuchaz.ships.core;

import java.util.Arrays;
import java.util.List;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class CoreModTransformer implements IClassTransformer {
	
	private static ObfuscationAwareAdapter m_adapterHead = null;
	private static ObfuscationAwareAdapter m_adapterTail = null;
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] classData) {
		
		if (classData == null) {
			// if class data is null, ignore it
			// if the classloader crashes and burns, it's not our fault
			// if you're investigating a burning classloader and you see this function in the stack trace,
			// Ships Mod is not causing the problem! =P
			return null;
		}
		
		try {
			// don't transform some important stuff
			List<String> privilegedPackages = Arrays.asList("cuchaz.ships.", "cuchaz.modsShared", "net.minecraftforge.", "cpw.");
			for (String privilegedPackage : privilegedPackages) {
				if (name.startsWith(privilegedPackage)) {
					return classData;
				}
			}
			
			// do we know about the obfuscation state yet?
			if (CoreModPlugin.isObfuscatedEnvironment == null) {
				return classData;
			}
			
			// set up the adapter chain
			if (m_adapterHead == null || m_adapterTail == null) {
				ObfuscationAwareAdapter adapter = new TileEntityInventoryAdapter(Opcodes.ASM4, null, CoreModPlugin.isObfuscatedEnvironment);
				m_adapterHead = adapter;
				adapter = new EntityMoveAdapter(Opcodes.ASM4, adapter, CoreModPlugin.isObfuscatedEnvironment);
				adapter = new WorldAdapter(Opcodes.ASM4, adapter, CoreModPlugin.isObfuscatedEnvironment);
				adapter = new EntityRendererAdapter(Opcodes.ASM4, adapter, CoreModPlugin.isObfuscatedEnvironment);
				adapter = new EntityLadderAdapter(Opcodes.ASM4, adapter, CoreModPlugin.isObfuscatedEnvironment);
				adapter = new EntityDistanceAdapter(Opcodes.ASM4, adapter, CoreModPlugin.isObfuscatedEnvironment);
				adapter = new ServerConfigurationManagerAdapter(Opcodes.ASM4, adapter, CoreModPlugin.isObfuscatedEnvironment);
				adapter = new EntityPlayerAdapter(Opcodes.ASM4, adapter, CoreModPlugin.isObfuscatedEnvironment);
				m_adapterTail = adapter;
			}
			
			// run the transformations
			ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			m_adapterHead.setPreviousClassVisitor(classWriter);
			new ClassReader(classData).accept(m_adapterTail, 0);
			return classWriter.toByteArray();
			
		} catch (Throwable t) {
			
			// NOTE: using the logger here causes class loading circles. Need to use stdout
			System.out.println("Exception occurred while transforming class " + name + ":" + transformedName + ". This class has been skipped and left un-transformed.");
			t.printStackTrace(System.out);
			return classData;
		}
	}
}
