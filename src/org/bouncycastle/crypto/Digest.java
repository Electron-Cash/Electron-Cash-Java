package org.bouncycastle.crypto;

public interface Digest
{
    public String getAlgorithmName();
    public int getDigestSize();
    public void update(byte in);
    public void update(byte[] in, int inOff, int len);
    public int doFinal(byte[] out, int outOff);
    public void reset();
}
