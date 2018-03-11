package main;

import java.io.*; 

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec; 
import java.util.Arrays; 
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider; 
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec; 
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
  
import org.bouncycastle.jce.interfaces.ECPublicKey;  
import java.security.KeyFactory;  
import java.security.Security;  
import java.security.MessageDigest;
import java.util.ArrayList; import java.util.Arrays; import java.util.Collections; import java.util.List;

public class Util {


    
	public Util()
	
	{ 
		 
    } // end  func


	public static String padLeftHexString(String s, int expectedLength) {
		  
		 String retval=s;
		 int numberOfZeroes=0;
		 String zeroString="";
		 int myLength= s.length();
		 int i;
		 
		 if (expectedLength > myLength) {
		     numberOfZeroes = expectedLength-myLength;
		     for (i=0; i < numberOfZeroes; i++) {
		    	 zeroString=zeroString+"0";
		     }
		     retval= zeroString+retval;
		 }
		 
		 return retval;
	 }
	
	 
	 
} // end class
		
		
		 
 
 