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

import java.util.List;

import org.junit.Test;

import cuchaz.modsShared.Util;
import cuchaz.ships.persistence.BlockStoragePersistence;
import cuchaz.ships.propulsion.Propulsion;

public class TestShipSpeed {
	
	// yeah, I know these aren't really unit tests
	
	// @Test
	public void testLongShip5x3Sail() throws Exception {
		printSpeeds("H4sIAAAAAAAAAH3OWw6CQAxA0bYJf8RNyMpYiStWkZcKAjOxhMnkhv5M56QvEakkxPqPe0gL8XB7gD3BGrAXWAvWgfVgA9gI9gb7gH3BJrAZ7Ae2gK25eVr4f/E37r1G32tSS/pTUzA76Q1zL7e95oiyjrP8FoVbFOblpmAG8+IOgx0GO3JTMMttA4jxGjfUAgAA");
	}
	
	// @Test
	public void testShortShip5x3Sail() throws Exception {
		printSpeeds("H4sIAAAAAAAAAG2QWwqAMAwE82j/xEvoyTyJJ1dbrNbGWSiUIZOFiMh6tsidXJ7VDzCN7OMquAqugmvgGrgGroPr4Dq4CdwEbgI3g5vBHeZ6pq3d4kn5z/s4M/Qebb6yJdz4ZX3XwBSYwb6j8dhh8u+ITIEZ7KsdDh0u/47IFJhFdgHalVXR1AIAAA==");
	}
	
	// @Test
	public void testRaft() throws Exception {
		printSpeeds("H4sIAAAAAAAAAG3LwQkAIAwEwb2AP23S/ntQBMEY7h6CSwYYKw1o3Jmm2tKfaktTbcnKWBkrY8PYMPa7e+vzvBuNrylJCAEAAA==");
	}
	
	private void printSpeeds(final String encodedBlocks) throws Exception {
		final String methodName = Thread.currentThread().getStackTrace()[2].getMethodName().substring(4);
		new MinecraftRunner() {
			
			@Override
			public void onRun() throws Exception {
				BlocksStorage shipBlocks = BlockStoragePersistence.readAnyVersion(encodedBlocks);
				ShipPhysics physics = new ShipPhysics(shipBlocks);
				Propulsion propulsion = new Propulsion(shipBlocks);
				
				ShipPhysics.SimulationResult linearSim = physics.simulateLinearAcceleration(propulsion);
				ShipPhysics.SimulationResult angularSim = physics.simulateAngularAcceleration(propulsion);
				
				System.out.println(methodName + ", " + shipBlocks.getNumBlocks() + " blocks:");
				System.out.println("\t" + propulsion.dumpMethods());
				// System.out.print( dumpThrustAndDrag( physics, propulsion ) );
				System.out.println(String.format("\tMass: %.2f Kg", physics.getMass()));
				System.out.println(String.format("\tThrust: %.2f N", Util.perTick2ToPerSecond2(propulsion.getTotalThrust(0))));
				System.out.println(String.format("\tLinear speed: %.2f m/s", Util.perTickToPerSecond(linearSim.topSpeed)));
				System.out.println(String.format("\tTime to linear speed: %.2f s", Util.ticksToSeconds(linearSim.elapsedTicks)));
				System.out.println(String.format("\tAngular speed: %.2f deg/s", Util.perTickToPerSecond(angularSim.topSpeed)));
				System.out.println(String.format("\tTime to angular speed: %.2f s", Util.ticksToSeconds(angularSim.elapsedTicks)));
			}
		}.run(encodedBlocks, methodName);
	}
	
	@SuppressWarnings("unused")
	private String dumpThrustAndDrag(ShipPhysics physics, Propulsion propulsion) {
		double topLinearSpeed = Util.perSecondToPerTick(20);
		double topAngularSpeed = Util.perSecondToPerTick(90);
		int numSteps = 100;
		
		StringBuilder buf = new StringBuilder();
		buf.append("\tLinear Acceleration:\n");
		dumpAccelerations(buf, physics.getLinearAcceleration(propulsion, topLinearSpeed, numSteps));
		buf.append("\tAngular Acceleration:\n");
		dumpAccelerations(buf, physics.getAngularAcceleration(propulsion, topAngularSpeed, numSteps));
		return buf.toString();
	}
	
	private void dumpAccelerations(StringBuilder buf, List<ShipPhysics.AccelerationEntry> entries) {
		// print out the entries
		buf.append("\t\tSpeed");
		for (ShipPhysics.AccelerationEntry entry : entries) {
			buf.append(String.format(",%.4f", Util.perTickToPerSecond(entry.speed)));
		}
		buf.append("\n");
		buf.append("\t\tThrust");
		for (ShipPhysics.AccelerationEntry entry : entries) {
			buf.append(String.format(",%.4f", Util.perTick2ToPerSecond2(entry.accelerationDueToThrust)));
		}
		buf.append("\n");
		buf.append("\t\tDrag");
		for (ShipPhysics.AccelerationEntry entry : entries) {
			buf.append(String.format(",%.4f", Util.perTick2ToPerSecond2(entry.accelerationDueToDrag)));
		}
		buf.append("\n");
	}
}
