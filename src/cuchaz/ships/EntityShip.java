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
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.BlockSide;
import cuchaz.modsShared.BlockUtils;
import cuchaz.modsShared.CircleRange;
import cuchaz.modsShared.CompareReal;
import cuchaz.modsShared.RotatedBB;
import cuchaz.ships.packets.PacketPilotShip;
import cuchaz.ships.propulsion.Propulsion;

public class EntityShip extends Entity 
{
	public static final int LinearThrottleMax = 100;
	public static final int LinearThrottleMin = -25;
	public static final int LinearThrottleStep = 2;
	public static final int AngularThrottleMax = 1;
	public static final int AngularThrottleMin = -1;
	
	// data watcher IDs. Entity uses [0,1]. We can use [2,31]
	private static final int WatcherIdWaterHeight = 3;
	
	public float motionYaw;
	public int linearThrottle;
	public int angularThrottle;
	
	private ShipWorld m_shipWorld;
	private ShipPhysics m_physics;
	private Propulsion m_propulsion;
	private double m_shipBlockX;
	private double m_shipBlockY;
	private double m_shipBlockZ;
	private int m_pilotActions;
	private int m_oldPilotActions;
	private BlockSide m_sideShipForward;
	private boolean m_sendPilotChangesToServer;
	private boolean m_hasInfoFromServer;
	private double m_xFromServer;
	private double m_yFromServer;
	private double m_zFromServer;
	private float m_yawFromServer;
	private float m_pitchFromServer;
	private TreeSet<ChunkCoordinates> m_previouslyDisplacedWaterBlocks;
	private ShipCollider m_collider;
	
	public EntityShip( World world )
	{
		super( world );
		yOffset = 0.0f;
		motionX = 0.0;
		motionY = 0.0;
		motionZ = 0.0;
		motionYaw = 0.0f;
		linearThrottle = 0;
		angularThrottle = 0;
		
		m_shipWorld = null;
		m_physics = null;
		m_propulsion = null;
		m_shipBlockX = 0;
		m_shipBlockY = 0;
		m_shipBlockZ = 0;
		m_pilotActions = 0;
		m_oldPilotActions = 0;
		m_sideShipForward = null;
		m_sendPilotChangesToServer = false;
		m_hasInfoFromServer = false;
		m_xFromServer = 0;
		m_yFromServer = 0;
		m_zFromServer = 0;
		m_yawFromServer = 0;
		m_pitchFromServer = 0;
		m_previouslyDisplacedWaterBlocks = null;
		m_collider = new ShipCollider( this );
	}
	
	@Override
	protected void entityInit( )
	{
		// this gets called inside super.Entity( World )
		// it seems to be used to init the data watcher
		
		// allocate a slot for the block data
		dataWatcher.addObject( WatcherIdWaterHeight, -1 );
	}
	
	public void setShipWorld( ShipWorld shipWorld )
	{
		// if the blocks are invalid, just kill the ship
		if( !shipWorld.isValid() )
		{
			setDead();
			Ships.logger.warning( "Ship world is invalid. Killed ship." );
			return;
		}
		
		// reset the motion again. For some reason, the client entity gets bogus velocities from somewhere...
		motionX = 0.0;
		motionY = 0.0;
		motionZ = 0.0;
		motionYaw = 0.0f;
		
		m_shipWorld = shipWorld;
		shipWorld.setShip( this );
		m_physics = new ShipPhysics( m_shipWorld.getBlocksStorage() );
		m_propulsion = new Propulsion( m_shipWorld.getBlocksStorage() );
		
		// get the ship center of mass so we can convert between ship/block spaces
		Vec3 centerOfMass = m_physics.getCenterOfMass();
		m_shipBlockX = -centerOfMass.xCoord;
		m_shipBlockY = -centerOfMass.yCoord;
		m_shipBlockZ = -centerOfMass.zCoord;
		
		m_collider.computeShipBoundingBox( boundingBox, posX, posY, posZ, rotationYaw );
		
		// LOGGING
		Ships.logger.info( String.format(
			"%s EntityShip %d initialized at (%.2f,%.2f,%.2f) + (%.4f,%.4f,%.4f)",
			worldObj.isRemote ? "CLIENT" : "SERVER",
			entityId,
			posX, posY, posZ,
			motionX, motionY, motionZ
		) );
		
		ShipLocator.registerShip( this );
	}
	
