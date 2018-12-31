package org.electroncash.util;

public class MethodParamsTuple { private String method;
  
  public MethodParamsTuple() {}
  
  public String getMethod() { return method; }
  
  private Object[] params;
  public void setMethod(String method) { this.method = method; }
  
  public Object[] getParams() {
    return params;
  }
  
  public void setParams(Object[] params) { this.params = params; }
}
