package main;

import org.bouncycastle.asn1.eac.ECDSAPublicKey;
import org.bouncycastle.util.Arrays;

import electrol.util.BigInteger;
import electrol.util.StringUtils;
  
public class Main {


	public static String generateRawBitcoinAddress(ECDSAPublicKey ecPublicKey) {
		BigInteger affineXCoord = ecPublicKey.getFirstCoefA();
		BigInteger affineYCoord = ecPublicKey.getFirstCoefA();
		return StringUtils.format(affineXCoord,64) + StringUtils.format(affineYCoord,64);
	}

	public static void main() {

		//Security.addProvider(new BouncyCastleProvider());
		//Security.getProviders();

		System.out.println("STARTING PROGRAmM...");

		String testSeed;
		Seed mySeed = new Seed();
		testSeed = mySeed.generateSeed();
		System.out.println("test seed "+testSeed);

		testSeed = "raw program because index dutch current minute leaf analyst conduct reject nephew";
		System.out.println("ALTERNATE SEED FOR DEBUGGING");
		System.out.println(testSeed);

		testSeed = "fix glare garden safe ill speed stool table oblige admit barrel acid";
		byte[] bip32root = MasterKeys.get_seed_from_mnemonic(testSeed);
		byte[] root512 = Bitcoin.get_root512_from_bip32root(bip32root);
		byte[] kBytes = Arrays.copyOfRange(root512, 0, 32);
		byte[] cBytes = Arrays.copyOfRange(root512, 32, 64);
		String jonaldPubKey = Bitcoin.get_pubkey_from_secret(kBytes);
		String serializedXprv = Bitcoin.serializeXprv(cBytes, kBytes);
		byte[] cKbytes = Util.hexStringToByteArray(jonaldPubKey);
		String serializedXpub = Bitcoin.serializeXpub(cBytes, cKbytes);
		String final_xprv = Bitcoin.base_encode_58(serializedXprv);
		String masterXPRV = final_xprv;
		System.out.println("(MASTER) final_xprv");
		System.out.println(final_xprv);
		String final_xpub = Bitcoin.base_encode_58(serializedXpub);
		System.out.println("final_xpub");
		System.out.println(final_xpub);
		
		String deserializedXprvPieces[] = Bitcoin.deserialize_xkey(final_xprv, true);
		String xprv_c = deserializedXprvPieces[4];
		String xprv_k = deserializedXprvPieces[5];

		System.out.println("deserializedXprvPieces c " + xprv_c);
		System.out.println("deserializedXprvPieces k " + xprv_k);
		
		String deserialized_xpub[] = Bitcoin.deserialize_xkey(final_xpub, false);

		System.out.println("---------");
		
		String my_c = deserialized_xpub[4];
		String my_cK = deserialized_xpub[5];
		System.out.println("my CK "+my_cK);
		System.out.println("my C "+my_c);
		
		System.out.println("______________________________________");

		System.out.println("______________CKD PRIV_________________");
		System.out.println("______________________________________");

		String catHat = Bitcoin.fetchPrivateKey(masterXPRV, true, 1);
		System.out.println("catHat is "+catHat);
		catHat = Bitcoin.fetchPrivateKey(masterXPRV, false, 2);
		System.out.println("catHat is "+catHat);
		catHat = Bitcoin.fetchPrivateKey(masterXPRV, true, 4);
		System.out.println("catHat is "+catHat);
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
		Runner runner1= new Runner();

		Runner runner2= new Runner();
		runner1.start();
		runner2.start();

	} // end main func

} // end class
