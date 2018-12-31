package org.electroncash.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import org.json.me.JSONException;

public class BitcoinMainnet
{
  public static final boolean TESTNET = false;
  public static final int WIF_PREFIX = 128;
  public static final int ADDRTYPE_P2PKH = 0;
  public static final int ADDRTYPE_P2SH = 5;
  public static final String SEGWIT_HRP = "bc";
  public static final String GENESIS = "000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f";
  public static final int BITCOIN_CASH_FORK_BLOCK_HEIGHT = 478559;
  public static final String BITCOIN_CASH_FORK_BLOCK_HASH = "000000000000000000651ef99cb9fcbe0dadde1d424bd9f15ff20136191a5eec";
  
  public BitcoinMainnet() {}
  
  public static org.json.me.JSONObject getDefaultServers()
  {
    InputStream inputStream = BitcoinMainnet.class.getResourceAsStream("/servers.json");
    try {
      return Util.getJsonObject(inputStream);
    }
    catch (JSONException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
  
  public static org.json.me.JSONArray getCheckpoints() {
    InputStream inputStream = BitcoinMainnet.class.getResourceAsStream("/checkpoints.json");
    try {
      return Util.getJsonArray(inputStream);
    }
    catch (JSONException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
  
  public static final Hashtable getXPRV_HEADERS() {
    Hashtable XPRV_HEADERS = new Hashtable(5);
    XPRV_HEADERS.put("standard", new Integer(76066276));
    XPRV_HEADERS.put("p2wpkh-p2sh", new Integer(77428856));
    XPRV_HEADERS.put("p2wsh-p2sh", new Integer(43364357));
    XPRV_HEADERS.put("p2wpkh", new Integer(78791436));
    XPRV_HEADERS.put("p2wsh", new Integer(76066276));
    return XPRV_HEADERS;
  }
  
  public static final Hashtable getXPUB_HEADERS() {
    Hashtable XPUB_HEADERS = new Hashtable(5);
    XPUB_HEADERS.put("standard", new Integer(76067358));
    XPUB_HEADERS.put("p2wpkh-p2sh", new Integer(77429938));
    XPUB_HEADERS.put("p2wsh-p2sh", new Integer(43365439));
    XPUB_HEADERS.put("p2wpkh", new Integer(78792518));
    XPUB_HEADERS.put("p2wsh", new Integer(44728019));
    return XPUB_HEADERS;
  }
  
  public static final Hashtable getDefaultPorts() {
    Hashtable defaultPorts = new Hashtable();
    defaultPorts.put("t", "50001");
    defaultPorts.put("s", "50002");
    return defaultPorts;
  }
}
