package org.bouncycastle.math.field;

import electrol.util.BigInteger;

public interface FiniteField
{
    BigInteger getCharacteristic();

    int getDimension();
}
