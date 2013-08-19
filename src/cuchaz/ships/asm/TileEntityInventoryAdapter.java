package cuchaz.ships.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TileEntityInventoryAdapter extends ClassVisitor
{
	private static final String InventoryInterfaceName = "net/minecraft/inventory/IInventory";
	private static final String TileEntityClassName = "net/minecraft/tileentity/TileEntity";
	private static final String WorkbenchContainerClassName = "net/minecraft/inventory/ContainerWorkbench";
	private static final String PlayerClassName = "net/minecraft/entity/player/EntityPlayer";
	private static final String InventoryPlayerClassName = "net/minecraft/entity/player/InventoryPlayer";
	private static final String WorldClassName = "net/minecraft/world/World";
	
	private static final String Intermediary = "cuchaz/ships/asm/ShipIntermediary";
	
	private String m_name;
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
		m_name = name;
		m_superName = superName;
		m_interfaces = interfaces;
	}
	
	@Override
	public MethodVisitor visitMethod( int access, final String methodName, String methodDesc, String signature, String[] exceptions )
	{
		// UNDONE: generalize to all world-based container subclasses
		// like ContainerRepair, ContainerEnchantment
		
		// should we transform this method?
		final boolean isInventoryMethod = implementsInterface( InventoryInterfaceName ) && m_superName.equals( TileEntityClassName ) && methodName.equals( "isUseableByPlayer" );
		final boolean isWorkbenchContainerMethod = m_name.equals( WorkbenchContainerClassName ) && methodName.equals( "canInteractWith" );
		final boolean isContainerConstructor = m_name.equals( WorkbenchContainerClassName ) && methodName.equals( "<init>" );
		if( ( isInventoryMethod || isWorkbenchContainerMethod ) && methodDesc.equals( String.format( "(L%s;)Z", PlayerClassName ) ) )
		{
			return new MethodVisitor( api, cv.visitMethod( access, methodName, methodDesc, signature, exceptions ) )
			{
				@Override
				public void visitMethodInsn( int opcode, String calledOwner, String calledName, String calledDesc )
				{
					// should we transform this method call?
					if( opcode == Opcodes.INVOKEVIRTUAL && calledOwner.equals( PlayerClassName ) && calledName.equals( "getDistanceSq" ) && calledDesc.equals( "(DDD)D" ) )
					{
						// get the this type
						String thisType = null;
						if( isInventoryMethod )
						{
							thisType = TileEntityClassName;
						}
						else if( isWorkbenchContainerMethod )
						{
							thisType = WorkbenchContainerClassName;
						}
						else
						{
							throw new Error( "Unable to determine this type!" );
						}
						
						// we're replacing this method call
						// invokevirtual net.minecraft.entity.player.EntityPlayer.getDistanceSq(double, double, double) : double [187]
						// with
						// ShipIntermediary.getEntityDistanceSq( player, x, y, z, this )
						// plan:
						// currently on the argument stack: player, x, y, z
						// so just push the this instance on the stack and invoke the intermediary method
						mv.visitVarInsn( Opcodes.ALOAD, 0 );
						mv.visitMethodInsn(
							Opcodes.INVOKESTATIC,
							Intermediary,
							"getEntityDistanceSq",
							String.format( "(L%s;DDDL%s;)D", PlayerClassName, thisType )
						);
					}
					else
					{
						super.visitMethodInsn( opcode, calledOwner, calledName, calledDesc );
					}
				}
			};
		}
		else if( isContainerConstructor && methodDesc.equals( String.format( "(L%s;L%s;III)V", InventoryPlayerClassName, WorldClassName ) ) )
		{
			return new MethodVisitor( api, cv.visitMethod( access, methodName, methodDesc, signature, exceptions ) )
			{
				@Override
				public void visitFieldInsn( int opcode, String owner, String name, String desc )
				{
					// should we hook this call?
					if( opcode == Opcodes.PUTFIELD && owner.equals( m_name ) && desc.equals( String.format( "L%s;", WorldClassName ) ) )
					{
						// we're replacing this field setter
						// this.worldObj = worldObj
						// with
						// this.worldObj = ShipIntermediary.translateWorld( worldObj, player )
						// plan:
						// currently on the argument stack: this, worldObj
						// so just push the player instance on the stack, invoke the intermediary method, then recall the setter
						mv.visitVarInsn( Opcodes.ALOAD, 1 );
						mv.visitMethodInsn(
							Opcodes.INVOKESTATIC,
							Intermediary,
							"translateWorld",
							String.format( "(L%s;L%s;)L%s;", WorldClassName, InventoryPlayerClassName, WorldClassName )
						);
						mv.visitFieldInsn( Opcodes.PUTFIELD, owner, name, desc );
					}
					else
					{
						super.visitFieldInsn( opcode, owner, name, desc );
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
