package main;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.json.me.JSONObject;

import electrol.java.util.HashSet;
import electrol.java.util.Map;
import electrol.java.util.Set;

public class Transaction {

	private static final String EC_GEN_PARAM_SPEC = "secp256k1";
	private static final String KEY_PAIR_GEN_ALGORITHM = "ECDSA";
	private static final String SIGHASHFORKID = "41";

	static final X9ECParameters curve = SECNamedCurves.getByName("secp256k1");
	static final ECDomainParameters domain = new ECDomainParameters(curve.getCurve(), curve.getG(), curve.getN(),
			curve.getH(), curve.getSeed());

	private String raw;
	
	public Transaction(String raw)
	{
		this();
		this.raw = raw;
	}
	public Transaction() {
		
	}
	public Transaction(Map map) {
		this((String)map.get("hex"));
	}
	public String getRaw() {
		return raw;
	}
	public JSONObject deserialize() {
		return new JSONObject();
	}
	
	public static Transaction from_io(Set input, Set output) {
		Transaction transaction = new Transaction();
		transaction.setInputs(input);
		transaction.setOutputs(output);
		return transaction;
	}
	public static String push_data(String data) {

		int OP_PUSHDATA1 = 76;
		String OP_PUSHDATA1_s = "4C";
		int OP_PUSHDATA2 = 77;
		int OP_PUSHDATA4 = 78;

		int n = data.length() / 2;
		String n_string = Integer.toHexString(n);

		if (n < OP_PUSHDATA1) {
			return n_string + data;
		}

		if (n < 256) {
			return OP_PUSHDATA1_s + n_string + data;
		}

		// This shouldn't happen.
		return "";

	}

	public static byte[] signData(byte[] data, PrivateKey key) throws Exception {
		Signature signer = Signature.getInstance("SHA256withDSA");
		signer.initSign(key);
		signer.update(data);
		return (signer.sign());
	}



	public static String serializePreimage(int i, BigInteger localBlockHeight, int prevout_n, TxIn[] txins,
			TxOut[] txouts) {

		String preimage = "";
		String nVersion = "01000000";
		String nHashtype = "41000000";
		String outpoint = "";
		String scriptCode = "";
		String amount = "";
		String nSequence = "";
		String hashSequence = "";
		String hashPrevouts = "";
		int j = 0;

		// Inputs and Sequences
		nSequence = "FEFFFFFF";
		for (j = 0; j < txins.length; j++) {
			hashSequence += nSequence;
			hashPrevouts += serializeOutpoint(txins[j]);
		}

		hashSequence = Bitcoin.Hash(hashSequence);
		hashPrevouts = Bitcoin.Hash(hashPrevouts);

		// Serialize Output
		String hashOutputs = "";
		for (j = 0; j < txouts.length; j++) {
			hashOutputs += serializeOutput(txouts[j]);
		}

		hashOutputs = Bitcoin.Hash(hashOutputs);
		outpoint = serializeOutpoint(txins[i]);
		BigInteger satoshiAmount = txins[i].getAmount();
		String addr = txins[i].getAddr().getAddr();
		String preimage_script = P2PKH_script(decode(addr));
		int len = preimage_script.length();
		scriptCode = Util.var_int(len / 2) + preimage_script;
		amount = Util.reverseByteString(Util.specialIntToHex(satoshiAmount, 8));
		String nLocktime = Util.reverseByteString(Util.specialIntToHex(localBlockHeight, 4));
		preimage = nVersion + hashPrevouts + hashSequence + outpoint + scriptCode + amount + nSequence + hashOutputs
				+ nLocktime + nHashtype;

		return preimage;

	}

	public static String serializeOutpoint(TxIn txin) {

		String zprevout_hash = txin.getHash();
		int zprevout_n = txin.getN();
		zprevout_hash = Util.reverseByteString(zprevout_hash);
		BigInteger bi_prevout = BigInteger.valueOf(zprevout_n);

		String zeropad = Util.reverseByteString(Util.specialIntToHex(bi_prevout, 4));
		String retval = zprevout_hash + zeropad; 
		return retval;
	}

