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
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import cuchaz.modsShared.Util;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.propulsion.Propulsion;

public class ShipPhysics
{
	private static final double AccelerationGravity = Util.perSecond2ToPerTick2( 9.8 );
	private static final double AirViscosity = 0.1;
	private static final double WaterViscosity = 3.0;
	private static final double AngularViscosityScale = 0.06;
	private static final double BaseLinearDrag = Util.perSecondToPerTick( 0.01 );
	private static final double BaseAngularDrag = Util.perSecondToPerTick( 1 );
	private static final float AngularAccelerationFactor = 20.0f;
	private static final int NumSimulationTicks = 20*Util.TicksPerSecond;
	
	private static class DisplacementEntry
	{
		public int numBlocksAtSurface;
		public int numBlocksUnderwater;
		public int numFillableBlocks;
		
		public DisplacementEntry( )
		{
			numBlocksAtSurface = 0;
			numBlocksUnderwater = 0;
			numFillableBlocks = 0;
		}
	}
	
	public static class AccelerationEntry
	{
		public double speed;
		public double accelerationDueToThrust;
		public double accelerationDueToDrag;
		
		public AccelerationEntry( double speed, double accelerationDueToThrust, double accelerationDueToDrag )
		{
			this.speed = speed;
			this.accelerationDueToThrust = accelerationDueToThrust;
			this.accelerationDueToDrag = accelerationDueToDrag;
		}
	}
	
	public static class SimulationResult
	{
		public double topSpeed;
		public double elapsedTicks;
		
		public SimulationResult( double topSpeed, double elapsedSeconds )
		{
			this.topSpeed = topSpeed;
			this.elapsedTicks = elapsedSeconds;
		}
	}
	
	private BlocksStorage m_blocks;
	private TreeMap<Integer,DisplacementEntry> m_displacement;
	private double m_shipMass;
	private Vec3 m_centerOfMass;
	private Double m_equilibriumWaterHeight;
	private Double m_sinkWaterHeight;
	
	public ShipPhysics( BlocksStorage blocks )
	{
		m_blocks = blocks;
		
		// get all the watertight blocks
		BlockSet watertightBlocks = new BlockSet();
		for( Coords coords : m_blocks.coords() )
		{
			if( MaterialProperties.isWatertight( getBlock( coords ) ) )
			{
				watertightBlocks.add( coords );
			}
		}
		
		int minY = m_blocks.getBoundingBox().minY;
		int maxY = m_blocks.getBoundingBox().maxY;
		
		// initialize displacement
		m_displacement = new TreeMap<Integer,DisplacementEntry>();
		for( int y=minY; y<=maxY+1; y++ )
		{
			m_displacement.put( y, new DisplacementEntry() );
		}

		// compute displacement for the ship blocks
		for( Coords coords : watertightBlocks )
		{
			for( int y=maxY+1; y>=coords.y; y-- )
			{
				DisplacementEntry entry = m_displacement.get( y );
				if( y == coords.y )
				{
					entry.numBlocksAtSurface++;
				}
				else
				{
					entry.numBlocksUnderwater++;
				}
			}
		}
		
		// update displacement for trapped air blocks
		for( int y=minY; y<=maxY+1; y++ )
		{
			DisplacementEntry entry = m_displacement.get( y );
			for( Coords coords : m_blocks.getGeometry().getTrappedAir( y ) )
			{
				if( y == coords.y )
				{
					entry.numBlocksAtSurface++;
				}
				else
				{
					entry.numBlocksUnderwater++;
				}
			}
		}
		
		// compute the total mass
		m_shipMass = 0.0;
		for( Coords coords : m_blocks.coords() )
		{
			m_shipMass += MaterialProperties.getMass( getBlock( coords ) );
		}
		
		// compute some extra stuff
		m_centerOfMass = computeCenterOfMass();
		m_equilibriumWaterHeight = computeEquilibriumWaterHeight();
		m_sinkWaterHeight = computeSinkWaterHeight();
	}
	
	public double getMass( )
	{
		return m_shipMass;
	}
	
	public Vec3 getCenterOfMass( )
	{
		return m_centerOfMass;
	}
	
	public double getNetUpAcceleration( double waterHeight )
	{
		// the net up force is the difference of the weight and the buoyancy
		return ( getDisplacedWaterMass( waterHeight ) - m_shipMass )*AccelerationGravity/m_shipMass;
	}
	
	public double getDisplacedWaterMass( double waterHeight )
	{
		// get the surface block level
		int surfaceLevel = MathHelper.floor_double( waterHeight );
		DisplacementEntry entry = m_displacement.get( surfaceLevel );
		
		// compute the mass of the displaced water
		double displacedWaterMass = 0;
		if( entry != null )
		{
			double surfaceFraction = getBlockFractionSubmerged( surfaceLevel, waterHeight );
			displacedWaterMass = ( (double)entry.numBlocksUnderwater + (double)entry.numBlocksAtSurface*surfaceFraction )*getWaterBlockMass();
		}
		
		return displacedWaterMass;
	}
	
