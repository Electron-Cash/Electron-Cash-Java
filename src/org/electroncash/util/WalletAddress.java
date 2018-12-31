package org.electroncash.util;

public class WalletAddress
{
  private String addr;
  private int index;
  private boolean change;
  private String pubkey;
  
  public WalletAddress(String addr, int index, boolean change, String pubkey) {
    this.addr = addr;
    this.index = index;
    this.change = change;
    this.pubkey = pubkey;
  }
  
  public String getAddr() {
    return addr;
  }
  
  public int getIndex() {
    return index;
  }
  
  public boolean getChange() {
    return change;
  }
  
  public String getPubkey() {
    return pubkey;
  }
  
  public String toString() {
    return "addr:" + addr + ", index" + index + ", change " + change + ",pubkey " + pubkey;
  }
}
