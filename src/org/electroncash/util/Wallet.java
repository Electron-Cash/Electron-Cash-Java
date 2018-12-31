package org.electroncash.util;

import java.io.IOException;
import java.util.Enumeration;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;


public class Wallet
{
  private Storage storage;
  private static int localBlockHeight;
  private boolean up_to_date = false;
  private Map unverified_tx;
  private JSONObject verified_tx;
  private Network network;
  private Verifier verifier;
  private JSONArray receiving_addresses;
  private JSONArray change_addresses;
  private JSONObject history;
  private boolean use_change;
  private boolean multiple_change;
  private JSONObject labels;
  private JSONArray frozen_addresses;
  private JSONArray frozen_coins;
  private String tx_fees = "";
  private Map txi = new HashMap();
  private Map txo = new HashMap();
  private JSONObject pruned_txo;
  private JSONObject transactions = new JSONObject();
  
  public Wallet(Storage storage) throws JSONException {
    this.storage = storage;
    unverified_tx = new HashMap();
    use_change = storage.get("use_change", true);
    verified_tx = storage.get("verified_tx3", new JSONObject());
    history = storage.get("addr_history", new JSONObject());
    multiple_change = storage.get("multiple_change", false);
    labels = storage.get("labels", new JSONObject());
    frozen_addresses = storage.get("frozen_addresses", new JSONArray());
    frozen_coins = storage.get("frozen_coins", new JSONArray());
    loadAddresses();
    loadTransactions();
  }
  
  private void loadTransactions() throws JSONException {
    JSONObject txi_object = storage.get("txi", new JSONObject());
    Enumeration txi_keys = txi_object.keys();
    while (txi_keys.hasMoreElements()) {
      String key = (String)txi_keys.nextElement();
      JSONObject value = txi_object.getJSONObject(key);
      txi.put(key, value);
    }
    JSONObject txo_object = storage.get("txo", new JSONObject());
    Enumeration txo_keys = txo_object.keys();
    while (txo_keys.hasMoreElements()) {
      String key = (String)txo_keys.nextElement();
      JSONObject value = txo_object.getJSONObject(key);
      txo.put(key, value);
    }
    pruned_txo = storage.get("pruned_txo", new JSONObject());
    JSONObject tx_list = storage.get("transactions", new JSONObject());
    Enumeration tx = tx_list.keys();
    while (tx.hasMoreElements()) {
      String key = (String)tx.nextElement();
      String value = tx_list.getString(key);
      
      if ((txi.containsKey(key)) && (txo.containsKey(key))) {
        transactions.put(key, value);
      }
    }
  }
  
  private void save_transactions(boolean write) throws JSONException, DataLengthException, IllegalStateException, InvalidCipherTextException, IOException
  {
    storage.put("transactions", transactions);
    Iterator txiKeyIterator = this.txi.keySet().iterator();
    JSONObject txi = new JSONObject();
    while (txiKeyIterator.hasNext()) {
      String key = (String)txiKeyIterator.next();
      txi.put(key, (JSONObject)txi.get(key));
    }
    storage.put("txi", txi);
    Iterator txoKeyIterator = this.txo.keySet().iterator();
    JSONObject txo = new JSONObject();
    while (txoKeyIterator.hasNext()) {
      String key = (String)txoKeyIterator.next();
      txo.put(key, (JSONObject)txo.get(key));
    }
    storage.put("txo", txo);
    storage.put("tx_fees", tx_fees);
    storage.put("pruned_txo", pruned_txo);
    storage.put("addr_history", history);
    if (write) storage.write();
  }
  
  private void save_verified_tx(boolean write) throws JSONException, DataLengthException, IllegalStateException, InvalidCipherTextException, IOException
  {
    storage.put("verified_tx3", verified_tx);
    if (write) storage.write();
  }
  
  private void loadAddresses() throws JSONException {
    JSONObject addresses = storage.get("addresses", new JSONObject());
    if (addresses.length() != 0) {
      receiving_addresses = addresses.getJSONArray("receiving");
      change_addresses = addresses.getJSONArray("change");
    }
  }
  
  private void set_up_to_date(boolean up_to_date) throws DataLengthException, IllegalStateException, InvalidCipherTextException, JSONException, IOException
  {
    this.up_to_date = up_to_date;
    if (up_to_date) {
      save_transactions(true);
      if ((verifier != null) && (verifier.is_up_to_date())) {
        save_verified_tx(true);
      }
    }
  }
  

  private boolean is_up_to_date() { return up_to_date; }
  
  public String getXPRV() throws JSONException {
    JSONObject keystore = storage.get("keystore", new JSONObject());
    return keystore.getString("xprv");
  }
  
  public static int getLocalBlockHeight()
  {
    return localBlockHeight;
  }
  
  public static void setLocalBlockHeight(int blockHeight) {
    localBlockHeight = blockHeight;
  }
}
