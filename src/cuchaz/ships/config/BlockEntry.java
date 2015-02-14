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

public class BlockEntry {
	
	public double mass;
	public double displacement;
	public boolean isWatertight;
	public boolean isSeparator;
	public boolean isWater;
	
	public BlockEntry(double mass, double displacement, boolean isWatertight, boolean isSeparator, boolean isWater) {
		this.mass = mass;
		this.displacement = displacement;
		this.isWatertight = isWatertight;
		this.isSeparator = isSeparator;
		this.isWater = isWater;
	}
}
