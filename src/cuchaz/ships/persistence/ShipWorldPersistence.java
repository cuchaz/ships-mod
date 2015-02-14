/*******************************************************************************
 * Copyright (c) 2014 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
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

import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;

import cuchaz.modsShared.blocks.BlockMap;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.BlocksStorage;
import cuchaz.ships.ShipWorld;
import cuchaz.ships.Ships;

public enum ShipWorldPersistence {
	V1(1) {
		
		@Override
		public ShipWorld onRead(World world, DataInputStream in) throws IOException, UnrecognizedPersistenceVersion {
			// read the blocks
			BlocksStorage storage = BlockStoragePersistence.readAnyVersion(in);
			
			// read the tile entities
			BlockMap<TileEntity> tileEntities = new BlockMap<TileEntity>();
			int numTileEntities = in.readInt();
			for (int i = 0; i < numTileEntities; i++) {
				// create the tile entity
				NBTTagCompound nbt = CompressedStreamTools.read(in);
				TileEntity tileEntity = TileEntity.createAndLoadEntity(nbt);
				if (tileEntity == null) {
					Ships.logger.warning("Unable to restore tile entity: " + nbt.getString("id"));
					continue;
				}
				Coords coords = new Coords(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
				tileEntities.put(coords, tileEntity);
			}
			
			// read the hanging entities
			BlockMap<EntityHanging> hangingEntities = new BlockMap<EntityHanging>();
			int numHangingEntities = in.readInt();
			for (int i = 0; i < numHangingEntities; i++) {
				// create the hanging entity
				NBTTagCompound nbt = CompressedStreamTools.read(in);
				EntityHanging hangingEntity = (EntityHanging)EntityList.createEntityFromNBT(nbt, world);
				if (hangingEntity == null) {
					Ships.logger.warning("Unable to restore hanging entity: " + nbt.getString("id"));
					continue;
				}
				Coords coords = new Coords(hangingEntity.field_146063_b, hangingEntity.field_146064_c, hangingEntity.field_146062_d);
				hangingEntities.put(coords, hangingEntity);
			}
			
			// read the biome
			int biomeId = in.readInt();
			
			return new ShipWorld(world, storage, tileEntities, hangingEntities, biomeId);
		}
		
		@Override
		public void onWrite(ShipWorld shipWorld, DataOutputStream out) throws IOException {
			// write out the blocks
			BlockStoragePersistence.V1.write(shipWorld.getBlocksStorage(), out);
			
			// write out the tile entities
			out.writeInt(shipWorld.tileEntities().size());
			for (TileEntity tileEntity : shipWorld.tileEntities().values()) {
				NBTTagCompound nbt = new NBTTagCompound();
				try {
					tileEntity.writeToNBT(nbt);
				} catch (Throwable t) {
					Ships.logger.warning(t, "Tile entity %s on a ship at (%d,%d,%d) did not behave during a save operation!", tileEntity.getClass().getName(), tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord);
				}
				CompressedStreamTools.write(nbt, out);
			}
			
			// write out the hanging entities
			out.writeInt(shipWorld.hangingEntities().size());
			for (EntityHanging hangingEntity : shipWorld.hangingEntities().values()) {
				NBTTagCompound nbt = new NBTTagCompound();
				try {
					hangingEntity.writeToNBTOptional(nbt);
				} catch (Throwable t) {
					Ships.logger.warning(t, "Hanging entity %s on a ship at (%d,%d,%d) did not behave during a save operation!", hangingEntity.getClass().getName(), hangingEntity.field_146063_b, hangingEntity.field_146064_c, hangingEntity.field_146062_d);
				}
				CompressedStreamTools.write(nbt, out);
			}
			
			// write out the biome
			out.writeInt(shipWorld.getBiomeId());
		}
	};
	
	private static final String Encoding = "UTF-8";
	
	private static TreeMap<Integer,ShipWorldPersistence> m_versions;
	
	static {
		m_versions = new TreeMap<Integer,ShipWorldPersistence>();
		for (ShipWorldPersistence persistence : values()) {
			m_versions.put(persistence.m_version, persistence);
		}
	}
	
	private int m_version;
	
	private ShipWorldPersistence(int version) {
		m_version = version;
	}
	
	protected abstract ShipWorld onRead(World world, DataInputStream in) throws IOException, UnrecognizedPersistenceVersion;
	
	protected abstract void onWrite(ShipWorld shipWorld, DataOutputStream out) throws IOException;
	
	private static ShipWorldPersistence get(int version) {
		return m_versions.get(version);
	}
	
	private static ShipWorldPersistence getNewestVersion() {
		return m_versions.lastEntry().getValue();
	}
	
	public static ShipWorld readAnyVersion(World world, String data) throws PersistenceException {
		try {
			// STREAM MADNESS!!! @_@ MADNESS, I TELL YOU!!
			DataInputStream in = new DataInputStream(new GZIPInputStream(new Base64InputStream(new ByteArrayInputStream(data.getBytes(Encoding)))));
			ShipWorld shipWorld = readAnyVersion(world, in);
			in.close();
			return shipWorld;
		} catch (IOException ex) {
			throw new CorruptedPersistence(ex);
		}
	}
	
	public static ShipWorld readAnyVersion(World world, byte[] data) throws PersistenceException {
		return readAnyVersion(world, data, false);
	}
	
	public static ShipWorld readAnyVersion(World world, byte[] data, boolean isCompressed) throws PersistenceException {
		if (isCompressed) {
			try {
				return readAnyVersion(world, new GZIPInputStream(new ByteArrayInputStream(data)));
			} catch (IOException ex) {
				throw new CorruptedPersistence(ex);
			}
		}
		return readAnyVersion(world, new ByteArrayInputStream(data));
	}
	
	private static ShipWorld readAnyVersion(World world, InputStream in) throws PersistenceException {
		try {
			DataInputStream din = new DataInputStream(in);
			int version = din.readByte();
			ShipWorldPersistence persistence = get(version);
			if (persistence == null) {
				throw new UnrecognizedPersistenceVersion(version);
			}
			return persistence.onRead(world, din);
		} catch (Exception ex) {
			throw new CorruptedPersistence(ex);
		}
	}
	
	public static String writeNewestVersionToString(ShipWorld shipWorld) {
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new Base64OutputStream(buffer)));
			writeNewestVersion(shipWorld, out);
			out.close();
			return new String(buffer.toByteArray(), Encoding);
		} catch (IOException ex) {
			throw new Error(ex);
		}
	}
	
	public static byte[] writeNewestVersion(ShipWorld shipWorld) {
		return writeNewestVersion(shipWorld, false);
	}
	
	public static byte[] writeNewestVersion(ShipWorld shipWorld, boolean useCompression) {
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			if (useCompression) {
				GZIPOutputStream gzipOut = new GZIPOutputStream(buf);
				writeNewestVersion(shipWorld, gzipOut);
				gzipOut.finish();
			} else {
				writeNewestVersion(shipWorld, buf);
			}
			return buf.toByteArray();
		} catch (IOException ex) {
			// byte buffers should never throw an IOException, so writing a crap-ton of boilerplate code to handle
			// those exception is pretty ridiculous. Just rethrow as an error
			throw new Error(ex);
		}
	}
	
	public static void writeNewestVersion(ShipWorld shipWorld, OutputStream out) throws IOException {
		DataOutputStream dout = new DataOutputStream(out);
		ShipWorldPersistence persistence = getNewestVersion();
		dout.writeByte(persistence.m_version);
		persistence.onWrite(shipWorld, dout);
	}
}
