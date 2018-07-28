package electrol.main;

import org.bouncycastle.asn1.eac.ECDSAPublicKey;
import org.bouncycastle.util.Arrays;

import electrol.main.BlockchainsUtil;
import electrol.util.BigInteger;
import electrol.util.BitcoinCashBase32;
import electrol.util.Constants;
import electrol.util.StringUtils;
import electrol.util.Util;

public class AddressUtil {

	public static final int OP_PUSHDATA1 = 76;
	public static final int OP_PUSHDATA2 = 77;
	public static final int OP_PUSHDATA4 = 78;
	public static final int OP_HASH160 = 169;

	public static final int OP_DUP = 118;
	public static final int OP_EQUAL = 135;
	public static final int OP_EQUALVERIFY = 136;
	public static final int OP_CHECKSIG = 172;
	public static final int OP_SINGLEBYTE_END = 0xF0;
	public static final int ADDRESS_TYPE_CHANGE = 1;
	public static final int ADDRESS_TYPE_RECEIVING = 0;
	public static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
	
	public static final byte P2PKH = 0;
	public static final byte P2SH = 8;

	
	private static final BigInteger[] POLYMOD_GENERATORS = new BigInteger[] { new BigInteger("98f2bc8e61", 16),
			new BigInteger("79b76d99e2", 16), new BigInteger("f33e5fb3c4", 16), new BigInteger("ae2eabe2a8", 16),
			new BigInteger("1e4f43e470", 16) };

	private static final BigInteger POLYMOD_AND_CONSTANT = new BigInteger("07ffffffff", 16);

	private String xpub;

	public AddressUtil(String xpub) {
		this.xpub = xpub;
	}
	
