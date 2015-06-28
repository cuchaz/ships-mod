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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import cuchaz.modsShared.Util;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.config.BlockProperties;
import cuchaz.ships.propulsion.Propulsion;

public class ShipPhysics {
	
	private static final double AccelerationGravity = Util.perSecond2ToPerTick2(9.8);
	private static final double AirViscosity = 0.1;
	private static final double WaterViscosity = 3.0;
	private static final double AngularViscosityScale = 0.06;
	private static final double BaseLinearDrag = Util.perSecondToPerTick(0.01);
	private static final double BaseAngularDrag = Util.perSecondToPerTick(1);
	private static final float AngularAccelerationFactor = 20.0f;
	private static final int NumSimulationTicks = 20 * Util.TicksPerSecond;
	
	public static class AccelerationEntry {
		
		public double speed;
		public double accelerationDueToThrust;
		public double accelerationDueToDrag;
		
		public AccelerationEntry(double speed, double accelerationDueToThrust, double accelerationDueToDrag) {
			this.speed = speed;
			this.accelerationDueToThrust = accelerationDueToThrust;
			this.accelerationDueToDrag = accelerationDueToDrag;
		}
	}
	
	public static class SimulationResult {
		
		public double topSpeed;
		public double elapsedTicks;
		
		public SimulationResult(double topSpeed, double elapsedSeconds) {
			this.topSpeed = topSpeed;
			this.elapsedTicks = elapsedSeconds;
		}
	}
	
	private static class ScaledDisplacementEntry {
		
		double surfaceDisplacement;
		double underwaterDisplacement;
	}
	
	private BlocksStorage m_blocks;
	private double m_shipMass;
	private Vec3 m_centerOfMass;
	private Double m_equilibriumWaterHeight;
	private Integer m_sinkWaterHeight;
	private Map<Integer,ScaledDisplacementEntry> m_displacement;
	
	public ShipPhysics(BlocksStorage blocks) {
		m_blocks = blocks;
		
		// compute the total mass
		m_shipMass = 0.0;
		for (Coords coords : m_blocks.coords()) {
			m_shipMass += BlockProperties.getMass(getBlock(coords));
		}
		
		// compute some extra stuff
		m_centerOfMass = computeCenterOfMass();
		m_displacement = new TreeMap<Integer,ScaledDisplacementEntry>();
		m_equilibriumWaterHeight = computeEquilibriumWaterHeight();
		m_sinkWaterHeight = m_blocks.getDisplacement().getLastFillY();
		
		// is the ship unsinkable?
		ScaledDisplacementEntry lastDisplacement = getScaledDisplacement(m_blocks.getDisplacement().getMaxY() + 1);
		if (lastDisplacement.surfaceDisplacement + lastDisplacement.underwaterDisplacement > m_shipMass) {
			m_sinkWaterHeight = null;
		}
	}
	
	public double getMass() {
		return m_shipMass;
	}
	
	public Vec3 getCenterOfMass() {
		return m_centerOfMass;
	}
	
	public double getNetUpAcceleration(double waterHeight) {
		// the net up force is the difference of the weight and the buoyancy
		return (getDisplacedWaterMass(waterHeight) - m_shipMass) * AccelerationGravity / m_shipMass;
	}
	
	public double getDisplacedWaterMass(double waterHeight) {
		// get the surface block level
		int surfaceLevel = MathHelper.floor_double(waterHeight);
		
		// compute the mass of the displaced water
		double surfaceFraction = getBlockFractionSubmerged(surfaceLevel, waterHeight);
		return ((double)getUnderwaterDisplacement(surfaceLevel) + (double)getSurfaceDisplacement(surfaceLevel) * surfaceFraction) * getWaterBlockMass();
	}
	
	public Double getEquilibriumWaterHeight() {
		return m_equilibriumWaterHeight;
	}
	
	public Integer getSinkWaterHeight() {
		return m_sinkWaterHeight;
	}
	
	public boolean willItFloat() {
		return m_equilibriumWaterHeight != null;
	}
	
	public double getLinearAccelerationDueToThrust(Propulsion propulsion, double speed) {
		// thrust: f = ma, so a = f/m
		// returns: meters/tick/tick
		return propulsion.getTotalThrust(speed) / m_shipMass;
	}
	
