package org.electroncash.util;

public class Server
{
  private String host;
  private int port;
  private String protocol;
  
  public Server(String host, int port, String protocol) {
    this.host = host;
    this.port = port;
    this.protocol = protocol;
  }
  
  public Server() {}
  
  public String getHost() { return host; }
  
  public void setHost(String host)
  {
    this.host = host;
  }
  
  public int getPort() {
    return port;
  }
  
  public void setPort(int port) {
    this.port = port;
  }
  
  public String getProtocol() {
    return protocol;
  }
  
  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }
  
  public String toString() { return host + ":" + port + ":" + protocol; }
}
