package org.bouncycastle.math.ec.endo;

import electrol.util.BigInteger;

public interface GLVEndomorphism extends ECEndomorphism
{
    BigInteger[] decomposeScalar(BigInteger k);
}
