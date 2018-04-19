package main;

import java.io.UnsupportedEncodingException;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;

import electrol.util.BigInteger;
import electrol.util.StringUtils;

public class Bitcoin {

	// These are constants in secp256k1
	private static BigInteger curve_p = new BigInteger(
			"115792089237316195423570985008687907853269984665640564039457584007908834671663");
	private static BigInteger curve_a = new BigInteger("0");
	private static BigInteger curve_b = new BigInteger("7");
	// https://github.com/credentials/bouncycastle-ext/blob/master/src/org/bouncycastle/math/ec/pairing/ECCurveWithPairing.java

	private static final String EC_GEN_PARAM_SPEC = "secp256k1";
	private static final String KEY_PAIR_GEN_ALGORITHM = "ECDSA";

	public Bitcoin()

	{

	} // end func

	// https://stackoverflow.com/questions/4582277/biginteger-powbiginteger
	public static BigInteger bigPow(BigInteger base, BigInteger exponent) {

		// This method is to allow exponents on Big Integers.

		BigInteger result = BigInteger.ONE;
		while (exponent.signum() > 0) {
			if (exponent.testBit(0))
				result = result.multiply(base);
			base = base.multiply(base);
			exponent = exponent.shiftRight(1);
		}
		return result;
	}

	public static String[] ECC_YfromX(String x, boolean odd) {

		// This method will return a Y value from a corresponding X value on an elliptic
		// curve.

		String[] retval = new String[2];
		BigInteger bi_x = new BigInteger(x);
		BigInteger bi_Mx, bi_My2, bi_My;
		BigInteger bi_4 = new BigInteger("4");
		BigInteger bi_offset;
		int offset;
		boolean curve_contains_point = false;

		for (offset = 0; offset < 128; offset++) {
			bi_offset = BigInteger.valueOf(offset);

			bi_Mx = bi_x.add(bi_offset);
			// since a = 0 for secp256k1, we can simplify:
			bi_My2 = ((bi_Mx.pow(3)).mod(curve_p)).add(curve_b.mod(curve_p));
			bi_My = (bigPow(bi_My2, ((curve_p.add(BigInteger.ONE)).divide(bi_4)))).mod(curve_p);

			curve_contains_point = curveContainsPoint(bi_Mx, bi_My);

			if (curve_contains_point) {
				if (odd) {
					retval[0] = bi_My.toString();
					retval[1] = bi_offset.toString();
				} else {
					retval[0] = curve_p.subtract(bi_My).toString();
					retval[1] = bi_offset.toString();
				}
			}

			return retval;
		} // end for loop
		System.out.println("ECC_YfromX: No Y found");
		System.exit(0);
		return retval;
	} // end function

	public static String[] ser_to_point(String Aser) {

		// This method used for ECC derivation

		String[] retval = new String[2];
		String x, y;

		if (Aser.substring(0, 2).equals("04")) {
			x = Aser.substring(1, 33);
			y = Aser.substring(33, Aser.length());
			return retval;
		}

		boolean odd = false;
		if (Aser.substring(0, 2) == "03") {
			odd = true;
		}
		String Mx = Aser.substring(1, Aser.length());

		String[] y_parts = ECC_YfromX(Mx, odd);
		retval[0] = Mx;
		retval[1] = y_parts[0];
		return retval;
	}

	public static String base_decode_58(String v) {

		// This method using to decode a string for base58.

		BigInteger bi_58 = new BigInteger("58");
		BigInteger bi_256 = new BigInteger("256");

		String b58chars = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

		byte[] vBytes = v.getBytes();

		BigInteger val1 = BigInteger.ZERO;
		BigInteger val2 = BigInteger.ZERO;
		BigInteger long_value = BigInteger.ZERO;

		BigInteger cc = BigInteger.ZERO;

		int c = 0;
		int charpos = 0;
		int i = 0;
		BigInteger bi_charpos = BigInteger.ZERO;
		int kounter = vBytes.length - 1;
		int kkk = Util.unsignedToBytes(vBytes[kounter]);

		for (int counter = vBytes.length - 1; counter >= 0; counter--) {
			c = vBytes[counter];
			int ccc = Util.unsignedToBytes(vBytes[counter]);
			cc = BigInteger.valueOf(ccc);
			charpos = b58chars.indexOf(ccc);
			bi_charpos = BigInteger.valueOf(charpos);
			val1 = bi_58.pow(i);
			i++;
			val2 = val1.multiply(bi_charpos);
			long_value = long_value.add(val2);

		}

		BigInteger[] divMod = null;
		BigInteger div = null;
		BigInteger mod = null;
		String result = "";
		String modhex = "";

		// Main decoding loop

		while (long_value.compareTo(bi_256) > -1) {
			divMod = long_value.divideAndRemainder(bi_256);
			div = divMod[0];
			mod = divMod[1];

			modhex = Integer.toHexString(mod.intValue());

			// ensure hexbyte contains leading 0 if necessary.
			if (modhex.length() == 1) {
				modhex = "0" + modhex;
			}

			result = result + modhex;

			long_value = div;
		}
		String long_value_string = long_value.toString();
		if (long_value_string.length() == 1) {
			long_value_string = "0" + long_value_string;
		}
		result = result + long_value_string;

		// Extra zero padding if necessary.
		byte oneByte = Byte.parseByte("1");
		boolean has_leading_zeroes = true;
		int nPad = 0;
		byte myByte;
		while (has_leading_zeroes) {
			myByte = (byte)vBytes[nPad];
			if (myByte == oneByte) {
				nPad++;
			} else {
				has_leading_zeroes = false;
			}

		} // end while

		for (int n = 0; n < nPad; n++) {
			result = result + "00";
		}

		byte[] finalBytes = Util.hexStringToByteArray(result);

		// REVERSE IN PLACE
		for (i = 0; i < finalBytes.length / 2; i++) {
			byte temp = finalBytes[i];
			finalBytes[i] = finalBytes[finalBytes.length - i - 1];
			finalBytes[finalBytes.length - i - 1] = temp;
		}
		// END REVERSE CODE

		result = Util.bytesToHex(finalBytes);
		result = result.substring(0, (result.length() - 8));
		return result;

	}

