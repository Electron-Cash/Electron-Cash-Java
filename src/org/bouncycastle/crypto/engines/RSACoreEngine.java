package org.bouncycastle.crypto.engines;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.electroncash.util.BigInteger;

class RSACoreEngine
{
    private RSAKeyParameters key;
    private boolean          forEncryption;

    public void init(
        boolean          forEncryption,
        CipherParameters param)
    {
        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom    rParam = (ParametersWithRandom)param;

            key = (RSAKeyParameters)rParam.getParameters();
        }
        else
        {
            key = (RSAKeyParameters)param;
        }

        this.forEncryption = forEncryption;
    }

    public int getInputBlockSize()
    {
        int     bitSize = key.getModulus().bitLength();

        if (forEncryption)
        {
            return (bitSize + 7) / 8 - 1;
        }
        return (bitSize + 7) / 8;
        
    }

    public int getOutputBlockSize()
    {
        int     bitSize = key.getModulus().bitLength();

        if (forEncryption)
        {
            return (bitSize + 7) / 8;
        }
        return (bitSize + 7) / 8 - 1;
    }

    public BigInteger convertInput(
        byte[]  in,
        int     inOff,
        int     inLen)
    {
        if (inLen > (getInputBlockSize() + 1))
        {
            throw new DataLengthException("input too large for RSA cipher.");
        }
        else if (inLen == (getInputBlockSize() + 1) && !forEncryption)
        {
            throw new DataLengthException("input too large for RSA cipher.");
        }

        byte[]  block;

        if (inOff != 0 || inLen != in.length)
        {
            block = new byte[inLen];

            System.arraycopy(in, inOff, block, 0, inLen);
        }
        else
        {
            block = in;
        }

        BigInteger res = new BigInteger(1, block);
        if (res.compareTo(key.getModulus()) >= 0)
        {
            throw new DataLengthException("input too large for RSA cipher.");
        }

        return res;
    }

    public byte[] convertOutput(
        BigInteger result)
    {
        byte[]      output = result.toByteArray();

        if (forEncryption)
        {
            if (output[0] == 0 && output.length > getOutputBlockSize())        
            {
                byte[]  tmp = new byte[output.length - 1];

                System.arraycopy(output, 1, tmp, 0, tmp.length);

                return tmp;
            }

            if (output.length < getOutputBlockSize())    
            {
                byte[]  tmp = new byte[getOutputBlockSize()];

                System.arraycopy(output, 0, tmp, tmp.length - output.length, output.length);

                return tmp;
            }
        }
        else
        {
            if (output[0] == 0)       
            {
                byte[]  tmp = new byte[output.length - 1];

                System.arraycopy(output, 1, tmp, 0, tmp.length);

                return tmp;
            }
        }

        return output;
    }

    public BigInteger processBlock(BigInteger input)
    {
        if (key instanceof RSAPrivateCrtKeyParameters)
        {
            RSAPrivateCrtKeyParameters crtKey = (RSAPrivateCrtKeyParameters)key;

            BigInteger p = crtKey.getP();
            BigInteger q = crtKey.getQ();
            BigInteger dP = crtKey.getDP();
            BigInteger dQ = crtKey.getDQ();
            BigInteger qInv = crtKey.getQInv();

            BigInteger mP, mQ, h, m;

            mP = (input.remainder(p)).modPow(dP, p);
            mQ = (input.remainder(q)).modPow(dQ, q);
            h = mP.subtract(mQ);
            h = h.multiply(qInv);
            h = h.mod(p);             
            m = h.multiply(q);
            m = m.add(mQ);

            return m;
        }
        return input.modPow(
                        key.getExponent(), key.getModulus());
    }
}
