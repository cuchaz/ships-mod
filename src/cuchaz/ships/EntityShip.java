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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.modsShared.EntityUtils;
import cuchaz.modsShared.Environment;
import cuchaz.modsShared.blocks.BlockSet;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.modsShared.blocks.Envelopes;
import cuchaz.modsShared.math.CircleRange;
import cuchaz.modsShared.math.CompareReal;
import cuchaz.modsShared.math.RotatedBB;
import cuchaz.modsShared.perf.DelayTimer;
import cuchaz.ships.config.BlockProperties;
import cuchaz.ships.packets.PacketPilotShip;
import cuchaz.ships.packets.PacketRequestShipBlocks;
import cuchaz.ships.packets.PacketShipLaunched;
import cuchaz.ships.persistence.PersistenceException;
import cuchaz.ships.persistence.ShipPersistence;
import cuchaz.ships.propulsion.Propulsion;

public class EntityShip extends Entity {
	
	public static final int LinearThrottleMax = 100;
	public static final int LinearThrottleMin = -25;
	public static final int LinearThrottleStep = 2;
	public static final int AngularThrottleMax = 1;
	public static final int AngularThrottleMin = -1;
	
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
	private ShipCollider m_collider;
	private WaterDisplacer m_waterDisplacer;
	private RainDisplacer m_rainDisplacer;
	private DelayTimer m_throttleKillDelay;
	private Map<Integer,Entity> m_ridersLastTick;
	
	public EntityShip(World world) {
		super(world);
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
		m_collider = new ShipCollider(this);
		m_waterDisplacer = new WaterDisplacer(this);
		m_rainDisplacer = new RainDisplacer(this);
		m_throttleKillDelay = null;
		m_ridersLastTick = new TreeMap<Integer,Entity>();
	}
	
	@Override
	protected void entityInit() {
		// nothing to do
	}
	
	public void setShipWorld(ShipWorld shipWorld) {
		// if the blocks are invalid, just kill the ship
		if (!shipWorld.isValid()) {
			setDead();
			Ships.logger.warning("Ship world is invalid. Killed ship.");
			return;
		}
		
		// reset the motion again. For some reason, the client entity gets bogus velocities from somewhere...
		motionX = 0.0;
		motionY = 0.0;
		motionZ = 0.0;
		motionYaw = 0.0f;
		
		m_shipWorld = shipWorld;
		shipWorld.setShip(this);
		m_physics = new ShipPhysics(m_shipWorld.getBlocksStorage());
		m_propulsion = new Propulsion(m_shipWorld.getBlocksStorage());
		
		// get the ship center of mass so we can convert between ship/block spaces
		Vec3 centerOfMass = m_physics.getCenterOfMass();
		m_shipBlockX = -centerOfMass.xCoord;
		m_shipBlockY = -centerOfMass.yCoord;
		m_shipBlockZ = -centerOfMass.zCoord;
		
		m_collider.computeShipBoundingBox(boundingBox, posX, posY, posZ, rotationYaw);
		
		// LOGGING
		Ships.logger.info(String.format("EntityShip %d initialized at (%.2f,%.2f,%.2f) + (%.4f,%.4f,%.4f)",
			this.getEntityId(), posX, posY, posZ, motionX, motionY, motionZ
		));
	}
	