	public static String base_encode_58(String v) {

		// This method is used to encode a string for base58.

		BigInteger bi_58 = new BigInteger("58");
		BigInteger bi_256 = new BigInteger("256");
		String b58chars = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
		String suffixHash = Hash(v);
		byte[] vBytes = Util.hexStringToByteArray(v);

		v = v + suffixHash.substring(0, 8);
		int i = 0;
		BigInteger val1 = BigInteger.ZERO;
		BigInteger val2 = BigInteger.ZERO;
		BigInteger long_value = BigInteger.ZERO;
		BigInteger cc = BigInteger.ZERO;
		byte[] zz = Util.hexStringToByteArray(v);

		// First prepare main variable "long_value"
		for (int counter = zz.length - 1; counter >= 0; counter--) {

			int ccc = Util.unsignedToBytes(zz[counter]);
			cc = BigInteger.valueOf(ccc);
			val1 = bi_256.pow(i);

			val2 = val1.multiply(cc);

			long_value = long_value.add(val2);
			i++;

		}

		BigInteger[] divMod = null;
		BigInteger div = null;
		BigInteger mod = null;
		String mychar = "";
		String result = "";

		// Main encoding loop
		while (long_value.compareTo(bi_58) > -1) {
			divMod = long_value.divideAndRemainder(bi_58);
			div = divMod[0];
			mod = divMod[1];
			mychar = b58chars.substring(mod.intValue(), mod.intValue() + 1);
			result = result + mychar;
			long_value = div;
		}

		// handle final character
		mychar = b58chars.substring(long_value.intValue(), long_value.intValue() + 1);
		result = result + mychar;

		// Extra zero padding if necessary.
		byte zeroByte = Byte.parseByte("0");
		boolean has_leading_zeroes = true;
		int nPad = 0;
		byte myByte;
		while (has_leading_zeroes) {
			myByte = vBytes[nPad];
			if (myByte == zeroByte) {
				nPad++;
			} else {
				has_leading_zeroes = false;
			}

		} // end while

		for (int n = 0; n < nPad; n++) {
			result = result + "0";
		}

		StringBuffer retval = new StringBuffer();
		retval.append(result);
		retval = retval.reverse();
		return retval.toString();
	}

	public static String Hash(String x) {

		// This method will perform a double SHA-256 Hash
		Digest digest = new SHA256Digest();
		byte[] xBytes = Util.hexStringToByteArray(x);
		digest.update(xBytes, 0, xBytes.length);
		byte[] out = new byte[32];
		digest.doFinal(out, 0);
		digest.update(out, 0, out.length);
		digest.doFinal(out, 0);
		return Util.bytesToHex(out);
	}

	// This method will peform a RIPEMD160 Hash of the SHA256 Hash.
	public static String ripeHash(String x) {

		byte[] xBytes = Util.hexStringToByteArray(x);
		
		// --- Hash sha256
		Digest digest = new SHA256Digest();
		digest.update(xBytes, 0, xBytes.length);
		byte[] out = new byte[32];
		digest.doFinal(out, 0);
		
		// ripemd160
		digest = new RIPEMD160Digest();
		digest.update(out, 0, out.length);
		out = new byte[20];
		digest.doFinal(out, 0);
		
		return Util.bytesToHex(out);
	}

