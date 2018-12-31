package org.electroncash.util;

import org.electroncash.AlertUtil;

public class ApplicationContext {
  private AlertUtil alertUtil;
  private int latestBlockHeight;
  private boolean latestBlock;
  private String defaultServer;
  private org.electroncash.security.SecureSocketConnection socketConnection;
  private Config config;
  private String password;
  
  public ApplicationContext() {}
  
  public int getLatestBlockHeight() {
    return latestBlockHeight;
  }
  
  public void setLatestBlockHeight(int latestBlockHeight) {
    this.latestBlockHeight = latestBlockHeight;
  }
  
  public boolean isLatestBlock() {
    return latestBlock;
  }
  
  public void setLatestBlock(boolean latestBlock) {
    this.latestBlock = latestBlock;
  }
  
  public String getDefaultServer() {
    return defaultServer;
  }
  
  public void setDefaultServer(String defaultServer) {
    this.defaultServer = defaultServer;
  }
  
  public org.electroncash.security.SecureSocketConnection getSocketConnection() {
    return socketConnection;
  }
  
  public void setSocketConnection(org.electroncash.security.SecureSocketConnection socketConnection) {
    this.socketConnection = socketConnection;
  }
  
  public Config getConfig() {
    return config;
  }
  
  public void setConfig(Config config) {
    this.config = config;
  }
  
  public String getPassword() {
    return password;
  }
  
  public void setPassword(String password) {
    this.password = password;
  }
  
  public AlertUtil getAlertUtil() {
    return alertUtil;
  }
  
  public void setAlertUtil(AlertUtil alertUtil) {
    this.alertUtil = alertUtil;
  }
}
