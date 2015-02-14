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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.Bits;
import cuchaz.ships.BlockStorage;
import cuchaz.ships.BlocksStorage;
import cuchaz.ships.Ships;

public enum BlockStoragePersistence {
	V1(1) {
		
		@Override
		protected BlocksStorage onRead(DataInputStream in) throws IOException {
			// read the block name/id mappings
			Map<Integer,String> map = Maps.newTreeMap();
			int numMappings = in.readUnsignedShort();
			for (int i = 0; i < numMappings; i++) {
				map.put(in.readUnsignedShort(), in.readUTF());
			}
			
			// read the block data
			Set<String> warnedBlocks = Sets.newHashSet();
			int numBlocks = in.readInt();
			BlocksStorage blocks = new BlocksStorage();
			for (int i = 0; i < numBlocks; i++) {
				Coords coords = new Coords(in.readShort(), in.readShort(), in.readShort());
				int packed = in.readUnsignedShort();
				
				BlockStorage storage = new BlockStorage();
				storage.meta = Bits.unpackUnsigned(packed, 4, 12);
				
				// do block id mapping to get the block
				String blockName = map.get(Bits.unpackUnsigned(packed, 12, 0));
				storage.block = (Block)Block.blockRegistry.getObject(blockName);
				if (storage.block == null) {
					// this block must not be present on the server, warn someone
					if (!warnedBlocks.contains(blockName)) {
						Ships.logger.warning("Can't find block %s on this server! Block has been replaced with wood planks", blockName);
						warnedBlocks.add(blockName);
					}
					storage.block = Blocks.planks;
				}
				
				blocks.setBlock(coords, storage);
			}
			return blocks;
		}
		
		@Override
		protected void onWrite(BlocksStorage blocks, DataOutputStream out) throws IOException {
			// write out block name/id mappings
			Map<Integer,String> map = Maps.newTreeMap();
			for (Coords coords : blocks.coords()) {
				Block block = blocks.getBlock(coords).block;
				map.put(Block.getIdFromBlock(block), Block.blockRegistry.getNameForObject(block));
			}
			out.writeShort(map.size());
			for (Map.Entry<Integer,String> entry : map.entrySet()) {
				out.writeShort(entry.getKey());
				out.writeUTF(entry.getValue());
			}
			
			// write the block data
			out.writeInt(blocks.getNumBlocks());
			for (Coords coords : blocks.coords()) {
				out.writeShort(coords.x);
				out.writeShort(coords.y);
				out.writeShort(coords.z);
				BlockStorage block = blocks.getBlock(coords);
				out.writeShort(Bits.packUnsigned(Block.getIdFromBlock(block.block), 12, 0) | Bits.packUnsigned(block.meta, 4, 12));
			}
		}
	};
	
	private static final String Encoding = "UTF-8";
	
	private static TreeMap<Integer,BlockStoragePersistence> m_versions;
	
	static {
		m_versions = new TreeMap<Integer,BlockStoragePersistence>();
		for (BlockStoragePersistence persistence : values()) {
			m_versions.put(persistence.m_version, persistence);
		}
	}
	
	private int m_version;
	
	private BlockStoragePersistence(int version) {
		m_version = version;
	}
	
	protected abstract BlocksStorage onRead(DataInputStream in) throws IOException;
	
	protected abstract void onWrite(BlocksStorage blocks, DataOutputStream out) throws IOException;
	
	private static BlockStoragePersistence get(int version) {
		return m_versions.get(version);
	}
	
	private static BlockStoragePersistence getNewestVersion() {
		return m_versions.lastEntry().getValue();
	}
	
	public static BlocksStorage readAnyVersion(String data) throws UnrecognizedPersistenceVersion {
		try {
			// STREAM MADNESS!!! @_@ MADNESS, I TELL YOU!!
			DataInputStream in = new DataInputStream(new GZIPInputStream(new Base64InputStream(new ByteArrayInputStream(data.getBytes(Encoding)))));
			BlocksStorage blocks = readAnyVersion(in);
			in.close();
			return blocks;
		} catch (IOException ex) {
			throw new UnrecognizedPersistenceVersion();
		}
	}
	
	public static BlocksStorage readAnyVersion(byte[] data) throws UnrecognizedPersistenceVersion {
		try {
			return readAnyVersion(new ByteArrayInputStream(data));
		} catch (IOException ex) {
			throw new Error(ex);
		}
	}
	
	public static BlocksStorage readAnyVersion(InputStream in) throws IOException, UnrecognizedPersistenceVersion {
		DataInputStream din = new DataInputStream(in);
		int version = din.readByte();
		BlockStoragePersistence persistence = get(version);
		if (persistence == null) {
			throw new UnrecognizedPersistenceVersion(version);
		}
		return persistence.onRead(din);
	}
	
	public static String writeNewestVersionToString(BlocksStorage blocks) {
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new Base64OutputStream(buffer)));
			writeNewestVersion(blocks, out);
			out.close();
			return new String(buffer.toByteArray(), Encoding);
		} catch (IOException ex) {
			throw new Error(ex);
		}
	}
	
	public static byte[] writeNewestVersion(BlocksStorage blocks) {
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			writeNewestVersion(blocks, buf);
			return buf.toByteArray();
		} catch (IOException ex) {
			// byte buffers should never throw an IOException, so writing a crap-ton of boilerplate code to handle
			// those exception is pretty ridiculous. Just rethrow as an error
			throw new Error(ex);
		}
	}
	
	public static void writeNewestVersion(BlocksStorage blocks, OutputStream out) throws IOException {
		getNewestVersion().write(blocks, out);
	}
	
	public void write(BlocksStorage blocks, OutputStream out) throws IOException {
		DataOutputStream dout = new DataOutputStream(out);
		dout.writeByte(m_version);
		onWrite(blocks, dout);
	}
}