	public double getLinearAccelerationDueToDrag(Vec3 velocity, double waterHeight) {
		// UNDONE: it may be more efficient to cache these computations
		
		// which side is the leading side?
		BlockSide leadingSide = null;
		double bestDot = Double.NEGATIVE_INFINITY;
		for (BlockSide side : BlockSide.values()) {
			double dot = side.getDx() * velocity.xCoord + side.getDz() * velocity.zCoord;
			if (dot > bestDot) {
				bestDot = dot;
				leadingSide = side;
			}
		}
		assert (leadingSide != null);
		
		// compute the viscosity
		double airSurfaceArea = 0;
		double waterSurfaceArea = 0;
		for (Coords coords : m_blocks.getGeometry().getEnvelopes().getEnvelope(leadingSide).toBlockSet()) {
			double fractionSubmerged = leadingSide.getFractionSubmerged(coords.y, waterHeight);
			waterSurfaceArea += fractionSubmerged;
			airSurfaceArea += 1 - fractionSubmerged;
		}
		double linearViscosity = AirViscosity * airSurfaceArea + WaterViscosity * waterSurfaceArea;
		
		// how fast are we going?
		double speed = velocity.lengthVector();
		
		// compute the drag force using a quadratic drag approximation
		return BaseLinearDrag + speed * speed * linearViscosity / m_shipMass;
	}
	
	public float getAngularAccelerationDueToThrust(Propulsion propulsion) {
		return (float)getLinearAccelerationDueToThrust(propulsion, 0) * AngularAccelerationFactor;
	}
	
	public float getAngularAccelerationDueToDrag(float motionYaw, double waterHeight) {
		// compute the viscosity in both directions
		double angularViscosity = 0 + getAngularViscosity(BlockSide.North, waterHeight, m_centerOfMass.xCoord) + getAngularViscosity(BlockSide.East, waterHeight, m_centerOfMass.zCoord);
		
		return (float) (BaseAngularDrag + motionYaw * motionYaw * angularViscosity / m_shipMass);
	}
	
	public List<AccelerationEntry> getLinearAcceleration(Propulsion propulsion, double stopSpeed, int numSteps) {
		if (m_equilibriumWaterHeight == null) {
			throw new IllegalArgumentException("Cannot compute acceleration for a non-buoyant ship!");
		}
		
		List<AccelerationEntry> entries = new ArrayList<AccelerationEntry>(numSteps);
		Vec3 velocity = Vec3.createVectorHelper(0, 0, 0);
		for (int i = 0; i < numSteps; i++) {
			double speed = interpolateSpeed(stopSpeed, numSteps, i);
			velocity.xCoord = speed * propulsion.getFrontSide().getDx();
			velocity.zCoord = speed * propulsion.getFrontSide().getDz();
			
			entries.add(new AccelerationEntry(speed, getLinearAccelerationDueToThrust(propulsion, speed), getLinearAccelerationDueToDrag(velocity, m_equilibriumWaterHeight)));
		}
		return entries;
	}
	
	public List<AccelerationEntry> getAngularAcceleration(Propulsion propulsion, double stopSpeed, int numSteps) {
		if (m_equilibriumWaterHeight == null) {
			throw new IllegalArgumentException("Cannot compute acceleration for a non-buoyant ship!");
		}
		
		List<AccelerationEntry> entries = new ArrayList<AccelerationEntry>(numSteps);
		for (int i = 0; i < numSteps; i++) {
			double speed = interpolateSpeed(stopSpeed, numSteps, i);
			
			entries.add(new AccelerationEntry(speed, getAngularAccelerationDueToThrust(propulsion), getAngularAccelerationDueToDrag((float)speed, m_equilibriumWaterHeight)));
		}
		return entries;
	}
	
	private double interpolateSpeed(double stopSpeed, int numSteps, int i) {
		return (double)i / (double) (numSteps - 1) * stopSpeed;
	}
	
	public SimulationResult simulateLinearAcceleration(Propulsion propulsion) {
		if (m_equilibriumWaterHeight == null) {
			throw new IllegalArgumentException("Cannot simulate acceleration for a non-buoyant ship!");
		}
		
		// discrete-time simulation of forward acceleration from rest
		double speed = 0;
		Vec3 velocity = Vec3.createVectorHelper(0, 0, 0);
		int i = 0;
		for (; i < NumSimulationTicks; i++) {
			velocity.xCoord = speed * propulsion.getFrontSide().getDx();
			velocity.zCoord = speed * propulsion.getFrontSide().getDz();
			
			double thrustAcceleration = getLinearAccelerationDueToThrust(propulsion, speed);
			double dragAcceleration = getLinearAccelerationDueToDrag(velocity, m_equilibriumWaterHeight);
			dragAcceleration = Math.min(speed + thrustAcceleration, dragAcceleration);
			double netAcceleration = thrustAcceleration - dragAcceleration;
			speed += netAcceleration;
			
			// did the speed stop changing?
			if (Math.abs(netAcceleration) < 1e-4) {
				break;
			}
		}
		return new SimulationResult(speed, i);
	}
	
