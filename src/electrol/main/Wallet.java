package electrol.main;

import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.util.BigInteger;
import electrol.util.Constants;

public class Wallet {
 
	private Storage storage;
	private static int localBlockHeight;
	public Wallet (){ 
		this.storage = new Storage(Constants.STORAGE_PATH);
	}
	  
	public String getXPRV() throws JSONException {
		JSONObject keystore = storage.get("keystore", new JSONObject());
		return keystore.getString("xprv");
	}
	
	public static BigInteger getLocalBlockHeight() {
		
		  return new BigInteger(String.valueOf(localBlockHeight));
	}
	public static void setLocalBlockHeight(int blockHeight) {
		
		  localBlockHeight = blockHeight;
	}
	
	
} // end class
		
		
		 
 
 