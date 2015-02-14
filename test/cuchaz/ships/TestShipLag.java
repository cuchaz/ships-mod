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

import static org.junit.Assert.*;
import org.junit.Test;

import cuchaz.modsShared.perf.Profiler;
import cuchaz.ships.persistence.BlockStoragePersistence;

public class TestShipLag {
	
	private static final String BigShip = "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";
	
	// @Test
	public void testShipGeometry() throws Exception {
		new MinecraftRunner() {
			
			@Override
			public void onRun() throws Exception {
				System.out.println("Geometry:");
				Profiler.reset();
				
				BlocksStorage shipBlocks = BlockStoragePersistence.readAnyVersion(BigShip);
				
				// time it
				long time = System.currentTimeMillis();
				shipBlocks.getGeometry();
				long diff = System.currentTimeMillis() - time;
				
				System.out.println(String.format("Time: %.2fs", diff / 1000.0));
				System.out.println(Profiler.getReport());
				
				// this shouldn't take more than 0.1 seconds
				assertTrue(diff <= 100);
			}
		}.run();
	}
	
	// @Test
	public void testShipDisplacement() throws Exception {
		new MinecraftRunner() {
			
			@Override
			public void onRun() throws Exception {
				System.out.println("Displacement:");
				Profiler.reset();
				
				BlocksStorage shipBlocks = BlockStoragePersistence.readAnyVersion(BigShip);
				
				// time it
				long time = System.currentTimeMillis();
				shipBlocks.getDisplacement();
				long diff = System.currentTimeMillis() - time;
				
				System.out.println(String.format("Time: %.2fs", diff / 1000.0));
				System.out.println(Profiler.getReport());
				
				// this shouldn't take more than 0.1 seconds
				assertTrue(diff <= 100);
			}
		}.run();
	}
}
