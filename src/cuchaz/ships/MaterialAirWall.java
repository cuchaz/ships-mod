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
package cuchaz.ships;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.MaterialTransparent;

public class MaterialAirWall extends MaterialTransparent {
	
	public MaterialAirWall(MapColor color) {
		super(color);
	}
	
	@Override
	public boolean blocksMovement() {
		// block movement so water won't flow into this material
		return true;
	}
	
	@Override
	public boolean isSolid() {
		// pretend we're solid so that we block rain
		// apparently this doesn't prevent player movement, or suffocate players, so we're all good
		return true;
	}
}
