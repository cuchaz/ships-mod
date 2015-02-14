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
package cuchaz.ships;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.util.MathHelper;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockSetHeightIndex;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.BlockUtils.Neighbors;
import cuchaz.modsShared.blocks.Coords;

public class ShipDisplacement {
	
	private static class ClassifiedSegment {
		
		BlockSet segment;
		BlockSet surfaceBlocks;
		BlockSet underwaterBlocks;
		boolean isTrapped;
	}
	
	private static class DisplacementEntry {
		
		public int numFillableBlocks;
		public BlockSet trappedAir;
		public BlockSet surfaceBlocks;
		public BlockSet underwaterBlocks;
		
		public DisplacementEntry() {
			numFillableBlocks = 0;
			trappedAir = new BlockSet();
			surfaceBlocks = new BlockSet();
			underwaterBlocks = new BlockSet();
		}
	}
	
	public static final Neighbors BoundaryNeighbors = Neighbors.Edges;
	public static final Neighbors VoidBlockNeighbors = Neighbors.Faces;
	private static final DisplacementEntry EmptyEntry = new DisplacementEntry();
	
	private BlockSet m_blocks;
	private List<BlockSet> m_outerBoundaries;
	private List<BlockSet> m_holes;
	private TreeMap<Integer,DisplacementEntry> m_displacement;
	
	public ShipDisplacement(BlockSet blocks) {
		m_blocks = blocks;
		m_outerBoundaries = new ArrayList<BlockSet>();
		m_holes = null;
		
		computeBoundaryAndHoles();
		computeDisplacement();
	}
	
	public BlockSet getBlocks() {
		return m_blocks;
	}
	
	public List<BlockSet> getOuterBoundaries() {
		return m_outerBoundaries;
	}
	
	public List<BlockSet> getHoles() {
		return m_holes;
	}
	
	public int getMinY() {
		return m_blocks.getBoundingBox().minY;
	}
	
	public int getMaxY() {
		return m_blocks.getBoundingBox().maxY;
	}
	
	public BlockSet getTrappedAir(int y) {
		return get(y).trappedAir;
	}
	
	public BlockSet getTrappedAirFromWaterHeight(int waterHeightInBlockSpace) {
		// remember, the water height is the y-value of the surface of the water
		// it's always at the top of the water block
		// therefore, if we want the y coord of the top water block, we need to subtract 1
		return getTrappedAir(waterHeightInBlockSpace - 1);
	}
	
	public BlockSet getTrappedAirFromWaterHeight(double waterHeightInBlockSpace) {
		// for double water height values, round up to the top of the block, then subtract 1
		// or, just round down
		return getTrappedAir(MathHelper.floor_double(waterHeightInBlockSpace));
	}
	
	public BlockSet getSurfaceBlocks(int y) {
		return get(y).surfaceBlocks;
	}
	
	public BlockSet getUnderwaterBlocks(int y) {
		return get(y).underwaterBlocks;
	}
	
	public int getNumFillableBlocks(int y) {
		return get(y).numFillableBlocks;
	}
	
	public Integer getLastFillY() {
		for (Map.Entry<Integer,DisplacementEntry> entry : m_displacement.descendingMap().entrySet()) {
			if (entry.getValue().numFillableBlocks > 0) {
				return entry.getKey() + 1; // + 1 to get to the top of the block
			}
		}
		return null;
	}
	
	private DisplacementEntry get(int y) {
		if (m_displacement.isEmpty()) {
			return EmptyEntry;
		}
		
		// if y is too big, clamp it. ie when the ship is underwater, we get the max trapped air
		y = Math.min(y, m_displacement.lastKey());
		
		DisplacementEntry entry = m_displacement.get(y);
		if (entry == null) {
			entry = EmptyEntry;
		}
		return entry;
	}
	
	private void computeBoundaryAndHoles() {
		// first, get all blocks touching the ship on an edge (aka the boundary)
		final BlockSet boundaryBlocks = BlockUtils.getBoundary(m_blocks, BoundaryNeighbors);
		
		// boundaryBlocks will have some number of connected components. Find them all and classify each as inner/outer
		m_holes = new ArrayList<BlockSet>();
		for (BlockSet component : BlockUtils.getConnectedComponents(boundaryBlocks, VoidBlockNeighbors)) {
			// is this component the outer boundary?
			if (BlockUtils.isConnectedToShell(component.iterator().next(), m_blocks, VoidBlockNeighbors)) {
				m_outerBoundaries.add(component);
			} else {
				// compute the hole from the boundary
				m_holes.add(BlockUtils.getHoleFromInnerBoundary(component, m_blocks, VoidBlockNeighbors));
			}
		}
	}
	
