package org.bouncycastle.math.ec.endo;

import org.electroncash.util.BigInteger;

public interface GLVEndomorphism extends ECEndomorphism
{
    BigInteger[] decomposeScalar(BigInteger k);
}
