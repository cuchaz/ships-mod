/*******************************************************************************
 * Copyright (c) 2013 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.render;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

public class ShaderLoader {
	
	private static int BufferSize = 1024 * 64; // 64 KiB
	
	private static HashMap<String,Integer> m_shaderTypes;
	
	static {
		m_shaderTypes = new HashMap<String,Integer>();
		m_shaderTypes.put("frag", GL20.GL_FRAGMENT_SHADER);
		m_shaderTypes.put("vert", GL20.GL_VERTEX_SHADER);
	}
	
	public static boolean areShadersSupported() {
		return GLContext.getCapabilities().OpenGL20;
	}
	
	public static int load(ResourceLocation loc) throws IOException {
		int shaderId = GL20.glCreateShader(getShaderType(loc.getResourcePath()));
		GL20.glShaderSource(shaderId, readResource(loc));
		GL20.glCompileShader(shaderId);
		return shaderId;
	}
	
	public static int createProgram(int shaderId) {
		int programId = GL20.glCreateProgram();
		GL20.glAttachShader(programId, shaderId);
		GL20.glLinkProgram(programId);
		GL20.glValidateProgram(programId);
		return programId;
	}
	
	private static int getShaderType(String path) {
		// get the extension
		String filename = new File(path).getName();
		String extension = filename.substring(filename.lastIndexOf('.') + 1);
		return m_shaderTypes.get(extension);
	}
	
	private static String readResource(ResourceLocation loc) throws IOException {
		// open the resource
		InputStream in = Minecraft.getMinecraft().getResourceManager().getResource(loc).getInputStream();
		
		// read it all into a buffer
		ByteArrayOutputStream out = new ByteArrayOutputStream(BufferSize);
		byte[] buf = new byte[BufferSize];
		while (true) {
			int numBytesRead = in.read(buf);
			if (numBytesRead == -1) {
				break;
			}
			out.write(buf, 0, numBytesRead);
		}
		// assume the character encoding is UTF-8
		return new String(out.toByteArray(), "UTF-8");
	}
}
