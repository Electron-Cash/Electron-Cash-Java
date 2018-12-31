package org.electroncash.util;

public class TxOut
{
  private String addr;
  private long amount;
  
  public TxOut(String zaddr, long zamount) {
    addr = zaddr;
    amount = zamount;
  }
  
  public void setAddr(String zaddr) {
    addr = zaddr;
  }
  
  public void setAmount(long zamount) {
    amount = zamount;
  }
  
  public long getAmount() {
    return amount;
  }
  
  public String getAddr() {
    return addr;
  }
}
