package org.electroncash.util;

import java.io.PrintStream;
import org.bouncycastle.util.Arrays;
import org.json.me.JSONArray;
import org.json.me.JSONObject;

public class Verifier
{
  public Verifier() {}
  
  public HashMap merkle_roots = new HashMap();
  
  public VerifiedTx verify_merkle(JSONObject r, Network network) throws org.json.me.JSONException, java.io.IOException {
    if (r.has("error")) {
      System.out.println("get error " + r);
      return null;
    }
    JSONArray params = r.getJSONArray("params");
    JSONObject merkle = r.getJSONObject("result");
    


    String tx_hash = params.getString(0);
    int tx_height = merkle.getInt("block_height");
    int pos = merkle.getInt("pos");
    String merkle_root = hash_merkle_root(merkle.getJSONArray("merkle"), tx_hash, pos);
    JSONObject header = network.blockchain().read_header(tx_height, null);
    if ((header == null) || (!header.get("merkle_root").equals(merkle_root)))
    {

      System.out.println("merkle verification failed for " + tx_hash);
      return null;
    }
    
    merkle_roots.put(tx_hash, merkle_root);
    return new VerifiedTx(tx_hash, tx_height, header.getInt("timestamp"), pos);
  }
  
  public String hash_merkle_root(JSONArray merkle_s, String target_hash, int pos)
    throws org.json.me.JSONException
  {
    byte[] h = BlockchainsUtil.hashDecode(target_hash);
    for (int i = 0; i < merkle_s.length(); i++) {
      String item = merkle_s.getString(i);
      if ((pos >> i & 0x1) != 0) {
        h = BlockchainsUtil.Hash(Arrays.concatenate(BlockchainsUtil.hashDecode(item), h));
      }
      else {
        h = BlockchainsUtil.Hash(Arrays.concatenate(h, BlockchainsUtil.hashDecode(item)));
      }
    }
    return BlockchainsUtil.hashEncode(h);
  }
  


  public boolean is_up_to_date()
  {
    return false;
  }
}
