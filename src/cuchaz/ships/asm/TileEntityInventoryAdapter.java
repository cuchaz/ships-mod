package cuchaz.ships.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TileEntityInventoryAdapter extends ClassVisitor
{
	private static final String InventoryInterfaceName = "net/minecraft/inventory/IInventory";
	private static final String TileEntityClassName = "net/minecraft/tileentity/TileEntity";
	private static final String PlayerClassName = "net/minecraft/entity/player/EntityPlayer";
	
	private static final String Intermediary = "cuchaz/ships/asm/ShipTileEntityIntermediary";
	
	private String m_superName;
	private String[] m_interfaces;
	
	public TileEntityInventoryAdapter( int api, ClassVisitor cv )
	{
		super( api, cv );
	}
	
	@Override
	public void visit( int version, int access, String name, String signature, String superName, String[] interfaces )
	{
		super.visit( version, access, name, signature, superName, interfaces );
		
		// save the class details for later visit methods
		m_superName = superName;
		m_interfaces = interfaces;
	}
	
	@Override
	public MethodVisitor visitMethod( int access, final String methodName, String methodDesc, String signature, String[] exceptions )
	{
		// should we transform this method?
		if( implementsInterface( InventoryInterfaceName ) && m_superName.equals( TileEntityClassName ) && methodName.equals( "isUseableByPlayer" ) && methodDesc.equals( String.format( "(L%s;)Z", PlayerClassName ) ) )
		{
			return new MethodVisitor( api, cv.visitMethod( access, methodName, methodDesc, signature, exceptions ) )
			{
				@Override
				public void visitMethodInsn( int opcode, String calledOwner, String calledName, String calledDesc )
				{
					// should we transform this method call?
					if( opcode == Opcodes.INVOKEVIRTUAL && calledOwner.equals( PlayerClassName ) && calledName.equals( "getDistanceSq" ) && calledDesc.equals( "(DDD)D" ) )
					{
						// we're replacing this method call
						// invokevirtual net.minecraft.entity.player.EntityPlayer.getDistanceSq(double, double, double) : double [187]
						// with
						// ShipTileIntermediary.getEntityDistanceSq( player, tileEntity )
						// plan:
						// currently on the argument stack: player, x, y, z
						// so just push the tileEntity on and invoke the intermediary method
						mv.visitVarInsn( Opcodes.ALOAD, 0 );
						mv.visitMethodInsn(
							Opcodes.INVOKESTATIC,
							Intermediary,
							"getEntityDistanceSq",
							String.format( "(L%s;DDDL%s;)D", PlayerClassName, TileEntityClassName )
						);
					}
					else
					{
						super.visitMethodInsn( opcode, calledOwner, calledName, calledDesc );
					}
				}
			};
		}
		else
		{
			return super.visitMethod( access, methodName, methodDesc, signature, exceptions );
		}
	}
	
	private boolean implementsInterface( String targetInterfaceName )
	{
		for( String interfaceName : m_interfaces )
		{
			if( interfaceName.equalsIgnoreCase( targetInterfaceName ) )
			{
				return true;
			}
		}
		return false;
	}
}
