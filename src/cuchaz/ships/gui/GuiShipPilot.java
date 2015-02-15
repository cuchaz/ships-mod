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
package cuchaz.ships.gui;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.gui.GuiBase;
import cuchaz.ships.EntityShip;
import cuchaz.ships.PilotAction;

public abstract class GuiShipPilot extends GuiBase {
	
	protected static enum ForwardSideMethod {
		ByPlayerLook {
			
			@Override
			public BlockSide compute(EntityShip ship, EntityPlayer player) {
				// get the center of the ship block in world space
				Vec3 blockCenter = Vec3.createVectorHelper(0.5, 0.5, 0.5);
				ship.blocksToShip(blockCenter);
				ship.shipToWorld(blockCenter);
				
				// which xz side is facing the player?
				// get a vector from the block to the player
				Vec3 direction = Vec3.createVectorHelper(player.posX - blockCenter.xCoord, 0, player.posZ - blockCenter.zCoord);
				ship.worldToShipDirection(direction);
				
				// find the side whose inverted normal vector best matches the vector to the player
				double maxDot = Double.NEGATIVE_INFINITY;
				BlockSide sideShipForward = null;
				for (BlockSide side : BlockSide.xzSides()) {
					double dot = -side.getDx() * direction.xCoord + -side.getDz() * direction.zCoord;
					if (dot > maxDot) {
						maxDot = dot;
						sideShipForward = side;
					}
				}
				return sideShipForward;
			}
		},
		ByHelm {
			
			@Override
			public BlockSide compute(EntityShip ship, EntityPlayer player) {
				return ship.getPropulsion().getFrontSide();
			}
		};
		
		public abstract BlockSide compute(EntityShip ship, EntityPlayer player);
	}
	
	private EntityShip m_ship;
	private List<PilotAction> m_allowedActions;
	private int m_lastActions;
	private BlockSide m_forwardSide;
	
	protected GuiShipPilot(int width, int height, ResourceLocation background, EntityShip ship, EntityPlayer player, List<PilotAction> allowedActions, ForwardSideMethod forwardSideMethod) {
		super(width, height, background, false);
		
		m_ship = ship;
		m_allowedActions = allowedActions;
		m_lastActions = 0;
		m_forwardSide = forwardSideMethod.compute(ship, player);
	}
	
	protected EntityShip getShip() {
		return m_ship;
	}
	
	protected BlockSide getForwardSide() {
		return m_forwardSide;
	}
	
	@Override
	public void initGui() {
		super.initGui();
		
		// show this GUI near the bottom so it doesn't block much of the screen
		setPos(
			(width - m_width)/2,
			height - m_height - 48
		);
		
		// try to let the player look around while in this gui
		allowUserInput = true;
		mc.inGameHasFocus = true;
		mc.mouseHelper.grabMouseCursor();
		
		PilotAction.setActionCodes(mc.gameSettings);
	}
	
	@Override
	public void updateScreen() {
		
		// get the actions, if any
		int actions = PilotAction.getActiveActions(mc.gameSettings, m_allowedActions);
		
		if (actions != m_lastActions) {
			// something changed
			applyActions(actions);
		}
		m_lastActions = actions;
	}
	
	@Override
	protected void mouseClicked(int x, int y, int button) {
		// NOTE: button 1 is the RMB
		if (button == 1) {
			close();
		}
	}
	
	@Override
	public void onGuiClosed() {
		// make sure we stop piloting the ship
		applyActions(0);
	}
	
	private void applyActions(int actions) {
		m_ship.setPilotActions(actions, m_forwardSide, true);
	}
}
