package org.electroncash.security;

import java.util.Random;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.prng.DigestRandomGenerator;
import org.bouncycastle.crypto.prng.RandomGenerator;

public class SecureRandom extends Random
{
  private static final RandomGenerator sha1Generator = new DigestRandomGenerator(new SHA1Digest());
  private RandomGenerator generator;
  
  public SecureRandom()
  {
    this(sha1Generator);
    setSeed(System.currentTimeMillis());
  }
  

  protected SecureRandom(RandomGenerator generator)
  {
    super(0L);
    this.generator = generator;
  }
  
  public byte[] generateSeed(int numBytes)
  {
    byte[] rv = new byte[numBytes];
    
    nextBytes(rv);
    
    return rv;
  }
  
  public void setSeed(byte[] inSeed)
  {
    generator.addSeedMaterial(inSeed);
  }
  
  public void nextBytes(byte[] bytes)
  {
    generator.nextBytes(bytes);
  }
  
  public void setSeed(long rSeed)
  {
    if (rSeed != 0L)
    {
      generator.addSeedMaterial(rSeed);
    }
  }
  
  public int nextInt()
  {
    byte[] intBytes = new byte[4];
    
    nextBytes(intBytes);
    
    int result = 0;
    
    for (int i = 0; i < 4; i++)
    {
      result = (result << 8) + (intBytes[i] & 0xFF);
    }
    
    return result;
  }
  
  protected final int next(int numBits)
  {
    int size = (numBits + 7) / 8;
    byte[] bytes = new byte[size];
    
    nextBytes(bytes);
    
    int result = 0;
    
    for (int i = 0; i < size; i++)
    {
      result = (result << 8) + (bytes[i] & 0xFF);
    }
    
    return result & (1 << numBits) - 1;
  }
}
