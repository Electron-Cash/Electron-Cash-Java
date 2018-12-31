package org.bouncycastle.math.ec;

import org.electroncash.util.BigInteger;

public interface ECMultiplier {

	ECPoint multiply(ECPoint p, BigInteger k);
}