	@Override
	public void setDead( )
	{
		super.setDead();
		
		// LOGGING
		Ships.logger.info( String.format( "%s EntityShip %d died!",
			worldObj.isRemote ? "CLIENT" : "SERVER",
			entityId
		) );
		
		if( !worldObj.isRemote )
		{
			// remove all the air wall blocks
			if( m_previouslyDisplacedWaterBlocks != null )
			{
				for( ChunkCoordinates coords : m_previouslyDisplacedWaterBlocks )
				{
					if( worldObj.getBlockId( coords.posX, coords.posY, coords.posZ ) == Ships.m_blockAirWall.blockID )
					{
						worldObj.setBlock( coords.posX, coords.posY, coords.posZ, Block.waterStill.blockID );
					}
				}
			}
		}
		else
		{
			// if riders are in a block, move them on top of the block
			for( Entity rider : getCollider().getRiders() )
			{
				// is the rider inside a block?
				List<AxisAlignedBB> worldBoxes = new ArrayList<AxisAlignedBB>();
				BlockUtils.getWorldCollisionBoxes( worldBoxes, rider.worldObj, rider.boundingBox );
				boolean inBlock = !worldBoxes.isEmpty();
				
				if( inBlock )
				{
					// move the rider to the top of the boxes
					double dy = 0;
					for( AxisAlignedBB box : worldBoxes )
					{
						dy = Math.max( dy, box.maxY - rider.boundingBox.minY );
					}
					rider.moveEntity( 0, dy, 0 );
				}
			}
		}
		
		ShipLocator.unregisterShip( this );
	}
	
	@Override
	protected void readEntityFromNBT( NBTTagCompound nbt )
	{
		setWaterHeight( nbt.getInteger( "waterHeight" ) );
		setShipWorld( new ShipWorld( worldObj, nbt.getByteArray( "blocks" ) ) );
	}
	
	@Override
	protected void writeEntityToNBT( NBTTagCompound nbt )
	{
		nbt.setInteger( "waterHeight", getWaterHeight() );
		nbt.setByteArray( "blocks", m_shipWorld.getData() );
	}
	
	public ShipWorld getShipWorld( )
	{
		return m_shipWorld;
	}
	
	public Propulsion getPropulsion( )
	{
		return m_propulsion;
	}
	
	public ShipCollider getCollider( )
	{
		return m_collider;
	}
	
	public int getWaterHeight( )
	{
		return dataWatcher.getWatchableObjectInt( WatcherIdWaterHeight );
	}
	public void setWaterHeight( int val )
	{
		dataWatcher.updateObject( WatcherIdWaterHeight, val );
	}
	
	@Override
	public boolean canBeCollidedWith()
    {
        return true;
    }
	
	@Override
	public void setPosition( double x, double y, double z )
	{
		posX = x;
        posY = y;
        posZ = z;
        
        if( m_collider != null )
        {
        	m_collider.computeShipBoundingBox( boundingBox, posX, posY, posZ, rotationYaw );
        }
	}
	
	@Override
	public void setPositionAndRotation2( double x, double y, double z, float yaw, float pitch, int alwaysThree )
	{
		// NOTE: this function should really be called onGetUpdatedPositionFromServer()
		// also, server positions are off by as much as 0.03 in {x,y,z}
		
		// just save the info and we'll deal with it on the next update tick
		m_hasInfoFromServer = true;
		m_xFromServer = x;
		m_yFromServer = y;
		m_zFromServer = z;
		m_yawFromServer = yaw;
		m_pitchFromServer = pitch;
	}
	