	public Double getEquilibriumWaterHeight( )
	{
		return m_equilibriumWaterHeight;
	}
	
	public boolean willItFloat( )
	{
		return m_equilibriumWaterHeight != null;
	}
	
	public double getLinearAccelerationDueToThrust( Propulsion propulsion, double speed )
	{
		// thrust is in N (which is Kg*m/s/s) and mass is in Kg
		return propulsion.getTotalThrust( speed )/m_shipMass;
	}
	
	public double getLinearAccelerationDueToDrag( Vec3 velocity, double waterHeight )
	{
		// UNDONE: it may be more efficient to cache these computations
		
		// which side is the leading side?
		BlockSide leadingSide = null;
		double bestDot = Double.NEGATIVE_INFINITY;
		for( BlockSide side : BlockSide.values() )
		{
			double dot = side.getDx()*velocity.xCoord + side.getDz()*velocity.zCoord;
			if( dot > bestDot )
			{
				bestDot = dot;
				leadingSide = side;
			}
		}
		assert( leadingSide != null );
		
		// compute the viscosity
		double airSurfaceArea = 0;
		double waterSurfaceArea = 0;
		for( Coords coords : m_blocks.getGeometry().getEnvelopes().getEnvelope( leadingSide ).toBlockSet() )
		{
			double fractionSubmerged = leadingSide.getFractionSubmerged( coords.y, waterHeight );
			waterSurfaceArea += fractionSubmerged;
			airSurfaceArea += 1 - fractionSubmerged;
		}
		double linearViscosity = AirViscosity*airSurfaceArea + WaterViscosity*waterSurfaceArea;
		
		// how fast are we going?
		double speed = velocity.lengthVector();
		
		// compute the drag force using a quadratic drag approximation
		return BaseLinearDrag + speed*speed*linearViscosity/m_shipMass;
	}
	
	public float getAngularAccelerationDueToThrust( Propulsion propulsion )
	{
		return (float)getLinearAccelerationDueToThrust( propulsion, 0 )*AngularAccelerationFactor;
	}
	
	public float getAngularAccelerationDueToDrag( float motionYaw, double waterHeight )
	{
		// compute the viscosity in both directions
		double angularViscosity = 0
			+ getAngularViscosity( BlockSide.North, waterHeight, m_centerOfMass.xCoord )
			+ getAngularViscosity( BlockSide.East, waterHeight, m_centerOfMass.zCoord );
		
		return (float)( BaseAngularDrag + motionYaw*motionYaw*angularViscosity/m_shipMass );
	}
	
	public List<AccelerationEntry> getLinearAcceleration( Propulsion propulsion, double stopSpeed, int numSteps )
	{
		if( m_equilibriumWaterHeight == null )
		{
			throw new IllegalArgumentException( "Cannot compute acceleration for a non-buoyant ship!" );
		}
		
		List<AccelerationEntry> entries = new ArrayList<AccelerationEntry>( numSteps );
		Vec3 velocity = Vec3.createVectorHelper( 0, 0, 0 );
		for( int i=0; i<numSteps; i++ )
		{
			double speed = interpolateSpeed( stopSpeed, numSteps, i );
			velocity.xCoord = speed*propulsion.getFrontSide().getDx();
			velocity.zCoord = speed*propulsion.getFrontSide().getDz();
			
			entries.add( new AccelerationEntry(
				speed,
				getLinearAccelerationDueToThrust( propulsion, speed ),
				getLinearAccelerationDueToDrag( velocity, m_equilibriumWaterHeight )
			) );
		}
		return entries;
	}
	
	public List<AccelerationEntry> getAngularAcceleration( Propulsion propulsion, double stopSpeed, int numSteps )
	{
		if( m_equilibriumWaterHeight == null )
		{
			throw new IllegalArgumentException( "Cannot compute acceleration for a non-buoyant ship!" );
		}
		
		List<AccelerationEntry> entries = new ArrayList<AccelerationEntry>( numSteps );
		for( int i=0; i<numSteps; i++ )
		{
			double speed = interpolateSpeed( stopSpeed, numSteps, i );
			
			entries.add( new AccelerationEntry(
				speed,
				getAngularAccelerationDueToThrust( propulsion ),
				getAngularAccelerationDueToDrag( (float)speed, m_equilibriumWaterHeight )
			) );
		}
		return entries;
	}
	
	private double interpolateSpeed( double stopSpeed, int numSteps, int i )
	{
		return (double)i/(double)( numSteps - 1 ) * stopSpeed;
	}

