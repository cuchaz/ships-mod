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
package cuchaz.ships.persistence;

public class PersistenceException extends Exception {
	
	private static final long serialVersionUID = -5267418089825144274L;
	
	public PersistenceException(String message) {
		super(message);
	}
	
	public PersistenceException(String message, Exception cause) {
		super(message, cause);
	}
}
