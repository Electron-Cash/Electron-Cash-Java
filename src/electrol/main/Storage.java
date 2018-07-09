package electrol.main;

import org.bouncycastle.util.encoders.Base64;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.java.util.Arrays;
import electrol.util.Files;

public class Storage {

	private final Integer FINAL_SEED_VERSION = new Integer(17);   
	
	private String raw;
	private String path;
	private boolean modified = false;
	JSONObject data = new JSONObject();
	public Storage(String path ){
		this.path = path;
		if(Files.isExist(path)) {
			raw = Files.read(path);
			byte[] rawByte = new byte[4];
			try {
				rawByte = Base64.decode(raw);
				//System.out.println(rawByte.length);
			}catch(Exception e) {	}
			if(!"BIE1".equals(new String(Arrays.slice(rawByte, 0, 4)))){
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
	
	private void loadData(String rawByte) {
		try {
			data = new JSONObject(rawByte);
			String wallet_type = data.optString("wallet_type");
			System.out.println("wallet type "+wallet_type);
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	public void write() {
		if(!modified) {
			return;
		}
		
		Files.write(data,path );
		modified = false;
	}
	
	

}