	@Override
	public void onUpdate( )
	{
		// did we die already?
		if( isDead )
		{
			return;
		}
		
		// don't do any updating until we get blocks
		if( m_shipWorld == null )
		{
			return;
		}
		
		double waterHeightInBlockSpace = shipToBlocksY( worldToShipY( getWaterHeight() ) );
		
		adjustMotionDueToGravityAndBuoyancy( waterHeightInBlockSpace );
		adjustMotionDueToThrustAndDrag( waterHeightInBlockSpace );
		
		double dx = motionX;
		double dy = motionY;
		double dz = motionZ;
		float dYaw = motionYaw;
		
		// did we get an updated position from the server?
		if( m_hasInfoFromServer )
		{
			// position deltas are easy
			dx += m_xFromServer - posX;
			dy += m_yFromServer - posY;
			dz += m_zFromServer - posZ;
			
			// we need fancy math to get the correct rotation delta
			double yawRadClient = CircleRange.mapMinusPiToPi( Math.toRadians( rotationYaw ) );
			double yawRadServer = CircleRange.mapMinusPiToPi( Math.toRadians( m_yawFromServer ) );
			double yawDelta = CircleRange.newByShortSegment( yawRadClient, yawRadServer ).getLength();
			
			// was the rotation delta actually positive?
			if( !CompareReal.eq( CircleRange.mapMinusPiToPi( yawRadClient + yawDelta ), yawRadServer ) )
			{
				// nope. it's a negative delta
				yawDelta = -yawDelta;
			}
			yawDelta = Math.toDegrees( yawDelta );
			
			dYaw += yawDelta;
			
			// just apply the pitch directly
			rotationPitch = m_pitchFromServer;
			
			m_hasInfoFromServer = false;
		}
		
		/* LOGGING
		Ships.logger.fine( String.format( "%s Ship movement: p=(%.4f,%.4f,%.4f), d=(%.4f,%.4f,%.4f), dYaw=%.1f",
			worldObj.isRemote ? "CLIENT" : "SERVER",
			posX, posY, posZ,
			dx, dy, dz, dYaw
		) );
		*/
		
		// did we even move a noticeable amount?
		final double Epsilon = 1e-3;
		if( Math.abs( dx ) >= Epsilon || Math.abs( dy ) >= Epsilon || Math.abs( dz ) >= Epsilon || Math.abs( dYaw ) >= Epsilon )
		{
			List<Entity> riders = m_collider.getRiders();
			
			// save the old values
			prevPosX = posX;
			prevPosY = posY;
			prevPosZ = posZ;
			prevRotationYaw = rotationYaw;
			prevRotationPitch = rotationPitch;
			
			// move the ship
			m_collider.moveShip( dx, dy, dz, dYaw );
			
			// recalculate the deltas
			dx = posX - prevPosX;
			dy = posY - prevPosY;
			dz = posZ - prevPosZ;
			dYaw = rotationYaw - prevRotationYaw;
			
			moveWater( waterHeightInBlockSpace );
			moveRiders( riders, dx, dy, dz, dYaw );
		}
		
		// did the ship sink?
		if( isSunk( waterHeightInBlockSpace ) )
		{
			Ships.logger.info( String.format( "%s Ship Sunk!",
				worldObj.isRemote ? "CLIENT" : "SERVER"
			) );
			
			// unlaunch the ship at the bottom of the ocean
			ShipUnlauncher unlauncher = new ShipUnlauncher( this );
			unlauncher.snapToLaunchDirection();
			unlauncher.unlaunch();
			return;
		}
		
		// update the world
		m_shipWorld.updateEntities();
	}
	
