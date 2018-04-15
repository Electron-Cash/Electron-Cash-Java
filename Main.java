package main;

import java.io.*;
import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
 
import java.security.KeyFactory; 
import java.security.NoSuchAlgorithmException; 
import java.security.PrivateKey; 
import java.security.PublicKey; 
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import org.bouncycastle.jce.provider.BouncyCastleProvider; 
  
public class Main {

	private static final String EC_GEN_PARAM_SPEC = "secp256k1";
	private static final String KEY_PAIR_GEN_ALGORITHM = "ECDSA";

	public static String generateRawBitcoinAddress(ECPublicKey ecPublicKey) {
		ECPoint q = ecPublicKey.getQ();
		BigInteger affineXCoord = q.getAffineXCoord().toBigInteger();
		BigInteger affineYCoord = q.getAffineYCoord().toBigInteger();
		return String.format("%064x", affineXCoord) + String.format("%064x", affineYCoord);
	}

	public static void main(String[] args) {

		Security.addProvider(new BouncyCastleProvider());
		Security.getProviders();

		System.out.println("STARTING PROGRAmM...");
		KeyFactory keyFactory = null;
		try {
			keyFactory = KeyFactory.getInstance("ECDSA");
		} catch (NoSuchAlgorithmException e) {

			System.out.println("NO SUCH ALGORITHM EXCEPTION");
			System.exit(0);

		}

		String testSeed;
		Seed mySeed = new Seed();
		testSeed = mySeed.generateSeed();
		System.out.println(testSeed);

		testSeed = "raw program because index dutch current minute leaf analyst conduct reject nephew";
		System.out.println("ALTERNATE SEED FOR DEBUGGING");
		System.out.println(testSeed);

		testSeed = "fix glare garden safe ill speed stool table oblige admit barrel acid";

		byte[] bip32root = MasterKeys.get_seed_from_mnemonic(testSeed);
		byte[] root512 = Bitcoin.get_root512_from_bip32root(bip32root);
		// System.out.println("root 512 is "+Bitcoin.bytesToHex(root512));
		byte[] kBytes = Arrays.copyOfRange(root512, 0, 32);
		byte[] cBytes = Arrays.copyOfRange(root512, 32, 64);
		String jonaldPubKey = Bitcoin.get_pubkey_from_secret(kBytes);
		// System.out.println("pubkey");
		// System.out.println(jonaldPubKey);
		String serializedXprv = Bitcoin.serializeXprv(cBytes, kBytes);
		byte[] cKbytes = Util.hexStringToByteArray(jonaldPubKey);
		String serializedXpub = Bitcoin.serializeXpub(cBytes, cKbytes);

		String final_xprv = Bitcoin.base_encode_58(serializedXprv);
		System.out.println("(MASTER) final_xprv");
		System.out.println(final_xprv);

		String masterXPRV = final_xprv;

		String final_xpub = Bitcoin.base_encode_58(serializedXpub);
		System.out.println("final_xpub");
		System.out.println(final_xpub);

		// Clean up comments and variable names before proceeding

		String deserializedXprvPieces[] = Bitcoin.deserialize_xkey(final_xprv, true);
		String xprv_c = deserializedXprvPieces[4];
		String xprv_k = deserializedXprvPieces[5];

		System.out.println("deserializedXprvPieces c " + xprv_c);

		System.out.println("deserializedXprvPieces k " + xprv_k);

		String deserialized_xpub[] = Bitcoin.deserialize_xkey(final_xpub, false);

		System.out.println("---------");

		String my_c = deserialized_xpub[4];
		String my_cK = deserialized_xpub[5];

		// System.out.println("my CK "+my_cK);

		// System.out.println("AAA");
		// System.out.println("my c "+my_c);

		System.out.println("______________________________________");

		System.out.println("______________CKD PRIV_________________");
		System.out.println("______________________________________");

		String catHat = Bitcoin.fetchPrivateKey(masterXPRV, true, 1);
		// System.out.println("catHat is "+catHat);

		catHat = Bitcoin.fetchPrivateKey(masterXPRV, false, 2);
		// System.out.println("catHat is "+catHat);

		catHat = Bitcoin.fetchPrivateKey(masterXPRV, true, 4);
		// System.out.println("catHat is "+catHat);

		String testPrivKey = catHat;

		System.out.println("______________________________________");

		System.out.println("______________CKD PUB_________________");
		System.out.println("______________________________________");

		// FIRST DERIVED RECEIVING ADDR KEY -- JUST BASE
		String[] newKeys = Bitcoin._CKD_pub(my_cK, my_c, "00000000");
		String my_cK2 = newKeys[0];
		String my_c2 = newKeys[1];
		System.out.println("test11 is " + my_cK2 + " " + my_c2);

		// FIRST DERIVED CHANGE ADDR KEY -- JUST BASE
		String[] changeKeys = Bitcoin._CKD_pub(my_cK, my_c, "00000001");
		String my_cK2_change = changeKeys[0];
		String my_c2_change = changeKeys[1];
		System.out.println("test11 is " + my_cK2 + " " + my_c2);

		// DERIVED PUBKEY (FIRST RECEIVING ADDRESS)
		newKeys = Bitcoin._CKD_pub(my_cK2, my_c2, "00000000");
		String my_cK3 = newKeys[0];
		String my_c3 = newKeys[1];
		System.out.println("test12 is " + my_cK3 + " " + my_c3);

		// SECOND RECEIVING ADDRESS
		newKeys = Bitcoin._CKD_pub(my_cK2, my_c2, "00000001");
		my_cK3 = newKeys[0];
		my_c3 = newKeys[1];
		System.out.println("test13 is " + my_cK3 + " " + my_c3);

		// THIRD RECEIVING ADDRESS
		newKeys = Bitcoin._CKD_pub(my_cK2, my_c2, "00000002");
		my_cK3 = newKeys[0];
		my_c3 = newKeys[1];
		System.out.println("test14 is " + my_cK3 + " " + my_c3);

		// DERIVED PUBKEY (FIRST CHANGE ADDRESS)
		newKeys = Bitcoin._CKD_pub(my_cK2_change, my_c2_change, "00000000");
		my_cK3 = newKeys[0];
		my_c3 = newKeys[1];
		System.out.println("test15 is " + my_cK3 + " " + my_c3);

		// SECOND CHANGE ADDRESS
		newKeys = Bitcoin._CKD_pub(my_cK2_change, my_c2_change, "00000001");
		my_cK3 = newKeys[0];
		my_c3 = newKeys[1];
		System.out.println("test16 is " + my_cK3 + " " + my_c3);

		// THIRD CHANGE ADDRESS
		newKeys = Bitcoin._CKD_pub(my_cK2_change, my_c2_change, "00000002");
		my_cK3 = newKeys[0];
		my_c3 = newKeys[1];
		System.out.println("test17 is " + my_cK3 + " " + my_c3);

		String pubKeyHash = Bitcoin.ripeHash(my_cK3);
		System.out.println("pubkey hash is "+pubKeyHash);

		byte[] pubKeyHashBytes = Util.hexStringToByteArray(pubKeyHash);

		String cash_address = CashAddr.toCashAddress(BitcoinCashAddressType.P2PKH, pubKeyHashBytes);
		System.out.println("third change address is "+cash_address);

		System.out.println("______________________________________");

		System.out.println("______________TRANSACTIONS_________________");
		System.out.println("______________________________________");

		String rawTx=Transaction.Generate();
		System.out.println("rawTx is"+rawTx);
		 
		// MULTITHREAD
		// Runner runner1= new Runner();

		// Runner runner2= new Runner();
		// runner1.start();
		// runner2.start();

	} // end main func

} // end class
