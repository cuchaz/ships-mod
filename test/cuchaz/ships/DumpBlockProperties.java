package cuchaz.ships;

import java.io.File;
import java.io.FileWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cuchaz.ships.config.BlockEntry;
import cuchaz.ships.config.BlockProperties;

public class DumpBlockProperties {
	
	public static void main(String[] args)
	throws Exception {
		new DumpBlockProperties().go();
	}
	
	public void go()
	throws Exception {
		new MinecraftRunner() {
			
			@Override
			@SuppressWarnings("unchecked")
			public void onRun()
			throws Exception {
				
				// gather all the blocks we care about
				final Map<String,Block> blocksToCheck = Maps.newLinkedHashMap();
				for (String blockId : (Iterable<String>)Block.blockRegistry.getKeys()) {
					Block block = (Block)Block.blockRegistry.getObject(blockId);
					if (block != null) {
						blocksToCheck.put(blockId, block);
					}
				}
				
				// add ships blocks
				blocksToCheck.put("cuchaz.ships:blockShip", Ships.m_blockShip);
				blocksToCheck.put("cuchaz.ships:blockHelm", Ships.m_blockHelm);
				blocksToCheck.put("cuchaz.ships:blockBerth", Ships.m_blockBerth);
				blocksToCheck.put("cuchaz.ships:blockProjector", Ships.m_blockProjector);
				
				// sort the blocks in number order
				List<String> keys = Lists.newArrayList(blocksToCheck.keySet());
				Collections.sort(keys, new Comparator<String>() {
					
					@Override
					public int compare(String a, String b) {
						int idA = Block.getIdFromBlock(blocksToCheck.get(a));
						int idB = Block.getIdFromBlock(blocksToCheck.get(b));
						return idA - idB;
					}
				});
				
				// setup out file
				FileWriter out = new FileWriter(new File("../cuchazinteractive/config/blockProperties.json"));
				
				// check the blocks
				out.write("{\n");
				for (int i = 0; i < keys.size(); i++) {
					String blockId = keys.get(i);
					Block block = blocksToCheck.get(blockId);
					BlockEntry entry = BlockProperties.getEntry(block);
					
					out.write(String.format("\"%s\": [%f, %f, %b, %b, %b]",
						blockId,
						entry.mass, entry.displacement,
						entry.isWatertight, entry.isSeparator, entry.isWater
					));
					
					if (i == blocksToCheck.size() - 1) {
						out.write("\n");
					} else {
						out.write(",\n");
					}
				}
				out.write("}\n");
				
				out.close();
				
				System.out.println("Done!");
			}
		}.run();
	}
}