	@Override
	public boolean interactFirst( EntityPlayer player )
	{
		// only do this on the client
		if( !worldObj.isRemote )
		{
			return false;
		}
		
		// find out what block the player is targeting
		TreeSet<MovingObjectPosition> intersections = m_collider.getBlocksPlayerIsLookingAt( player );
		if( intersections.isEmpty() )
		{
			// LOGGING
			Ships.logger.fine( String.format( "%s EntityShip.interact(): no hit",
				worldObj.isRemote ? "CLIENT" : "SERVER"
			) );
			
			// was there no hit? forward the interaction to the world
			clickWorldBlock( player, false );
			
			return false;
		}
		
		// just get the first intersected block (it's the closest one)
		// UNDONE: could optimize this by trying to find the closest block first... but we probably don't care for now
		MovingObjectPosition intersection = intersections.first();
		
		// activate the block
		Block block = Block.blocksList[m_shipWorld.getBlockId( intersection.blockX, intersection.blockY, intersection.blockZ )];
		
		// LOGGING
		Ships.logger.fine( String.format( "%s EntityShip.interact(): (%d,%d,%d) %s",
			worldObj.isRemote ? "CLIENT" : "SERVER",
			intersection.blockX, intersection.blockY, intersection.blockZ,
			block.getUnlocalizedName()
		) );
		
		return block.onBlockActivated(
			m_shipWorld,
			intersection.blockX, intersection.blockY, intersection.blockZ,
			player,
			intersection.sideHit,
			(float)intersection.hitVec.xCoord, (float)intersection.hitVec.yCoord, (float)intersection.hitVec.zCoord
		);
	}
	
	@Override
	public boolean hitByEntity( Entity attackingEntity )
	{
		// NOTE: return true to ignore the attack
		
		// only do this on the client
		if( !worldObj.isRemote )
		{
			return true;
		}
		
		// ignore attacks by non-players
		if( !( attackingEntity instanceof EntityPlayer ) )
		{
			return true;
		}
		EntityPlayer player = (EntityPlayer)attackingEntity;
		
		// LOGGING
		Ships.logger.info( String.format( "%s EntityShip.hitByEntity(): hit by player %s",
			worldObj.isRemote ? "CLIENT" : "SERVER",
			player.getDisplayName()
		) );
		
		// what did the player hit?
		TreeSet<MovingObjectPosition> intersections = m_collider.getBlocksPlayerIsLookingAt( player );
		if( !intersections.isEmpty() )
		{
			// ignore hits to ship blocks
		}
		else
		{
			// forward the interaction to the world
			clickWorldBlock( player, true );
		}
		
		return true;
	}
	
	private boolean isSunk( double waterHeight )
	{
		// is the ship completely underwater?
		boolean isUnderwater = waterHeight > m_shipWorld.getBoundingBox().maxY + 1.5;
		
		// UNDONE: will have to use something smarter for submarines!
		// UNDONE: also un-floodable ships like rafts
		return motionY == 0 && isUnderwater;
	}
	
	public void worldToShip( Vec3 v )
	{
		double x = worldToShipX( v.xCoord, v.zCoord );
		double y = worldToShipY( v.yCoord );
		double z = worldToShipZ( v.xCoord, v.zCoord );
		
		v.xCoord = x;
		v.yCoord = y;
		v.zCoord = z;
	}
	
	public void worldToShipDirection( Vec3 v )
	{
		// just apply the rotation
		float yawRad = (float)Math.toRadians( rotationYaw );
		float cos = MathHelper.cos( yawRad );
		float sin = MathHelper.sin( yawRad );
		double x = v.xCoord*cos - v.zCoord*sin;
		double z = v.xCoord*sin + v.zCoord*cos;
		
		v.xCoord = x;
		v.zCoord = z;
	}
	
	public double worldToShipX( double x, double z )
	{
		float yawRad = (float)Math.toRadians( rotationYaw );
		double cos = MathHelper.cos( yawRad );
		double sin = MathHelper.sin( yawRad );
		return ( x - posX )*cos - ( z - posZ )*sin;
	}
	
	public double worldToShipY( double y )
	{
		return y - posY;
	}
	
	public double worldToShipZ( double x, double z )
	{
		float yawRad = (float)Math.toRadians( rotationYaw );
		double cos = MathHelper.cos( yawRad );
		double sin = MathHelper.sin( yawRad );
		return ( x - posX )*sin + ( z - posZ )*cos;
	}
	
	public void shipToWorld( Vec3 v )
	{
		double x = shipToWorldX( v.xCoord, v.zCoord );
		double y = shipToWorldY( v.yCoord );
		double z = shipToWorldZ( v.xCoord, v.zCoord );
		
		v.xCoord = x;
		v.yCoord = y;
		v.zCoord = z;
	}
	
