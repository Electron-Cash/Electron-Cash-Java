package org.electroncash.util;

import java.util.Enumeration;
import org.bouncycastle.crypto.DataLengthException;
import org.electroncash.nio.ByteBuffer;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public class ReceivingAddress
{
  private static final int COMMAND_LEN = 12;
  public static final int MAX_SIZE = 33554432;
  
  public ReceivingAddress() {}
  
  public String get_address(Storage storage, boolean type) throws JSONException, DataLengthException, IllegalStateException, org.bouncycastle.crypto.InvalidCipherTextException, java.io.IOException
  {
    String addressType;
    if (type) {
      addressType = "change";
    }
    else {
      addressType = "receiving";
    }
    JSONObject object = storage.get("addresses", new JSONObject());
    

    JSONArray addrJson = object.getJSONArray(addressType);
    JSONObject txi = storage.get("txi", new JSONObject());
    Set txiSet = getTx(txi);
    JSONObject txo = storage.get("txo", new JSONObject());
    Set txoSet = getTx(txo);
    AddressUtil addressUtil = new AddressUtil(storage.get("keystore", new JSONObject()).getString("xpub"));
    for (int i = 0; i < addrJson.length(); i++) {
      String addrString = addrJson.getString(i);
      if ((!txiSet.contains(addrString)) && (!txoSet.contains(addrString)))
      {

        return AddressUtil.generateCashAddrFromLegacyAddr(addrJson.getString(i));
      }
    }
    int n = addrJson.length();
    String legacyAddress = addressUtil.generateOneLegacyAddress(n, type ? 1 : 0);
    addrJson.put(legacyAddress);
    object.put(addressType, addrJson);
    storage.put("addresses", object);
    storage.write();
    return get_address(storage, type);
  }
  
  public void deserializePayload(BitcoinPacketHeader header, ByteBuffer in) throws Exception
  {
	  
	  byte[] payloadBytes = new byte[header.size];
      in.get(payloadBytes, 0, header.size);

      byte[] hash;
      hash = Bitcoin.doubleHash(payloadBytes);
      if (header.checksum[0] != hash[0] || header.checksum[1] != hash[1] ||
              header.checksum[2] != hash[2] || header.checksum[3] != hash[3]) {
          throw new Exception("Checksum failed to verify, actual " +Util.bytesToHex(hash));
      }
  }
  

  public static class BitcoinPacketHeader
  {
    public static final int HEADER_LENGTH = 20;
    public final byte[] header;
    public final String command;
    public final int size;
    public final byte[] checksum;
    
    public BitcoinPacketHeader(ByteBuffer in)
      throws Exception
    {
      header = new byte[20];
      in.get(header, 0, header.length);
      
      int cursor = 0;
      
      while ((header[cursor] != 0) && (cursor < 12)) cursor++;
      byte[] commandBytes = new byte[cursor];
      System.arraycopy(header, 0, commandBytes, 0, cursor);
      command = new String(commandBytes, "ascii");
      cursor = 12;
      
      size = ((int)ReceivingAddress.readUint32(header, cursor));
      cursor += 4;
      
      if ((size > 33554432) || (size < 0)) {
        throw new Exception("Message size too large: " + size);
      }
      checksum = new byte[4];
      System.arraycopy(header, cursor, checksum, 0, 4);
      cursor += 4;
    }
  }
  
  public Set getTx(JSONObject tx) throws JSONException { Enumeration e = tx.keys();
    Set txSet = new HashSet();
    while (e.hasMoreElements()) {
      String txHash = (String)e.nextElement();
      JSONObject txAddresses = tx.getJSONObject(txHash);
      if (txAddresses.length() > 0) {
        Enumeration txA = txAddresses.keys();
        while (txA.hasMoreElements()) {
          txSet.add(txA.nextElement());
        }
      }
    }
    return txSet;
  }
  
  public static long readUint32(byte[] bytes, int offset) {
    return bytes[offset] & 0xFF | 
      (bytes[(offset + 1)] & 0xFF) << 8 | 
      (bytes[(offset + 2)] & 0xFF) << 16 | 
      (bytes[(offset + 3)] & 0xFF) << 24;
  }
  
  public void seekPastMagicBytes(ByteBuffer in) throws Exception { int magicCursor = 3;
    for (;;) {
      byte b = in.get();
      byte expectedByte = (byte)(255 >>> magicCursor * 8);
      if (b == expectedByte) {
        magicCursor--;
        if (magicCursor >= 0) {}
      }
      else
      {
        magicCursor = 3;
      }
    }
  }
}
