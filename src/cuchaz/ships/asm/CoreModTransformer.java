package cuchaz.ships.asm;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class CoreModTransformer implements IClassTransformer
{
	@Override
	public byte[] transform( String name, String transformedName, byte[] classData )
	{
		// route everything through our adapter
        ClassReader reader = new ClassReader( classData );
        ClassWriter writer = new ClassWriter( ClassWriter.COMPUTE_MAXS );
        reader.accept( new TileEntityAdapter( Opcodes.ASM4, writer ), 0 );
        return writer.toByteArray();
	}
}
