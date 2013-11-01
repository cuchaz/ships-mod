package cuchaz.ships.asm;

import java.util.Arrays;
import java.util.List;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class CoreModTransformer implements IClassTransformer
{
	@Override
	public byte[] transform( String name, String transformedName, byte[] classData )
	{
		// NOTE: name and transformedName are always the same
		
		// don't transform some important stuff
		List<String> privilegedPackages = Arrays.asList( "cuchaz.ships.", "net.minecraftforge.", "cpw." );
		for( String privilegedPackage : privilegedPackages )
		{
			if( name.startsWith( privilegedPackage ) )
			{
				return classData;
			}
		}
		
		// do we know about the obfuscation state yet?
		if( CoreModPlugin.isObfuscatedEnvironment == null )
		{
			return classData;
		}
		
		ClassReader reader = new ClassReader( classData );
		ClassWriter writer = new ClassWriter( ClassWriter.COMPUTE_MAXS );
		ClassVisitor tailAdapter = writer;
		
		// set up the adapter chain
		tailAdapter = new TileEntityInventoryAdapter( Opcodes.ASM4, tailAdapter, CoreModPlugin.isObfuscatedEnvironment );
		tailAdapter = new EntityMoveAdapter( Opcodes.ASM4, tailAdapter, CoreModPlugin.isObfuscatedEnvironment );

		/* now add class-specific adapters
		if( name.equals( "net/minecraft/network/packet/Packet54PlayNoteBlock" ) )
		{
			adapters.add( BlockEventPacketAdapter.class );
		}
		*/
		
		// run the transformations
		reader.accept( tailAdapter, 0 );
		return writer.toByteArray();
	}
}
