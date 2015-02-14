/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.persistence;

public class CorruptedPersistence extends PersistenceException {
	
	private static final long serialVersionUID = 6468194186150584767L;
	
	public CorruptedPersistence(Exception cause) {
		super("Saved ship appears corrupted!", cause);
	}
}
