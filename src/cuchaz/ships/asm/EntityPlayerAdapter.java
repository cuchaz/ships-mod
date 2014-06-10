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
package cuchaz.ships.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class EntityPlayerAdapter extends ObfuscationAwareAdapter
{
	private final String EntityPlayerClassName;
	
	private String m_className;
	
	public EntityPlayerAdapter( int api, ClassVisitor cv, boolean isObfuscatedEnvironment )
	{
		super( api, cv, isObfuscatedEnvironment );
		
		m_className = null;
		
		// cache the runtime class names
		EntityPlayerClassName = getRuntimeClassName( "net/minecraft/entity/player/EntityPlayer" );
	}
	
	@Override
	public void visit( int version, int access, String name, String signature, String superName, String[] interfaces )
	{
		super.visit( version, access, name, signature, superName, interfaces );
		
		m_className = name;
	}
	
	@Override
	public MethodVisitor visitMethod( int access, final String methodName, String methodDesc, String signature, String[] exceptions )
	{
		if( m_className.equals( EntityPlayerClassName ) )
		{
			// void wakeUpPlayer( boolean, boolean, boolean )
			// func_70999_a
			if( methodDesc.equals( "(ZZZ)V" )
				&& methodName.equals( getRuntimeMethodName( m_className, "wakeUpPlayer", "func_70999_a" ) ) )
			{
				return new MethodVisitor( api, cv.visitMethod( access, methodName, methodDesc, signature, exceptions ) )
				{
					@Override
					public void visitCode( )
					{
						// call ShipIntermediary.onPlayerWakeUp( wasSleepSuccessful )
						mv.visitVarInsn( Opcodes.ALOAD, 0 ); // this
						mv.visitVarInsn( Opcodes.ILOAD, 3 ); // wasSleepSuccessful
						mv.visitMethodInsn( Opcodes.INVOKESTATIC, ShipIntermediary.Path, "onPlayerWakeUp", String.format( "(L%s;Z)V", EntityPlayerClassName ) );
					}
				};
			}
		}

		return super.visitMethod( access, methodName, methodDesc, signature, exceptions );
	}
}
