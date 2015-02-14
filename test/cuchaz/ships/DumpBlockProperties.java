package cuchaz.ships;

import java.io.File;
import java.io.FileWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cpw.mods.fml.common.registry.LanguageRegistry;
import cuchaz.ships.config.BlockEntry;
import cuchaz.ships.config.BlockProperties;

public class DumpBlockProperties {
	
	public static void main(String[] args) throws Exception {
		new DumpBlockProperties().go();
	}
	
	public void go() throws Exception {
		new MinecraftRunner() {
			
			@Override
			@SuppressWarnings("unchecked")
			public void onRun() throws Exception {
				// gather all the blocks we care about
				final Map<String,Block> blocksToCheck = Maps.newLinkedHashMap();
				for (String blockId : (Iterable<String>)Block.blockRegistry.getKeys()) {
					Block block = (Block)Block.blockRegistry.getObject(blockId);
					if (block != null) {
						blocksToCheck.put(block.getUnlocalizedName(), block);
					}
				}
				
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
					Block block = blocksToCheck.get(keys.get(i));
					BlockEntry entry = BlockProperties.getEntry(block);
					
					// try to get the display name for the block
					String name = block.getLocalizedName();
					if (!isGoodName(name, block)) {
						// try to get the name from an item
						ItemStack stack = new ItemStack(block);
						if (stack.getItem() != null) {
							name = stack.getDisplayName();
						}
					}
					
					if (!isGoodName(name, block)) {
						// check the Forge language registry
						name = LanguageRegistry.instance().getStringLocalization(block.getUnlocalizedName(), "en_US");
					}
					
					if (!isGoodName(name, block)) {
						// as a last ditch effort, try to parse the class name
						String[] nameParts = block.getClass().getSimpleName().split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
						name = "";
						for (int j = 1; j < nameParts.length; j++) {
							if (name.length() > 0) {
								name += " ";
							}
							name += nameParts[j];
						}
					}
					
					out.write(String.format("\"%s\": [\"%s\", %f, %f, %b, %b, %b]",
						block.getUnlocalizedName(),
						name, entry.mass, entry.displacement,
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
			
			private boolean isGoodName(String name, Block block) {
				return name != null && name.length() > 0 && !name.equals(block.getUnlocalizedName() + ".name");
			}
		}.run();
	}
}