	public static String toCashAddress(byte addressType, byte[] hash) {
		byte[] prefixBytes = getPrefixBytes(Constants.MAINNET_PREFIX);
		byte[] payloadBytes = Arrays.concatenate(new byte[] { addressType }, hash);
		payloadBytes = convertBits(payloadBytes, 8, 5, false);
		byte[] allChecksumInput = Arrays.concatenate(
				Arrays.concatenate(Arrays.concatenate(prefixBytes, new byte[] { 0 }), payloadBytes),
				new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 });
		byte[] checksumBytes = AddressUtil.calculateChecksumBytesPolymod(allChecksumInput);
		checksumBytes = convertBits(checksumBytes, 8, 5, true);
		String cashAddress = BitcoinCashBase32.encode(Arrays.concatenate(payloadBytes, checksumBytes));
		return Constants.MAINNET_PREFIX + Constants.SEPARATOR_COLON + cashAddress;
	}

	public static BitcoinCashAddressDecodedParts decodeCashAddress(String bitcoinCashAddress) {
		if (!AddressUtil.isValidCashAddress(bitcoinCashAddress)) {
			
			throw new RuntimeException("Address wasn't valid: " + bitcoinCashAddress);
		}

		BitcoinCashAddressDecodedParts decoded = new BitcoinCashAddressDecodedParts();
		// String[] addressParts = bitcoinCashAddress.split(SEPARATOR);
		String[] addressParts = StringUtils.split(bitcoinCashAddress, Constants.SEPARATOR_COLON);
		if (addressParts.length == 2) {
			decoded.setPrefix(addressParts[0]);
		}

		byte[] addressData = BitcoinCashBase32.decode(addressParts[1]);
		addressData = Arrays.copyOfRange(addressData, 0, addressData.length - 8);
		addressData = BitcoinCashBitArrayConverter.convertBits(addressData, 5, 8, true);
		byte versionByte = addressData[0];
		byte[] hash = Arrays.copyOfRange(addressData, 1, addressData.length);

		decoded.setAddressType(getAddressTypeFromVersionByte(versionByte));
		decoded.setHash(hash);

		return decoded;
	}

	private static byte[] getPrefixBytes(String prefixString) {
		byte[] prefixBytes = new byte[prefixString.length()];

		char[] charArray = prefixString.toCharArray();
		for (int i = 0; i < charArray.length; i++) {
			prefixBytes[i] = (byte) (charArray[i] & 0x1f);
		}

		return prefixBytes;
	}

	private static byte[] convertBits(byte[] bytes8Bits, int from, int to, boolean strictMode) {
		// Copyright (c) 2017 Pieter Wuille

		int length = (int) (strictMode ? Math.floor((double) bytes8Bits.length * from / to)
				: Math.ceil((double) bytes8Bits.length * from / to));
		int mask = ((1 << to) - 1) & 0xff;
		byte[] result = new byte[length];
		int index = 0;
		int accumulator = 0;
		int bits = 0;
		for (int i = 0; i < bytes8Bits.length; i++) {
			byte value = bytes8Bits[i];
			accumulator = (((accumulator & 0xff) << from) | (value & 0xff));
			bits += from;
			while (bits >= to) {
				bits -= to;
				result[index] = (byte) ((accumulator >> bits) & mask);
				++index;
			}
		}
		if (!strictMode) {
			if (bits > 0) {
				result[index] = (byte) ((accumulator << (to - bits)) & mask);
				++index;
			}
		} else {
			if (!(bits < from && ((accumulator << (to - bits)) & mask) == 0)) {
				throw new RuntimeException("Strict mode was used but input couldn't be converted without padding");
			}
		}

		return result;
	}

	public static String to_scripthash_hex_from_cashaddr(String address) {
		byte[] p2pkh = decodeCashAddress(address).getHash();
		String s = Util.bytesToHex(p2pkh);
		s = Transaction.P2PKH_script(s);
		return Util.hash_to_hex_str(BlockchainsUtil.HashSingle(Util.hexStringToByteArray(s)));
	}
	
	public static String to_scripthash_hex_from_legacy(String address) {
		String hex = Transaction.decode(address);
		hex = Transaction.P2PKH_script(hex);
		return Util.hash_to_hex_str(BlockchainsUtil.HashSingle(Util.hexStringToByteArray(hex)));
	}

	
	public String generateRawBitcoinAddress(ECDSAPublicKey ecPublicKey) {
		BigInteger affineXCoord = ecPublicKey.getFirstCoefA();
		BigInteger affineYCoord = ecPublicKey.getFirstCoefA();
		return StringUtils.format(affineXCoord, 64) + StringUtils.format(affineYCoord, 64);
	}

	public static String generateCashAddrFromLegacyAddr(String legacyAddr) {
		String hex = Transaction.decode(legacyAddr);
		byte[] address = Util.hexStringToByteArray(hex);
		return toCashAddress(P2PKH, address).substring("bitcoincash:".length());
	}

	public String[] generatePubKeyHashAddresses(int index, int address_type) {
		String deserialized_xpub[] = Bitcoin.deserialize_xkey(xpub, false);
		String[] newKeys = Bitcoin._CKD_pub(deserialized_xpub[5], deserialized_xpub[4], "0000000" + address_type);
		String[] pubKeyHashes = new String[index];
		for (int i = 0; i < index; i++) {
			String hex = Integer.toHexString(i);
			String n = Util.padLeftHexString(hex, 8);
			String[] newKeys1 = Bitcoin._CKD_pub(newKeys[0], newKeys[1], n);
			String pubKeyHash = Bitcoin.ripeHash(newKeys1[0]);
			pubKeyHashes[i] = pubKeyHash;
		}
		return pubKeyHashes;
	}

	public String[] generateCashAddresses(int number, int address_type) {
		return generateCashAddresses(generatePubKeyHashAddresses(number, address_type));
	}

	public String[] generateCashAddresses(String[] pubKeyHash) {
		String[] addresses = new String[pubKeyHash.length];
		for (int i = 0; i < pubKeyHash.length; i++) {
			byte[] pubKeyHashBytes = Util.hexStringToByteArray(pubKeyHash[i]);
			String cash_address = toCashAddress(P2PKH, pubKeyHashBytes);
			addresses[i] = cash_address;
		}
		return addresses;
	}

	public String[] generateLegacyAddresses(String[] pubKeyHash) {
		String[] addresses = new String[pubKeyHash.length];
		for (int i = 0; i < pubKeyHash.length; i++) {
			addresses[i] = legacyAddrfromPubKeyHash(pubKeyHash[i]);
		}
		return addresses;
	}

	public String[] generateLegacyAddresses(int number, int address_type) {
		return generateLegacyAddresses(generatePubKeyHashAddresses(number, address_type));
	}

	public String generateOneCashAddress(int number, int address_type) {
		String pubKeyHash = getPubKeyHash(number, address_type);
		byte[] pubKeyHashBytes = Util.hexStringToByteArray(pubKeyHash);
		return toCashAddress(P2PKH, pubKeyHashBytes);
	}

	public String generateOneLegacyAddress(int number, int address_type) {
		String pubKeyHash = getPubKeyHash(number, address_type);
		return legacyAddrfromPubKeyHash(pubKeyHash);
	}

	public String getPubKeyHash(int number, int address_type) {
		String deserialized_xpub[] = Bitcoin.deserialize_xkey(xpub, false);
		String[] newKeys = Bitcoin._CKD_pub(deserialized_xpub[5], deserialized_xpub[4], "0000000" + address_type);
		String hex = Integer.toHexString(number);
		String n = Util.padLeftHexString(hex, 8);
		String[] newKeys1 = Bitcoin._CKD_pub(newKeys[0], newKeys[1], n);
		String pubKeyHash = Bitcoin.ripeHash(newKeys1[0]);
		return pubKeyHash;
	}

	public static boolean isValidCashAddress(String bitcoinCashAddress) {
		try {
			String prefix = "bitcoincash";
			if (bitcoinCashAddress.startsWith(prefix)) {
				bitcoinCashAddress = bitcoinCashAddress.substring(prefix.length() + 1);
			}

			if (!isSingleCase(bitcoinCashAddress))
				return false;

			bitcoinCashAddress = bitcoinCashAddress.toLowerCase();

			byte[] checksumData = Arrays.concatenate(Arrays.concatenate(getPrefixBytes(prefix), new byte[] { 0x00 }),
					BitcoinCashBase32.decode(bitcoinCashAddress));

			byte[] calculateChecksumBytesPolymod = calculateChecksumBytesPolymod(checksumData);
			return new BigInteger(calculateChecksumBytesPolymod).compareTo(BigInteger.ZERO) == 0;
		} catch (RuntimeException re) {
			return false;
		}
	}

	static byte[] calculateChecksumBytesPolymod(byte[] checksumInput) {
		BigInteger c = BigInteger.ONE;

		for (int i = 0; i < checksumInput.length; i++) {
			byte c0 = c.shiftRight(35).byteValue();
			c = c.and(POLYMOD_AND_CONSTANT).shiftLeft(5)
					.xor(new BigInteger(StringUtils.format(checksumInput[i], 2), 16));

			if ((c0 & 0x01) != 0)
				c = c.xor(POLYMOD_GENERATORS[0]);
			if ((c0 & 0x02) != 0)
				c = c.xor(POLYMOD_GENERATORS[1]);
			if ((c0 & 0x04) != 0)
				c = c.xor(POLYMOD_GENERATORS[2]);
			if ((c0 & 0x08) != 0)
				c = c.xor(POLYMOD_GENERATORS[3]);
			if ((c0 & 0x10) != 0)
				c = c.xor(POLYMOD_GENERATORS[4]);
		}

		byte[] checksum = c.xor(BigInteger.ONE).toByteArray();
		if (checksum.length == 5) {
			return checksum;
		} else {
			byte[] newChecksumArray = new byte[5];

			System.arraycopy(checksum, Math.max(0, checksum.length - 5), newChecksumArray,
					Math.max(0, 5 - checksum.length), Math.min(5, checksum.length));

			return newChecksumArray;
		}
	}

	private static boolean isSingleCase(String bitcoinCashAddress) {
		if (bitcoinCashAddress.equals(bitcoinCashAddress.toLowerCase())) {
			return true;
		}
		if (bitcoinCashAddress.equals(bitcoinCashAddress.toUpperCase())) {
			return true;
		}
		return false;
	}

	public static String legacyAddressDecode(String v) {

		BigInteger bi_58 = new BigInteger("58");
		byte[] vBytes = v.getBytes();
		BigInteger long_value = BigInteger.ZERO;
		int charpos = 0;
		int i = 0;
		BigInteger bi_charpos = BigInteger.ZERO;
		for (int counter = 0; counter <= vBytes.length - 1; counter++) {
			int c = vBytes[counter];
			charpos = Constants.BASE_58_CHARS.indexOf(c);
			bi_charpos = BigInteger.valueOf(charpos);
			long_value = long_value.multiply(bi_58).add(bi_charpos);
		}

		String zerostring = "";
		for (i = 0; i < v.length(); i++) {
			char ch = v.charAt(i);
			if (ch != '1') {
				break;
			}
			zerostring = zerostring + "00";
		}

		return zerostring + long_value.toString(16);
	}

	public static boolean addressIsValid(String txt) {

		if (txt.length() > 35) {
			return isValidCashAddress(txt);
		} else {
			String v = legacyAddressDecode(txt);
			String result = v.substring(0, v.length() - 8);
			String check = v.substring(v.length() - 8, v.length());
			String hash = Bitcoin.Hash(result);
			String hashPiece = hash.substring(0, 8);
			if (check.toUpperCase().equals(hashPiece.toUpperCase())) {
				return true;
			} else {
				return false;
			}
		}
	}

	public String getCassAddr(String pubKeyHash) {

		byte[] pubKeyHashBytes = Util.hexStringToByteArray(pubKeyHash);

		return toCashAddress(P2PKH, pubKeyHashBytes);
	}

	public static String encodeAddress(String addressBytes) {
        try {
            String encodedBytes = "00" + addressBytes;
            
            String checksum = Bitcoin.Hash(encodedBytes);
    		String checksumhead = (checksum.substring(0, 8));
            
            encodedBytes = encodedBytes + checksumhead;
            encodedBytes = encodedBytes.toLowerCase();
            encodedBytes = Base58.encode(ByteUtilities.toByteArray(encodedBytes));
            return encodedBytes;
        } catch (Exception e) {
            return null;
        }
    }

	
	public static String legacyAddrfromPubKeyHash(String pubKeyHash) {

		BigInteger bi_58 = new BigInteger("58");
		String b58chars = Constants.BASE_58_CHARS;

		StringBuffer addr = new StringBuffer("00").append(pubKeyHash);
		String checksum = Bitcoin.Hash(addr.toString());
		String checksumhead = (checksum.substring(0, 8));
		addr.append(checksumhead);
		BigInteger int_payload = new BigInteger(addr.toString(), 16);
		String mychar = "";
		StringBuffer result = new StringBuffer();

		while (int_payload.compareTo(bi_58) > -1) {
			BigInteger[] divMod = int_payload.divideAndRemainder(bi_58);
			BigInteger div = divMod[0];
			BigInteger mod = divMod[1];
			mychar = b58chars.substring(mod.intValue(), mod.intValue() + 1);
			result.append(mychar);
			int_payload = div;
		}

		mychar = b58chars.substring(int_payload.intValue(), int_payload.intValue() + 1);

		result.append(mychar).append("1");
		return StringUtils.reverse(result.toString());

	}
	
	public static String getLegacyAddressByScripthash(String scriptPubKey) {
        try {

            byte[] script = Util.hexStringToByteArray(scriptPubKey);
            byte[] address = new byte[21];
            address[0] = 0;
            System.arraycopy(script, 3, address, 1, 20);
            byte[] address256256 = BlockchainsUtil.Hash(address);;
            byte[] address25 = new byte[25];
            System.arraycopy(address, 0, address25, 0, address.length);
            System.arraycopy(address256256, 0, address25, address25.length - 4, 4);
            return Base58.encode(address25);
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

	
	public static byte getAddressTypeFromVersionByte(byte option) {
		if(option == P2PKH) {
			return P2PKH;
		}
		else {
			return P2SH;
		}
	}
}