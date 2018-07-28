package electrol.main;

public class MasterKeys {

	public String normalizeText(String s){
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
	
	public byte[] get_seed_from_mnemonic(String mnemonic) {
		String salt = "electrum";
		int iterations = 2048;
		byte[] saltBytes = salt.getBytes();
		byte[] mnemonicBytes = normalizeText(mnemonic).getBytes();
		return PBKDF2.hmac("SHA512", mnemonicBytes, saltBytes, iterations);
	}
} 
