package main;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.bouncycastle.util.encoders.Base64;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.java.util.Arrays;
import electrol.util.Files;

public class Storage {

	private final int OLD_SEED_VERSION = 4;        //electrum versions < 2.0
	private final int NEW_SEED_VERSION = 11;       //electrum versions >= 2.0
	private final int FINAL_SEED_VERSION = 17;     //electrum >= 2.7 will set this to prevent
	//old versions from overwriting new format

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
	
	public JSONObject get(String key,JSONObject defaultValue) {
		JSONObject v = data.optJSONObject(key);
		if(v != null) {
			return (JSONObject)deepClone(v);
		}
		return defaultValue;
    }
	public JSONArray get(String key,JSONArray defaultValue) {
		JSONArray v = data.optJSONArray(key);
		if(v != null) {
			return (JSONArray)deepClone(v);
		}
		return defaultValue;
    }
	public boolean get(String key,boolean defaultValue) {
		return data.optBoolean(key, defaultValue);
    }
	
	private void put(String key, Integer value){
		try {
			
			if(value != null) {
				if(data.has(key)) {
					if(!value.equals(data.get(key))) {
						modified = true;
						data.put(key, (Integer)deepClone(value));
					}
				}
				else {
					modified = true;
					data.put(key, (Integer)deepClone(value));
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
	
	public static Object deepClone(Object object) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(object);
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bais);
			return ois.readObject();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
