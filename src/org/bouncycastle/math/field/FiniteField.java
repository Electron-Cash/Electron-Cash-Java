package org.bouncycastle.math.field;

import org.electroncash.util.BigInteger;

public interface FiniteField
{
    BigInteger getCharacteristic();

    int getDimension();
}
