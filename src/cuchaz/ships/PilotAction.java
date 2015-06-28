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

import net.minecraft.client.settings.GameSettings;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public enum PilotAction {
	Forward {
		
		@Override
		public void applyToShip(EntityShip ship) {
			ship.linearThrottle = EntityShip.LinearThrottleMax;
		}
		
		@Override
		public void resetShip(EntityShip ship) {
			ship.linearThrottle = 0;
		}
	},
	Backward {
		
		@Override
		public void applyToShip(EntityShip ship) {
			ship.linearThrottle = EntityShip.LinearThrottleMin;
		}
		
		@Override
		public void resetShip(EntityShip ship) {
			ship.linearThrottle = 0;
		}
	},
	Left {
		
		@Override
		public void applyToShip(EntityShip ship) {
			ship.angularThrottle = EntityShip.AngularThrottleMax;
		}
		
		@Override
		public void resetShip(EntityShip ship) {
			ship.angularThrottle = 0;
		}
	},
	Right {
		
		@Override
		public void applyToShip(EntityShip ship) {
			ship.angularThrottle = EntityShip.AngularThrottleMin;
		}
		
		@Override
		public void resetShip(EntityShip ship) {
			ship.angularThrottle = 0;
		}
	},
	ThrottleUp {
		
		private boolean m_isForwardAllowed = true;
		
		@Override
		public void applyToShip(EntityShip ship) {
			if (ship.linearThrottle < 0) {
				// add a backstop so we stop at zero throttle
				m_isForwardAllowed = false;
			}
			
			if ( (ship.linearThrottle < 0) || (ship.linearThrottle == 0 && m_isForwardAllowed) || (ship.linearThrottle > 0)) {
				// increase the throttle, but make sure we stop at zero
				if (ship.linearThrottle < 0 && ship.linearThrottle > -EntityShip.LinearThrottleStep) {
					ship.linearThrottle = 0;
				} else {
					ship.linearThrottle += EntityShip.LinearThrottleStep;
				}
			}
		}
		
		@Override
		public void resetShip(EntityShip ship) {
			m_isForwardAllowed = true;
		}
	},
	ThrottleDown {
		
		private boolean m_isReverseAllowed = true;
		
		@Override
		public void applyToShip(EntityShip ship) {
			if (ship.linearThrottle > 0) {
				// add a backstop so we stop at zero throttle
				m_isReverseAllowed = false;
			}
			
			if ( (ship.linearThrottle > 0) || (ship.linearThrottle == 0 && m_isReverseAllowed) || (ship.linearThrottle < 0)) {
				// decrease the throttle, but make sure we stop at zero
				if (ship.linearThrottle > 0 && ship.linearThrottle < EntityShip.LinearThrottleStep) {
					ship.linearThrottle = 0;
				} else {
					ship.linearThrottle -= EntityShip.LinearThrottleStep;
				}
			}
		}
		
		@Override
		public void resetShip(EntityShip ship) {
			m_isReverseAllowed = true;
		}
	};
	
	private int m_keyCode;
	
	private PilotAction() {
		m_keyCode = -1;
	}
	
	public static void setActionCodes(GameSettings settings) {
		Forward.m_keyCode = settings.keyBindForward.getKeyCode();
		Backward.m_keyCode = settings.keyBindBack.getKeyCode();
		Left.m_keyCode = settings.keyBindLeft.getKeyCode();
		Right.m_keyCode = settings.keyBindRight.getKeyCode();
		ThrottleUp.m_keyCode = settings.keyBindForward.getKeyCode();
		ThrottleDown.m_keyCode = settings.keyBindBack.getKeyCode();
	}
	
	public static int getActiveActions(GameSettings settings, List<PilotAction> allowedActions) {
		// roll up the actions into a bit vector
		int actions = 0;
		for (PilotAction action : allowedActions) {
			if (isKeyDown(action.m_keyCode)) {
				actions |= 1 << action.ordinal();
			}
		}
		return actions;
	}
	
	private static boolean isKeyDown(int code) {
		// NOTE: the KeyBinding classes are worthless when the GUI is active
		// so we need to query lwjgl directly
		if (isKey(code)) {
			return Keyboard.isKeyDown(code);
		} else if (isButton(code)) {
			return Mouse.isButtonDown(code + 100);
		}
		
		// no idea what this key is...
		return false;
	}

	private static boolean isKey(int code) {
		return code >= 0 && code < Keyboard.KEYBOARD_SIZE;
	}
	
	private static boolean isButton(int code) {
		int button = code + 100;
		return button >= 0 && button < Mouse.getButtonCount();
	}

	public static void applyToShip(EntityShip ship, int actions) {
		for (PilotAction action : values()) {
			if (action.isActive(actions)) {
				action.applyToShip(ship);
			}
		}
	}
	
	public static void resetShip(EntityShip ship, int actions, int oldActions) {
		for (PilotAction action : values()) {
			if (action.isActive(oldActions) && !action.isActive(actions)) {
				action.resetShip(ship);
			}
		}
	}
	
	public boolean isActive(int actions) {
		return ((actions >> ordinal()) & 0x1) == 1;
	}
	
	protected abstract void applyToShip(EntityShip ship);
	
	protected void resetShip(EntityShip ship) {
		// do nothing
	}
}
