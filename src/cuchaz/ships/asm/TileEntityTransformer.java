package cuchaz.ships.asm;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class TileEntityTransformer implements IClassTransformer
{
	@Override
	public byte[] transform( String name, String transformedName, byte[] classData )
	{
		// UNDONE: implement transformations here
		
		// TEMP: simple pass-through transformer
        ClassReader reader = new ClassReader( classData );
        ClassWriter writer = new ClassWriter( ClassWriter.COMPUTE_MAXS );
        reader.accept( writer, 0 );
        return writer.toByteArray();
	}
}
