package org.electroncash.util;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.util.encoders.Hex;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public class BlockchainsUtil
{
	private static BigInteger MAX_TARGET = new BigInteger("26959535291011309493156476344723991336010898738574164086137773096960");
	public static BigInteger ONE_LEFT_SHIFT_256 = new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639936");
	private static BigInteger MASK64 = new BigInteger("18446744073709551615");
	private static Map blockchains;

	public BlockchainsUtil() {}

	public static BigInteger bits_to_work(int bits) { return ONE_LEFT_SHIFT_256.divide(bits_to_target(bits).add(BigInteger.ONE)); }

	public static Map read_blockchain() throws ClassCastException, IllegalArgumentException, NullPointerException, RuntimeException, IOException
	{
		blockchains = new HashMap();
		blockchains.put(new Integer(0), new Blockchain(0, null));

		Files.getOrCreateDir("forks");
		Vector filterDirList = Files.listFilterDir("forks/", "fork_");
		for (int i = 0; i < filterDirList.size(); i++) {
			String[] arr = StringUtils.split(filterDirList.elementAt(i).toString(), "_");
			Integer checkpoint = Integer.valueOf(arr[2]);
			Integer parent_id = Integer.valueOf(arr[1]);
			blockchains.put(checkpoint, new Blockchain(checkpoint.intValue(), parent_id));
		}
		return blockchains;
	}

	public static BigInteger bits_to_target(int bits)
	{
		if (bits == 0)
			return BigInteger.ZERO;
		byte size = (byte)(bits >> 24);
		BigInteger word = BigInteger.valueOf(bits & 0xFFFFFF);
		if (size <= 3) {
			return word.shiftRight(8 * (3 - size));
		}
		return word.shiftLeft(8 * (size - 3));
	}

	public static int target_to_bits(BigInteger target) {
		if (target.equals(BigInteger.ZERO))
			return 0;
		target = target.min(MAX_TARGET);
		int size = (target.bitLength() + 7) / 8;
		int compact; 
		if (size <= 3) {
			compact = target.and(MASK64).shiftLeft(8 * (3 - size)).intValue();
		} else {
			compact = target.shiftRight(8 * (size - 3)).and(MASK64).intValue();
		}
		if ((compact & 0x800000) > 0) {
			compact >>= 8;
			size++;
		}
		return compact | size << 24;
	}

	public static String serializeHeader(JSONObject res) throws NumberFormatException, JSONException {
		return int_to_hex(res.optInt("version"), 4) + revHex(res.get("prev_block_hash").toString()) + revHex(res.get("merkle_root").toString()) + int_to_hex(res.optInt("timestamp"), 4) + long_to_hex(res.optInt("bits"), 4) + long_to_hex(res.optLong("nonce"), 4);
	}

	public static String hash_header(JSONObject header) throws JSONException {
		if (header == null) {
			return "0000000000000000000000000000000000000000000000000000000000000000";
		}
		if (header.getString("prev_block_hash") == null)
			header.put("prev_block_hash", "0000000000000000000000000000000000000000000000000000000000000000");
		String s = serializeHeader(header);
		return hashEncode(Hash(bfh(s)));
	}

	public static JSONObject deserializeHeader(byte[] h, int height) throws JSONException { JSONObject map = new JSONObject();
	map.put("version", hexToInt(Arrays.slice(h, 0, 4)));
	map.put("prev_block_hash", hashEncode(Arrays.slice(h, 4, 36)));
	map.put("merkle_root", hashEncode(Arrays.slice(h, 36, 68)));
	map.put("timestamp", hexToInt(Arrays.slice(h, 68, 72)));
	map.put("bits", hexToIntNonce(Arrays.slice(h, 72, 76)));
	map.put("nonce", hexToIntNonce(Arrays.slice(h, 76, 80)));
	map.put("block_height", new Integer(height));
	return map;
	}

	public static long hexToInt(byte[] hex) { return Long.parseLong(bh2u(Arrays.reverse(hex)), 16); }

	public static long hexToIntNonce(byte[] hex)
	{
		return Long.parseLong(bh2u(Arrays.reverse(hex)), 16);
	}

	public static String hashEncode(byte[] b) {
		return bh2u(Arrays.reverse(b));
	}

	public static byte[] hashDecode(String hex) { return Arrays.reverse(bfh(hex)); }

	public static String bh2u(byte[] b) {
		return Hex.toHexString(b);
	}

	public static byte[] bfh(String hex) { return Hex.decode(hex); }

	public static String int_to_hex(int i, int length) {
		String dec = Integer.toHexString(i);
		return revHex(getZero(2 * length - dec.length()) + dec);
	}

	public static String long_to_hex(long i, int length) { String dec = Long.toString(i, 16);
	return revHex(getZero(2 * length - dec.length()) + dec);
	}

	public static String revHex(String hex) { return bh2u(Arrays.reverse(bfh(hex))); }

	public static byte[] Hash(byte[] xBytes)
	{
		byte[] output = HashSingle(xBytes);
		return HashSingle(output);
	}

	public static byte[] HashSingle(byte[] xBytes) {
		SHA256Digest digest = new SHA256Digest();
		digest.update(xBytes, 0, xBytes.length);
		byte[] output = new byte[digest.getDigestSize()];
		digest.doFinal(output, 0);
		return output;
	}

	public static Blockchain check_header(JSONObject header) throws JSONException, IOException {
		if (header.length() == 0) {
			return null;
		}
		Iterator it = blockchains.values().iterator();
		while (it.hasNext()) {
			Blockchain blockchain = (Blockchain)it.next();
			if (blockchain.check_header(header)) {
				return blockchain;
			}
		}
		return null;
	}

	public static Blockchain can_connect(JSONObject header) throws JSONException, IOException { if (header.length() == 0) {
		return null;
	}
	Iterator it = blockchains.values().iterator();
	while (it.hasNext()) {
		Blockchain blockchain = (Blockchain)it.next();
		if (blockchain.can_connect(header, true)) {
			return blockchain;
		}
	}
	return null;
	}

	public static byte[] root_from_proof(byte[] hash, byte[][] branch, int index) { for (int i = 0; i < branch.length; i++) {
		if ((index & 0x1) > 0) {
			hash = Hash(org.bouncycastle.util.Arrays.concatenate(branch[i], hash));
		}
		else {
			hash = Hash(org.bouncycastle.util.Arrays.concatenate(hash, branch[i]));
		}
		index >>= 1;
	}
	if (index > 0) {
		System.out.println("index out of range for branch");
	}
	return hash;
	}

	public static void verify_proven_chunk(int chunk_base_height, byte[] chunk_data) throws Exception { HeaderChunk chunk = new HeaderChunk(chunk_base_height, chunk_data);
	short header_count = (short)(chunk_data.length / 80);
	String prev_header_hash = null;
	for (short i = 0; i < header_count; i = (short)(i + 1)) {
		byte[] raw_header = chunk.get_header_at_index(i);
		JSONObject header = deserializeHeader(raw_header, chunk_base_height + i);
		String this_header_hash = hash_header(header);
		if ((i > 0) && (prev_header_hash != null) && 
				(!prev_header_hash.equals(header.getString("prev_block_hash")))) {
			throw new Exception("prev hash mismatch: " + prev_header_hash + " vs " + header.get("prev_block_hash"));
		}

		prev_header_hash = this_header_hash;
	}
	}

	private static String getZero(int i)
	{
		switch (i) {
		case 0:  return "";
		case 1:  return "0";
		case 2:  return "00";
		case 3:  return "000";
		case 4:  return "0000";
		case 5:  return "00000";
		case 6:  return "000000";
		case 7:  return "0000000";
		case 8:  return "00000000";
		case 9:  return "000000000";
		case 10:  return "0000000000";
		case 11:  return "00000000000";
		case 12:  return "000000000000";
		case 13:  return "0000000000000";
		case 14:  return "00000000000000";
		case 15:  return "000000000000000";
		case 16:  return "0000000000000000"; }
		return "";
	}
}
