package main; 


/**
 * Copyright (c) 2018 Tobias Brandt
 * 
 * Distributed under the MIT software license, see the accompanying file LICENSE
 * or http://www.opensource.org/licenses/mit-license.php.
 */

public class BitcoinCashAddressType {
	
	public static final byte P2PKH = 0;
	public static final byte P2SH = 8;
	
	public static byte getVersionByte(byte option) {
		if(option == P2PKH) {
			return P2PKH;
		}
		else {
			return P2SH;
		}
	}
}