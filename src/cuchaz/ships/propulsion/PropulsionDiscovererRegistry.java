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
import java.util.List;

public class PropulsionDiscovererRegistry {
	
	private static List<PropulsionDiscoverer> m_discoverers;
	
	static {
		m_discoverers = new ArrayList<PropulsionDiscoverer>();
		addDiscoverer(new SailDiscoverer());
	}
	
	public static void addDiscoverer(PropulsionDiscoverer discoverer) {
		m_discoverers.add(discoverer);
	}
	
	public static Iterable<PropulsionDiscoverer> discoverers() {
		return m_discoverers;
	}
}