	public static String serializeInput(TxIn txin) {

		// dealing with prevout_hash and prevout_n
		BigInteger satoshiValue = txin.getAmount();

		String signature = txin.getSignature();
		String pubkey = txin.getAddr().getPubkey();
		int pubkey_len = pubkey.length() / 2;
		String varInt = Util.var_int(pubkey_len);
		String script = push_script(signature) + varInt + pubkey;

		String s = serializeOutpoint(txin);
		int length = script.length() / 2;
		varInt = Util.var_int(length);
		// normally used for lock time. here will be constant
		String sequence = "FEFFFFFF";
		String amount = Util.specialIntToHex(satoshiValue, 8);
		String retval = s + varInt + script + sequence; // DO NOT ADD VALUE - ONLY FOR OFFLINE SIGNING +amount;
		return retval;
	}

	public static int estimated_input_size(TxIn txin) {
		return serializeInput(txin).length() / 2;
	}
	
	public static String op_push(int i) {
		if (i < 76) {
			return Integer.toHexString(i);
		} else if (i < 255) {
			return "4c" + Integer.toHexString(i);
		} else if (i < 65535) {
			return "4d" + Util.padLeftHexString(Integer.toHexString(i), 4);
		} else {
			return "4e" + Util.padLeftHexString(Integer.toHexString(i), 8);
		}
	}

	public static String push_script(String x) {

		int len = x.length() / 2;
		return op_push(len) + x;

	}

	public static String serializeOutput(TxOut txout) {

		String s;

		String addr = txout.getAddr();
 
		BigInteger satoshiAmount = txout.getAmount(); 
		String satoshiAmountHex = satoshiAmount.toString(16);
		satoshiAmountHex = Util.padLeftHexString(satoshiAmountHex, 16);
		satoshiAmountHex = Util.reverseByteString(satoshiAmountHex);
		s = satoshiAmountHex;
		String script = P2PKH_script(decode(addr)); 
		int len1 = script.length() / 2; 
		String varInt = Util.var_int(len1);
		s = s + varInt + script;
		return s;
	}

	public static String serialize(TxIn[] txins, TxOut[] txouts, String nVersion, String nLocktime) {

		String retval = "";

		int i;
		String input_string = "";
		String output_string = "";

		for (i = 0; i < txins.length; i++) {

			input_string += serializeInput(txins[i]);
		}

		String input_len = Integer.toHexString(txins.length);
		input_len = Util.padLeftHexString(input_len, 2);
		input_string = input_len + input_string;

		for (i = 0; i < txouts.length; i++) {
			output_string += serializeOutput(txouts[i]);
		}

		String output_len = Integer.toHexString(txouts.length);
		output_len = Util.padLeftHexString(output_len, 2);
		output_string = output_len + output_string;

		retval = nVersion + input_string + output_string + nLocktime;

		return retval;
	}

	public static String P2PKH_script(String hash160) {

		String OP_DUP = "76";
		String OP_HASH160 = "A9";
		String OP_EQUALVERIFY = "88";
		String OP_CHECKSIG = "AC";

		String push_data = push_data(hash160);

		String retval = OP_DUP + OP_HASH160 + push_data + OP_EQUALVERIFY + OP_CHECKSIG;

		return retval;

	}

	public static String decode(String txt) {

		String b58chars = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

		BigInteger bi_58 = new BigInteger("58");
		BigInteger bi_256 = new BigInteger("256");
		String retval = "";

		BigInteger val1 = BigInteger.ZERO;
		BigInteger val2 = BigInteger.ZERO;
		BigInteger long_value = BigInteger.ZERO;

		BigInteger bi_charpos, cc;
		int value = 0, c, charpos;
		int i = 0;
		byte[] vBytes = txt.getBytes();

		for (int counter = 0; counter < vBytes.length; counter++) {
			c = vBytes[counter];

			int ccc = Util.unsignedToBytes(vBytes[counter]);
			cc = BigInteger.valueOf(ccc);
			charpos = b58chars.indexOf(ccc);
			bi_charpos = BigInteger.valueOf(charpos);
			long_value = long_value.multiply(bi_58).add(bi_charpos);

		}

		retval = long_value.toString(16);
		int len = retval.length();
		retval = retval.substring(0, len - 8);
		return retval;
	}