	public void shipToWorldDirection( Vec3 v )
	{
		// just apply the rotation
		float yawRad = (float)Math.toRadians( rotationYaw );
		float cos = MathHelper.cos( yawRad );
		float sin = MathHelper.sin( yawRad );
		double x = v.xCoord*cos + v.zCoord*sin;
		double z = -v.xCoord*sin + v.zCoord*cos;
		
		v.xCoord = x;
		v.zCoord = z;
	}
	
	public double shipToWorldX( double x, double z )
	{
		float yawRad = (float)Math.toRadians( rotationYaw );
		double cos = MathHelper.cos( yawRad );
		double sin = MathHelper.sin( yawRad );
		return x*cos + z*sin + posX;
	}
	
	public double shipToWorldY( double y )
	{
		return y + posY;
	}
	
	public double shipToWorldZ( double x, double z )
	{
		float yawRad = (float)Math.toRadians( rotationYaw );
		double cos = MathHelper.cos( yawRad );
		double sin = MathHelper.sin( yawRad );
		return -x*sin + z*cos + posZ;
	}
	
	public void shipToBlocks( Vec3 v )
	{
		v.xCoord = shipToBlocksX( v.xCoord );
		v.yCoord = shipToBlocksY( v.yCoord );
		v.zCoord = shipToBlocksZ( v.zCoord );
	}
	
	public double shipToBlocksX( double x )
	{
		return x - m_shipBlockX;
	}
	
	public double shipToBlocksY( double y )
	{
		return y - m_shipBlockY;
	}
	
	public double shipToBlocksZ( double z )
	{
		return z - m_shipBlockZ;
	}
	
	public void blocksToShip( Vec3 v )
	{
		v.xCoord = blocksToShipX( v.xCoord );
		v.yCoord = blocksToShipY( v.yCoord );
		v.zCoord = blocksToShipZ( v.zCoord );
	}
	
	public double blocksToShipX( double x )
	{
		return x + m_shipBlockX;
	}
	
	public double blocksToShipY( double y )
	{
		return y + m_shipBlockY;
	}
	
	public double blocksToShipZ( double z )
	{
		return z + m_shipBlockZ;
	}
	
	public RotatedBB worldToBlocks( AxisAlignedBB box )
	{
		// transform the box center into block space
		Vec3 center = Vec3.createVectorHelper(
			( box.minX + box.maxX )/2,
			( box.minY + box.maxY )/2,
			( box.minZ + box.maxZ )/2
		);
		worldToShip( center );
		shipToBlocks( center );
		
		// build a box of the same dimensions in blocks space
		double dxh = ( box.maxX - box.minX )/2;
		double dyh = ( box.maxY - box.minY )/2;
		double dzh = ( box.maxZ - box.minZ )/2;
		box = AxisAlignedBB.getBoundingBox(
			center.xCoord - dxh, center.yCoord - dyh, center.zCoord - dzh,
			center.xCoord + dxh, center.yCoord + dyh, center.zCoord + dzh
		);
		
		return new RotatedBB( box, -rotationYaw );
	}
	
	public RotatedBB blocksToWorld( AxisAlignedBB box )
	{
		// transform the box center into world space
		Vec3 center = Vec3.createVectorHelper(
			( box.minX + box.maxX )/2,
			( box.minY + box.maxY )/2,
			( box.minZ + box.maxZ )/2
		);
		blocksToShip( center );
		shipToWorld( center );
		
		// build a box of the same dimensions in world space
		double dxh = ( box.maxX - box.minX )/2;
		double dyh = ( box.maxY - box.minY )/2;
		double dzh = ( box.maxZ - box.minZ )/2;
		box = AxisAlignedBB.getBoundingBox(
			center.xCoord - dxh, center.yCoord - dyh, center.zCoord - dzh,
			center.xCoord + dxh, center.yCoord + dyh, center.zCoord + dzh
		);
		
		return new RotatedBB( box, rotationYaw );
	}
	
