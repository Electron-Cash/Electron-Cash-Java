package main;

import electrol.util.StringUtils;

public class MasterKeys {

	public MasterKeys()

	{

	} // end func

	/*static private String normalizeText(String mytext) {

		// Clean up extra whitespace.
		// regex delimiter here is anything that's not a word
		String[] words = StringUtils.split(mytext,"\\W+");
		StringBuffer buffer = new StringBuffer();
		int i = 0;
		for(i=0; i < words.length-1; i++) {
			buffer.append(words[i]+" ");
		}
		buffer.append(words[i-1]);
		return buffer.toString().toLowerCase();
	}*/

	public static String normalizeText(String s){
	    if(s.length() == 0 ) 
	    	return "";
	    int timesSpace = 0;
	    String res = "";

	    for (int i = 0; i < s.length(); i++) {
	        char c = s.charAt(i);

	        if(c == ' '){
	            timesSpace++;
	            if(timesSpace < 2)
	                res += c;
	        }else{
	            res += c;
	            timesSpace = 0;
	        }
	    }
	    return res.trim().toLowerCase();
	}
	
	static public byte[] get_seed_from_mnemonic(String mnemonic) {

		// Gets a byte array of the bip32 root "seed"
		// from the electrum mnemonic phrase.
		String salt = "electrum";
		int iterations = 2048;
		mnemonic = normalizeText(mnemonic);
		byte[] saltBytes = salt.getBytes();
		byte[] mnemonicBytes = mnemonic.getBytes();
		byte[] seedBytes = PBKDF2.hmac("SHA512", mnemonicBytes, saltBytes, iterations);
		return seedBytes;

	}

	// -------------------DEBUGGING MOSTLY OR DISPLAY READABLE TO
	// USER-----------------------
	public static String get_hex_version_of_seed(byte[] seedBytes) {

		return Util.bytesToHex(seedBytes);
	}

} // end class
