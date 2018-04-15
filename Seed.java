package main;

import java.io.*;

import java.math.BigInteger; 
import java.util.ArrayList;
import java.util.Random;
 
 
public class Seed {

	
	private boolean seedHasCorrectChecksum(String seed) {
	
		//returns true if seed has the correct checksum.
		//only supports standard Electrum seeds, not old style seeds.
		String sha=Bitcoin.hmac_sha_512(seed,"Seed version");
		if (sha.substring(0,2).equals("01")) { return true; }
		else { return false; }
	}
	
	
   

ArrayList<String> wordList = new ArrayList<String>();
	
	public Seed()
	
	{ 
		
        
        // The name of the file to open.
        String wordListFileName = "C:/Users/Owner/eclipse-workspace/ElectronCash-FP/wordlist/english.txt";

        // This will reference one line at a time
        String line = null;

        try {
            // FileReader reads text files in the default encoding.
            FileReader fileReader = 
                new FileReader(wordListFileName);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = 
                new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) {
                 wordList.add(line);
            }   

            // Always close files.
            bufferedReader.close();         
        }
        catch(FileNotFoundException ex) {
            System.out.println(
                "Unable to open file '" + 
                wordListFileName + "'");                
        }
        catch(IOException ex) {
            System.out.println(
                "Error reading file '" 
                + wordListFileName + "'");                  
            // Or we could just do this: 
            // ex.printStackTrace();
        }
        
		
	}
	
	private BigInteger mnemonic_decode(String seed) {
		
		BigInteger n = new BigInteger("2048"); 
		BigInteger i = BigInteger.ZERO,kk = null;
		String[] words = seed.split(" ");
		int k;
	   
		
		 for ( String word : words) { 
			 k = wordList.indexOf(word);
			 kk= BigInteger.valueOf(k); 
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
         
         while (validSeedFound == false) {
              
        	  encoded_mnemonic = mnemonic_encode(b);
        	  decoded_mnemonic = mnemonic_decode(encoded_mnemonic);
        	  
        	  // DOUBLE CHECK IF ITS VALID - (CAN BE DECODED)
        	  if (decoded_mnemonic.compareTo(BigInteger.ZERO)!=1 ) { 
        		   System.out.println("ERROR DECODING SEED"); 
        		   System.exit(0);
        	  }
        	  
        	 if (seedHasCorrectChecksum(encoded_mnemonic) == true) {
              validSeedFound=true; 
        	 }
        	 
        b = b.add(BigInteger.ONE);	 
         }
         
		String retval=encoded_mnemonic;
        return retval;
        		
    } // end  func
} // end class
		
		
		 
 
 