package cuchaz.ships.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class EntityMoveAdapter extends ClassVisitor
{
	private static final String EntityClassName = "net/minecraft/entity/Entity";
	
	private String m_name;
	
	public EntityMoveAdapter( int api, ClassVisitor cv )
	{
		super( api, cv );
	}
	
	@Override
	public void visit( int version, int access, String name, String signature, String superName, String[] interfaces )
	{
		super.visit( version, access, name, signature, superName, interfaces );
		
		// save the class details for later visit methods
		m_name = name;
	}
	
	@Override
	public MethodVisitor visitMethod( int access, final String methodName, String methodDesc, String signature, String[] exceptions )
	{
		return new MethodVisitor( api, cv.visitMethod( access, methodName, methodDesc, signature, exceptions ) )
		{
			@Override
			public void visitMethodInsn( int opcode, String calledOwner, String calledName, String calledDesc )
			{
				// should we transform this method call?
				if( opcode == Opcodes.INVOKEVIRTUAL && InheritanceUtils.extendsClass( calledOwner, EntityClassName ) && calledName.equals( "moveEntity" ) && calledDesc.equals( "(DDD)V" ) )
				{
					mv.visitMethodInsn( Opcodes.INVOKESTATIC, ShipIntermediary.Path, "onEntityMove", String.format( "(L%s;DDD)V", EntityClassName ) );
				}
				else
				{
					super.visitMethodInsn( opcode, calledOwner, calledName, calledDesc );
				}
			}
		};
	}
}
