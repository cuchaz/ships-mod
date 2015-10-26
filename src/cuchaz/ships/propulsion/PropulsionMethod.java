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

import cuchaz.modsShared.blocks.BlockSet;

public abstract class PropulsionMethod {
	
	private String m_name;
	private String m_namePlural;
	private BlockSet m_coords;
	
	protected PropulsionMethod(String name, String namePlural) {
		this(name, namePlural, new BlockSet());
	}
	
	protected PropulsionMethod(String name, String namePlural, BlockSet coords) {
		m_name = name;
		m_namePlural = namePlural;
		m_coords = coords;
	}
	
	public String getName() {
		return m_name;
	}
	
	public String getNamePlural() {
		return m_namePlural;
	}
	
	public BlockSet getCoords() {
		return m_coords;
	}
	
	public abstract double getThrust(double speed);
	
	public void update(double waterHeightInBlockSpace) {
		// do nothing by default
	}
}
