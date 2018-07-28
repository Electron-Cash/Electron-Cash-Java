package electrol.main;

import java.io.IOException;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.util.encoders.Base64;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.java.util.Arrays;
import electrol.util.AESEncryption;
import electrol.util.Files;

public class Storage {

	private final Integer FINAL_SEED_VERSION = new Integer(17);   
	
	private byte[] raw;
	private String path;
	private boolean modified = false;
	JSONObject data = new JSONObject();
	AESEncryption encryption = AESEncryption.getInstance();
	
	public Storage(String path, String secret) throws InvalidCipherTextException, IOException {
		encryption.init(secret);
		this.path = path;
		if(Files.isExist(path)) {
			raw = Files.read(path);
			byte[] rawByte = Base64.decode(raw);
			if("BIE1".equals(new String(Arrays.slice(rawByte, 0, 4)))){
				rawByte = encryption.decrypt(raw);
				loadData(rawByte);
			}
			else {
				loadData(raw);
			}
		}
		else {
			put("seed_version", FINAL_SEED_VERSION);
		}
		write();
	}
	public String getPath() {
		return path;
	}
	public JSONObject get(String key,JSONObject defaultValue) {
		JSONObject v = data.optJSONObject(key);
		if(v != null) {
			return v;
		}
		return defaultValue;
    }
	public JSONArray get(String key,JSONArray defaultValue) {
		JSONArray v = data.optJSONArray(key);
		if(v != null) {
			return v;
		}
		return defaultValue;
    }
	public boolean get(String key,boolean defaultValue) {
		return data.optBoolean(key, defaultValue);
    }
	public String get(String key,String defaultValue) {
		return data.optString(key, defaultValue);
    }
	
	public void put(String key, Integer value){
		try {
			
			if(value != null) {
				if(data.has(key)) {
					if(!value.equals(data.get(key))) {
						modified = true;
						data.put(key, value);
					}
				}
				else {
					modified = true;
					data.put(key, value);
				}
			}
			else if(data.has(key)){
				modified = true;
				data.remove(key);
			}
		}catch (JSONException e) {
			System.out.println("Can not save json");
		}

	}

	public void put(String key, String value){
		try {
			
			if(value != null) {
				if(data.has(key)) {
					if(!value.equals(data.get(key))) {
						modified = true;
						data.put(key, value);
					}
				}
				else {
					modified = true;
					data.put(key, value);
				}
			}
			else if(data.has(key)){
				modified = true;
				data.remove(key);
			}
		}catch (JSONException e) {
			System.out.println("Can not save json");
		}

	}
	
	public void put(String key, JSONObject value){
		try {
			
			if(value != null) {
				
				if(data.has(key)) {
					modified = true;
					if(!value.equals(data.get(key))) {
						modified = true;
						data.put(key, value);
					}
				}
				else {
					modified = true;
					data.put(key, value);
				}
			}
			else if(data.has(key)){
				modified = true;
				data.remove(key);
			}
		}catch (JSONException e) {
			System.out.println("Can not save json");
		}

	}
	
	private void loadData(byte[] rawByte) {
		try {
			if(rawByte.length == 0) {
				data = new JSONObject();
			}
			else {
				data = new JSONObject(new String(rawByte));
			}
			String wallet_type = data.optString("wallet_type");
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	public void write() {
		if(!modified) {
			return;
		}
		try {
			byte[] save = encryption.encrypt(data.toString());
			Files.write(save, path);
		} catch (InvalidCipherTextException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		modified = false;
	}
	
	

}