	public static PrivateKey getPrivateKeyObjectfromString(String mykey) {

		// Bouncy Castle used to provide Crypto Libraries
		Security.addProvider(new BouncyCastleProvider());
		PrivateKey privKey = null;

		byte[] data = Util.hexStringToByteArray(mykey);

		KeyFactory keyFactory = null;
		try {
			keyFactory = KeyFactory.getInstance(KEY_PAIR_GEN_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			System.out.println("NO SUCH ALGORITHM EXCEPTION");
			System.exit(0);
		}

		try {

			ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(EC_GEN_PARAM_SPEC);
			ECPrivateKeySpec ecPrivateKeySpec = new ECPrivateKeySpec(new BigInteger(1, data), spec);
			privKey = keyFactory.generatePrivate(ecPrivateKeySpec);

		} catch (InvalidKeySpecException e) {
			System.out.println("INVALID KEY SPEC EXCEPTION");
			System.exit(0);
		}

		return privKey;

	}

	public static String GetSignature(String privkey, byte[] myhash) {

		ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
		signer.init(true, new ECPrivateKeyParameters(new BigInteger(privkey, 16), domain));
		BigInteger[] signature = signer.generateSignature(myhash);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			DERSequenceGenerator seq = new DERSequenceGenerator(baos);
			seq.addObject(new ASN1Integer(signature[0]));
			seq.addObject(new ASN1Integer(toCanonicalS(signature[1])));
			seq.close();
			return Util.bytesToHex(baos.toByteArray());
		} catch (IOException e) {
		}
		return "";

	}

	private static BigInteger toCanonicalS(BigInteger s) {
		if (s.compareTo(curve.getN().shiftRight(1)) <= 0) {
			return s;
		} else {
			return curve.getN().subtract(s);
		}
	}

	static Set txout = new HashSet();
	static Set txin = new HashSet();
	//TxOut
	public static Set getOutputs() {

		return txout;
	}

	public void setOutputs(Set ztxout) {
		txout = ztxout;
	}
	
	public static Set getInputs() {

		return txin;
	}

	public void setInputs(Set ztxin) {
		txin = ztxin;
	}
	
	public static TxIn[] Sign(TxIn[] inputs, TxOut[] outputs) {

		int i;
		String sig = "";
		String privateKey = "";
		String pre_hash = "";
		String masterXPRV = Wallet.getXPRV();
		int addr_index = 0;
		boolean addr_change = false;
		BigInteger localBlockHeight = Wallet.getLocalBlockHeight();
		int prevout = 0;
		for (i = 0; i < inputs.length; i++) {

			// First regenerate private Key.
			addr_index = inputs[i].getAddr().getIndex();
			addr_change = inputs[i].getAddr().getChange();
			privateKey = Bitcoin.fetchPrivateKey(masterXPRV, addr_change, addr_index);
			prevout = inputs[i].getN();
			pre_hash = Bitcoin.Hash(serializePreimage(i, localBlockHeight, prevout, inputs, outputs));
			sig = GetSignature(privateKey, Util.hexStringToByteArray(pre_hash));
			sig = sig + SIGHASHFORKID;
			inputs[i].setSignature(sig);
		}

		return inputs;

	}

	public static String Generate()

	{
		// This is the main wrapper function for this class.

		// Get the data we need -- Inputs and Outputs
		Object[] in = getInputs().toArray();
		Object[] out = getOutputs().toArray();

		TxIn[] inputs = new TxIn[in.length];
		TxOut[] outputs = new TxOut[out.length];
		// Sign the inputs
		inputs = Sign(inputs, outputs);

		// Get the blockheight , locktime, version
		BigInteger localBlockHeight = Wallet.getLocalBlockHeight();
		String nLocktime = Util.reverseByteString(Util.specialIntToHex(localBlockHeight, 4));
		String nVersion = "01000000";

		// Serialize the TX and return
		String rawTx = "";
		rawTx = serialize(inputs, outputs, nVersion, nLocktime);
		return rawTx;

	} // end func
} // end class
