package main;

import java.math.BigInteger;

public class TxOut {

 private String addr;
 private BigInteger amount;
    
	public TxOut (String zaddr, BigInteger zamount)
	
	{ 
		addr=zaddr;
		amount=zamount;		
		
	}
	
	public void setAddr(String zaddr) {
		addr=zaddr;
	}
	
	public void setAmount(BigInteger zamount) {
		amount=zamount;
	}
	
	public BigInteger getAmount() {
		return amount;
	}
	
	public String getAddr() {
		return addr;
	}
	
	  
} // end class
		
		
		 
 
 