package org.bouncycastle.crypto.tls;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DigitallySigned
{
    protected SignatureAndHashAlgorithm algorithm;
    protected byte[] signature;

    public DigitallySigned(SignatureAndHashAlgorithm algorithm, byte[] signature)
    {
        if (signature == null)
        {
            throw new IllegalArgumentException("'signature' cannot be null");
        }

        this.algorithm = algorithm;
        this.signature = signature;
    }

    public SignatureAndHashAlgorithm getAlgorithm()
    {
        return algorithm;
    }

    public byte[] getSignature()
    {
        return signature;
    }
    public void encode(OutputStream output) throws IOException
    {
        if (algorithm != null)
        {
            algorithm.encode(output);
        }
        TlsUtils.writeOpaque16(signature, output);
    }
    public static DigitallySigned parse(TlsContext context, InputStream input) throws IOException
    {
        SignatureAndHashAlgorithm algorithm = null;
        if (TlsUtils.isTLSv12(context))
        {
            algorithm = SignatureAndHashAlgorithm.parse(input);
        }
        byte[] signature = TlsUtils.readOpaque16(input);
        return new DigitallySigned(algorithm, signature);
    }
}
