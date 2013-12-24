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
package cuchaz.ships.asm;

import java.io.IOException;
import java.util.logging.Level;

import org.objectweb.asm.ClassReader;

import cuchaz.ships.Ships;

public class InheritanceUtils
{
	public static boolean extendsClass( String className, String targetClassName )
	{
		// base case
		if( className.equalsIgnoreCase( targetClassName ) )
		{
			return true;
		}
		
		// is this class an array? Just ignore arrays
		if( className.startsWith( "[" ) )
		{
			return false;
		}
		
		// load the super class and test recursively
		try
		{
			ClassReader classReader = new ClassReader( className.replace( '.', '/' ) );
			String superClassName = classReader.getSuperName();
			if( superClassName != null )
			{
				return extendsClass( superClassName, targetClassName );
			}
		}
		catch( IOException ex )
		{
			Ships.logger.log( Level.WARNING, "Unable to read class: " + className, ex );
		}
		
		return false;
	}
	
	public static boolean implementsInterface( String interfaceName, String targetInterfaceName )
	{
		// base case
		if( interfaceName.equalsIgnoreCase( targetInterfaceName ) )
		{
			return true;
		}
		
		// recurse
		String className = interfaceName.replace( '.', '/' );
		try
		{
			ClassReader classReader = new ClassReader( className );
			for( String i : classReader.getInterfaces() )
			{
				if( implementsInterface( i, targetInterfaceName ) )
				{
					return true;
				}
			}
		}
		catch( IOException ex )
		{
			Ships.logger.log( Level.WARNING, "Unable to read class: " + className, ex );
		}
		
		return false;
	}
}
