package main;

import java.io.*;

import java.util.Random;


import electrol.java.util.ArrayList;
import electrol.util.BigInteger;
import electrol.util.StringUtils;


public class Seed {
	final static char CR = '\r';
	final static char LF = '\n';
	
	ArrayList wordList = new ArrayList();
	private boolean seedHasCorrectChecksum(String seed) {
		//returns true if seed has the correct checksum.
		//only supports standard Electrum seeds, not old style seeds.
		//System.out.println(seed);
		String sha=Bitcoin.hmac_sha_512(seed,"Seed version");
		if (sha.substring(0,2).equals("01")) { 
			return true; 
		}
		else { 
			return false; 
		}
	}
	public Seed()
	{ 
		try {
			InputStream is = getClass().getResourceAsStream("/wordlist/english.txt");
			int c = 0 ;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			while((c=is.read()) != -1) {
				if(c==LF) {
					String item = new String(out.toByteArray()); 
					wordList.add(item.trim());
					out = new ByteArrayOutputStream();
				}
				else {
					out.write(c);
				}
			}
		}	       
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	private BigInteger mnemonic_decode(String seed) {

		BigInteger n = new BigInteger("2048"); 
		BigInteger i = BigInteger.ZERO;

		String[] words = StringUtils.split(seed, " ");//seed.split(" ");
		for (int w= 0 ;w < words.length ; w++) { 
			int k = wordList.indexOf(words[w]);
			BigInteger kk= BigInteger.valueOf(k); 
			i=i.multiply(n);
			i=i.add(kk);
		}
		return i;
	}


	private String mnemonic_encode(BigInteger i) {

		String retval=""; 
		BigInteger n = new BigInteger("2048"); 
		while (i.compareTo(BigInteger.ZERO)==1 ) { 

			BigInteger x = i.mod(n);
			i = i.divide(n);
			retval=retval+wordList.get(x.intValue())+" ";

		}
		retval=retval.trim();  
		return retval;
	}

	public String generateSeed() {

		String encoded_mnemonic="";
		BigInteger decoded_mnemonic = null;

		// I believe this is already uniformly distributed.
		// Electron uses ecdsa.util.randrange to 
		// accomplish uniform distribution.
		// !!! - should double check this.

		// 132 bits
		BigInteger b = new BigInteger(132,new Random()); 
		boolean validSeedFound= false;
		while (!validSeedFound) {

			encoded_mnemonic = mnemonic_encode(b);
			decoded_mnemonic = mnemonic_decode(encoded_mnemonic);

			// DOUBLE CHECK IF ITS VALID - (CAN BE DECODED)
			if (decoded_mnemonic.compareTo(BigInteger.ZERO)!=1 ) { 
				System.out.println("ERROR DECODING SEED"); 
				System.exit(0);
			}

			if (seedHasCorrectChecksum(encoded_mnemonic)) {
				validSeedFound=true; 
			}

			b = b.add(BigInteger.ONE);	 
		}
		return encoded_mnemonic;
	} // end  func
} // end class




