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

public class EntityRendererAdapter extends ObfuscationAwareAdapter {
	
	private final String EntityRendererClassName;
	
	private String m_className;
	
	public EntityRendererAdapter(int api, ClassVisitor cv, boolean isObfuscatedEnvironment) {
		super(api, cv, isObfuscatedEnvironment);
		
		// cache the runtime class names
		EntityRendererClassName = "net/minecraft/client/renderer/EntityRenderer";
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		
		m_className = name;
	}
	
	@Override
	public MethodVisitor visitMethod(int access, final String methodName, String methodDesc, String signature, String[] exceptions) {
		if (m_className.equals(EntityRendererClassName)) {
			// public void getMouseOver( float )
			// func_78473_a
			if (methodDesc.equals("(F)V") && methodName.equals(getRuntimeMethodName(m_className, "getMouseOver", "func_78473_a"))) {
				return new MethodVisitor(api, cv.visitMethod(access, methodName, methodDesc, signature, exceptions)) {
					
					@Override
					public void visitInsn(int opcode) {
						if (opcode == Opcodes.RETURN) {
							// just before the final return statement, insert our call
							mv.visitMethodInsn(Opcodes.INVOKESTATIC, ShipIntermediaryClient.Path, "onFoundHit", String.format("()V"));
						}
						
						// and add the return as normal
						super.visitInsn(opcode);
					}
				};
			}
		}
		
		return super.visitMethod(access, methodName, methodDesc, signature, exceptions);
	}
}
