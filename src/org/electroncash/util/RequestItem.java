package org.electroncash.util;

import org.json.me.JSONArray;

public class RequestItem {
	private String method;
	private Object[] params;
	private int id;

	public RequestItem(String method, Object[] params, int id) { 
		this.method = method;
		this.params = params;
		this.id = id;
	}

	public String toJsonString() { 
		StringBuffer string = new StringBuffer("{");
		string.append("\"id\": \"" + id);
		string.append("\", \"method\": \"" + method);
		JSONArray array = new JSONArray();
		for (int i = 0; i < params.length; i++) {
			array.put(params[i]);
		}
		string.append("\", \"params\": " + array.toString()+"}");
		return string.toString();
	}

	public String toString() {
		StringBuffer string = new StringBuffer("('");
		string.append(method + "', [");
		for (int i = 0; i < params.length; i++) {
			string.append(params[i]);
			if (i != params.length - 1) {
				string.append(", ");
			}
		}
		string.append("], ");
		string.append(id + ")");
		return string.toString();
	}

	public Integer getId() { return new Integer(id); }

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) { this.method = method; }

	public Object[] getParams() {
		return params;
	}

	public void setParams(Object[] params) { this.params = params; }
}