	@Override
	public void setDead() {
		super.setDead();
		
		// LOGGING
		Ships.logger.info("EntityShip %d died!", getEntityId());
		
		// only restore blocks on the server
		if (Environment.isServer()) {
			m_waterDisplacer.restore();
			m_rainDisplacer.restore();
		}
		
		// use the ship unlauncher to move ship riders to the new ship unlaunch position
		List<Entity> riders = getCollider().getRiders();
		if (!riders.isEmpty()) {
			ShipUnlauncher unlauncher = new ShipUnlauncher(this);
			for (Entity rider : riders) {
				// for players, only adjust position on the client
				boolean isPlayer = rider instanceof EntityPlayer;
				if ( (isPlayer && Environment.isClient()) || !isPlayer) {
					unlauncher.applyUnlaunch(rider);
				}
			}
		}
		
		// if anyone is interacting with something on the ship, make them stop
		if (!this.worldObj.isRemote) {
			AxisAlignedBB queryBox = boundingBox.copy();
			final float dist = 5; // must be at least the reach distance
			queryBox.expand(dist, dist, dist);
			@SuppressWarnings("unchecked")
			List<EntityPlayer> players = (List<EntityPlayer>)this.worldObj.getEntitiesWithinAABB(EntityPlayer.class, queryBox);
			for (EntityPlayer player : players) {
				if (player.openContainer != null) {
					// this player is near a ship and has an open container
					// force the player to close the container to prevent duplication issues
					// TODO: check to see if the container is actually on the ship, this is definitely too general
					player.closeScreen();
				}
			}
		}
	}
	
	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) {
		try {
			ShipPersistence.readAnyVersion(this, nbt);
		} catch (PersistenceException ex) {
			Ships.logger.warning(ex, "Unable to read ship. Removing ship from world.");
			setDead();
		}
	}
	
	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) {
		ShipPersistence.writeNewestVersion(this, nbt);
	}
	
	public ShipWorld getShipWorld() {
		return m_shipWorld;
	}
	
	public Propulsion getPropulsion() {
		return m_propulsion;
	}
	
	public ShipCollider getCollider() {
		return m_collider;
	}
	
	public WaterDisplacer getWaterDisplacer() {
		return m_waterDisplacer;
	}
	
	public RainDisplacer getRainDisplacer() {
		return m_rainDisplacer;
	}
	
	@Override
	public boolean canBeCollidedWith() {
		// yes the ship can be collided with in general
		return true;
	}
	
	@Override
	public AxisAlignedBB getBoundingBox() {
		// but don't let vanilla Minecraft handle the collisions
		return null;
	}
	
	@Override
	public AxisAlignedBB getCollisionBox(Entity entity) {
		// ships are not pushable by entities either
		return null;
	}
	
	@Override
	public void setPosition(double x, double y, double z) {
		posX = x;
		posY = y;
		posZ = z;
		
		if (m_collider != null) {
			m_collider.computeShipBoundingBox(boundingBox, posX, posY, posZ, rotationYaw);
		}
	}
	
	@Override
	public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int alwaysThree) {
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
	public void onUpdate() {
		// did we die already?
		if (isDead) {
			return;
		}
		
		// do we have any packets waiting?
		if (m_shipWorld == null) {
			PacketShipLaunched packet = PacketShipLaunched.instance.getPacket(this);
			if (packet != null) {
				packet.process(this);
			} else {
				// ask for blocks
				Ships.net.getDispatch().sendToServer(new PacketRequestShipBlocks(getEntityId()));
			}
		}
		
		// don't do any updating until we get blocks
		if (m_shipWorld == null) {
			return;
		}
		
		// do propulsion things
		double waterHeightInBlockSpace = shipToBlocksY(worldToShipY(getWaterHeight()));
		m_propulsion.update(waterHeightInBlockSpace);
		adjustMotionDueToGravityAndBuoyancy(waterHeightInBlockSpace);
		adjustMotionDueToThrustAndDrag(waterHeightInBlockSpace);
		
		double dx = motionX;
		double dy = motionY;
		double dz = motionZ;
		float dYaw = motionYaw;
		
		// did we get an updated position from the server?
		if (m_hasInfoFromServer) {
			// position deltas are easy
			dx += m_xFromServer - posX;
			dy += m_yFromServer - posY;
			dz += m_zFromServer - posZ;
			
			// we need fancy math to get the correct rotation delta
			double yawRadClient = CircleRange.mapMinusPiToPi(Math.toRadians(rotationYaw));
			double yawRadServer = CircleRange.mapMinusPiToPi(Math.toRadians(m_yawFromServer));
			double yawDelta = CircleRange.newByShortSegment(yawRadClient, yawRadServer).getLength();
			
			// was the rotation delta actually positive?
			if (!CompareReal.eq(CircleRange.mapMinusPiToPi(yawRadClient + yawDelta), yawRadServer)) {
				// nope. it's a negative delta
				yawDelta = -yawDelta;
			}
			yawDelta = Math.toDegrees(yawDelta);
			
			dYaw += yawDelta;
			
			// just apply the pitch directly
			rotationPitch = m_pitchFromServer;
			
			m_hasInfoFromServer = false;
		}
		
		// did we even move a noticeable amount?
		final double Epsilon = 1e-3;
		if (Math.abs(dx) >= Epsilon || Math.abs(dy) >= Epsilon || Math.abs(dz) >= Epsilon || Math.abs(dYaw) >= Epsilon) {
			List<Entity> riders = m_collider.getRiders();
			
			// save the old values
			prevPosX = posX;
			prevPosY = posY;
			prevPosZ = posZ;
			prevRotationYaw = rotationYaw;
			prevRotationPitch = rotationPitch;
			
			// move the ship
			m_collider.moveShip(dx, dy, dz, dYaw);
			
			// recalculate the deltas
			dx = posX - prevPosX;
			dy = posY - prevPosY;
			dz = posZ - prevPosZ;
			dYaw = rotationYaw - prevRotationYaw;
			
			m_waterDisplacer.update(waterHeightInBlockSpace);
			m_rainDisplacer.update();
			moveRiders(riders, dx, dy, dz, dYaw);
			
			// are there no riders?
			if (riders.isEmpty() && (linearThrottle != 0 || angularThrottle != 0)) {
				// kill the throttle after a delay
				if (m_throttleKillDelay == null) {
					m_throttleKillDelay = new DelayTimer(20 * 2);
				}
				if (m_throttleKillDelay.isDelayedUpdate()) {
					linearThrottle = 0;
					angularThrottle = 0;
					m_throttleKillDelay = null;
				}
			} else {
				// stop the timer
				m_throttleKillDelay = null;
			}
		}
		
		// update the world
		m_shipWorld.updateEntities();
	}
	
	public double getWaterHeight() {
		// search in the ship box for water blocks (and air wall blocks)
		BlockSet waterCoords = new BlockSet();
		BlockUtils.worldRangeQuery(waterCoords, worldObj, boundingBox);
		
		Iterator<Coords> iter = waterCoords.iterator();
		while (iter.hasNext()) {
			Coords coords = iter.next();
			Block block = worldObj.getBlock(coords.x, coords.y, coords.z);
			if (!BlockProperties.isWater(block)) {
				iter.remove();
			}
		}
		
		if (waterCoords.isEmpty()) {
			return 0;
		}
		
		// compute the average y from the top envelope of these blocks
		double sum = 0;
		BlockSet topEnvelope = new Envelopes(waterCoords).getEnvelope(BlockSide.Top).toBlockSet();
		for (Coords coords : topEnvelope) {
			sum += coords.y + 1; // +1 for the top of the block
		}
		return sum / topEnvelope.size();
	}
	
	@Override
	public boolean interactFirst(EntityPlayer player) {
		// NOTE: return true if we handled the interaction
		
		// what did the player hit?
		double reachDist = EntityUtils.getPlayerReachDistance(player);
		HitList hits = new HitList();
		hits.addHits(this, player, reachDist);
		HitList.Entry hit = hits.getClosestHit();
		if (hit == null || hit.type != HitList.Type.Ship) {
			return false;
		}
		
		// activate the block
		// NOTE: blocks can do all kinds of crazy things, be defensive here
		Block block = m_shipWorld.getBlock(hit.hit.blockX, hit.hit.blockY, hit.hit.blockZ);
		try {
			return block.onBlockActivated(m_shipWorld, hit.hit.blockX, hit.hit.blockY, hit.hit.blockZ, player, hit.hit.sideHit, (float)hit.hit.hitVec.xCoord, (float)hit.hit.hitVec.yCoord, (float)hit.hit.hitVec.zCoord);
		} catch (Throwable t) {
			Ships.logger.error(t, "Error activating block %s", Block.blockRegistry.getNameForObject(block));
			return false;
		}
	}
	
	@Override
	public boolean hitByEntity(Entity attackingEntity) {
		// NOTE: return true to ignore the attack
		
		// always ignore attacks to ships
		return true;
	}
	
	public void worldToShip(Vec3 v) {
		double x = worldToShipX(v.xCoord, v.zCoord);
		double y = worldToShipY(v.yCoord);
		double z = worldToShipZ(v.xCoord, v.zCoord);
		
		v.xCoord = x;
		v.yCoord = y;
		v.zCoord = z;
	}
	
	public void worldToShipDirection(Vec3 v) {
		// just apply the rotation
		double yawRad = Math.toRadians(rotationYaw);
		double cos = Math.cos(yawRad);
		double sin = Math.sin(yawRad);
		double x = v.xCoord * cos - v.zCoord * sin;
		double z = v.xCoord * sin + v.zCoord * cos;
		
		v.xCoord = x;
		v.zCoord = z;
	}
	
	public double worldToShipX(double x, double z) {
		double yawRad = Math.toRadians(rotationYaw);
		double cos = Math.cos(yawRad);
		double sin = Math.sin(yawRad);
		return (x - posX) * cos - (z - posZ) * sin;
	}
	
	public double worldToShipY(double y) {
		return y - posY;
	}
	
	public double worldToShipZ(double x, double z) {
		double yawRad = Math.toRadians(rotationYaw);
		double cos = Math.cos(yawRad);
		double sin = Math.sin(yawRad);
		return (x - posX) * sin + (z - posZ) * cos;
	}
	
	public void shipToWorld(Vec3 v) {
		double x = shipToWorldX(v.xCoord, v.zCoord);
		double y = shipToWorldY(v.yCoord);
		double z = shipToWorldZ(v.xCoord, v.zCoord);
		
		v.xCoord = x;
		v.yCoord = y;
		v.zCoord = z;
	}
	
	public void shipToWorldDirection(Vec3 v) {
		// just apply the rotation
		double yawRad = Math.toRadians(rotationYaw);
		double cos = Math.cos(yawRad);
		double sin = Math.sin(yawRad);
		double x = v.xCoord * cos + v.zCoord * sin;
		double z = -v.xCoord * sin + v.zCoord * cos;
		
		v.xCoord = x;
		v.zCoord = z;
	}
	
	public double shipToWorldX(double x, double z) {
		double yawRad = Math.toRadians(rotationYaw);
		double cos = Math.cos(yawRad);
		double sin = Math.sin(yawRad);
		return x * cos + z * sin + posX;
	}
	
	public double shipToWorldY(double y) {
		return y + posY;
	}
	
	public double shipToWorldZ(double x, double z) {
		double yawRad = Math.toRadians(rotationYaw);
		double cos = Math.cos(yawRad);
		double sin = Math.sin(yawRad);
		return -x * sin + z * cos + posZ;
	}
	
	public void shipToBlocks(Vec3 v) {
		v.xCoord = shipToBlocksX(v.xCoord);
		v.yCoord = shipToBlocksY(v.yCoord);
		v.zCoord = shipToBlocksZ(v.zCoord);
	}
	
	public double shipToBlocksX(double x) {
		return x - m_shipBlockX;
	}
	
	public double shipToBlocksY(double y) {
		return y - m_shipBlockY;
	}
	
	public double shipToBlocksZ(double z) {
		return z - m_shipBlockZ;
	}
	
	public void blocksToShip(Vec3 v) {
		v.xCoord = blocksToShipX(v.xCoord);
		v.yCoord = blocksToShipY(v.yCoord);
		v.zCoord = blocksToShipZ(v.zCoord);
	}
	
	public double blocksToShipX(double x) {
		return x + m_shipBlockX;
	}
	
	public double blocksToShipY(double y) {
		return y + m_shipBlockY;
	}
	
	public double blocksToShipZ(double z) {
		return z + m_shipBlockZ;
	}
	
	public RotatedBB worldToBlocks(AxisAlignedBB box) {
		// transform the box center into block space
		Vec3 center = Vec3.createVectorHelper(
			(box.minX + box.maxX)/2,
			(box.minY + box.maxY)/2,
			(box.minZ + box.maxZ)/2
		);
		worldToShip(center);
		shipToBlocks(center);
		
		// build a box of the same dimensions in blocks space
		double dxh = (box.maxX - box.minX)/2;
		double dyh = (box.maxY - box.minY)/2;
		double dzh = (box.maxZ - box.minZ)/2;
		box = AxisAlignedBB.getBoundingBox(
			center.xCoord - dxh, center.yCoord - dyh,
			center.zCoord - dzh, center.xCoord + dxh,
			center.yCoord + dyh, center.zCoord + dzh
		);
		
		return new RotatedBB(box, -rotationYaw);
	}
	
	public RotatedBB blocksToWorld(AxisAlignedBB box) {
		// transform the box center into world space
		Vec3 center = Vec3.createVectorHelper(
			(box.minX + box.maxX)/2,
			(box.minY + box.maxY)/2,
			(box.minZ + box.maxZ)/2
		);
		blocksToShip(center);
		shipToWorld(center);
		
		// build a box of the same dimensions in world space
		double dxh = (box.maxX - box.minX)/2;
		double dyh = (box.maxY - box.minY)/2;
		double dzh = (box.maxZ - box.minZ)/2;
		box = AxisAlignedBB.getBoundingBox(
			center.xCoord - dxh, center.yCoord - dyh,
			center.zCoord - dzh, center.xCoord + dxh,
			center.yCoord + dyh, center.zCoord + dzh
		);
		
		return new RotatedBB(box, rotationYaw);
	}
	
	private void adjustMotionDueToGravityAndBuoyancy(double waterHeightInBlockSpace) {
		/*
		only simulate buoyancy if we're outside of the epsilon for the equilibrium y pos
		final double EquilibriumWaterHeightEpsilon = 0.05;
		double distToEquilibrium = waterHeightInBlockSpace - m_physics.getEquilibriumWaterHeight();
		if (Math.abs(distToEquilibrium) > EquilibriumWaterHeightEpsilon) {
		*/
		
		Vec3 velocity = Vec3.createVectorHelper(0, motionY, 0);
		
		double accelerationDueToBouyancy = m_physics.getNetUpAcceleration(waterHeightInBlockSpace);
		double accelerationDueToDrag = m_physics.getLinearAccelerationDueToDrag(velocity, waterHeightInBlockSpace);
		
		// make sure drag acceleration doesn't reverse the velocity!
		// NOTE: drag is always positive right now. We'll fix the sign later
		accelerationDueToDrag = Math.min(Math.abs(motionY + accelerationDueToBouyancy), accelerationDueToDrag);
		
		// make sure drag opposes velocity
		if (Math.signum(accelerationDueToDrag) == Math.signum(motionY)) {
			accelerationDueToDrag *= -1;
		}
		
		motionY += accelerationDueToBouyancy + accelerationDueToDrag;
	}
	
	private void adjustMotionDueToThrustAndDrag(double waterHeightInBlockSpace) {
		
		// process pilot actions
		PilotAction.resetShip(this, m_pilotActions, m_oldPilotActions);
		PilotAction.applyToShip(this, m_pilotActions);
		m_oldPilotActions = m_pilotActions;
		
		// clamp the throttle
		if (linearThrottle < LinearThrottleMin) {
			linearThrottle = LinearThrottleMin;
		}
		if (linearThrottle > LinearThrottleMax) {
			linearThrottle = LinearThrottleMax;
		}
		
		// get the velocity direction
		double velocityDirX = motionX;
		double velocityDirZ = motionZ;
		double speed = Math.sqrt(velocityDirX * velocityDirX + velocityDirZ * velocityDirZ);
		if (speed > 0) {
			velocityDirX /= speed;
			velocityDirZ /= speed;
		}
		
		// get the velocity in block coords
		Vec3 velocityInBlockCoords = Vec3.createVectorHelper(motionX, 0, motionZ);
		worldToShipDirection(velocityInBlockCoords);
		
		// compute the linear acceleration due to thrust
		double linearAccelerationDueToThrustX = 0;
		double linearAccelerationDueToThrustZ = 0;
		if (m_sideShipForward != null) {
			if (m_sendPilotChangesToServer) {
				// send a packet to the server
				PacketPilotShip packet = new PacketPilotShip(getEntityId(), m_pilotActions, m_sideShipForward, linearThrottle, angularThrottle);
				Ships.net.getDispatch().sendToServer(packet);
				m_sendPilotChangesToServer = false;
			}
			
			// compute the forward vector
			float yawRad = (float)Math.toRadians(rotationYaw);
			float cos = MathHelper.cos(yawRad);
			float sin = MathHelper.sin(yawRad);
			double forwardX = m_sideShipForward.getDx() * cos + m_sideShipForward.getDz() * sin;
			double forwardZ = -m_sideShipForward.getDx() * sin + m_sideShipForward.getDz() * cos;
			
			// compute the acceleration
			double linearAccelerationDueToThrust = m_physics.getLinearAccelerationDueToThrust(m_propulsion, speed) * linearThrottle / LinearThrottleMax;
			linearAccelerationDueToThrustX = forwardX * linearAccelerationDueToThrust;
			linearAccelerationDueToThrustZ = forwardZ * linearAccelerationDueToThrust;
		}
		
		// compute the linear acceleration due to drag
		double linearAccelerationDueToDrag = m_physics.getLinearAccelerationDueToDrag(velocityInBlockCoords, waterHeightInBlockSpace);
		
		// make sure drag acceleration doesn't reverse the velocity!
		double nextSpeedX = motionX + linearAccelerationDueToThrustX;
		double nextSpeedZ = motionZ + linearAccelerationDueToThrustZ;
		double nextSpeed = Math.sqrt(nextSpeedX * nextSpeedX + nextSpeedZ * nextSpeedZ);
		linearAccelerationDueToDrag = Math.min(nextSpeed, linearAccelerationDueToDrag);
		
		// apply the linear acceleration
		motionX += linearAccelerationDueToThrustX - velocityDirX * linearAccelerationDueToDrag;
		motionZ += linearAccelerationDueToThrustZ - velocityDirZ * linearAccelerationDueToDrag;
		
		// get the angular acceleration
		double angularAccelerationDueToThrust = m_physics.getAngularAccelerationDueToThrust(m_propulsion) * angularThrottle / AngularThrottleMax;
		double angularAccelerationDueToDrag = m_physics.getAngularAccelerationDueToDrag(motionYaw, waterHeightInBlockSpace);
		
		// make sure drag acceleration doesn't reverse the velocity!
		angularAccelerationDueToDrag = Math.min(Math.abs(motionYaw + angularAccelerationDueToThrust), angularAccelerationDueToDrag);
		
		// make sure the drag is opposed to the velocity
		if (Math.signum(angularAccelerationDueToDrag) == Math.signum(motionYaw)) {
			angularAccelerationDueToDrag *= -1;
		}
		
		// apply the angular acceleration
		motionYaw += angularAccelerationDueToThrust + angularAccelerationDueToDrag;
	}
	
	private void moveRiders(List<Entity> riders, double dx, double dy, double dz, float dYaw) {
		// remove all current riders from the last known riders
		// meaning, only lost riders will be left
		for (Entity rider : riders) {
			if (m_ridersLastTick.containsKey(rider.getEntityId())) {
				m_ridersLastTick.remove(rider.getEntityId());
			}
		}
		for (Entity rider : m_ridersLastTick.values()) {
			Vec3 delta = getRiderDelta(rider, dx, dy, dz, dYaw);
			
			// impart some velocity to the old rider
			rider.motionX += delta.xCoord;
			rider.motionY += delta.yCoord;
			rider.motionZ += delta.zCoord;
			
			// apply the delta for one last time
			rider.rotationYaw -= dYaw;
			rider.setPosition(rider.posX + delta.xCoord, rider.posY + delta.yCoord, rider.posZ + delta.zCoord);
		}
		
		// update the last known riders
		m_ridersLastTick.clear();
		for (Entity rider : riders) {
			m_ridersLastTick.put(rider.getEntityId(), rider);
		}
		
		// first, move the riders
		for (Entity rider : riders) {
			Vec3 delta = getRiderDelta(rider, dx, dy, dz, dYaw);
			
			// apply the transformation
			rider.rotationYaw -= dYaw;
			rider.setPosition(rider.posX + delta.xCoord, rider.posY + delta.yCoord, rider.posZ + delta.zCoord);
		}
	}
	
	public void setPilotActions(int actions, BlockSide sideShipForward, boolean sendPilotChangesToServer) {
		m_pilotActions = actions;
		m_sideShipForward = sideShipForward;
		m_sendPilotChangesToServer = sendPilotChangesToServer;
	}
	
	private Vec3 getRiderDelta(Entity rider, double shipDx, double shipDy, double shipDz, double shipDyaw) {
		Vec3 p = Vec3.createVectorHelper(0, 0, 0);
		
		// apply rotation of position relative to the ship center
		p.xCoord = rider.posX + shipDx;
		p.zCoord = rider.posZ + shipDz;
		worldToShip(p);
		float yawRad = (float)Math.toRadians(shipDyaw);
		float cos = MathHelper.cos(yawRad);
		float sin = MathHelper.sin(yawRad);
		double x = p.xCoord * cos + p.zCoord * sin;
		double z = -p.xCoord * sin + p.zCoord * cos;
		p.xCoord = x;
		p.zCoord = z;
		shipToWorld(p);
		
		// convert the new position into a delta vector
		p.xCoord -= rider.posX;
		p.yCoord = shipDy;
		p.zCoord -= rider.posZ;
		return p;
	}
	
	@SideOnly(Side.CLIENT)
	private void clickWorldBlock(EntityPlayer player, MovingObjectPosition hit, boolean isLeftButton) {
		// is this even a block?
		if (hit == null || hit.typeOfHit != MovingObjectType.BLOCK) {
			return;
		}
		
		// what item is the player using?
		ItemStack heldItem = player.getHeldItem();
		
		// do the click
		// NOTE: this part emulates part of Minecraft.click()
		PlayerControllerMP playerController = Minecraft.getMinecraft().playerController;
		if (isLeftButton) {
			playerController.clickBlock(hit.blockX, hit.blockY, hit.blockZ, hit.sideHit);
		} else {
			boolean result = !ForgeEventFactory.onPlayerInteract(player, Action.RIGHT_CLICK_BLOCK, hit.blockX, hit.blockY, hit.blockZ, hit.sideHit, player.worldObj).isCanceled();
			if (result && playerController.onPlayerRightClick(player, worldObj, heldItem, hit.blockX, hit.blockY, hit.blockZ, hit.sideHit, hit.hitVec)) {
				player.swingItem();
			}
			
			if (heldItem != null) {
				if (heldItem.stackSize == 0) {
					player.inventory.mainInventory[player.inventory.currentItem] = null;
				} else if (playerController.isInCreativeMode()) {
					Minecraft.getMinecraft().entityRenderer.itemRenderer.resetEquippedProgress();
				}
			}
		}
	}
}
