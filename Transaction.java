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
import javax.swing.plaf.basic.BasicTreeUI.TreeCancelEditingAction;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.crypto.DerivationFunction;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec; 
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint; 
import org.bouncycastle.math.raw.Mod;
import org.bouncycastle.jce.interfaces.ECPublicKey; 

import java.security.KeyFactory;  
import java.security.Security;  
import java.security.MessageDigest;
import java.security.PrivateKey;
 
import java.security.PublicKey; 
import java.security.Security;
import java.security.Signature;

public class Transaction {

 
    
	public Transaction()
	
	{ }
	
	
	public static String push_data (String data) {
  
		
	int OP_PUSHDATA1=76;
	String OP_PUSHDATA1_s="4C";
	int OP_PUSHDATA2=77;
	int OP_PUSHDATA4=78;
	
	int n=data.length()/2;
	String n_string = Integer.toHexString(n);
			
	if (n <OP_PUSHDATA1) {
		return n_string+data;
	}
	
	if (n < 256) {
		return OP_PUSHDATA1_s+n_string+data;
	}
	
	
	//This shouldn't happen.
	return "";
		
		
	}
	
	public static byte[] signData(byte[] data, PrivateKey key) throws Exception {
	    Signature signer = Signature.getInstance("SHA256withDSA");
	    signer.initSign(key);
	    signer.update(data);
	    return (signer.sign());
	  }
	
	
	
	
	public static String createSignatureFromKey (String zpre_hash, String zxprv ) {
		
		String xprv = "94a11d0bad3da900f5c7b19a92ef11ddb9aecf7bf9b1e11c0d9080c8cc2f3dc2";
		String pre_hash = "b6d0cbd50c5673e00803dc223bd93020ea560771ca1385a70d524ebafeb36a69";
		
		
		
		
		//sig 3044022049e296499025a11b16f6a645a5ed5a9fad9b21d2e2714b46b1a208b96a0d275702200bb3b49e3d717e349f33e58cf27d208e2dfb4729f99f17c65654ccc5cd09d090
	    byte [] data = Bitcoin.hexStringToByteArray(pre_hash);
	    //PrivateKey key = new PrivateKey()		
	    
	    
	   
	    
	    
	    
		return "";
	}
	
	
	public static String serializePreimage(int i, String addr,  BigInteger localBlockHeight, int prevout_n, TxIn [] txins, TxOut [] txouts) {
		
		String preimage="";
		String nVersion="01000000";  
		String nHashtype="41000000";
		String outpoint="";
		String scriptCode="";
		String amount="";
		String nSequence="";  
		String hashSequence="";
		String hashPrevouts="";
		int j=0;
		
		// Inputs and Sequences
		nSequence ="FEFFFFFF";
		for (j=0; j < txins.length; j++) {
			hashSequence+=nSequence;		
			hashPrevouts+=serializeOutpoint(txins[j]);
		}
		
		hashSequence = Bitcoin.Hash(hashSequence);
		hashPrevouts = Bitcoin.Hash(hashPrevouts);

		// Serialize Output
		String hashOutputs="";
				for ( j=0; j < txouts.length; j++) {
			    hashOutputs +=serializeOutput(txouts[j]);
		}
		hashOutputs=Bitcoin.Hash(hashOutputs);
		outpoint = serializeOutpoint (txins[i]);
		
		BigInteger satoshiAmount=txins[i].getAmount(); 
		System.out.println("satoshi amount is "+satoshiAmount);
		String preimage_script=P2PKH_script(decode(addr));
		int len=preimage_script.length();
		scriptCode=Bitcoin.var_int(len/2) + preimage_script;
		amount = Util.reverseByteString(Bitcoin.specialIntToHex(satoshiAmount,8));
		
		String nLocktime=Util.reverseByteString(Bitcoin.specialIntToHex(localBlockHeight,4));
		
		
		preimage=nVersion+hashPrevouts+hashSequence+outpoint+scriptCode+amount+nSequence+hashOutputs+nLocktime+nHashtype;
		
		
		// System.out.println("hashprevouts is "+hashPrevouts+ " hashSequence" + hashSequence);
		// System.out.println("outpoint " +outpoint+ "scriptCode "+scriptCode);
		// System.out.println("amount "+amount+ " nseqeunce "+nSequence);
		// System.out.println("hashoutputs "+hashOutputs+" nlocktime "+nLocktime);
		// System.out.println("pre image is "+preimage);
		 return preimage;
		
	}
	
	
	public static String serializeOutpoint (TxIn txin) {
		
		String zprevout_hash = txin.getHash();
		int zprevout_n = txin.getN();
		zprevout_hash=Util.reverseByteString(zprevout_hash);
		BigInteger bi_prevout = BigInteger.valueOf(zprevout_n);
		
		String zeropad= Bitcoin.specialIntToHex( bi_prevout, 4);
		return zprevout_hash+zeropad;
	}
	
	
	
