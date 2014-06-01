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
package cuchaz.ships.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cuchaz.ships.Ships;
import net.minecraft.block.Block;


public class BlockProperties
{
	private static Map<Block,BlockEntry> m_hardcodedEntries;
	private static Map<Block,BlockEntry> m_overriddenEntries;
	
	static
	{
		// read the overrides
		m_overriddenEntries = new HashMap<Block,BlockEntry>();
		readEntries( m_overriddenEntries, new File( "config/shipBlockProperties.cfg" ) );
		
		// add some hard-coded entries for vanilla blocks that have weird shapes, but common materials
		m_hardcodedEntries = new HashMap<Block,BlockEntry>();
		
		final double DoorSizeFactor = 0.3;
		addScaledEntry( Block.doorWood, new BlockEntry( DoorSizeFactor, DoorSizeFactor, false, false ) );
		addScaledEntry( Block.doorIron, new BlockEntry( DoorSizeFactor, DoorSizeFactor, false, false ) );
		
		final double TrapDoorSizeFactor = 0.2;
		addScaledEntry( Block.trapdoor, new BlockEntry( TrapDoorSizeFactor, TrapDoorSizeFactor, false, false ) );
		
		final double SlabSizeFactor = 0.5;
		addScaledEntry( Block.stoneSingleSlab, new BlockEntry( SlabSizeFactor, SlabSizeFactor, true, false ) );
		addScaledEntry( Block.woodSingleSlab, new BlockEntry( SlabSizeFactor, SlabSizeFactor, true, false ) );
		
		final double StairsSizeFactor = 0.75;
		addScaledEntry( Block.stairsWoodOak, new BlockEntry( StairsSizeFactor, StairsSizeFactor, true, false ) );
		addScaledEntry( Block.stairsWoodSpruce, new BlockEntry( StairsSizeFactor, StairsSizeFactor, true, false ) );
		addScaledEntry( Block.stairsWoodBirch, new BlockEntry( StairsSizeFactor, StairsSizeFactor, true, false ) );
		addScaledEntry( Block.stairsWoodJungle, new BlockEntry( StairsSizeFactor, StairsSizeFactor, true, false ) );
		addScaledEntry( Block.stairsNetherQuartz, new BlockEntry( StairsSizeFactor, StairsSizeFactor, true, false ) );
		addScaledEntry( Block.stairsCobblestone, new BlockEntry( StairsSizeFactor, StairsSizeFactor, true, false ) );
		addScaledEntry( Block.stairsBrick, new BlockEntry( StairsSizeFactor, StairsSizeFactor, true, false ) );
		addScaledEntry( Block.stairsStoneBrick, new BlockEntry( StairsSizeFactor, StairsSizeFactor, true, false ) );
		addScaledEntry( Block.stairsNetherBrick, new BlockEntry( StairsSizeFactor, StairsSizeFactor, true, false ) );
		addScaledEntry( Block.stairsSandStone, new BlockEntry( StairsSizeFactor, StairsSizeFactor, true, false ) );
		
		final double LadderSizeFactor = 0.1;
		addScaledEntry( Block.ladder, new BlockEntry( LadderSizeFactor, LadderSizeFactor, false, false ) );
		
		final double FenceSizeFactor = 0.5;
		addScaledEntry( Block.fence, new BlockEntry( FenceSizeFactor, FenceSizeFactor, false, false ) );
		addScaledEntry( Block.netherFence, new BlockEntry( FenceSizeFactor, FenceSizeFactor, false, false ) );
		addScaledEntry( Block.fenceIron, new BlockEntry( FenceSizeFactor, FenceSizeFactor, false, false ) );
		
		final double PressurePlateFactor = 0.2;
		addScaledEntry( Block.pressurePlateGold, new BlockEntry( PressurePlateFactor, PressurePlateFactor, false, false ) );
		addScaledEntry( Block.pressurePlateIron, new BlockEntry( PressurePlateFactor, PressurePlateFactor, false, false ) );
		addScaledEntry( Block.pressurePlatePlanks, new BlockEntry( PressurePlateFactor, PressurePlateFactor, false, false ) );
		addScaledEntry( Block.pressurePlateStone, new BlockEntry( PressurePlateFactor, PressurePlateFactor, false, false ) );
		
		final double ThinPaneFactor = 0.2;
		addScaledEntry( Block.thinGlass, new BlockEntry( ThinPaneFactor, ThinPaneFactor, true, false ) );
	}
	
