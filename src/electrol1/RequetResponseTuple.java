package electrol1;

import org.json.me.JSONObject;

public class RequetResponseTuple {
	private RequestQueueItem request;
	private JSONObject response;
	
	public RequetResponseTuple(RequestQueueItem request, JSONObject response) {
		this.request = request;
		this.response = response;
	}
	public RequestQueueItem getRequest() {
		return request;
	}
	public void setRequest(RequestQueueItem request) {
		this.request = request;
	}
	public JSONObject getResponse() {
		return response;
	}
	public void setResponse(JSONObject response) {
		this.response = response;
	}
	
}
