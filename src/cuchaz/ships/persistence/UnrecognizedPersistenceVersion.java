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

public class UnrecognizedPersistenceVersion extends PersistenceException {
	
	private static final long serialVersionUID = 4074487319960658175L;
	
	private int m_version;
	
	public UnrecognizedPersistenceVersion() {
		this(-1);
	}
	
	public UnrecognizedPersistenceVersion(int version) {
		super(buildMessage(version));
		m_version = version;
	}
	
	private static String buildMessage(int version) {
		if (version >= 0) {
			return String.format("Version %d was not recognized!", version);
		} else {
			return "Unrecognized version!";
		}
	}
	
	public int getVersion() {
		return m_version;
	}
}
