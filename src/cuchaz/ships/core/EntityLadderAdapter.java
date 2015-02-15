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

public class EntityLadderAdapter extends ObfuscationAwareAdapter {
	
	private final String EntityLivingBaseClassName;
	
	private String m_className;
	
	public EntityLadderAdapter(int api, ClassVisitor cv, boolean isObfuscatedEnvironment) {
		super(api, cv, isObfuscatedEnvironment);
		
		m_className = null;
		
		// cache the runtime class names
		EntityLivingBaseClassName = "net/minecraft/entity/EntityLivingBase";
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		
		m_className = name;
	}
	
	@Override
	public MethodVisitor visitMethod(int access, final String methodName, String methodDesc, String signature, String[] exceptions) {
		if (m_className.equals(EntityLivingBaseClassName)) {
			// boolean isOnLadder()
			// func_70617_f_
			if (methodDesc.equals("()Z") && methodName.equals(getRuntimeMethodName(m_className, "isOnLadder", "func_70617_f_"))) {
				return new MethodVisitor(api, cv.visitMethod(access, methodName, methodDesc, signature, exceptions)) {
					
					@Override
					public void visitCode() {
						// insert a call to our intermediate
						// nothing on the stack, push this to stack, then invoke intermediary
						mv.visitVarInsn(Opcodes.ALOAD, 0);
						mv.visitMethodInsn(Opcodes.INVOKESTATIC, ShipIntermediary.Path, "isEntityOnShipLadder", String.format("(L%s;)Z", EntityLivingBaseClassName));
						
						// boolean return value is on stack now, if it's true, return it
						// or rather, if it's false, skip to the rest of the method
						// isTrue = val != 0
						Label label = new Label();
						mv.visitInsn(Opcodes.DUP);
						mv.visitJumpInsn(Opcodes.IFEQ, label);
						mv.visitInsn(Opcodes.IRETURN);
						mv.visitLabel(label);
						mv.visitInsn(Opcodes.POP);
						
						super.visitCode();
					}
				};
			}
		}
		
		return super.visitMethod(access, methodName, methodDesc, signature, exceptions);
	}
}
