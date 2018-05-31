package main.electron;

import java.math.BigInteger;
import electrol.java.util.ArrayList;
import electrol.java.util.Arrays;

import main.BitcoinCashAddressDecodedParts;
import main.BitcoinCashAddressType;
import main.Bitcoin;
import main.Util;
import main.CashAddr;
import main.electron.Base58;


public class Address {

	public byte[] hash160;
	public BitcoinCashAddressType kind;


	public Address(byte[] bs, BitcoinCashAddressType bitcoinCashAddressType) {
		// TODO Auto-generated constructor stub
		hash160 = bs;
		kind = bitcoinCashAddressType;
	}

	@Override
	public int hashCode(){
		return ((hash160[1] & 0xff)<<24) | ((hash160[2] & 0xff)<<16) | ((hash160[3] & 0xff)<<8) | (hash160[4] & 0xff) ;
	}

	@Override
	public boolean equals(Object otherAddress){
		Address a = null;
		if(otherAddress instanceof Address){
			a = (Address) otherAddress;
		} else {
			return false;
		}
		try {
			String localAddressString = toString((byte)1);
			String otherAddressString = a.toString((byte)1);
			return localAddressString.equals(otherAddressString);
		} catch (Exception ex){
			return false;
		}
	}

	public static Address from_string(String address) throws Exception {
		if(address.length()>35) {
			return from_cashaddr_string(address);
		}
		try {
			byte[] raw = Base58.decode_check(address);
			if (raw.length != 21) {
				throw new Exception();
			}
			byte[] result = new byte[20];
			for(int i = 0; i < 20; i++){
				result[i] = raw[i+1];
			}
			return new Address(result, BitcoinCashAddressType.P2PKH);
		} catch(Exception e){
			throw e;
		}

	}

	public static ArrayList from_strings(String[] addresses) throws Exception {
		ArrayList result = new ArrayList();
		for(String address:addresses){
			try {
				Address addr = from_string(address);
				result.add(addr);
			} catch(Exception e) {
				throw e;
			}
		}
		return result;
	}

	public String doubleSHA(String input) {
		return "";
	}

	public String toString(byte fmt) throws Exception {
		String result;
		switch(fmt){
			case 0:
				result = CashAddr.toCashAddress(kind, hash160);
				break;
			case 1:
				byte[] beBytes= new byte[21];
				beBytes[0] = (byte)0;
				for(int i = 0 ; i < 20; i++){
					beBytes[i + 1] = hash160[i];
				}
				result = Base58.encode_check(beBytes);
				break;
			default:
				throw new Exception();
		}
		return result;
	}

	public static Address from_cashaddr_string(String string) throws Exception {
	        //Construct from a cashaddress string.'''
	        String prefix = "bitcoincash";
	        if(string.toUpperCase().equals(string))
	            prefix = prefix.toUpperCase();
	        if(!string.startsWith(prefix + ":")) {
	            string = prefix +":"+ string;
	        }
	        BitcoinCashAddressDecodedParts bcadp = CashAddr.decodeCashAddress(string);
	        if(!bcadp.getPrefix().equals(prefix)) {
	            throw new Exception("address has unexpected prefix {} "+ bcadp.getPrefix());
	        }
	        return new Address(bcadp.getHash(), bcadp.getAddressType());
	}

	public static Address from_pubkey(String pubkey) throws Exception {
		int len = pubkey.length() / 2;
		if ((len == 33) && (pubkey.substring(0,2) == "02" || pubkey.substring(0,2) == "03" )) {
				throw new Exception();
		}
		if ((len == 65) && (pubkey.substring(0,2) == "04")) {
				throw new Exception();
		}
		byte[] hash160 = Util.hexStringToByteArray(Bitcoin.ripeHash(pubkey));
		return new Address(hash160, BitcoinCashAddressType.P2PKH);

	}

	public static Address from_P2PKH_hash(byte[] hash){
		return new Address(hash, BitcoinCashAddressType.P2PKH);
	}

	public static Address from_P2SH_hash(byte[] hash){
		return new Address(hash, BitcoinCashAddressType.P2SH);
	}
}