	public SimulationResult simulateAngularAcceleration(Propulsion propulsion) {
		if (m_equilibriumWaterHeight == null) {
			throw new IllegalArgumentException("Cannot simulate acceleration for a non-buoyant ship!");
		}
		
		// determine the top speed numerically
		// again, I'm too lazy to write down the equations and solve them analytically...
		double thrustAcceleration = getAngularAccelerationDueToThrust(propulsion);
		float speed = 0;
		int i = 0;
		for (; i < NumSimulationTicks; i++) {
			double dragAcceleration = getAngularAccelerationDueToDrag(speed, m_equilibriumWaterHeight);
			dragAcceleration = Math.min(speed + thrustAcceleration, dragAcceleration);
			double netAcceleration = thrustAcceleration - dragAcceleration;
			speed += netAcceleration;
			
			// did the speed stop changing?
			if (Math.abs(netAcceleration) < 1e-4) {
				break;
			}
		}
		return new SimulationResult(speed, i);
	}
	
	public String dumpBlockProperties() {
		StringBuilder buf = new StringBuilder();
		for (Coords coords : m_blocks.coords()) {
			double mass = BlockProperties.getMass(getBlock(coords));
			buf.append(String.format("%3d,%3d,%3d %4d %4.1f\n", coords.x, coords.y, coords.z, m_blocks.getBlock(coords).block, mass));
		}
		return buf.toString();
	}
	
	private double getUnderwaterDisplacement(int y) {
		return getScaledDisplacement(y).underwaterDisplacement;
	}
	
	private double getSurfaceDisplacement(int y) {
		return getScaledDisplacement(y).surfaceDisplacement;
	}
	
	private ScaledDisplacementEntry getScaledDisplacement(int y) {
		ScaledDisplacementEntry entry = m_displacement.get(y);
		if (entry == null) {
			entry = new ScaledDisplacementEntry();
			entry.surfaceDisplacement = 0;
			for (Coords coords : m_blocks.getDisplacement().getSurfaceBlocks(y)) {
				entry.surfaceDisplacement += BlockProperties.getDisplacement(getBlock(coords));
			}
			entry.underwaterDisplacement = 0;
			for (Coords coords : m_blocks.getDisplacement().getUnderwaterBlocks(y)) {
				entry.underwaterDisplacement += BlockProperties.getDisplacement(getBlock(coords));
			}
		}
		return entry;
	}
	
	private Double computeEquilibriumWaterHeight() {
		// travel up each layer until we find the one that displaces too much water
		int minY = m_blocks.getBoundingBox().minY;
		int maxY = m_blocks.getBoundingBox().maxY;
		for (int y = minY; y <= maxY + 1; y++) {
			double underwaterDisplacement = getUnderwaterDisplacement(y);
			double surfaceDisplacement = getSurfaceDisplacement(y);
			
			// assume water completely submerges this layer
			double displacedWaterMass = (underwaterDisplacement + surfaceDisplacement) * getWaterBlockMass();
			
			// did we displace too much water?
			if (displacedWaterMass > m_shipMass) {
				// good, the water height is in this block level
				
				// now solve for the water height
				return y + (m_shipMass - underwaterDisplacement * getWaterBlockMass()) / surfaceDisplacement / getWaterBlockMass();
			}
		}
		
		// The ship will sink!
		return null;
	}
	
	private Vec3 computeCenterOfMass() {
		Vec3 com = Vec3.createVectorHelper(0, 0, 0);
		double totalMass = 0.0;
		for (Coords coords : m_blocks.coords()) {
			double mass = BlockProperties.getMass(getBlock(coords));
			totalMass += mass;
			com.xCoord += mass * (coords.x + 0.5);
			com.yCoord += mass * (coords.y + 0.5);
			com.zCoord += mass * (coords.z + 0.5);
		}
		com.xCoord /= totalMass;
		com.yCoord /= totalMass;
		com.zCoord /= totalMass;
		return com;
	}
	
	private double getBlockFractionSubmerged(int y, double waterHeight) {
		// can use any NSEW side
		return BlockSide.North.getFractionSubmerged(y, waterHeight);
	}
	
	private double getAngularViscosity(BlockSide side, double waterHeight, double center) {
		int centerCoord = (int)center;
		double viscosity = 0;
		for (Coords coords : m_blocks.getGeometry().getEnvelopes().getEnvelope(side).toBlockSet()) {
			double fractionSubmerged = side.getFractionSubmerged(coords.y, waterHeight);
			double dist = Math.abs(side.getU(coords.x, coords.y, coords.z) - centerCoord);
			viscosity += (fractionSubmerged * WaterViscosity + (1 - fractionSubmerged) * AirViscosity) * dist;
		}
		return viscosity * AngularViscosityScale;
	}
	
	private Block getBlock(Coords coords) {
		return m_blocks.getBlock(coords).block;
	}
	
	private double getWaterBlockMass() {
		// UNDONE: use block y make the mass increase with depth
		return BlockProperties.getMass(Blocks.water);
	}
}
