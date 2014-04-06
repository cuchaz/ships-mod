/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;

import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.Bits;
import cuchaz.ships.BlockStorage;
import cuchaz.ships.BlocksStorage;

public enum BlockStoragePersistence
{
	V1( 1 )
	{
		@Override
		protected BlocksStorage onRead( DataInputStream in, int numBlocks ) throws IOException
		{
			BlocksStorage blocks = new BlocksStorage();
			for( int i=0; i<numBlocks; i++ )
			{
				Coords coords = new Coords( in.readInt(), in.readInt(), in.readInt() );
				BlockStorage block = new BlockStorage( in.readInt(), in.readInt() );
				blocks.setBlock( coords, block );
			}
			return blocks;
		}
		
		@Override
		protected void onWrite( BlocksStorage blocks, DataOutputStream out ) throws IOException
		{
			out.writeInt( blocks.getNumBlocks() );
			for( Coords coords : blocks.coords() )
			{
				out.writeInt( coords.x );
				out.writeInt( coords.y );
				out.writeInt( coords.z );
				BlockStorage block = blocks.getBlock( coords );
				out.writeInt( block.id );
				out.writeInt( block.meta );
			}
		}
	},
	V2( 2 )
	{
		@Override
		protected BlocksStorage onRead( DataInputStream in, int numBlocks ) throws IOException
		{
			BlocksStorage blocks = new BlocksStorage();
			for( int i=0; i<numBlocks; i++ )
			{
				Coords coords = new Coords( in.readShort(), in.readShort(), in.readShort() );
				int n = in.readShort();
				BlockStorage block = new BlockStorage();
				block.id = Bits.unpackUnsigned( n, 12, 0 );
				block.meta = Bits.unpackUnsigned( n, 4, 12 );
				blocks.setBlock( coords, block );
			}
			return blocks;
		}
		
		@Override
		protected void onWrite( BlocksStorage blocks, DataOutputStream out ) throws IOException
		{
			out.writeInt( blocks.getNumBlocks() );
			for( Coords coords : blocks.coords() )
			{
				out.writeShort( coords.x );
				out.writeShort( coords.y );
				out.writeShort( coords.z );
				BlockStorage block = blocks.getBlock( coords );
				out.writeShort( Bits.packUnsigned( block.id, 12, 0 ) | Bits.packUnsigned( block.meta, 4, 12 ) );
			}
		}
	};
	
	private static final String Encoding = "UTF-8";
	
	private static TreeMap<Integer,BlockStoragePersistence> m_versions;
	
	static
	{
		m_versions = new TreeMap<Integer,BlockStoragePersistence>();
		for( BlockStoragePersistence persistence : values() )
		{
			m_versions.put( persistence.m_version, persistence );
		}
	}
	
	private int m_version;
	
	private BlockStoragePersistence( int version )
	{
		m_version = version;
	}
	
	protected abstract BlocksStorage onRead( DataInputStream in, int numBlocks ) throws IOException;
	protected abstract void onWrite( BlocksStorage blocks, DataOutputStream out ) throws IOException;
	
	private static BlockStoragePersistence get( int version )
	{
		return m_versions.get( version );
	}
	
	private static BlockStoragePersistence getNewestVersion( )
	{
		return m_versions.lastEntry().getValue();
	}
	
	public static BlocksStorage readAnyVersion( String data )
	throws UnrecognizedPersistenceVersion
	{
		try
		{
			// STREAM MADNESS!!! @_@  MADNESS, I TELL YOU!!
			DataInputStream in = new DataInputStream( new GZIPInputStream( new Base64InputStream( new ByteArrayInputStream( data.getBytes( Encoding ) ) ) ) );
			BlocksStorage blocks = readAnyVersion( in );
			in.close();
			return blocks;
		}
		catch( IOException ex )
		{
			throw new UnrecognizedPersistenceVersion(); 
		}
	}
	
	public static BlocksStorage readAnyVersion( byte[] data )
	throws UnrecognizedPersistenceVersion
	{
		try
		{
			return readAnyVersion( new ByteArrayInputStream( data ) );
		}
		catch( IOException ex )
		{
			throw new Error( ex );
		}
	}
	
	public static BlocksStorage readAnyVersion( InputStream in )
	throws IOException, UnrecognizedPersistenceVersion
	{
		DataInputStream din = new DataInputStream( in );
		
		// get the version and number of blocks
		int firstInt = din.readInt();
		
		int version = 1;
		int numBlocks = 0;
		if( firstInt < 0 )
		{
			// if the first int is negative, it's a version number
			version = -firstInt;
			numBlocks = din.readInt();
		}
		else
		{
			// if it's positive, it's a number of blocks and we'll assume V1
			numBlocks = firstInt;
		}
		
		BlockStoragePersistence persistence = get( version );
		if( persistence == null )
		{
			throw new UnrecognizedPersistenceVersion( version );
		}
		return persistence.onRead( din, numBlocks );
	}
	
	public static String writeNewestVersionToString( BlocksStorage blocks )
	{
		try
		{
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream( new GZIPOutputStream( new Base64OutputStream( buffer ) ) );
			writeNewestVersion( blocks, out );
			out.close();
			return new String( buffer.toByteArray(), Encoding );
		}
		catch( IOException ex )
		{
			throw new Error( ex );
		}
	}
	
	public static byte[] writeNewestVersion( BlocksStorage blocks )
	{
		try
		{
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			writeNewestVersion( blocks, buf );
			return buf.toByteArray();
		}
		catch( IOException ex )
		{
			// byte buffers should never throw an IOException, so writing a crap-ton of boilerplate code to handle
			// those exception is pretty ridiculous. Just rethrow as an error
			throw new Error( ex );
		}
	}
	
	public static void writeNewestVersion( BlocksStorage blocks, OutputStream out )
	throws IOException
	{
		getNewestVersion().write( blocks, out );
	}
	
	public void write( BlocksStorage blocks, OutputStream out )
	throws IOException
	{
		DataOutputStream dout = new DataOutputStream( out );
		
		// NOTE: the original V1 didn't write out a version number
		// the first int is the number of blocks, which must be positive
		// so let's write a negative version number, so we can tell the difference
		// between it and a number of blocks
		dout.writeInt( -m_version );
		onWrite( blocks, dout );
	}
}
