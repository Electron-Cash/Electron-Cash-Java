package electrol.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public class BitcoinMainnet {
	
	public static final String HEADERS_URL="bitcoincash.com";
	public static final boolean TESTNET = false;
	public static final int WIF_PREFIX = 0x80;
	public static final int ADDRTYPE_P2PKH = 0;
	public static final int ADDRTYPE_P2SH = 5;
	public static final String SEGWIT_HRP = "bc";
	public static final String GENESIS = "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f";
	public static final String HEADERS_PATH ="/files/blockchain_headers";
	public static final BigInteger BITCOIN_CASH_FORK_BLOCK_HEIGHT = new BigInteger("478559");
	public static final String BITCOIN_CASH_FORK_BLOCK_HASH = "000000000000000000651ef99cb9fcbe0dadde1d424bd9f15ff20136191a5eec";
	
	public static JSONObject getDefaultServers() {
		InputStream inputStream = BitcoinMainnet.class.getResourceAsStream("/servers.json");
		try {
			return Utils.getJsonObject(inputStream);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static JSONArray getCheckpoints() {
		InputStream inputStream = BitcoinMainnet.class.getResourceAsStream("/checkpoints.json");
		try {
			return Utils.getJsonArray(inputStream);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static final Hashtable getXPRV_HEADERS() {
		Hashtable XPRV_HEADERS = new Hashtable(5);
		XPRV_HEADERS.put("standard", new Integer(0x0488ade4));                   //xprv
		XPRV_HEADERS.put("p2wpkh-p2sh", new Integer(0x049d7878));                //yprv
		XPRV_HEADERS.put("p2wsh-p2sh", new Integer(0x0295b005));                 //Yprv
		XPRV_HEADERS.put("p2wpkh", new Integer(0x04b2430c));                     //zprv
		XPRV_HEADERS.put("p2wsh", new Integer(0x0488ade4));                      //Zprv
		return XPRV_HEADERS;
	}

	public static final Hashtable getXPUB_HEADERS() {
		Hashtable XPUB_HEADERS = new Hashtable(5);
		XPUB_HEADERS.put("standard", new Integer(0x0488b21e));                   //xpub
		XPUB_HEADERS.put("p2wpkh-p2sh", new Integer(0x049d7cb2));                //ypub
		XPUB_HEADERS.put("p2wsh-p2sh", new Integer(0x0295b43f));                 //Ypub
		XPUB_HEADERS.put("p2wpkh", new Integer(0x04b24746));                     //zpub
		XPUB_HEADERS.put("p2wsh", new Integer(0x02aa7ed3));                      //Zpub
		return XPUB_HEADERS;
	}

	public static final Hashtable getDefaultPorts() {
		Hashtable defaultPorts = new Hashtable();
		defaultPorts.put("t", "50001");
		defaultPorts.put("s", "50002");
		return defaultPorts;
	}
}
