/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.core;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ServerConfigurationManagerAdapter extends ObfuscationAwareAdapter {
	
	private final String ServerConfigurationManagerClassName;
	private final String EntityPlayerMPClassName;
	private final String ChunkProviderServerClassName;
	
	private String m_className;
	
	public ServerConfigurationManagerAdapter(int api, ClassVisitor cv, boolean isObfuscatedEnvironment) {
		super(api, cv, isObfuscatedEnvironment);
		
		m_className = null;
		
		// cache the runtime class names
		ServerConfigurationManagerClassName = "net/minecraft/server/management/ServerConfigurationManager";
		EntityPlayerMPClassName = "net/minecraft/entity/player/EntityPlayerMP";
		ChunkProviderServerClassName = "net/minecraft/world/gen/ChunkProviderServer";
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		
		m_className = name;
	}
	
	@Override
	public MethodVisitor visitMethod(int access, final String methodName, String methodDesc, String signature, String[] exceptions) {
		if (m_className.equals(ServerConfigurationManagerClassName)) {
			// EntityPlayerMP respawnPlayer( EntityPlayerMP, int, boolean )
			// func_72368_a
			if (methodDesc.equals(String.format("(L%s;IZ)L%s;", EntityPlayerMPClassName, EntityPlayerMPClassName)) && methodName.equals(getRuntimeMethodName(m_className, "respawnPlayer", "func_72368_a"))) {
				return new MethodVisitor(api, cv.visitMethod(access, methodName, methodDesc, signature, exceptions)) {
					
					private int m_newPlayerIndex = 0;
					
					@Override
					public void visitVarInsn(int opcode, int index) {
						if (opcode == Opcodes.ALOAD) {
							// we can't actually tell what type is being loaded here
							// but in all the bytecode I've seen for respawnPlayer,
							// the ALOAD right before the loadChunk call is the index of the new player
							m_newPlayerIndex = index;
						}
						
						super.visitVarInsn(opcode, index);
					}
					
					@Override
					public void visitMethodInsn(int opcode, String calledOwner, String calledName, String calledDesc) {
						// did we find the variable for the new player yet?
						if (m_newPlayerIndex > 0) {
							// look for a call to:
							// ChunkProviderServer.loadChunk( int, int )
							// func_73158_c
							if (calledOwner.equals(ChunkProviderServerClassName) && calledName.equals(getRuntimeMethodName(ChunkProviderServerClassName, "loadChunk", "func_73158_c"))) {
								// insert a call to:
								// ShipIntermediary.onPlayerRespawn( oldPlayer, newPlayer, dimension )
								mv.visitVarInsn(Opcodes.ALOAD, 1); // old player
								mv.visitVarInsn(Opcodes.ALOAD, m_newPlayerIndex); // new player
								mv.visitVarInsn(Opcodes.ILOAD, 2); // dimension
								mv.visitMethodInsn(Opcodes.INVOKESTATIC, ShipIntermediary.Path, "onPlayerRespawn", String.format("(L%s;L%s;I)V", EntityPlayerMPClassName, EntityPlayerMPClassName));
							}
						}
						
						// also call the original method
						super.visitMethodInsn(opcode, calledOwner, calledName, calledDesc);
					}
				};
			}
		}
		
		return super.visitMethod(access, methodName, methodDesc, signature, exceptions);
	}
}
