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
package cuchaz.ships.propulsion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cuchaz.modsShared.blocks.BlockArray;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.BlocksStorage;
import cuchaz.ships.Ships;

public class Propulsion {
	
	public static class MethodCount {
		
		private int m_numInstances;
		private String m_name;
		private String m_namePlural;
		
		public MethodCount(PropulsionMethod method) {
			m_numInstances = 0;
			m_name = method.getName();
			m_namePlural = method.getNamePlural();
		}
		
		public String getName() {
			if (m_numInstances == 1) {
				return m_name;
			} else {
				return m_namePlural;
			}
		}
		
		@Override
		public String toString() {
			return String.format("%d %s", m_numInstances, getName());
		}
	}
	
	private BlocksStorage m_blocksStorage;
	private Coords m_helmCoords;
	private BlockSide m_frontSide;
	private List<PropulsionMethod> m_methods;
	private Map<Class<? extends PropulsionMethod>,MethodCount> m_typeCounts;
	
	public Propulsion(BlocksStorage blocksStorage) {
		m_blocksStorage = blocksStorage;
		
		// compute parameters
		m_helmCoords = findHelm(m_blocksStorage);
		
		m_methods = new ArrayList<PropulsionMethod>();
		if (m_blocksStorage.getShipType().isPaddleable()) {
			// use a paddle!
			m_methods.add(new Paddle());
			
			// front side is only decided after pilot interacts with the ship block
			m_frontSide = null;
		} else if (m_helmCoords != null) {
			m_frontSide = BlockSide.getByXZOffset(m_blocksStorage.getBlock(m_helmCoords).meta);
			
			// discover the propulsion types
			for (PropulsionDiscoverer discoverer : PropulsionDiscovererRegistry.discoverers()) {
				m_methods.addAll(discoverer.getPropulsionMethods(m_blocksStorage, m_frontSide));
			}
		} else {
			Ships.logger.warning("Non-paddleable ship doesn't have a helm! This wasn't supposed to happen, and will probably cause problems later!");
		}
		
		// count the types
		m_typeCounts = new HashMap<Class<? extends PropulsionMethod>,MethodCount>();
		for (PropulsionMethod method : m_methods) {
			MethodCount count = m_typeCounts.get(method.getClass());
			if (count == null) {
				count = new MethodCount(method);
				m_typeCounts.put(method.getClass(), count);
			}
			count.m_numInstances++;
		}
	}
	
	public BlockSide getFrontSide() {
		return m_frontSide;
	}
	
	public BlockArray getEnevelope() {
		BlockArray envelope = m_blocksStorage.getGeometry().getEnvelopes().getEnvelope(BlockSide.Top).newEmptyCopy();
		for (PropulsionMethod method : m_methods) {
			for (Coords coords : method.getCoords()) {
				// keep the top-most block
				Coords oldCoords = envelope.getBlock(coords.x, coords.z);
				if (oldCoords == null || coords.y > oldCoords.y) {
					envelope.setBlock(coords.x, coords.z, coords);
				}
			}
		}
		return envelope;
	}
	
	public Iterable<PropulsionMethod> methods() {
		return m_methods;
	}
	
	public Iterable<MethodCount> methodCounts() {
		return m_typeCounts.values();
	}
	
	public double getTotalThrust(double speed) {
		double totalThrust = 0;
		for (PropulsionMethod method : m_methods) {
			totalThrust += method.getThrust(speed);
		}
		return totalThrust;
	}
	
	public void update(double waterHeightInBlockSpace) {
		for (PropulsionMethod method : m_methods) {
			method.update(waterHeightInBlockSpace);
		}
	}
	
	public String dumpMethods() {
		StringBuilder buf = new StringBuilder();
		String delimiter = "";
		for (Propulsion.MethodCount count : methodCounts()) {
			buf.append(delimiter);
			buf.append(count.toString());
			delimiter = ", ";
		}
		return buf.toString();
	}
	
	private Coords findHelm(BlocksStorage blocksStorage) {
		// UNDONE: optimize this by setting the helm coords instead of searching for the helm
		
		// always return the direction the helm is facing
		for (Coords coords : blocksStorage.coords()) {
			if (blocksStorage.getBlock(coords).block == Ships.m_blockHelm) {
				return coords;
			}
		}
		return null;
	}
}
