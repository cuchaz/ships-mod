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

import java.io.IOException;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL20;

import cuchaz.modsShared.ColorUtils;
import cuchaz.modsShared.Util;
import cuchaz.modsShared.blocks.BlockArray;
import cuchaz.modsShared.blocks.BlockSide;
import cuchaz.modsShared.blocks.BlockUtils;
import cuchaz.modsShared.blocks.BlockUtils.BlockConditionChecker;
import cuchaz.modsShared.blocks.BlockUtils.BlockExplorer;
import cuchaz.modsShared.blocks.BlockUtils.Neighbors;
import cuchaz.modsShared.blocks.Coords;
import cuchaz.ships.ShipLauncher;
import cuchaz.ships.Ships;
import cuchaz.ships.config.BlockProperties;
import cuchaz.ships.propulsion.Propulsion;
import cuchaz.ships.render.RenderShip2D;
import cuchaz.ships.render.ShaderLoader;

public class GuiShipPropulsion extends GuiShip {
	
	private static final ResourceLocation DesaturationShader = new ResourceLocation("ships", "shaders/desaturate.frag");
	
	private ShipLauncher m_shipLauncher;
	private Propulsion m_propulsion;
	private BlockArray m_shipEnvelope;
	private BlockArray m_helmEnvelope;
	private BlockArray m_propulsionEnvelope;
	private int m_desaturationProgramId;
	private double m_topLinearSpeed;
	private double m_topAngularSpeed;
	private String m_propulsionMethodsDescription;
	
	public GuiShipPropulsion(final World world, int helmX, int helmY, int helmZ) {
		
		// defaults
		m_shipLauncher = null;
		m_shipEnvelope = null;
		m_helmEnvelope = null;
		m_propulsionEnvelope = null;
		m_topLinearSpeed = 0;
		m_topAngularSpeed = 0f;
		m_desaturationProgramId = 0;
		
		// this should be the helm
		assert (world.getBlock(helmX, helmY, helmZ) == Ships.m_blockHelm);
		Coords helmCoords = new Coords(helmX, helmY, helmZ);
		
		// find the ship block
		Coords shipBlockCoords = BlockUtils.searchForBlock(helmX, helmY, helmZ, 10000, new BlockConditionChecker() {
			
			@Override
			public boolean isConditionMet(Coords coords) {
				return world.getBlock(coords.x, coords.y, coords.z) == Ships.m_blockShip;
			}
		}, new BlockExplorer() {
			
			@Override
			public boolean shouldExploreBlock(Coords coords) {
				return !BlockProperties.isSeparator(world.getBlock(coords.x, coords.y, coords.z));
			}
		}, Neighbors.Edges);
		if (shipBlockCoords == null) {
			// can't find a ship block
			return;
		}
		
		// get the ship info from the launcher
		m_shipLauncher = new ShipLauncher(world, shipBlockCoords);
		if (m_shipLauncher.getShipWorld() == null) {
			// there's no valid ship
			return;
		}
		
		if (m_shipLauncher.getShipType().isPaddleable()) {
			// paddleable ships cannot support a helm
			return;
		}
		
		m_shipEnvelope = m_shipLauncher.getShipEnvelope(BlockSide.Top);
		
		// compute an envelope for the helm
		helmCoords.x -= shipBlockCoords.x;
		helmCoords.y -= shipBlockCoords.y;
		helmCoords.z -= shipBlockCoords.z;
		m_helmEnvelope = m_shipEnvelope.newEmptyCopy();
		m_helmEnvelope.setBlock(helmCoords.x, helmCoords.z, helmCoords);
		
		// get the propulsion
		m_propulsion = new Propulsion(m_shipLauncher.getShipWorld().getBlocksStorage());
		m_propulsionEnvelope = m_propulsion.getEnevelope();
		
		// create our shader
		if (ShaderLoader.areShadersSupported()) {
			try {
				m_desaturationProgramId = ShaderLoader.createProgram(ShaderLoader.load(DesaturationShader));
			} catch (IOException ex) {
				Ships.logger.warning(ex, "Unable to load shader!");
			}
		}
		
		// compute the top speeds
		m_topLinearSpeed = 0;
		m_topAngularSpeed = 0;
		if (m_shipLauncher.getShipPhysics().willItFloat()) {
			m_topLinearSpeed = m_shipLauncher.getShipPhysics().simulateLinearAcceleration(m_propulsion).topSpeed;
			m_topAngularSpeed = m_shipLauncher.getShipPhysics().simulateAngularAcceleration(m_propulsion).topSpeed;
		}
		
		// build the description string
		String methods = m_propulsion.dumpMethods();
		if (methods.equals("")) {
			m_propulsionMethodsDescription = GuiString.NoPropulsion.getLocalizedText();
		} else {
			m_propulsionMethodsDescription = String.format(GuiString.FoundPropulsion.getLocalizedText(), methods);
		}
	}
	
