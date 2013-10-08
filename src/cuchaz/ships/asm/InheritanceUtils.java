package cuchaz.ships.asm;

import java.io.IOException;

import org.objectweb.asm.ClassReader;

public class InheritanceUtils
{
	public static boolean extendsClass( String className, String targetClassName )
	{
		// base case
		if( className.equalsIgnoreCase( targetClassName ) )
		{
			return true;
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
			ex.printStackTrace( System.err );
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
		try
		{
			ClassReader classReader = new ClassReader( interfaceName.replace( '.', '/' ) );
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
			ex.printStackTrace( System.err );
		}
		
		return false;
	}
}