	public static String serializeInput( TxIn txin, String script) {
		
		// dealing with prevout_hash and prevout_n
		 
		BigInteger zsatoshiValue =txin.getAmount();
		
		String s=serializeOutpoint(txin);
		int length = script.length()/2;
		String varInt=Bitcoin.var_int(length);
		
		// normally used for lock time.  here will be constant
		String sequence = "FEFFFFFF";
		String amount = Bitcoin.specialIntToHex(zsatoshiValue,8);
		
		// dont worry about estimated size, just assume we have a value.
		
		String retval= s+varInt+script+sequence+amount;
 
		return retval;
	}
	
	
	
	
	public static String serializeOutput (  TxOut txout) {
		
		
		
        String s;
        String addr=txout.getAddr();
        BigInteger satoshiAmount=txout.getAmount();
		String satoshiAmountHex=satoshiAmount.toString(16);
		satoshiAmountHex= Util.padLeftHexString(satoshiAmountHex,16);
		satoshiAmountHex=Util.reverseByteString(satoshiAmountHex);
		s=satoshiAmountHex;
		 
		String script=P2PKH_script(decode(addr));
		int len1=script.length()/2;
		String varInt=Bitcoin.var_int(len1);
		s=s+varInt+script;
		return s;
	}
	
	
	public static String P2PKH_script(String hash160) {
		
		 
		String OP_DUP="76";
		String OP_HASH160="A9";
		String OP_EQUALVERIFY="88";
		String OP_CHECKSIG="AC";
		
		String push_data = push_data(hash160);
		
		String retval=OP_DUP+OP_HASH160+push_data+OP_EQUALVERIFY+OP_CHECKSIG;
		
		return retval;
		
	}
	
	public static String decode(String txt) {

		 
		String b58chars = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

		BigInteger bi_58= new BigInteger("58");
		BigInteger bi_256 =  new BigInteger("256");
		String retval="";

		BigInteger val1 = BigInteger.ZERO;
		BigInteger val2 = BigInteger.ZERO;
		BigInteger long_value=BigInteger.ZERO;
		
		BigInteger  bi_charpos,cc;
		int value=0,c,charpos;
		int i =0;
		byte [] vBytes = txt.getBytes();
		

		for(int counter=0 ; counter < vBytes.length;counter++){
			 c=vBytes[counter];
			 
	         int ccc=Bitcoin.unsignedToBytes(vBytes[counter]);
	         cc= BigInteger.valueOf(ccc);    
	         charpos = b58chars.indexOf(ccc);
	         bi_charpos=BigInteger.valueOf(charpos); 
	      	 long_value=long_value.multiply(bi_58).add(bi_charpos);
 
	      	 
		} 
		
		
		retval=long_value.toString(16);
		int len=retval.length();
		retval=retval.substring(0, len-8); 
		return retval; 
	}
	
	
	public static void Generate() 
	
	{
		 
		//start with this: [(0, <Address 1FTF9bQhmpohLxoxMSmKS6unHuCYcEXhFs>, 1100)]
		
		int satoshiAmount=1100;
		
		String addr="1FTF9bQhmpohLxoxMSmKS6unHuCYcEXhFs";
		
		String satoshiAmountHex=Integer.toHexString(satoshiAmount);
		satoshiAmountHex= Util.padLeftHexString(satoshiAmountHex,16);
		satoshiAmountHex=Util.reverseByteString(satoshiAmountHex);
		
		String redeemScript=Bitcoin.ripeHash(addr);
		
		System.out.println("satoshiAmountHex "+satoshiAmountHex);
		 
		 
    } // end  func
} // end class
		
		
		 
 
 