	@Override
	protected void drawForeground(int mouseX, int mouseY, float partialTickTime) {
		drawHeaderText(GuiString.ShipPropulsion.getLocalizedText(), 0);
		
		if (m_shipLauncher == null) {
			drawText(GuiString.NoShipBlock.getLocalizedText(), 1);
		} else if (m_propulsion == null) {
			drawText(GuiString.InvalidShip.getLocalizedText(), 1);
		} else {
			// list the specs
			drawLabelValueText("Ship Mass", String.format("%.1f tonnes", m_shipLauncher.getShipPhysics().getMass()), 1);
			drawLabelValueText("Thrust", String.format("%.1f KN", Util.perTick2ToPerSecond2(m_propulsion.getTotalThrust(0))), 2);
			drawLabelValueText("Top Speed", String.format("%.1f m/s", Util.perTickToPerSecond(m_topLinearSpeed)), 3);
			drawLabelValueText("Turning Speed", String.format("%.1f deg/s", Util.perTickToPerSecond(m_topAngularSpeed)), 4);
			
			drawPropulsion();
			
			// list the propulsion types
			drawText(m_propulsionMethodsDescription, 12);
		}
	}
	
	private void drawPropulsion() {
		int x = LeftMargin;
		int y = getLineY(6);
		int width = m_width - LeftMargin * 2;
		int height = 64;
		
		RenderShip2D.drawWater(x, y, zLevel, width, height);
		
		if (m_shipEnvelope.getHeight() > m_shipEnvelope.getWidth()) {
			// rotate the envelopes so the long axis is across the GUI width
			m_shipEnvelope = BlockArray.Rotation.Ccw90.rotate(m_shipEnvelope);
			m_helmEnvelope = BlockArray.Rotation.Ccw90.rotate(m_helmEnvelope);
			m_propulsionEnvelope = BlockArray.Rotation.Ccw90.rotate(m_propulsionEnvelope);
		}
		
		// draw the ship silhouette to fill in any transparent areas
		RenderShip2D.drawShipAsColor(m_shipEnvelope, ColorUtils.getGrey(64), x, y, zLevel, width, height);
		
		if (ShaderLoader.areShadersSupported()) {
			// draw a desaturated ship
			GL20.glUseProgram(m_desaturationProgramId);
			RenderShip2D.drawShip(m_shipEnvelope, BlockSide.Top, m_shipLauncher.getShipWorld(), x, y, zLevel, width, height);
			GL20.glUseProgram(0);
		} else {
			// shaders aren't supported, so just render without saturation
			RenderShip2D.drawShip(m_shipEnvelope, BlockSide.Top, m_shipLauncher.getShipWorld(), x, y, zLevel, width, height);
		}
		
		// draw the propulsion blocks and the helm at full saturation
		RenderShip2D.drawShip(m_propulsionEnvelope, BlockSide.Top, m_shipLauncher.getShipWorld(), x, y, zLevel, width, height);
		RenderShip2D.drawShip(m_helmEnvelope, BlockSide.Top, m_shipLauncher.getShipWorld(), x, y, zLevel, width, height);
	}
}