	public SimulationResult simulateLinearAcceleration( Propulsion propulsion )
	{
		if( m_equilibriumWaterHeight == null )
		{
			throw new IllegalArgumentException( "Cannot simulate acceleration for a non-buoyant ship!" );
		}
		
		// discrete-time simulation of forward acceleration from rest
		double speed = 0;
		Vec3 velocity = Vec3.createVectorHelper( 0, 0, 0 );
		int i = 0;
		for( ; i<NumSimulationTicks; i++ )
		{
			velocity.xCoord = speed*propulsion.getFrontSide().getDx();
			velocity.zCoord = speed*propulsion.getFrontSide().getDz();
			
			double thrustAcceleration = getLinearAccelerationDueToThrust( propulsion, speed );
			double dragAcceleration = getLinearAccelerationDueToDrag( velocity, m_equilibriumWaterHeight );
			dragAcceleration = Math.min( speed + thrustAcceleration, dragAcceleration );
			double netAcceleration = thrustAcceleration - dragAcceleration;
			speed += netAcceleration;
			
			// did the speed stop changing?
			if( Math.abs( netAcceleration ) < 1e-4 )
			{
				break;
			}
		}
		return new SimulationResult( speed, i );
	}
	
	public SimulationResult simulateAngularAcceleration( Propulsion propulsion )
	{
		if( m_equilibriumWaterHeight == null )
		{
			throw new IllegalArgumentException( "Cannot simulate acceleration for a non-buoyant ship!" );
		}
		
		// determine the top speed numerically
		// again, I'm too lazy to write down the equations and solve them analytically...
		double thrustAcceleration = getAngularAccelerationDueToThrust( propulsion );
		float speed = 0;
		int i = 0;
		for( ; i<NumSimulationTicks; i++ )
		{
			double dragAcceleration = getAngularAccelerationDueToDrag( speed, m_equilibriumWaterHeight );
			dragAcceleration = Math.min( speed + thrustAcceleration, dragAcceleration );
			double netAcceleration = thrustAcceleration - dragAcceleration;
			speed += netAcceleration;
			
			// did the speed stop changing?
			if( Math.abs( netAcceleration ) < 1e-4 )
			{
				break;
			}
		}
		return new SimulationResult( speed, i );
	}
	
	public String dumpBlockProperties( )
	{
		StringBuilder buf = new StringBuilder();
		for( Coords coords : m_blocks.coords() )
		{
			double mass = MaterialProperties.getMass( getBlock( coords ) );
			buf.append( String.format( "%3d,%3d,%3d %4d %4.1f\n", coords.x, coords.y, coords.z, m_blocks.getBlock( coords ).id, mass ) );
		}
		return buf.toString();
	}
	
	private Double computeEquilibriumWaterHeight( )
	{
		// travel up each layer until we find the one that displaces too much water
		int minY = m_blocks.getBoundingBox().minY;
		int maxY = m_blocks.getBoundingBox().maxY;
		for( int y=minY; y<=maxY+1; y++ )
		{
			// assume water completely submerges this layer
			DisplacementEntry entry = m_displacement.get( y );
			double displacedWaterMass = ( entry.numBlocksUnderwater + entry.numBlocksAtSurface )*getWaterBlockMass();
			
			// did we displace too much water?
			if( displacedWaterMass > m_shipMass )
			{
				// good, the water height is in this block level
				
				// now solve for the water height
				return y + ( m_shipMass - entry.numBlocksUnderwater*getWaterBlockMass() )/entry.numBlocksAtSurface/getWaterBlockMass();
			}
		}
		
		// The ship will sink!
		return null;
	}
	
	private Double computeSinkWaterHeight( )
	{
		for( DisplacementEntry entry : m_displacement.descendingMap().values() )
		{
			// UNDONE: write this. Need double-value entries
		}
	}
	
	private Vec3 computeCenterOfMass( )
	{
		Vec3 com = Vec3.createVectorHelper( 0, 0, 0 );
		double totalMass = 0.0;
		for( Coords coords : m_blocks.coords() )
		{
			double mass = MaterialProperties.getMass( getBlock( coords ) );
			totalMass += mass;
			com.xCoord += mass*( coords.x + 0.5 );
			com.yCoord += mass*( coords.y + 0.5 );
			com.zCoord += mass*( coords.z + 0.5 );
		}
		com.xCoord /= totalMass;
		com.yCoord /= totalMass;
		com.zCoord /= totalMass;
		return com;
	}
	
	private double getBlockFractionSubmerged( int y, double waterHeight )
	{
		// can use any NSEW side
		return BlockSide.North.getFractionSubmerged( y, waterHeight );
	}
	
	private double getAngularViscosity( BlockSide side, double waterHeight, double center )
	{
		int centerCoord = (int)center;
		double viscosity = 0;
		for( Coords coords : m_blocks.getGeometry().getEnvelopes().getEnvelope( side ).toBlockSet() )
		{
			double fractionSubmerged = side.getFractionSubmerged( coords.y, waterHeight );
			double dist = Math.abs( side.getU( coords.x, coords.y, coords.z ) - centerCoord );
			viscosity += ( fractionSubmerged*WaterViscosity + ( 1 - fractionSubmerged )*AirViscosity )*dist;
		}
		return viscosity*AngularViscosityScale;
	}
	
	private Block getBlock( Coords coords )
	{
		return Block.blocksList[m_blocks.getBlock( coords ).id];
	}
	
	private double getWaterBlockMass( )
	{
		// UNDONE: use block y make the mass increase with depth
		return MaterialProperties.getMass( Block.waterStill );
	}
}
