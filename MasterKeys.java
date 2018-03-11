package main;

import java.io.*;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
 
public class MasterKeys {

	
 
	public MasterKeys()
	
	{ 
		 
    } // end  func
	
	
	static private String normalizeText(String mytext) {

	    //Clean up extra whitespace.
	    //regex delimiter here is anything that's not a word
	    String[] words = mytext.split("\\W+");
	    String delimiter = " ";
	    mytext = String.join(delimiter, words);
	    return mytext.toLowerCase();
	}
	
static public byte[] get_seed_from_mnemonic(String mnemonic) {
	
	// Gets a byte array of the bip32 root "seed" 
	// from the electrum mnemonic phrase.
	
    String retval="";
    String salt = "electrum";
    int iterations = 2048;
    
    mnemonic=normalizeText(mnemonic);
    byte[] saltBytes = salt.getBytes();
    byte[] mnemonicBytes = mnemonic.getBytes();
  
    byte[] seedBytes =PBKDF2.hmac("SHA512", mnemonicBytes, saltBytes, iterations);
    return seedBytes;

}
	



//-------------------DEBUGGING MOSTLY OR DISPLAY READABLE TO USER-----------------------
  public static String get_hex_version_of_seed(byte[] seedBytes) {
	  
	  return Bitcoin.bytesToHex(seedBytes);
      }
  

	
	
	
} // end class
		
		
		 
 
 