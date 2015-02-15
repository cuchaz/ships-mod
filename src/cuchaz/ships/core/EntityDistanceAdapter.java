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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class EntityDistanceAdapter extends ObfuscationAwareAdapter {
	
	private final String EntityClassName;
	
	private String m_className;
	
	public EntityDistanceAdapter(int api, ClassVisitor cv, boolean isObfuscatedEnvironment) {
		super(api, cv, isObfuscatedEnvironment);
		
		m_className = null;
		
		// cache the runtime class names
		EntityClassName = "net/minecraft/entity/Entity";
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		
		m_className = name;
	}
	
	@Override
	public MethodVisitor visitMethod(int access, final String methodName, String methodDesc, String signature, String[] exceptions) {
		if (m_className.equals(EntityClassName)) {
			// double getDistanceSqToEntity( Entity )
			// func_70068_e
			if (methodDesc.equals(String.format("(L%s;)D", EntityClassName)) && methodName.equals(getRuntimeMethodName(m_className, "getDistanceSqToEntity", "func_70068_e"))) {
				return new MethodVisitor(api, cv.visitMethod(access, methodName, methodDesc, signature, exceptions)) {
					
					@Override
					public void visitCode() {
						// insert a call to our intermediate
						// nothing on the stack, push this to stack, push the target entity to the stack, then invoke intermediary
						mv.visitVarInsn(Opcodes.ALOAD, 0);
						mv.visitVarInsn(Opcodes.ALOAD, 1);
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, ShipIntermediary.Path, "getDistanceSqToEntity", String.format("(L%s;L%s;)D", EntityClassName, EntityClassName));
						
						// double return value is on stack now, if it's >= 0, return it
						// or rather, if < 0, skip to the rest of the method
						Label label = new Label();
						mv.visitInsn(Opcodes.DUP2);
						mv.visitInsn(Opcodes.DCONST_0);
						mv.visitInsn(Opcodes.DCMPL); // pushes 1 if val > 0, - if val == 0, -1 if val < 0
						mv.visitJumpInsn(Opcodes.IFLT, label);
						mv.visitInsn(Opcodes.DRETURN);
						mv.visitLabel(label);
						mv.visitInsn(Opcodes.POP2);
						
						super.visitCode();
					}
				};
			}
		}
		
		return super.visitMethod(access, methodName, methodDesc, signature, exceptions);
	}
}