	public static void addScaledEntry( Block block, BlockEntry entry )
	{
		// scale the default mass by the scale factor
		double scaleFactor = entry.mass;
		double defaultMass = DefaultBlockProperties.getEntry( block ).mass;
		entry.mass = defaultMass*scaleFactor;
		
		m_hardcodedEntries.put( block, entry );
	}
	
	public static void addEntry( Block block, BlockEntry entry )
	{
		m_hardcodedEntries.put( block, entry );
	}
	
	public static double getMass( Block block )
	{
		return getEntry( block ).mass;
	}
	
	public static double getDisplacement( Block block )
	{
		return getEntry( block ).displacement;
	}
	
	public static boolean isWatertight( Block block )
	{
		return getEntry( block ).isWatertight;
	}
	
	public static boolean isSeparator( Block block )
	{
		return getEntry( block ).isSeparator;
	}
	
	public static BlockEntry getEntry( Block block )
	{
		BlockEntry entry = null;
		
		// first, check the overrides
		entry = m_overriddenEntries.get( block );
		if( entry != null )
		{
			return entry;
		}
		
		// then, check the hard-coded entries
		entry = m_hardcodedEntries.get( block );
		if( entry != null )
		{
			return entry;
		}
		
		// finally, rely on the defaults
		return DefaultBlockProperties.getEntry( block );
	}
	
	private static void readEntries( Map<Block,BlockEntry> entries, File inFile )
	{
		if( !inFile.exists() )
		{
			return;
		}
		
		// build a map of the block names
		Map<String,Block> blocks = new HashMap<String,Block>();
		for( Block block : Block.blocksList )
		{
			if( block != null )
			{
				blocks.put( block.getUnlocalizedName(), block );
			}
		}
		
		try
		{
			// open the file for reading line-by-line
			BufferedReader in = new BufferedReader( new FileReader( inFile ) );
			String line = null;
			while( ( line = in.readLine() ) != null )
			{
				// skip blank or empty lines
				line = line.trim();
				if( line.length() <= 0 )
				{
					continue;
				}
				
				try
				{
					// read the block and the entry
					Block block = null;
					String[] parts = line.split( "=" );
					if( parts.length != 2 )
					{
						throw new IllegalArgumentException();
					}
					block = blocks.get( parts[0] );
					if( block == null )
					{
						throw new IllegalArgumentException( "Unknown block name: " + parts[0] );
					}
					BlockEntry entry = readEntry( parts[1] );
					
					// save the entry
					if( block != null && entry != null )
					{
						entries.put( block, entry );
					}
				}
				catch( RuntimeException ex )
				{
					Ships.logger.warning( ex, "Malformed block entry: %s", line );
				}
			}
			in.close();
			
			Ships.logger.info( "Read %d block entries from: %s", entries.size(), inFile.getAbsolutePath() );
		}
		catch( IOException ex )
		{
			Ships.logger.warning( ex, "Unable to read block properties!" );
		}
	}
	
	private static BlockEntry readEntry( String entryString )
	{
		String[] parts = entryString.split( "," );
		if( parts.length != 4 )
		{
			throw new IllegalArgumentException();
		}
		
		return new BlockEntry(
			Double.parseDouble( parts[0] ),
			Double.parseDouble( parts[1] ),
			Boolean.parseBoolean( parts[2] ),
			Boolean.parseBoolean( parts[3] )
		);
	}
}
