package electrol.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.java.util.Arrays;
import electrol.main.BlockchainsUtil;

public class Util {

	public static String padLeftHexString(String s, int expectedLength) {

		String retval = s;
		StringBuffer zeroString = new StringBuffer();
		int myLength = s.length();
		if (expectedLength > myLength) {
			int numberOfZeroes = expectedLength - myLength;
			for (int i = 0; i < numberOfZeroes; i++) {
				zeroString.append("0");
			}
			retval = zeroString.toString() + retval;
		}
		return retval;
	}

	public static String reverseByteString(String s) {
		byte[] myBytes = hexStringToByteArray(s);
		for (int i = 0; i < myBytes.length / 2; i++) {
			byte temp = myBytes[i];
			myBytes[i] = myBytes[myBytes.length - i - 1];
			myBytes[myBytes.length - i - 1] = temp;
		}
		String result = bytesToHex(myBytes);
		return result;

	}

	public static String specialIntToHex(BigInteger i, int length) {

		String s = i.toString(16);
		int generation = (2 * length) - s.length();
		String leading_zeros = new String(new char[generation]).replace('\0', '0');
		String retval = leading_zeros + s;
		return BlockchainsUtil.revHex(retval);
	}

	public static String var_int(int i) {
		BigInteger bi = BigInteger.valueOf(i);
		if (i < 253) {
			return specialIntToHex(bi, 1);
		} else if (i < 65535) {
			return "FD" + specialIntToHex(bi, 2);
		} else {
			return "FE" + specialIntToHex(bi, 4);
		}
	}

	public static String bytesToHex(byte[] bytes) {
		final char[] hexArray = Constants.HEX_STRING.toCharArray();
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

		StringBuffer sb = new StringBuffer();
		StringBuffer temp = new StringBuffer();

		for (int i = 0; i < hex.length() - 1; i += 2) {

			String output = hex.substring(i, (i + 2));
			int decimal = Integer.parseInt(output, 16);
			sb.append((char) decimal);
			temp.append(decimal);
		}

		return sb.toString();
	}

	public static String hash_to_hex_str(byte[] x) {
		return bytesToHex(Arrays.reverse(x));
	}

	public static int unsignedToBytes(byte a) {
		int b = a & 0xFF;
		return b;
	}

	public static JSONArray getJsonArray(InputStream inputStream) throws JSONException, IOException {
		return new JSONArray(readFromStream(inputStream));
	}

	public static JSONObject getJsonObject(InputStream inputStream) throws JSONException, IOException {
		return new JSONObject(readFromStream(inputStream));
	}

	public static String readFromStream(InputStream inputStream) throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length;
		//System.out.println("inputStream.read(buffer) "+inputStream.read(buffer));
		while ((length = inputStream.read(buffer)) != -1) {
			System.out.println(buffer);
			result.write(buffer, 0, length);
		}
		result.close();
		return result.toString();
	}

	public static float round(float num, int numDecim) {
		long p = 1;
		// next line – calculate pow(10,brDecim)
		for (int i = 0; i < numDecim; i++)
			p *= 10;
		return (float) (int) (p * num + 0.5) / p;
	}
}
