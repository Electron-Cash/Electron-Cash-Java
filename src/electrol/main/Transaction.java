package electrol.main;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.json.me.JSONException;

import electrol.java.util.HashSet;
import electrol.java.util.Set;
import electrol.main.BlockchainsUtil;
import electrol.util.BigInteger;
import electrol.util.Constants;
import electrol.util.Util;

public class Transaction {

	private Storage storage;
	
	private static final String SIGHASHFORKID = "41";
	private static final String NVERSION = "01000000";
	private static final String NSEQUENCE = "feffffff";
	private static final String NHASHTYPE = "41000000";
	private Set txout = new HashSet();
	private Set txin = new HashSet();

	private Transaction(Storage storage) {
		this.storage = storage;
	}

	public static Transaction from_io(Set input, Set output, Storage storage) {
		Transaction transaction = new Transaction(storage);
		transaction.setInputs(input);
		transaction.setOutputs(output);
		return transaction;
	}

	public static String push_data(String data) {

		int OP_PUSHDATA1 = 76;
		int OP_PUSHDATA2 = 77;
		int OP_PUSHDATA4 = 78;

		int n = data.length() / 2;
		String n_string = Integer.toHexString(n);

		if (n < OP_PUSHDATA1) {
			return n_string + data;
		}
		if (n < 256) {
			return "4c" + n_string + data;
		}
		return "";

	}

	public static String serializePreimage(int i, BigInteger localBlockHeight, int prevout_n, TxIn[] txins,
			TxOut[] txouts) {

		String hashSequence = "";
		String hashPrevouts = "";

		// Inputs and Sequences
		for (int j = 0; j < txins.length; j++) {
			hashSequence += NSEQUENCE;
			hashPrevouts += serializeOutpoint(txins[j]);
		}

		hashSequence = Bitcoin.Hash(hashSequence);
		hashPrevouts = Bitcoin.Hash(hashPrevouts);

		// Serialize Output
		String hashOutputs = "";
		for (int j = 0; j < txouts.length; j++) {
			hashOutputs += serializeOutput(txouts[j]);
		}

		hashOutputs = Bitcoin.Hash(hashOutputs);
		String outpoint = serializeOutpoint(txins[i]);
		BigInteger satoshiAmount = txins[i].getAmount();
		String addr = txins[i].getAddr().getAddr();
		String preimage_script = P2PKH_script(decode(addr));
		int len = preimage_script.length();
		String scriptCode = Util.var_int(len / 2) + preimage_script;
		String amount = BlockchainsUtil.int_to_hex(satoshiAmount, 8);
		String nLocktime = BlockchainsUtil.int_to_hex(localBlockHeight, 4);

		String preimage = NVERSION + hashPrevouts + hashSequence + outpoint + scriptCode + amount + NSEQUENCE
				+ hashOutputs + nLocktime + NHASHTYPE;

		return preimage;

	}

	public static String serializeOutpoint(TxIn txin) {

		String zprevout_hash = txin.getHash();
		zprevout_hash = Util.reverseByteString(zprevout_hash);
		String zeropad = BlockchainsUtil.int_to_hex(txin.getN(), 4);
		return zprevout_hash + zeropad;
	}

	public static String serializeInput(TxIn txin) {

		// dealing with prevout_hash and prevout_n
		BigInteger satoshiValue = txin.getAmount();

		String signature = txin.getSignature();
		String pubkey = txin.getAddr().getPubkey();
		int pubkey_len = pubkey.length() / 2;

		String script = push_script(signature) + Util.var_int(pubkey_len) + pubkey;
		String s = serializeOutpoint(txin);
		int length = script.length() / 2;
		// normally used for lock time. here will be constant
		String amount = Util.specialIntToHex(satoshiValue, 8);
		String retval = s + Util.var_int(length) + script + NSEQUENCE; // DO NOT ADD VALUE - ONLY FOR OFFLINE SIGNING
																		// +amount;
		return retval;
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

		String addr = txout.getAddr();
		BigInteger satoshiAmount = txout.getAmount();
		String satoshiAmountHex = BlockchainsUtil.int_to_hex(satoshiAmount, 8);
		String script;
		if (addr.length() > 35) {
			if (!addr.substring(0, 11).equals(Constants.MAINNET_PREFIX)) {
				addr = Constants.MAINNET_PREFIX+Constants.SEPARATOR_COLON + addr;
			}
			byte[] p2pkh = AddressUtil.decodeCashAddress(addr).getHash();
			script = Util.bytesToHex(p2pkh);
			script = P2PKH_script(script);

		} else {
			script = P2PKH_script(decode(addr));
		}
		int len1 = script.length() / 2;
		String varInt = Util.var_int(len1);
		String s = satoshiAmountHex + varInt + script;
		return s;
	}

