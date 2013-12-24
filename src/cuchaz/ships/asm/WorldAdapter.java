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
package cuchaz.ships.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class WorldAdapter extends ObfuscationAwareAdapter
{
	private final String WorldClassName;
	private final String EntityClassName;
	private final String AxisAlignedBBClassName;
	private final String IEntitySelectorClassName;
	private final String ListClassName = "java/util/List";
	
	private String m_className;
	
	public WorldAdapter( int api, ClassVisitor cv, boolean isObfuscatedEnvironment )
	{
		super( api, cv, isObfuscatedEnvironment );
		
		m_className = null;
		
		// cache the runtime class names
		WorldClassName = getRuntimeClassName( "net/minecraft/world/World" );
		EntityClassName = getRuntimeClassName( "net/minecraft/entity/Entity" );
		AxisAlignedBBClassName = getRuntimeClassName( "net/minecraft/util/AxisAlignedBB" );
		IEntitySelectorClassName = getRuntimeClassName( "net/minecraft/command/IEntitySelector" );
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
		if( m_className.equals( WorldClassName ) )
		{
			// public List getEntitiesWithinAABBExcludingEntity(Entity par1Entity, AxisAlignedBB par2AxisAlignedBB, IEntitySelector par3IEntitySelector)
			// func_94576_a
			if( methodDesc.equals( String.format( "(L%s;L%s;L%s;)L%s;", EntityClassName, AxisAlignedBBClassName, IEntitySelectorClassName, ListClassName ) )
				&& methodName.equals( getRuntimeMethodName( m_className, "getEntitiesWithinAABBExcludingEntity", "func_94576_a" ) ) )
			{
				return new MethodVisitor( api, cv.visitMethod( access, methodName, methodDesc, signature, exceptions ) )
				{
					@Override
					public void visitInsn( int opcode )
					{
						if( opcode == Opcodes.ARETURN )
						{
							// we're hooking just before the return statement
							// to call this: ShipIntermediary.getShipsWithinAABB( list, AxisAlignedBB, IEntitySelector )
							// current on stack: list
							// add the three extra arguments, then call the intermediary, which puts the list back on the stack
							mv.visitVarInsn( Opcodes.ALOAD, 0 );
							mv.visitVarInsn( Opcodes.ALOAD, 2 );
							mv.visitVarInsn( Opcodes.ALOAD, 3 );
							mv.visitMethodInsn( Opcodes.INVOKESTATIC, ShipIntermediary.Path, "getShipsWithinAABB", String.format( "(L%s;L%s;L%s;L%s;)L%s;", ListClassName, WorldClassName, AxisAlignedBBClassName, IEntitySelectorClassName, ListClassName ) );
						}
						
						super.visitInsn( opcode );
					}
				};
			}
		}
		
		return super.visitMethod( access, methodName, methodDesc, signature, exceptions );
	}
}
