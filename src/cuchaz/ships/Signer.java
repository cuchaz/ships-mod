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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Base64OutputStream;

public class Signer
{
	private static final String Provider = "SUN";
	private static final String Algorithm = "DSA";
	private static final String SignatureProtocol = "SHA1withDSA";
	
	@SuppressWarnings( "unused" )
	public static void main( String[] args )
	throws Exception
	{
		File dirAssets = new File( "src/assets/ships" );
		File fileSupporters = new File( dirAssets, "supporters.txt" );
		File fileSignature = new File( dirAssets, "supporters.sig" );
		File filePrivateKey = new File( "keys/supporters.key" );
		File filePublicKey = new File( "keys/supporters.pub" );
		
		// get the key
		PrivateKey key;
		if( false )
		{
			// generate new keys
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance( Algorithm, Provider );
			KeyPair pair = keyGen.genKeyPair();
			writeFile( filePrivateKey, pair.getPrivate().getEncoded() );
			writeFile( filePublicKey, pair.getPublic().getEncoded() );
			key = pair.getPrivate();
			
			// dump the public key
			System.out.println( Base64.encodeBase64String( pair.getPublic().getEncoded() ) );
		}
		else
		{
			// read the private key
			key = KeyFactory.getInstance( Algorithm, Provider ).generatePrivate( new PKCS8EncodedKeySpec( readFile( filePrivateKey ) ) );
		}
		
		// sign the supporters
		Signature signature = Signature.getInstance( SignatureProtocol, Provider );
		signature.initSign( key );
		signature.update( readFile( fileSupporters ) );
		writeFile( fileSignature, signature.sign() );
		System.out.println( "Signed!" );
		
		// verify the signature
		boolean verified = verify( readFile( fileSupporters ), readFile( fileSignature ), readFile( filePublicKey ) );
		System.out.println( "Verified: " + ( verified ? "Yes" : "No" ) );
		
		// read the supporters
		System.out.println( Supporters.getSortedNames() );
	}
	
	private static byte[] readFile( File file )
	throws IOException
	{
		FileInputStream in = new FileInputStream( file );
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len;
		while( ( len = in.read( buffer ) ) >= 0 )
		{
			buf.write( buffer, 0, len );
		}
		in.close();
		assert( file.length() == buf.size() );
		return buf.toByteArray();
	}
	
	private static void writeFile( File file, byte[] data )
	throws IOException
	{
		FileOutputStream out = new FileOutputStream( file );
		out.write( data );
		out.close();
		assert( file.length() == data.length );
	}
	
	public static boolean verify( byte[] data, byte[] signatureData, String pub )
	{
		try
		{
			return verify( data, signatureData, Base64.decodeBase64( pub ) );
		}
		catch( Exception ex )
		{
			// nope
			return false;
		}
	}
	
	public static boolean verify( byte[] data, byte[] signatureData, byte[] pubData )
	{
		try
		{
			PublicKey key = KeyFactory.getInstance( Algorithm, Provider ).generatePublic( new X509EncodedKeySpec( pubData ) );
			Signature signature = Signature.getInstance( SignatureProtocol, Provider );
			signature.initVerify( key );
			signature.update( data );
			return signature.verify( signatureData );
		}
		catch( Exception ex )
		{
			return false;
		}
	}
}
