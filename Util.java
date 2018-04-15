package main;
 

import java.math.BigInteger; 

public class Util {

	public Util()

	{

	} // end func

	public static String padLeftHexString(String s, int expectedLength) {

		// Expected Lengths is how many digits, not how many bytes.
		// Be careful when converting from python. "8" bytes may really mean 16 digits.

		String retval = s;
		int numberOfZeroes = 0;
		String zeroString = "";
		int myLength = s.length();
		int i;

		if (expectedLength > myLength) {
			numberOfZeroes = expectedLength - myLength;
			for (i = 0; i < numberOfZeroes; i++) {
				zeroString = zeroString + "0";
			}
			retval = zeroString + retval;
		}

		return retval;
	}

	public static String reverseByteString(String s) {

		int i;
		byte[] myBytes = hexStringToByteArray(s);

		// REVERSE IN PLACE
		for (i = 0; i < myBytes.length / 2; i++) {
			byte temp = myBytes[i];
			myBytes[i] = myBytes[myBytes.length - i - 1];
			myBytes[myBytes.length - i - 1] = temp;
		}
		// END REVERSE CODE

		String result = bytesToHex(myBytes);
		return result;

	}

	public static String specialIntToHex(BigInteger i, int length) {

		String s = i.toString(16);

		int generation = (2 * length) - s.length();

		String leading_zeros = new String(new char[generation]).replace("\0", "0");

		String retval = leading_zeros + s;

		return retval;

	}

	public static String var_int(int i) {
		// https://en.bitcoin.it/wiki/Protocol_documentation#Variable_length_integer
		// Returns Hex string.

		BigInteger bi = BigInteger.valueOf(i);

		if (i < 253) {
			return specialIntToHex(bi, 1);
		}

		else if (i < 65535) {
			return specialIntToHex(bi, 2);
		}

		else {
			return "";
		}

	}

	public static String bytesToHex(byte[] bytes) {
		final char[] hexArray = "0123456789ABCDEF".toCharArray();
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	public static String convertHexToAscii(String hex) {

		StringBuilder sb = new StringBuilder();
		StringBuilder temp = new StringBuilder();

		// 49204c6f7665204a617661 split into two characters 49, 20, 4c...
		for (int i = 0; i < hex.length() - 1; i += 2) {

			// grab the hex in pairs
			String output = hex.substring(i, (i + 2));
			// convert hex to decimal
			int decimal = Integer.parseInt(output, 16);
			// convert the decimal to character
			sb.append((char) decimal);

			temp.append(decimal);
		}

		return sb.toString();
	}

	public static int unsignedToBytes(byte a) {
		int b = a & 0xFF;
		return b;
	}

} // end class
		 
 
 