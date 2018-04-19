package electrol.util;

import java.io.IOException;

import electrol.java.util.HashMap;
import electrol.java.util.Map;

public class Config {
	public String fee_estimates="";
	Map map = new HashMap();
	public Config() {
		map.put("blockchain_index", new Integer(2));
		map.put("server", "");
	}
	
	public String path() {
		try {
			return Files.getDefaultPath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	
	public void set_key(String key, Object value) {
		map.put(key, value);
	}
	
	public void set_key(String key, Object value, boolean option) {
		map.put(key, value);
	}
	
	public Object get(String key, Object defaultValue) {
		Object value = map.get(key);
		if(value != null) {
			return value;
		}
		else {
			return defaultValue;
		}
	}
	public Object value(String key) {
		return map.get(key);
	}

	public boolean is_fee_estimates_update_required() {
		// TODO Auto-generated method stub
		return false;
	}

	public void update_fee_estimates(String i, int fee) {
		
		
	}

	public void requested_fee_estimates() {
		// TODO Auto-generated method stub
		
	}
}
