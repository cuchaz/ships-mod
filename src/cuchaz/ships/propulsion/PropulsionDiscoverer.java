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

import java.util.List;

import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.ships.BlocksStorage;

public interface PropulsionDiscoverer {
	
	public List<PropulsionMethod> getPropulsionMethods(BlocksStorage blocksStorage, BlockSide frontDirection);
}