	public static String getHexPubKeyfromECkeys(ECPublicKeyParameters ecPublicKey, boolean compressed) {

		// This method will get a string version of Pubkey from ECC object

		ECPoint ec = ecPublicKey.getQ();
		BigInteger affineXCoord = ec.getAffineXCoord().toBigInteger();
		BigInteger affineYCoord = ec.getAffineYCoord().toBigInteger();
		if (!compressed) {
			return StringUtils.format(affineXCoord,64) + StringUtils.format(affineYCoord,64);
		} else {
			// If odd, use 03 otherwise 02 - standard key compression rule
			if (affineYCoord.and(BigInteger.ONE).compareTo(BigInteger.ONE) == 0) {
				return "03" + StringUtils.format(affineXCoord, 64);
			} else {
				return "02" + StringUtils.format(affineXCoord,64);
			}
		}
	}

	public static String serializeXprv(byte[] c, byte[] k) {

		String xprv = "0488ADE4000000000000000000" + Util.bytesToHex(c) + "00" + Util.bytesToHex(k);
		return xprv;
	}

	public static String serializeXpub(byte[] c, byte[] Ck) {

		String xpub = "0488B21E000000000000000000" + Util.bytesToHex(c) + Util.bytesToHex(Ck);
		return xpub;
	}

	public static String[] deserialize_xkey(String xkey, boolean prv) {
		xkey = base_decode_58(xkey);
		String depth = xkey.substring(8, 10);
		String fingerprint = xkey.substring(10, 18);
		String child_number = xkey.substring(18, 26);
		String c = xkey.substring(26, 90);

		if (!prv) {
			if ((!xkey.substring(0, 8).equals("0488B21E")) && (!xkey.substring(0, 8).equals("0488b21e"))) {
				System.out.println("BAD KEY HEADER FAILURE.");
				System.exit(0);
			}
		}

		if (prv) {
			if ((!xkey.substring(0, 8).equals("0488ADE4")) && (!xkey.substring(0, 8).equals("0488ade4"))) {
				System.out.println("BAD KEY HEADER FAILURE.");
				System.exit(0);
			}
		}

		int n = 32;
		if (prv) {
			n = 33;
		}

		String K_or_k;
		K_or_k = xkey.substring(26 + (n * 2), xkey.length());
		String[] retval = new String[6];
		retval[0] = "standard"; // xtype
		retval[1] = depth;
		retval[2] = fingerprint;
		retval[3] = child_number;
		retval[4] = c;
		retval[5] = K_or_k;

		return retval;
	}

	public static byte[] get_root512_from_bip32root(byte[] bip32root) {
		byte[] root512 = hmac_sha_512_bytes(bip32root, "Bitcoin seed");
		return root512;
	}

	public static String fetchPrivateKey(String masterXPRV, boolean change, int index) {

		// This function returns a private key for an address. Main wrapper function.

		String deserializedXprvPieces[] = deserialize_xkey(masterXPRV, true);
		String xprv_c = deserializedXprvPieces[4];
		String xprv_k = deserializedXprvPieces[5];

		int change_int = 0;
		if (change) {
			change_int = 1;
		}

		String[] initial = CKD_priv(xprv_k, xprv_c, change_int);

		String[] second = CKD_priv(initial[0], initial[1], index);

		String retval = second[0];

		return retval;

	}

	public static String[] CKD_priv(String k, String c, int n) {

		// Child Key Derivation Private Key function

		BigInteger BIP32_PRIME = new BigInteger("80000000", 16);
		BigInteger bi_n = BigInteger.valueOf(n);
		BigInteger bi_prime = bi_n.and(BIP32_PRIME);
		boolean is_prime;

		if (bi_prime.compareTo(BigInteger.ZERO) == 1) {
			is_prime = true;
		} else {
			is_prime = false;
		}

		String hex_n = Integer.toHexString(n);
		hex_n = Util.padLeftHexString(hex_n, 8);

		String[] retval = _CKD_priv(k, c, hex_n, is_prime);
		return retval;

	}

	public static String[] _CKD_priv(String k, String c, String s, boolean is_prime) {

		
		
		X9ECParameters ecParameterSpec = ECNamedCurveTable.getByName(EC_GEN_PARAM_SPEC);
		BigInteger order = ecParameterSpec.getN();

		byte[] kBytes = Util.hexStringToByteArray(k);

		String cK = get_pubkey_from_secret(kBytes);

		String data;

		if (is_prime) {
			// for hardened keys. Isn't used in first version of this wallet
			data = "00" + k + s;
		}

		else {
			data = cK + s;
		}

		byte[] dataBytes = Util.hexStringToByteArray(data);
		byte[] I = hmac_sha_512_bytes_from_hex(dataBytes, c);

		String I_hex = Util.bytesToHex(I);
		String I_hex32 = I_hex.substring(0, 64); // first 32 bytes (64 chars)
		String I_hex32b = I_hex.substring(64, I_hex.length()); // first 32 bytes (64 chars)

		BigInteger bi_I32 = new BigInteger(I_hex32, 16);
		BigInteger bi_k = new BigInteger(k, 16);

		BigInteger val1 = bi_I32.add(bi_k);
		BigInteger val2 = val1.mod(order);
		String k_n = val2.toString(16);
		// Pad this so the key is exactly 32 bytes
		k_n = Util.padLeftHexString(k_n, 64);

		String[] retval = new String[2];

		retval[0] = k_n;

		retval[1] = I_hex32b;
		
	
		return retval;
	}