	private void adjustMotionDueToGravityAndBuoyancy( double waterHeightInBlockSpace )
	{
		/* only simulate buoyancy if we're outside of the epsilon for the equilibrium y pos
		final double EquilibriumWaterHeightEpsilon = 0.05;
		double distToEquilibrium = waterHeightInBlockSpace - m_physics.getEquilibriumWaterHeight();
		if( Math.abs( distToEquilibrium ) > EquilibriumWaterHeightEpsilon )
		{
		*/
		
		Vec3 velocity = Vec3.createVectorHelper( 0, motionY, 0 );
		
		double accelerationDueToBouyancy = m_physics.getNetUpAcceleration( waterHeightInBlockSpace );
		double accelerationDueToDrag = m_physics.getLinearAccelerationDueToDrag( velocity, waterHeightInBlockSpace );
		
		// make sure drag acceleration doesn't reverse the velocity!
		// NOTE: drag is always positive right now. We'll fix the sign later
		accelerationDueToDrag = Math.min( Math.abs( motionY + accelerationDueToBouyancy ), accelerationDueToDrag );
		
		// make sure drag opposes velocity
		if( Math.signum( accelerationDueToDrag ) == Math.signum( motionY ) )
		{
			accelerationDueToDrag *= -1;
		}
		
		motionY += accelerationDueToBouyancy + accelerationDueToDrag;
	}
	
	private void adjustMotionDueToThrustAndDrag( double waterHeightInBlockSpace )
	{
		// process pilot actions
		PilotAction.resetShip( this, m_pilotActions, m_oldPilotActions );
		PilotAction.applyToShip( this, m_pilotActions );
		m_oldPilotActions = m_pilotActions;
		
		// clamp the throttle
		if( linearThrottle < LinearThrottleMin )
		{
			linearThrottle = LinearThrottleMin;
		}
		if( linearThrottle > LinearThrottleMax )
		{
			linearThrottle = LinearThrottleMax;
		}
		
		// get the velocity direction
		double velocityDirX = motionX;
		double velocityDirZ = motionZ;
		double speed = Math.sqrt( velocityDirX*velocityDirX + velocityDirZ*velocityDirZ );
		if( speed > 0 )
		{
			velocityDirX /= speed;
			velocityDirZ /= speed;
		}
		
		// get the velocity in block coords
		Vec3 velocityInBlockCoords = Vec3.createVectorHelper( motionX, 0, motionZ );
		worldToShipDirection( velocityInBlockCoords );
		
		// compute the linear acceleration due to thrust
		double linearAccelerationDueToThrustX = 0;
		double linearAccelerationDueToThrustZ = 0;
		if( m_sideShipForward != null )
		{
			if( m_sendPilotChangesToServer )
			{
				// send a packet to the server
				PacketPilotShip packet = new PacketPilotShip( entityId, m_pilotActions, m_sideShipForward, linearThrottle, angularThrottle );
				PacketDispatcher.sendPacketToServer( packet.getCustomPacket() );
				m_sendPilotChangesToServer = false;
			}
			
			// compute the forward vector
			float yawRad = (float)Math.toRadians( rotationYaw );
			float cos = MathHelper.cos( yawRad );
			float sin = MathHelper.sin( yawRad );
			double forwardX = m_sideShipForward.getDx()*cos + m_sideShipForward.getDz()*sin;
			double forwardZ = -m_sideShipForward.getDx()*sin + m_sideShipForward.getDz()*cos;
			
			// compute the acceleration
			double linearAccelerationDueToThrust = m_physics.getLinearAccelerationDueToThrust( m_propulsion, speed )*linearThrottle/LinearThrottleMax;
			linearAccelerationDueToThrustX = forwardX*linearAccelerationDueToThrust;
			linearAccelerationDueToThrustZ = forwardZ*linearAccelerationDueToThrust;
		}
		
		// compute the linear acceleration due to drag
		double linearAccelerationDueToDrag = m_physics.getLinearAccelerationDueToDrag( velocityInBlockCoords, waterHeightInBlockSpace );
		
		// make sure drag acceleration doesn't reverse the velocity!
		double nextSpeedX = motionX + linearAccelerationDueToThrustX;
		double nextSpeedZ = motionZ + linearAccelerationDueToThrustZ;
		double nextSpeed = Math.sqrt( nextSpeedX*nextSpeedX + nextSpeedZ*nextSpeedZ );
		linearAccelerationDueToDrag = Math.min( nextSpeed, linearAccelerationDueToDrag );
		
		// apply the linear acceleration
		motionX += linearAccelerationDueToThrustX - velocityDirX*linearAccelerationDueToDrag;
		motionZ += linearAccelerationDueToThrustZ - velocityDirZ*linearAccelerationDueToDrag;
		
		// get the angular acceleration
		double angularAccelerationDueToThrust = m_physics.getAngularAccelerationDueToThrust( m_propulsion )*angularThrottle/AngularThrottleMax;
		double angularAccelerationDueToDrag = m_physics.getAngularAccelerationDueToDrag( motionYaw, waterHeightInBlockSpace );
		
		// make sure drag acceleration doesn't reverse the velocity!
		angularAccelerationDueToDrag = Math.min( Math.abs( motionYaw + angularAccelerationDueToThrust ), angularAccelerationDueToDrag );
		
		// make sure the drag is opposed to the velocity
		if( Math.signum( angularAccelerationDueToDrag ) == Math.signum( motionYaw ) )
		{
			angularAccelerationDueToDrag *= -1;
		}
		
		// TEMP
		//System.out.println( String.format( "%4.2f %4.2f", Util.perTick2ToPerSecond2( angularAccelerationDueToThrust ), Util.perTick2ToPerSecond2( angularAccelerationDueToDrag ) ) );
		
		// apply the angular acceleration
		motionYaw += angularAccelerationDueToThrust + angularAccelerationDueToDrag;
	}
	
