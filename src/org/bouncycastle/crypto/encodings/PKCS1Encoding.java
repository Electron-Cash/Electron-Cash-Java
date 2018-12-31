package org.bouncycastle.crypto.encodings;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.electroncash.security.SecureRandom;

public class PKCS1Encoding
    implements AsymmetricBlockCipher
{
    public static final String STRICT_LENGTH_ENABLED_PROPERTY = "org.bouncycastle.pkcs1.strict";
    
    private static final int HEADER_LENGTH = 10;

    private SecureRandom            random;
    private AsymmetricBlockCipher   engine;
    private boolean                 forEncryption;
    private boolean                 forPrivateKey;
    private boolean                 useStrictLength;
    private int                     pLen = -1;
    private byte[]                  fallback = null;

    public PKCS1Encoding(
        AsymmetricBlockCipher   cipher)
    {
        this.engine = cipher;
        this.useStrictLength = useStrict();
    }   

    public PKCS1Encoding(
        AsymmetricBlockCipher   cipher,
        int pLen)
    {
        this.engine = cipher;
        this.useStrictLength = useStrict();
        this.pLen = pLen;
    }

	public PKCS1Encoding(
    	AsymmetricBlockCipher   cipher,
        byte[] fallback)
    {
    	this.engine = cipher;
    	this.useStrictLength = useStrict();
    	this.fallback = fallback;
    	this.pLen = fallback.length;
    }
    private boolean useStrict()
    {
        String strict = System.getProperty(STRICT_LENGTH_ENABLED_PROPERTY);

        return strict == null || strict.equals("true");
    }

    public AsymmetricBlockCipher getUnderlyingCipher()
    {
        return engine;
    }

    public void init(
        boolean             forEncryption,
        CipherParameters    param)
    {
        AsymmetricKeyParameter  kParam;

        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom    rParam = (ParametersWithRandom)param;

            this.random = rParam.getRandom();
            kParam = (AsymmetricKeyParameter)rParam.getParameters();
        }
        else
        {
            this.random = new SecureRandom();
            kParam = (AsymmetricKeyParameter)param;
        }

        engine.init(forEncryption, param);

        this.forPrivateKey = kParam.isPrivate();
        this.forEncryption = forEncryption;
    }

    public int getInputBlockSize()
    {
        int     baseBlockSize = engine.getInputBlockSize();

        if (forEncryption)
        {
            return baseBlockSize - HEADER_LENGTH;
        }
        return baseBlockSize;
    }

    public int getOutputBlockSize()
    {
        int     baseBlockSize = engine.getOutputBlockSize();

        if (forEncryption)
        {
            return baseBlockSize;
        }
        return baseBlockSize - HEADER_LENGTH;
        
    }

    public byte[] processBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
        throws InvalidCipherTextException
    {
        if (forEncryption)
        {
            return encodeBlock(in, inOff, inLen);
        }
        return decodeBlock(in, inOff, inLen);
    }

    private byte[] encodeBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
        throws InvalidCipherTextException
    {
        if (inLen > getInputBlockSize())
        {
            throw new IllegalArgumentException("input data too large");
        }
        
        byte[]  block = new byte[engine.getInputBlockSize()];

        if (forPrivateKey)
        {
            block[0] = 0x01;                        // type code 1

            for (int i = 1; i != block.length - inLen - 1; i++)
            {
                block[i] = (byte)0xFF;
            }
        }
        else
        {
            random.nextBytes(block);                // random fill

            block[0] = 0x02;                        // type code 2
            for (int i = 1; i != block.length - inLen - 1; i++)
            {
                while (block[i] == 0)
                {
                    block[i] = (byte)random.nextInt();
                }
            }
        }

        block[block.length - inLen - 1] = 0x00;       // mark the end of the padding
        System.arraycopy(in, inOff, block, block.length - inLen, inLen);

        return engine.processBlock(block, 0, block.length);
    }
    
	private static int checkPkcs1Encoding(byte[] encoded, int pLen) {
		int correct = 0;
		correct |= (encoded[0] ^ 2);
		int plen = encoded.length - (pLen +  1);

		for (int i = 1; i < plen; i++) {
			int tmp = encoded[i];
			tmp |= tmp >> 1;
			tmp |= tmp >> 2;
			tmp |= tmp >> 4;
			correct |= (tmp & 1) - 1;
		}

		correct |= encoded[encoded.length - (pLen +1)];

		correct |= correct >> 1;
		correct |= correct >> 2;
		correct |= correct >> 4;
		return ~((correct & 1) - 1);
	}
    
    private byte[] decodeBlockOrRandom(byte[] in, int inOff, int inLen)
        throws InvalidCipherTextException
    {
        if (!forPrivateKey)
        {
            throw new InvalidCipherTextException("sorry, this method is only for decryption, not for signing");
        }

        byte[] block = engine.processBlock(in, inOff, inLen);
        byte[] random = null;
        if (this.fallback == null)
        {
            random = new byte[this.pLen];
            this.random.nextBytes(random);
        }
        else
        {
            random = fallback;
        }

        if (block.length < getOutputBlockSize())
        {
            throw new InvalidCipherTextException("block truncated");
        }

        if (useStrictLength && block.length != engine.getOutputBlockSize())
        {
            throw new InvalidCipherTextException("block incorrect size");
        }

        int correct = PKCS1Encoding.checkPkcs1Encoding(block, this.pLen);
		
        byte[] result = new byte[this.pLen];
        for (int i = 0; i < this.pLen; i++)
        {
            result[i] = (byte)((block[i + (block.length - pLen)] & (~correct)) | (random[i] & correct));
        }

        return result;
    }

    private byte[] decodeBlock(
        byte[]  in,
        int     inOff,
        int     inLen)
        throws InvalidCipherTextException
    {
		if (this.pLen != -1) {
    		return this.decodeBlockOrRandom(in, inOff, inLen);
    	}
    	
        byte[] block = engine.processBlock(in, inOff, inLen);

        if (block.length < getOutputBlockSize())
        {
            throw new InvalidCipherTextException("block truncated");
        }

        byte type = block[0];

        if (forPrivateKey)
        {
            if (type != 2)
            {
                throw new InvalidCipherTextException("unknown block type");
            }
        }
        else
        {
            if (type != 1)
            {
                throw new InvalidCipherTextException("unknown block type");
            }
        }

        if (useStrictLength && block.length != engine.getOutputBlockSize())
        {
            throw new InvalidCipherTextException("block incorrect size");
        }
        
        int start;
        
        for (start = 1; start != block.length; start++)
        {
            byte pad = block[start];
            
            if (pad == 0)
            {
                break;
            }
            if (type == 1 && pad != (byte)0xff)
            {
                throw new InvalidCipherTextException("block padding incorrect");
            }
        }
        start++;     
        
        if (start > block.length || start < HEADER_LENGTH)
        {
            throw new InvalidCipherTextException("no data in block");
        }

        byte[]  result = new byte[block.length - start];

        System.arraycopy(block, start, result, 0, result.length);

        return result;
    }
}