	public static String serialize(TxIn[] txins, TxOut[] txouts, String nVersion, String nLocktime) {
		String input_string = "";
		String output_string = "";

		for (int i = 0; i < txins.length; i++) {
			input_string += serializeInput(txins[i]);
		}

		String input_len = Util.var_int(txins.length);
		input_string = input_len + input_string;

		for (int i = 0; i < txouts.length; i++) {
			output_string += serializeOutput(txouts[i]);
		}

		String output_len = Util.var_int(txouts.length);
		return nVersion + input_string + output_len + output_string + nLocktime;

	}

	public static String P2PKH_script(String hash160) {

		String OP_DUP = "76";
		String OP_HASH160 = "a9";
		String OP_EQUALVERIFY = "88";
		String OP_CHECKSIG = "ac";
		String push_data = push_data(hash160);
		String retval = OP_DUP + OP_HASH160 + push_data + OP_EQUALVERIFY + OP_CHECKSIG;
		return retval;

	}

	public static String decode(String txt) {

		String b58chars = Constants.BASE_58_CHARS;
		BigInteger bi_58 = new BigInteger("58");
		BigInteger long_value = BigInteger.ZERO;
		byte[] vBytes = txt.getBytes();

		for (int counter = 0; counter < vBytes.length; counter++) {
			int ccc = Util.unsignedToBytes(vBytes[counter]);
			int charpos = b58chars.indexOf(ccc);
			BigInteger bi_charpos = BigInteger.valueOf(charpos);
			long_value = long_value.multiply(bi_58).add(bi_charpos);
		}

		StringBuffer zerostring = new StringBuffer();
		for (int i = 1; i < txt.length(); i++) { // i starts at 1 not 0 to skip normal "1" at beginning.
			char ch = txt.charAt(i);
			if (ch != '1') {
				break;
			}
			zerostring.append("00");
		}

		String retval = long_value.toString(16);
		int len = retval.length();
		if ((len & 1) != 0) {
			retval = "0" + retval; 
		}
		retval = zerostring.append(retval).toString();
		len = retval.length();
		return retval.substring(0, len - 8);
	}

	public String GetSignature(String privkey, byte[] myhash) {
		X9ECParameters curve = SECNamedCurves.getByName("secp256k1");
		ECDomainParameters domain = new ECDomainParameters(curve.getCurve(), curve.getG(), curve.getN(), curve.getH(),
				curve.getSeed());
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

	private BigInteger toCanonicalS(BigInteger s) {
		X9ECParameters curve = SECNamedCurves.getByName("secp256k1");
		if (s.compareTo(curve.getN().shiftRight(1)) <= 0) {
			return s;
		} else {
			return curve.getN().subtract(s);
		}
	}

	public TxIn[] Sign(TxIn[] inputs, TxOut[] outputs) throws JSONException {
		Wallet wallet = new Wallet(storage);
		String sig = "";
		String privateKey = "";
		String pre_hash = "";
		String masterXPRV = wallet.getXPRV();
		int addr_index = 0;
		boolean addr_change = false;
		BigInteger localBlockHeight = Wallet.getLocalBlockHeight();

		int prevout = 0;
		for (int i = 0; i < inputs.length; i++) {

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

	public void addInputs(Set coins) {
		txin.addAll(coins);
	}

	public void addOutputs(Set outputs) {
		txout.addAll(outputs);
	}

	public String Generate() throws JSONException {
		Object[] in = getInputs().toArray();
		Object[] out = getOutputs().toArray();

		TxIn[] inputs = new TxIn[in.length];
		TxOut[] outputs = new TxOut[out.length];

		for (int i = 0; i < in.length; i++) {
			inputs[i] = (TxIn) in[i];
		}
		for (int i = 0; i < out.length; i++) {
			outputs[i] = (TxOut) out[i];
		}
		inputs = Sign(inputs, outputs);

		// Get the blockheight , locktime, version
		BigInteger localBlockHeight = Wallet.getLocalBlockHeight();
		String nLocktime = BlockchainsUtil.int_to_hex(localBlockHeight, 4);

		// Serialize the TX and return
		return serialize(inputs, outputs, NVERSION, nLocktime);

	}

	public Set getOutputs() {

		return txout;
	}

	public void setOutputs(Set txout) {
		this.txout = txout;
	}

	public Set getInputs() {

		return txin;
	}

	public void setInputs(Set txin) {
		this.txin = txin;
	}
}