	private void moveWater( double waterHeightBlocks )
	{
		// get all the trapped air blocks
		int surfaceLevelBlocks = MathHelper.floor_double( waterHeightBlocks );
		TreeSet<ChunkCoordinates> trappedAirBlocks = m_shipWorld.getGeometry().getTrappedAir( surfaceLevelBlocks );
		if( trappedAirBlocks.isEmpty() )
		{
			// the ship is out of the water
			return;
		}
		
		int surfaceLevelWorld = getWaterHeight();
		
		// find the world water blocks that intersect the trapped air blocks
		TreeSet<ChunkCoordinates> displacedWaterBlocks = new TreeSet<ChunkCoordinates>();
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox( 0, 0, 0, 0, 0, 0 );
		Vec3 p = Vec3.createVectorHelper( 0, 0, 0 );
		for( ChunkCoordinates coords : trappedAirBlocks )
		{
			// compute the bounding box for the air block
			p.xCoord = coords.posX + 0.5;
			p.yCoord = coords.posY + 0.5;
			p.zCoord = coords.posZ + 0.5;
			blocksToShip( p );
			shipToWorld( p );
			
			m_collider.getBlockWorldBoundingBox( box, coords );
			
			// grow the bounding box just a bit so we get more robust collisions
			final double Delta = 0.1;
			box = box.expand( Delta, Delta, Delta );
			
			// query for all the world water blocks that intersect it
			int minX = MathHelper.floor_double( box.minX );
			int maxX = MathHelper.floor_double( box.maxX );
			int minY = MathHelper.floor_double( box.minY );
			int maxY = Math.min( MathHelper.floor_double( box.maxY ), surfaceLevelWorld - 1 );
			int minZ = MathHelper.floor_double( box.minZ );
			int maxZ = MathHelper.floor_double( box.maxZ );
			for( int x=minX; x<=maxX; x++ )
			{
				for( int z=minZ; z<=maxZ; z++ )
				{
					for( int y=minY; y<=maxY; y++ )
					{
						Material material = worldObj.getBlockMaterial( x, y, z );
						if( material == Material.water || material == Material.air || material == Ships.m_materialAirWall )
						{
							displacedWaterBlocks.add( new ChunkCoordinates( x, y, z ) );
						}
					}
				}
			}
		}
		
		// which are new blocks to displace?
		for( ChunkCoordinates coords : displacedWaterBlocks )
		{
			if( m_previouslyDisplacedWaterBlocks == null || !m_previouslyDisplacedWaterBlocks.contains( coords ) )
			{
				worldObj.setBlock( coords.posX, coords.posY, coords.posZ, Ships.m_blockAirWall.blockID );
			}
		}
		
		// which blocks are no longer displaced?
		if( m_previouslyDisplacedWaterBlocks != null )
		{
			for( ChunkCoordinates coords : m_previouslyDisplacedWaterBlocks )
			{
				if( !displacedWaterBlocks.contains( coords ) )
				{
					worldObj.setBlock( coords.posX, coords.posY, coords.posZ, Block.waterStill.blockID );
					
					// UNDONE: can get the fill effect back by only turning the surface level into air?
					// or make a special wake block that will self-convert back to water
				}
			}
		}
		
		m_previouslyDisplacedWaterBlocks = displacedWaterBlocks;
	}
	
