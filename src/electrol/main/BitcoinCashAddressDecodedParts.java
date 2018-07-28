package electrol.main;
 
// Helper class for CashAddr

public class BitcoinCashAddressDecodedParts {

	String prefix;

	byte addressType;

	byte[] hash;

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public byte getAddressType() {
		return addressType;
	}

	public void setAddressType(byte addressType) {
		this.addressType = addressType;
	}

	public byte[] getHash() {
		return hash;
	}

	public void setHash(byte[] hash) {
		this.hash = hash;
	}

}