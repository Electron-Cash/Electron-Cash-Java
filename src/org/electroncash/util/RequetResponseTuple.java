package org.electroncash.util;

import org.json.me.JSONObject;

public class RequetResponseTuple {
  private RequestItem request;
  private JSONObject response;
  
  public RequetResponseTuple(RequestItem request, JSONObject response) {
    this.request = request;
    this.response = response;
  }
  
  public RequestItem getRequest() { return request; }
  
  public void setRequest(RequestItem request) {
    this.request = request;
  }
  
  public JSONObject getResponse() { return response; }
  
  public void setResponse(JSONObject response) {
    this.response = response;
  }
}