	private void moveRiders( List<Entity> riders, double dx, double dy, double dz, float dYaw )
	{
		Vec3 p = Vec3.createVectorHelper( 0, 0, 0 );
		
		// first, move the riders
		for( Entity rider : riders )
		{
			// apply rotation of position relative to the ship center
			p.xCoord = rider.posX + dx;
			p.zCoord = rider.posZ + dz;
			worldToShip( p );
			float yawRad = (float)Math.toRadians( dYaw );
			float cos = MathHelper.cos( yawRad );
			float sin = MathHelper.sin( yawRad );
			double x = p.xCoord*cos + p.zCoord*sin;
			double z = -p.xCoord*sin + p.zCoord*cos;
			p.xCoord = x;
			p.zCoord = z;
			shipToWorld( p );
			
			// apply the transformation
			rider.rotationYaw -= dYaw;
			rider.setPosition(
				p.xCoord,
				rider.posY + dy,
				p.zCoord
			);
		}
	}
	
	public void setPilotActions( int actions, BlockSide sideShipForward, boolean sendPilotChangesToServer )
	{
		m_pilotActions = actions;
		m_sideShipForward = sideShipForward;
		m_sendPilotChangesToServer = sendPilotChangesToServer;
	}
	
	@SideOnly( Side.CLIENT )
	private void clickWorldBlock( EntityPlayer player, boolean isLeftButton )
	{
		// what block is the player aiming at?
		PlayerControllerMP playerController = Minecraft.getMinecraft().playerController;
		MovingObjectPosition hit = player.rayTrace( playerController.getBlockReachDistance(), 0 );
		if( hit == null || hit.typeOfHit != EnumMovingObjectType.TILE )
		{
			return;
		}
		
		// what item is the player using?
		ItemStack heldItem = player.getHeldItem();
		
		// do the click
		// NOTE: this part emulates part of Minecraft.click()
		if( isLeftButton )
		{
			playerController.clickBlock( hit.blockX, hit.blockY, hit.blockZ, hit.sideHit );
		}
		else
		{
			boolean result = !ForgeEventFactory.onPlayerInteract( player, Action.RIGHT_CLICK_BLOCK, hit.blockX, hit.blockY, hit.blockZ, hit.sideHit ).isCanceled();
			if( result && playerController.onPlayerRightClick( player, worldObj, heldItem, hit.blockX, hit.blockY, hit.blockZ, hit.sideHit, hit.hitVec ) )
			{
				player.swingItem();
			}
			
			if( heldItem != null )
            {
				if( heldItem.stackSize == 0 )
				{
					player.inventory.mainInventory[player.inventory.currentItem] = null;
				}
				else if( playerController.isInCreativeMode() )
				{
					Minecraft.getMinecraft().entityRenderer.itemRenderer.resetEquippedProgress();
				}
            }
		}
	}
}
