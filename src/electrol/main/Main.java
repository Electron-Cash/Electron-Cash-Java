package electrol.main;
import org.bouncycastle.util.Arrays;

import electrol.util.Util;
  
public class Main {
	
	public String[] getKeys(String seed) {
		MasterKeys masterKeys = new MasterKeys();
		byte[] bip32root = masterKeys.get_seed_from_mnemonic(seed);
		byte[] root512 = Bitcoin.get_root512_from_bip32root(bip32root);
		byte[] kBytes = Arrays.copyOfRange(root512, 0, 32);
		byte[] cBytes = Arrays.copyOfRange(root512, 32, 64);
		String jonaldPubKey = Bitcoin.get_pubkey_from_secret(kBytes);
		String serializedXprv = Bitcoin.serializeXprv(cBytes, kBytes);
		byte[] cKbytes = Util.hexStringToByteArray(jonaldPubKey);
		String serializedXpub = Bitcoin.serializeXpub(cBytes, cKbytes);
		String final_xprv = Bitcoin.base_encode_58(serializedXprv);
		String final_xpub = Bitcoin.base_encode_58(serializedXpub);
		return new String[] {final_xprv, final_xpub};
		
	}	
} 
