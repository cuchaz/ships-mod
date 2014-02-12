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
package cuchaz.ships;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;


public class Supporters
{
	public static final int InvalidSupporterId = -1;
	
	private static TreeMap<Integer,List<String>> m_supporters;
	
	static
	{
		try
		{
			m_supporters = Maps.newTreeMap();
			
			// load the supporters
			byte[] supporters = readResource( "supporters.txt" ); 
			
			// I wonder what this does...
			String thing = "MIIBuDCCASwGByqGSM44BAEwggEfAoGBAP1/U4EddRIpUt9KnC7s5Of2EbdSPO9EAMMeP4C2USZp"
				+ "RV1AIlH7WT2NWPq/xfW6MPbLm1Vs14E7gB00b/JmYLdrmVClpJ+f6AR7ECLCT7up1/63xhv4O1fn"
				+ "xqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith1yrv8iIDGZ3RSAHHAhUAl2BQjxUjC8yykrmCouuE"
				+ "C/BYHPUCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJ"
				+ "FnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImo"
				+ "g9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoDgYUAAoGBAN5wyqbzUkd8daZ6R3ndWgdqvkANa8fSYxNT"
				+ "vY0XAJwvT4spo6ZXg/TZVAt8Z1r/E04AOtQfUXr3aJ5spcYCdn6wkDuDWyuivq668vxKQG+erlDN"
				+ "v9nfU5NWOIXLoWycu+BLaFexcgytPncXFZrslYIp2uy7kflRUQaMle7EoLcX";
			byte[] sig = readResource( "supporters.sig" );
			if( Signer.verify( supporters, sig, thing ) )
			{
				Pattern pattern = Pattern.compile( "^Rank (\\d+):$" );
				
				// parse the supporters
				int currentRank = 0;
				BufferedReader in = new BufferedReader( new InputStreamReader( new ByteArrayInputStream( supporters ) ) );
				String line;
				while( ( line = in.readLine() ) != null )
				{
					// skip blank lines
					line = line.trim();
					if( line.length() <= 0 )
					{
						continue;
					}
					
					Matcher matcher = pattern.matcher( line );
					if( matcher.matches() )
					{
						// create a new rank
						currentRank = Integer.parseInt( matcher.group( 1 ) );
						m_supporters.put( currentRank, new ArrayList<String>() );
					}
					else
					{
						// read the supporter
						m_supporters.get( currentRank ).add( line );
					}
				}
				in.close();
			}
			else
			{
				m_supporters.put( 0, Arrays.asList( "Supporters file has been corrupted!" ) );
			}
		}
		catch( IOException ex )
		{
			Ships.logger.warning( ex, "Unable to load supporters!" );
		}
	}
	
	public static List<String> getSortedNames( )
	{
		return getSortedNames( 1 );
	}
	
	public static List<String> getSortedNames( int minRank )
	{
		List<String> names = new ArrayList<String>();
		for( int rank : m_supporters.descendingKeySet() )
		{
			names.addAll( m_supporters.get( rank ) );
			
			if( rank <= minRank )
			{
				break;
			}
		}
		return names;
	}
	
	public static int getId( String name )
	{
		name = normalizeName( name );
		for( int rank : m_supporters.descendingKeySet() )
		{
			List<String> names = m_supporters.get( rank );
			for( int i=0; i<names.size(); i++ )
			{
				if( normalizeName( names.get( i ) ).equals( name ) )
				{
					// build the id: 1 byte for the rank, 3 bytes for the index
					return rank | ( i << 8 );
				}
			}
		}
		return InvalidSupporterId;
	}
	
	public static String getName( int id )
	{
		int rank = id & 0xff;
		int index = ( id >> 8 ) & 0xffffff;
		List<String> names = m_supporters.get( rank );
		if( names != null && index < names.size() )
		{
			return names.get( index );
		}
		return null;
	}
	
	private static String normalizeName( String name )
	{
		return name.toLowerCase().replace( " ", "" );
	}
	
	private static byte[] readResource( String resourceName )
	throws IOException
	{
		InputStream in = null;
		try
		{
			String path = "/assets/ships/" + resourceName;
			in = Supporters.class.getResourceAsStream( path );
			if( in == null )
			{
				Ships.logger.warning( "Unable to read resource: %s", path );
			}
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int len;
			while( ( len = in.read( buffer ) ) >= 0 )
			{
				buf.write( buffer, 0, len );
			}
			return buf.toByteArray();
		}
		finally
		{
			if( in != null )
			{
				in.close();
			}
		}
	}
}
