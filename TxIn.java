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

public class TxIn {

 private String prevout_hash;
 private int prevout_n;
 private BigInteger satoshiAmount;
    
	public TxIn (String zprevout_hash, int zprevout_n, BigInteger zamount)
	
	{ 
		 prevout_hash=zprevout_hash;
		 prevout_n=zprevout_n;
		 satoshiAmount=zamount;
		
	}
	
	public void setHash(String zhash) {
		prevout_hash=zhash;
	}
	
	public void setAmount(BigInteger zamount) {
		satoshiAmount=zamount;
	}

	public void setN(int n) {
		prevout_n=n;
	}
	
	
	public String getHash() {
		return prevout_hash;
	}
	 
	public BigInteger getAmount () {
		return satoshiAmount;
	}
	
	public int getN() {
		return prevout_n;
	}
	
	  
} // end class
		
		
		 
 
 