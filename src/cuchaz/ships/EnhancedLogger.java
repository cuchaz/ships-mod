/*******************************************************************************
 * Copyright (c) 2014 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import cuchaz.modsShared.Environment;

public class EnhancedLogger {
	
	private Logger m_logger;
	
	public EnhancedLogger(Logger logger) {
		m_logger = logger;
	}
	
	public void log(Level level, String message, Throwable t) {
		// prepend the side name
		m_logger.log(level, Environment.getSide().name().toUpperCase() + " " + message, t);
	}
	
	public void log(Level level, String message) {
		// prepend the side name
		m_logger.log(level, Environment.getSide().name().toUpperCase() + " " + message);
	}
	
	// convenience formatter methods
	
	public void error(Throwable t, String message, Object... args) {
		log(Level.ERROR, String.format(message, args), t);
	}
	
	public void warning(String message, Object... args) {
		log(Level.WARN, String.format(message, args));
	}
	
	public void warning(Throwable t, String message, Object... args) {
		log(Level.WARN, String.format(message, args), t);
	}
	
	public void info(String message, Object... args) {
		log(Level.INFO, String.format(message, args));
	}
	
	public void info(Throwable t, String message, Object... args) {
		log(Level.INFO, String.format(message, args), t);
	}
}
