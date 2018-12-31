package org.electroncash.util;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.electroncash.nio.ByteBuffer;

public class PBKDF2
{
  public PBKDF2() {}
  
  public static byte[] shaHMAC(String hashFunction, byte[] key, byte[] password)
  {
    if (key.length == 0) {
      key = new byte[1];
    }
    org.bouncycastle.crypto.Digest d = null;
    if ("SHA256".equals(hashFunction)) {
      d = new org.bouncycastle.crypto.digests.SHA256Digest();
    }
    else if ("SHA384".equals(hashFunction)) {
      d = new SHA384Digest();
    }
    else if ("SHA512".equals(hashFunction)) {
      d = new SHA512Digest();
    }
    Mac HMAC = new HMac(d);
    CipherParameters cp = new KeyParameter(key);
    HMAC.init(cp);
    HMAC.update(password, 0, password.length);
    byte[] out = new byte[64];
    HMAC.doFinal(out, 0);
    return out;
  }
  

  private static byte[] F(String hashFunction, byte[] password, byte[] salt, int iterations, int i)
  {
    byte[] Si = new byte[salt.length + 4];
    System.arraycopy(salt, 0, Si, 0, salt.length);
    ByteBuffer buf = ByteBuffer.allocateDirect(4).putInt(i);
    buf.position(0);
    byte[] iByteArray = new byte[buf.limit()];
    buf.get(iByteArray);
    System.arraycopy(iByteArray, 0, Si, salt.length, iByteArray.length);
    byte[] U = shaHMAC(hashFunction, password, Si);
    byte[] T = new byte[U.length];
    System.arraycopy(U, 0, T, 0, T.length);
    for (int c = 1; c < iterations; c++) {
      U = shaHMAC(hashFunction, password, U);
      for (int k = 0; k < U.length; k++) {
        T[k] = ((byte)(T[k] ^ U[k]));
      }
    }
    return T;
  }
  
  public static byte[] hmac(String hashFunction, byte[] hashString, byte[] salt, int iterations)
  {
    int dkLen = 64;
    int hLen = 64;
    if ("SHA256".equals(hashFunction)) {
      dkLen = 32;
      hLen = 32;
    }
    else if ("SHA384".equals(hashFunction)) {
      dkLen = 48;
      hLen = 48;
    }
    else if ("SHA512".equals(hashFunction)) {
      dkLen = 64;
      hLen = 64;
    }
    int l = (int)Math.ceil(dkLen / hLen);
    byte[] dk = new byte[dkLen];
    for (int i = 1; i <= l; i++) {
      byte[] T = F(hashFunction, hashString, salt, iterations, i);
      for (int k = 0; k < T.length; k++) {
        if (i - 1 + k < dk.length) {
          dk[(i - 1 + k)] = T[k];
        }
      }
    }
    return dk;
  }
}
