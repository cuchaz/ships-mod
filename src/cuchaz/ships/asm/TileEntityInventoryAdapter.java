package cuchaz.ships.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public class TileEntityInventoryAdapter extends ObfuscationAwareAdapter
{
	private final String InventoryInterfaceName;
	private final String TileEntityClassName;
	private final String ContainerClassName;
	private final String PlayerClassName;
	private final String InventoryPlayerClassName;
	private final String WorldClassName;
	
	private String m_name;
	private String m_superName;
	private String[] m_interfaces;
	
	public TileEntityInventoryAdapter( int api, ClassVisitor cv, boolean isObfuscatedEnvironment )
	{
		super( api, cv, isObfuscatedEnvironment );
		
		// cache the runtime class names
		InventoryInterfaceName = getRuntimeClassName( "net/minecraft/inventory/IInventory" );
		TileEntityClassName = getRuntimeClassName( "net/minecraft/tileentity/TileEntity" );
		ContainerClassName = getRuntimeClassName( "net/minecraft/inventory/Container" );
		PlayerClassName = getRuntimeClassName( "net/minecraft/entity/player/EntityPlayer" );
		InventoryPlayerClassName = getRuntimeClassName( "net/minecraft/entity/player/InventoryPlayer" );
		WorldClassName = getRuntimeClassName( "net/minecraft/world/World" );
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
		// should we transform this method?
		// for performance, check method names first, class inheritance second, and finally interfaces third
		final boolean isTileEntityInventoryIsUseableByPlayer = methodName.equals( getRuntimeMethodName( m_name, "isUseableByPlayer", "func_70300_a" ) )
			&& extendsClass( TileEntityClassName )
			&& implementsInterface( InventoryInterfaceName );
		final boolean isContainerCanInteractWith = methodName.equals( getRuntimeMethodName( m_name, "canInteractWith", "func_75145_c" ) )
			&& extendsClass( ContainerClassName );
		if( ( isTileEntityInventoryIsUseableByPlayer || isContainerCanInteractWith ) && methodDesc.equals( String.format( "(L%s;)Z", PlayerClassName ) ) )
		{
			return new MethodVisitor( api, cv.visitMethod( access, methodName, methodDesc, signature, exceptions ) )
			{
				@Override
				public void visitMethodInsn( int opcode, String calledOwner, String calledName, String calledDesc )
				{
					// should we transform this method call?
					if( opcode == Opcodes.INVOKEVIRTUAL
						&& calledDesc.equals( "(DDD)D" )
						&& calledOwner.equals( PlayerClassName )
						&& calledName.equals( getRuntimeMethodName( calledOwner, "getDistanceSq", "func_70092_e" ) ) )
					{
						// get the this type
						String thisType = null;
						if( isTileEntityInventoryIsUseableByPlayer )
						{
							thisType = TileEntityClassName;
						}
						else if( isContainerCanInteractWith )
						{
							thisType = ContainerClassName;
						}
						else
						{
							throw new Error( "Unable to determine this type!" );
						}
						
						// we're replacing this method call
						// invokevirtual
						// net.minecraft.entity.player.EntityPlayer.getDistanceSq(double, double, double) : double [187]
						// with
						// ShipIntermediary.getEntityDistanceSq( player, x, y, z, this )
						// plan:
						// currently on the argument stack: player, x, y, z
						// so just push the this instance on the stack and invoke the intermediary method
						mv.visitVarInsn( Opcodes.ALOAD, 0 );
						mv.visitMethodInsn( Opcodes.INVOKESTATIC, ShipIntermediary.Path, "getEntityDistanceSq", String.format( "(L%s;DDDL%s;)D", PlayerClassName, thisType ) );
					}
					else
					{
						super.visitMethodInsn( opcode, calledOwner, calledName, calledDesc );
					}
				}
			};
		}
		else if( methodName.equals( "<init>" )
			&& methodDesc.equals( String.format( "(L%s;L%s;III)V", InventoryPlayerClassName, WorldClassName ) )
			&& extendsClass( ContainerClassName ) )
		{
			return new MethodVisitor( api, cv.visitMethod( access, methodName, methodDesc, signature, exceptions ) )
			{
				@Override
				public void visitFieldInsn( int opcode, String owner, String fieldName, String desc )
				{
					// should we hook this call?
					if( opcode == Opcodes.PUTFIELD
						&& desc.equals( String.format( "L%s;", WorldClassName ) )
						&& owner.equals( m_name ) )
					{
						// we're replacing this field setter
						// this.worldObj = worldObj
						// with
						// this.worldObj = ShipIntermediary.translateWorld( worldObj, player )
						// plan:
						// currently on the argument stack: this, worldObj
						// so just push the player instance on the stack, invoke the intermediary method, then recall the setter
						mv.visitVarInsn( Opcodes.ALOAD, 1 );
						mv.visitMethodInsn( Opcodes.INVOKESTATIC, ShipIntermediary.Path, "translateWorld", String.format( "(L%s;L%s;)L%s;", WorldClassName, InventoryPlayerClassName, WorldClassName ) );
						mv.visitFieldInsn( Opcodes.PUTFIELD, owner, fieldName, desc );
					}
					else
					{
						super.visitFieldInsn( opcode, owner, fieldName, desc );
					}
				}
			};
		}
		else
		{
			return super.visitMethod( access, methodName, methodDesc, signature, exceptions );
		}
	}
	
	private boolean extendsClass( String targetClassName )
	{
		return InheritanceUtils.extendsClass( m_superName, targetClassName );
	}
	
	private boolean implementsInterface( String targetInterfaceName )
	{
		for( String i : m_interfaces )
		{
			if( InheritanceUtils.implementsInterface( i, targetInterfaceName ) )
			{
				return true;
			}
		}
		return false;
	}
}
