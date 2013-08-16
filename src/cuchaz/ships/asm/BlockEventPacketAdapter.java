package cuchaz.ships.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class BlockEventPacketAdapter extends ClassVisitor
{
	public BlockEventPacketAdapter( int api, ClassVisitor cv )
	{
		super( api, cv );
	}
	
	@Override
	public MethodVisitor visitMethod( int access, final String methodName, String methodDesc, String signature, String[] exceptions )
	{
		// UNDONE: make the adapater add a ship entityId field to the packet
		return super.visitMethod( access, methodName, methodDesc, signature, exceptions );
	}
}
