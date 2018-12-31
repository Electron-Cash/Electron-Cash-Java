package org.electroncash.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Random;

public class Seed
{
  ArrayList wordList = new ArrayList();
  
  private boolean seedHasCorrectChecksum(String seed) {
    String sha = Bitcoin.hmac_sha_512(seed, "Seed version");
    if (sha.substring(0, 2).equals("01")) {
      return true;
    }
    return false;
  }
  
  public Seed() throws java.io.IOException {
    InputStream is = getClass().getResourceAsStream("/wordlist/english.txt");
    int c = 0;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    
    while ((c = is.read()) != -1) {
      if (c == 10) {
        String item = new String(out.toByteArray());
        wordList.add(item.trim());
        out = new ByteArrayOutputStream();
      } else {
        out.write(c);
      }
    }
  }
  
  private BigInteger mnemonic_decode(String seed) {
    BigInteger n = new BigInteger("2048");
    BigInteger i = BigInteger.ZERO;
    
    String[] words = StringUtils.split(seed, " ");
    for (int w = 0; w < words.length; w++) {
      int k = wordList.indexOf(words[w]);
      BigInteger kk = BigInteger.valueOf(k);
      i = i.multiply(n);
      i = i.add(kk);
    }
    return i;
  }
  
  private String mnemonic_encode(BigInteger i) {
    String retval = "";
    BigInteger n = new BigInteger("2048");
    while (i.compareTo(BigInteger.ZERO) == 1)
    {
      BigInteger x = i.mod(n);
      i = i.divide(n);
      retval = retval + wordList.get(x.intValue()) + " ";
    }
    
    retval = retval.trim();
    return retval;
  }
  
  public String generateSeed() throws Exception
  {
    String encoded_mnemonic = "";
    BigInteger decoded_mnemonic = null;
    
    BigInteger b = new BigInteger(132, new Random());
    boolean validSeedFound = false;
    while (!validSeedFound) {
      encoded_mnemonic = mnemonic_encode(b);
      decoded_mnemonic = mnemonic_decode(encoded_mnemonic);
      if (decoded_mnemonic.compareTo(BigInteger.ZERO) != 1) {
        throw new Exception("ERROR DECODING SEED");
      }
      if (seedHasCorrectChecksum(encoded_mnemonic)) {
        validSeedFound = true;
      }
      b = b.add(BigInteger.ONE);
    }
    return encoded_mnemonic;
  }
}
