package org.electroncash.util;

import java.io.IOException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.util.encoders.Base64;
import org.electroncash.security.AESEncryption;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;



public class Storage
{
  private final Integer FINAL_SEED_VERSION = new Integer(17);
  
  private boolean enableEncryption = true;
  private byte[] raw;
  private String path;
  private boolean modified = false;
  private JSONObject data = new JSONObject();
  private AESEncryption encryption = AESEncryption.getInstance();
  
  public Storage(String path, String secret, boolean enableEncryption) throws InvalidCipherTextException, IOException, JSONException {
    this.enableEncryption = enableEncryption;
    if (enableEncryption)
      encryption.init(secret);
    this.path = path;
    if (Files.isExist(path)) {
      raw = Files.read(path);
      if (enableEncryption) {
        byte[] rawByte = Base64.decode(raw);
        if ("BIE1".equals(new String(Arrays.slice(rawByte, 0, 4)))) {
          if (enableEncryption)
            raw = encryption.decrypt(raw);
          loadData(raw);
        }
      }
      else {
        loadData(raw);
      }
    } else {
      put("seed_version", FINAL_SEED_VERSION);
    }
    write();
  }
  
  public String getPath() {
    return path;
  }
  
  public JSONObject get(String key, JSONObject defaultValue) {
    JSONObject v = data.optJSONObject(key);
    if (v != null) {
      return v;
    }
    return defaultValue;
  }
  
  public JSONArray get(String key, JSONArray defaultValue) {
    JSONArray v = data.optJSONArray(key);
    if (v != null) {
      return v;
    }
    return defaultValue;
  }
  
  public boolean get(String key, boolean defaultValue) {
    return data.optBoolean(key, defaultValue);
  }
  
  public String get(String key, String defaultValue) {
    return data.optString(key, defaultValue);
  }
  
  public void put(String key, Integer value) throws JSONException {
    if (value != null) {
      if (data.has(key)) {
        if (!value.equals(data.get(key))) {
          modified = true;
          data.put(key, value);
        }
      } else {
        modified = true;
        data.put(key, value);
      }
    } else if (data.has(key)) {
      modified = true;
      data.remove(key);
    }
  }
  
  public void put(String key, String value) throws JSONException {
    if (value != null) {
      if (data.has(key)) {
        if (!value.equals(data.get(key))) {
          modified = true;
          data.put(key, value);
        }
      } else {
        modified = true;
        data.put(key, value);
      }
    } else if (data.has(key)) {
      modified = true;
      data.remove(key);
    }
  }
  
  public void put(String key, JSONObject value) throws JSONException {
    if (value != null)
    {
      if (data.has(key)) {
        modified = true;
        if (!value.equals(data.get(key))) {
          modified = true;
          data.put(key, value);
        }
      } else {
        modified = true;
        data.put(key, value);
      }
    } else if (data.has(key)) {
      modified = true;
      data.remove(key);
    }
  }
  
  private void loadData(byte[] rawByte) throws JSONException {
    if (rawByte.length == 0) {
      data = new JSONObject();
    } else {
      data = new JSONObject(new String(rawByte));
    }
  }
  
  public void write() throws DataLengthException, IllegalStateException, IOException, InvalidCipherTextException {
    if (!modified) {
      return;
    }
    if (enableEncryption) {
      byte[] save = encryption.encrypt(data.toString());
      Files.write(save, path);
    }
    else {
      Files.write(data, path);
    }
    modified = false;
  }
}
