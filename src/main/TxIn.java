package main;

import java.math.BigInteger;

public class TxIn {

 private String prevout_hash;
 private int prevout_n;
 private BigInteger satoshiAmount;
 private String script;
 private WalletAddress addr;
 private String signature;
    
	public TxIn (WalletAddress zaddr, String zprevout_hash, int zprevout_n, BigInteger zamount)
	
	{ 
		 prevout_hash=zprevout_hash;
		 prevout_n=zprevout_n;
		 satoshiAmount=zamount;
		 addr=zaddr;
		
	}
	
	public void setHash(String zhash) {
		prevout_hash=zhash;
	}
	
	public void setAmount(BigInteger zamount) {
		satoshiAmount=zamount;
	}

	public void setN(int n) {
		prevout_n=n;
	}
	
	public void setScript(String zscript) {
		script=zscript;
	}
	
	public void setAddr(WalletAddress zaddr) {
		addr=zaddr;
	}
	
	public void setSignature (String sig) {
		signature=sig;
	}
	
	public String getHash() {
		return prevout_hash;
	}
	 
	public WalletAddress getAddr() {
		return addr;
	}
	
	public BigInteger getAmount () {
		return satoshiAmount;
	}
	
	public int getN() {
		return prevout_n;
	}
	
	public String getScript() {
		return script;
	}
	
	public String getSignature() {
		return signature;
	}
	  
} // end class
		
		
		 
 
 