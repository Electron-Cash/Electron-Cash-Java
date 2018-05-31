package main;

public class WalletAddress {

 private String addr;
 private int index;
 private boolean change;
 private String pubkey;
    
	public WalletAddress (String zaddr, int zindex, boolean zchange,String zpubkey)
	
	{ 
		 index=zindex;
		 change=zchange;
		 addr=zaddr;
		 pubkey=zpubkey;
		
	}
	
	    
	public String getAddr() {
		return addr;
	}	
	
	public int getIndex () {
		return index;
	}
	 
	
	public boolean getChange() {
		return change;
	}
	
	public String getPubkey() {
		return pubkey;
	}
	  
} // end class
		
		
		 
 
 