	private void computeDisplacement() {
		// get the y-range
		int minY = getMinY();
		int maxY = getMaxY();
		
		m_displacement = new TreeMap<Integer,DisplacementEntry>();
		
		// pass 1: compute the displacement of ship blocks
		BlockSetHeightIndex shipIndex = new BlockSetHeightIndex(m_blocks);
		for (int y = minY; y <= maxY + 1; y++) {
			// build the displacement entry
			DisplacementEntry entry = new DisplacementEntry();
			m_displacement.put(y, entry);
			
			// separate surface blocks from underwater blocks
			BlockSet yBlocks = shipIndex.get(y);
			if (yBlocks != null) {
				entry.surfaceBlocks.addAll(yBlocks);
			}
			if (y > minY) {
				DisplacementEntry lowerEntry = m_displacement.get(y - 1);
				entry.underwaterBlocks.addAll(lowerEntry.surfaceBlocks);
				entry.underwaterBlocks.addAll(lowerEntry.underwaterBlocks);
			}
		}
		
		// pass 2: analyze the outer boundary for trapped air
		BlockSetHeightIndex boundaryIndex = new BlockSetHeightIndex();
		for (BlockSet blocks : m_outerBoundaries) {
			boundaryIndex.add(blocks);
		}
		List<ClassifiedSegment> boundarySegments = new ArrayList<ClassifiedSegment>();
		for (int y = minY; y <= maxY + 1; y++) {
			// get all the segments at y
			BlockSet blocksAtY = boundaryIndex.get(y);
			if (blocksAtY != null) {
				List<BlockSet> ySegments = BlockUtils.getConnectedComponents(blocksAtY, VoidBlockNeighbors);
				
				// merge them with existing segments
				int numFilledBlocks = 0;
				for (BlockSet ySegment : ySegments) {
					List<ClassifiedSegment> connectedSegments = getYConnectedSegments(ySegment, boundarySegments, VoidBlockNeighbors);
					if (connectedSegments.isEmpty()) {
						// add the new segment
						ClassifiedSegment classifiedSegment = new ClassifiedSegment();
						classifiedSegment.segment = ySegment;
						classifiedSegment.isTrapped = !BlockUtils.isConnectedToShell(ySegment.iterator().next(), m_blocks, VoidBlockNeighbors, y);
						classifiedSegment.surfaceBlocks = new BlockSet(ySegment);
						classifiedSegment.underwaterBlocks = new BlockSet();
						boundarySegments.add(classifiedSegment);
					} else {
						// count the number of possibly filled blocks
						int numPossiblyFilledBlocks = 0;
						for (ClassifiedSegment segment : connectedSegments) {
							if (segment.isTrapped) {
								numPossiblyFilledBlocks += segment.segment.size();
							}
						}
						
						// merge all the existing segments
						ClassifiedSegment baseSegment = connectedSegments.get(0);
						if (baseSegment.isTrapped) {
							// fill in holes for inner boundaries
							ySegment.addAll(BlockUtils.getHoleFromInnerBoundary(ySegment, m_blocks, VoidBlockNeighbors, y, y));
						}
						baseSegment.segment.addAll(ySegment);
						baseSegment.underwaterBlocks.addAll(baseSegment.surfaceBlocks);
						baseSegment.surfaceBlocks.clear();
						baseSegment.surfaceBlocks.addAll(ySegment);
						for (int i = 1; i < connectedSegments.size(); i++) {
							ClassifiedSegment nextSegment = connectedSegments.get(i);
							baseSegment.isTrapped = baseSegment.isTrapped && nextSegment.isTrapped;
							baseSegment.segment.addAll(nextSegment.segment);
							baseSegment.underwaterBlocks.addAll(nextSegment.surfaceBlocks);
							baseSegment.underwaterBlocks.addAll(nextSegment.underwaterBlocks);
							boundarySegments.remove(nextSegment);
						}
						
						// record the number of filled blocks
						if (!baseSegment.isTrapped) {
							numFilledBlocks += numPossiblyFilledBlocks;
						}
					}
				}
				
				// handle any filled blocks
				if (numFilledBlocks > 0) {
					m_displacement.get(y - 1).numFillableBlocks = numFilledBlocks;
				}
			}
			
			// compute the trapped air so far
			DisplacementEntry entry = m_displacement.get(y);
			for (ClassifiedSegment segment : boundarySegments) {
				if (segment.isTrapped) {
					entry.trappedAir.addAll(segment.segment);
					entry.surfaceBlocks.addAll(segment.surfaceBlocks);
					entry.underwaterBlocks.addAll(segment.underwaterBlocks);
				}
			}
		}
		
		// pass 3: analyze the holes
		BlockSetHeightIndex holeIndex = new BlockSetHeightIndex();
		for (BlockSet hole : m_holes) {
			holeIndex.add(hole);
		}
		BlockSet holeBlocks = new BlockSet();
		BlockSet underwaterBlocks = new BlockSet();
		for (int y = minY; y <= maxY + 1; y++) {
			DisplacementEntry entry = m_displacement.get(y);
			
			// add the blocks for this y
			BlockSet layer = holeIndex.get(y);
			if (layer != null) {
				holeBlocks.addAll(layer);
				entry.surfaceBlocks.addAll(layer);
			}
			entry.underwaterBlocks.addAll(underwaterBlocks);
			entry.trappedAir.addAll(holeBlocks);
			
			if (layer != null) {
				// update the underwater blocks for next time
				underwaterBlocks.addAll(layer);
			}
		}
	}
	
	private List<ClassifiedSegment> getYConnectedSegments(BlockSet ySegment, Iterable<ClassifiedSegment> segments, Neighbors neighbors) {
		List<ClassifiedSegment> connectedSegments = new ArrayList<ClassifiedSegment>();
		for (ClassifiedSegment segment : segments) {
			if (isYConnected(segment.segment, ySegment, neighbors)) {
				connectedSegments.add(segment);
			}
		}
		return connectedSegments;
	}
	
	private boolean isYConnected(BlockSet below, BlockSet at, Neighbors neighbors) {
		// NOTE: below is strictly below y, at is strictly at y
		Coords neighborCoords = new Coords(0, 0, 0);
		for (Coords coords : at) {
			for (int i = 0; i < neighbors.getNumNeighbors(); i++) {
				neighbors.getNeighbor(neighborCoords, coords, i);
				if (neighborCoords.y == coords.y - 1 && below.contains(neighborCoords)) {
					return true;
				}
			}
		}
		return false;
	}
}
