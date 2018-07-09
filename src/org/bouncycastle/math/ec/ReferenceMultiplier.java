package org.bouncycastle.math.ec;

import electrol.util.BigInteger;

public class ReferenceMultiplier extends AbstractECMultiplier
{
    protected ECPoint multiplyPositive(ECPoint p, BigInteger k)
    {
        return ECAlgorithms.referenceMultiply(p, k);
    }
}