    public static String[] _CKD_pub(String cK, String c, String s) {
		
    	X9ECParameters ecParameter = ECNamedCurveTable.getByName(EC_GEN_PARAM_SPEC);
		BigInteger order = ecParameter.getN();

		String data = cK + s;
		byte[] dataBytes = Util.hexStringToByteArray(data);
		byte[] I = hmac_sha_512_bytes_from_hex(dataBytes, c);
		String I_hex = Util.bytesToHex(I);
		String I_hex32 = I_hex.substring(0, 64); // first 32 bytes (64 chars)
		BigInteger bi_I32 = new BigInteger(I_hex32, 16);
		byte[] cK_bytes = new BigInteger(cK, 16).toByteArray();

		ECPoint ecPoint = ecParameter.getG().multiply(bi_I32).add(ecParameter.getCurve().decodePoint(cK_bytes));
		
		ECDomainParameters parameters = new ECDomainParameters(ecParameter.getCurve(),ecPoint, order, ecParameter.getH(),ecParameter.getSeed());
		
		ECPublicKeyParameters ecPublicKey = new ECPublicKeyParameters(ecPoint, parameters);

		String pubKeyHexFormat = getHexPubKeyfromECkeys(ecPublicKey, true);
		String retval[] = new String[2];
		retval[0] = pubKeyHexFormat;
		retval[1] = I_hex.substring(64);
		return retval;
	}

	public static String get_pubkey_from_secret(byte[] secret) {

		BigInteger secretExponent = new BigInteger(Util.bytesToHex(secret), 16);
		
		X9ECParameters ecParameter = ECNamedCurveTable.getByName(EC_GEN_PARAM_SPEC);
		
		ECCurve curve = ecParameter.getCurve();
		
		ECPoint ecPoint = ecParameter.getG().multiply(secretExponent);
		ECDomainParameters parameters = new ECDomainParameters(curve,ecPoint, ecParameter.getN(), ecParameter.getH(),ecParameter.getSeed());
		// Calculate the Public Key
		ECPublicKeyParameters ecPublicKey = new ECPublicKeyParameters(ecPoint, parameters);

		String pubKeyHexFormat = getHexPubKeyfromECkeys(ecPublicKey, true);
		return pubKeyHexFormat;
	}

	public static String hmac_sha_512(String message, String key) {
		String result = null;

		try {
			byte[] byteKey = key.getBytes("UTF-8");
			Digest d = new SHA512Digest();
			Mac sha512_HMAC = new HMac(d);

			CipherParameters cp = new KeyParameter(byteKey);
			sha512_HMAC.init(cp);
			byte[] mByte = message.getBytes("UTF-8");
			sha512_HMAC.update(mByte, 0, mByte.length);
			byte[] out = new byte[1024];
			sha512_HMAC.doFinal(out,0);
			result = Util.bytesToHex(out);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  finally {
			// We're done!
		}

		return result;
	}

	public static byte[] hmac_sha_512_bytes(byte[] message, String key) {
		return hmac_sha_512_bytes(message, key.getBytes());
	}
	public static byte[] hmac_sha_512_bytes(byte[] message, byte[] key) {

		Digest d = new SHA512Digest();
		Mac HMAC = new HMac(d);
		CipherParameters cp = new KeyParameter(key);
		HMAC.init(cp);
		HMAC.update(message, 0, message.length);
		byte[] out = new byte[64];
		HMAC.doFinal(out,0);
		return out;
	}
	
	public static byte[] hmac_sha_512_bytes_from_hex(byte[] message, String hexkey) {
		byte[] byteKey = Util.hexStringToByteArray(hexkey);
		return hmac_sha_512_bytes(message,byteKey);
	}

	public static boolean curveContainsPoint(BigInteger x, BigInteger y) {

		// Implemented natively here rather than use a library.
		// This function should therefore be heavily code reviewed.

		// expression is: (y*y - ((x*x*x) + b)) % p == 0
		BigInteger expression;
		BigInteger ySquared = y.multiply(y);
		BigInteger xCubed = x.multiply(x).multiply(x);
		expression = (ySquared.subtract(xCubed.add(curve_b))).mod(curve_p);
		if (expression.compareTo(BigInteger.ZERO) == 0) {
			return true;
		} else {
			return false;
		}
	}

} // end class
