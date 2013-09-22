package cuchaz.ships.render;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.Resource;
import net.minecraft.client.resources.ResourceManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL20;

public class ShaderLoader
{
	private static int BufferSize = 1024*64; // 64 KiB
	
	private static HashMap<String,Integer> m_shaderTypes;
	
	static
	{
		m_shaderTypes = new HashMap<String,Integer>();
		m_shaderTypes.put( "frag", GL20.GL_FRAGMENT_SHADER );
		m_shaderTypes.put( "vert", GL20.GL_VERTEX_SHADER );
	}
	
	public static int load( ResourceLocation loc )
	throws IOException
	{
		int shaderId = GL20.glCreateShader( getShaderType( loc.func_110623_a() ) );
		GL20.glShaderSource( shaderId, readResource( loc ) );
		GL20.glCompileShader( shaderId );
		return shaderId;
	}
	
	private static int getShaderType( String path )
	{
		// get the extension
		String filename = new File( path ).getName();
		String extension = filename.substring( filename.lastIndexOf( '.' ) + 1 );
		return m_shaderTypes.get( extension );
	}

	private static String readResource( ResourceLocation loc )
	throws IOException
	{
		// open the resource
		ResourceManager resourceManager = Minecraft.getMinecraft().func_110442_L();
		Resource resource = resourceManager.func_110536_a( loc );
		InputStream in = resource.func_110527_b();
		
		// read it all into a buffer
		ByteArrayOutputStream out = new ByteArrayOutputStream( BufferSize );
		byte[] buf = new byte[BufferSize];
		while( true )
		{
			int numBytesRead = in.read( buf );
			if( numBytesRead == -1 )
			{
				break;
			}
			out.write( buf, 0, numBytesRead );
		}
		// assume the character encoding is UTF-8
		return new String( out.toByteArray(), "UTF-8" );
	}
}
