package electrol.util;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.util.encoders.Hex;
import org.json.me.JSONObject;

import electrol.Blockchain;
import electrol.java.util.Arrays;
import electrol.java.util.HashMap;
import electrol.java.util.Map;

public class BlockchainsUtil {
	
	public static final BigInteger MAX_BITS = new BigInteger("1d00ffff");
	
	public static BigInteger bits_to_work(BigInteger bits) {
	    return new BigInteger("1").shiftLeft(256).divide((bits_to_target(bits).add(new BigInteger("1"))));
	}
	
	public static BigInteger bits_to_target(BigInteger bits) {
	    if(bits.equals(new BigInteger("0")))
	        return new BigInteger("0");
	    BigInteger size = bits.shiftRight(24);
	    //assert size.intValue() <= 0x1d;
	    BigInteger word = bits.and(new BigInteger("00ffffff",16));
	    //assert 0x8000 <= word.intValue() && word.longValue() <= 0x7fffff;
	    if(size.intValue() <= 3)
	        return word.shiftRight(8 * (3 - size.intValue()));
	    else
	        return word.shiftLeft(8 * (size.intValue() - 3));
	}
	
	public static BigInteger target_to_bits(BigInteger target) {
		if(target.equals(new BigInteger("0")))
	        return new BigInteger("0");
	    
	    target = target.min(bits_to_target(MAX_BITS));
	    int size = (target.bitCount() + 7) / 8;
	    BigInteger mask64 = new BigInteger("ffffffffffffffff",16);
	    BigInteger compact;
	    if(size <= 3) {
	        compact = target.and(mask64).shiftLeft(8 * (3 - size));
	    }
	    else {
	        compact = target.shiftRight(8 * (size - 3)).and(mask64);
	    }
	    if(!compact.and(new BigInteger("00800000",16)).equals(new BigInteger("0"))) {
	        compact = compact.shiftRight(8);
	        size += 1;
	    }
	    //assert compact.equals(compact.and(new BigInteger("007fffff",16)));
	    //assert size < 256;
	    return compact.or(new BigInteger(String.valueOf(size))).shiftLeft(24);
	}
	
	public static String serializeHeader(Map res) {
		return int_to_hex((BigInteger)res.get("version"), 4)
				+ revHex(res.get("prev_block_hash").toString())
				+ revHex(res.get("merkle_root").toString())
				+ int_to_hex((BigInteger)res.get("timestamp"), 4)
				+ int_to_hex((BigInteger)res.get("bits"), 4)
				+ int_to_hex((BigInteger)res.get("nonce"), 4);
	}
	
	public static String hash_header(Map header) {
		if(header == null) {
			return CharacterMultiply.multiply("0", 64);
		}
		if(header.get("prev_block_hash") == null)
			header.put("prev_block_hash", CharacterMultiply.multiply("00", 32));
		return hashEncode(Hash(bfh(serializeHeader(header))));
	}
	public static Map deserializeHeader(byte[] h, int height) {
		Map map = new HashMap(7);
		map.put("version", hexToInt(Arrays.slice(h, 0, 4)));
		map.put("prev_block_hash", hashEncode(Arrays.slice(h, 4, 36)));
		map.put("merkle_root", hashEncode(Arrays.slice(h, 36, 68)));
		map.put("timestamp", hexToInt(Arrays.slice(h, 68, 72)));
		map.put("bits", hexToInt(Arrays.slice(h, 72, 76)));
		map.put("nonce", hexToInt(Arrays.slice(h, 76, 80)));
		map.put("block_height", new Integer(height));
		return map;
	}
	public static BigInteger hexToInt(byte[] hex) {
		return new BigInteger(bh2u(Arrays.reverse(hex)),16);
	}

	
	public static String hashEncode(byte[] b) {
		return bh2u(Arrays.reverse(b));
	}
	public static byte[] hashDecode(String hex) {
		return bfh(StringUtils.reverse(hex));
	}
	public static String bh2u(byte[] b) {
		return Hex.toHexString(b);
	}
	public static byte[] bfh(String hex) {
		return Hex.decode(hex);
	}
	public static String int_to_hex(BigInteger hex,int length) {
		String dec = hex.toString(16);
		dec = CharacterMultiply.multiply("0",(2 * length - dec.length())) +dec;
		return revHex(dec);
	}
	public static String revHex(String hex) {
		return bh2u(Arrays.reverse(bfh(hex)));
	}
	
	public static byte[] Hash(byte[] xBytes) {
		// PERFORM DOUBLE SHA256 HASH
		SHA256Digest digest = new SHA256Digest();
		digest.update(xBytes, 0, xBytes.length);
		byte[] output = new byte[digest.getDigestSize()];
		digest.doFinal(output, 0);
		digest.update(output, 0, output.length);
		output = new byte[digest.getDigestSize()];
		digest.doFinal(output, 0);
		return output;
	}

	public static Blockchain check_header(Blockchain block,JSONObject header) {
		if(header.length() == 0) {
			
		}
		// TODO Auto-generated method stub
		return null;
	}
}
