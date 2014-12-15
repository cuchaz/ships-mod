package cuchaz.ships;

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

public class DumpBlockProperties
{
	public static void main( String[] args )
	throws Exception
	{
		new DumpBlockProperties().go();
	}
	
	public void go( )
	throws Exception
	{
		new MinecraftRunner( )
		{
			@Override
			@SuppressWarnings( "unchecked" )
			public void onRun( )
			throws Exception
			{
				// gather all the blocks we care about
				final Map<String,Block> blocksToCheck = Maps.newLinkedHashMap();
				for( String blockId : (Iterable<String>)Block.blockRegistry.getKeys() )
				{
		            Block block = (Block)Block.blockRegistry.getObject( blockId );
					if( block != null )
					{
						blocksToCheck.put( block.getUnlocalizedName(), block );
					}
				}
				
				// sort the blocks in number order
				List<String> keys = Lists.newArrayList(blocksToCheck.keySet());
				Collections.sort( keys, new Comparator<String>( ) {
					@Override
					public int compare( String a, String b )
					{
						int idA = Block.getIdFromBlock( blocksToCheck.get( a ) );
						int idB = Block.getIdFromBlock( blocksToCheck.get( b ) );
						return idA - idB;
					}
				} );
				
				// check the blocks
				System.out.println( "{" );
				for( int i=0; i<keys.size(); i++ )
				{
					Block block = blocksToCheck.get( keys.get( i ) );
					BlockEntry entry = BlockProperties.getEntry( block );
					
					// try to get the display name for the block
					String name = block.getLocalizedName();
					if( !isGoodName( name, block ) )
					{
						// try to get the name from an item
						ItemStack stack = new ItemStack( block );
						if( stack.getItem() != null )
						{
							name = stack.getDisplayName();
						}
					}
					
					if( !isGoodName( name, block ) )
					{
						// check the Forge language registry
						name = LanguageRegistry.instance().getStringLocalization( block.getUnlocalizedName(), "en_US" );
					}
					
					if( !isGoodName( name, block ) )
					{
						// as a last ditch effort, try to parse the class name
						String[] nameParts = block.getClass().getSimpleName().split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
						name = "";
						for( int j=1; j<nameParts.length; j++ )
						{
							if( name.length() > 0 )
							{
								name += " ";
							}
							name += nameParts[j];
						}
					}
					
					System.out.print( String.format( "\"%s\": [\"%s\", %f, %f, %b, %b, %b]",
						block.getUnlocalizedName(),
						name,
						entry.mass,
						entry.displacement,
						entry.isWatertight,
						entry.isSeparator,
						entry.isWater
					) );
					
					if( i == blocksToCheck.size() - 1 )
					{
						System.out.println();
					}
					else
					{
						System.out.println(",");
					}
				}
				System.out.println( "}" );
			}
			
			private boolean isGoodName( String name, Block block )
			{
				return name != null && name.length() > 0 && !name.equals( block.getUnlocalizedName() + ".name" );
			}
		}.run();
	}
}
