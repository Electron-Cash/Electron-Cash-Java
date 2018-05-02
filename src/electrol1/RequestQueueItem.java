package electrol.main;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public class RequestQueueItem {
	private String method;
	private Object[] params;
	private int id;
	public RequestQueueItem(String method, Object[] params, int id) {
		this.method = method;
		this.params = params;
		this.id = id;
	}
	public JSONObject toJson() {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("method", method);
			jsonObject.put("id", id);
			JSONArray array = new JSONArray();
			for(int i = 0;i<params.length;i++) {
				array.put(params[i]);
			}
			jsonObject.put("params", array);
		}catch(JSONException e) {
			e.printStackTrace();
		}
		return jsonObject;
	}
	public static void main(String[] args) {
		RequestQueueItem item = new RequestQueueItem("hello", new String[] {"hello"}, 1);
		System.out.println(item.toJson().toString());
	}
	public Integer getId() {
		return new Integer(id);
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public Object[] getParams() {
		return params;
	}
	public void setParams(Object[] params) {
		this.params = params;
	}
	
}
