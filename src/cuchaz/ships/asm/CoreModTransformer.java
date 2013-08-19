package cuchaz.ships.asm;

import java.util.ArrayList;
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
		// UNDONE: how does obfuscation play into all of this??
		
		// set up our adapter chain
		List<Class<? extends ClassVisitor>> adapters = new ArrayList<Class<? extends ClassVisitor>>();
		
		// route everything through our generic adapters
		adapters.add( TileEntityInventoryAdapter.class );
		
		/* now add class-specific adapters
        if( name.equals( "net/minecraft/network/packet/Packet54PlayNoteBlock" ) )
        {
        	adapters.add( BlockEventPacketAdapter.class );
        }
        */
        
		// run the transformations
        ClassReader reader = new ClassReader( classData );
        ClassWriter writer = new ClassWriter( ClassWriter.COMPUTE_MAXS );
        ClassVisitor tailAdapter = writer;
        for( Class<? extends ClassVisitor> c : adapters )
        {
        	try
			{
				tailAdapter = c.getConstructor( int.class, ClassVisitor.class ).newInstance( Opcodes.ASM4, tailAdapter );
			}
			catch( Exception ex )
			{
				System.err.println( "Unable to instantiate adapter: " + c.getName() );
				ex.printStackTrace( System.err );
			}
        }
		reader.accept( tailAdapter, 0 );
        return writer.toByteArray();
	}
}
