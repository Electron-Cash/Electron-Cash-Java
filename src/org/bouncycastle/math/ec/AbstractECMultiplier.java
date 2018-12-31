package org.bouncycastle.math.ec;

import org.electroncash.util.BigInteger;

public abstract class AbstractECMultiplier implements ECMultiplier {
	public ECPoint multiply(ECPoint p, BigInteger k) {
		int sign = k.signum();
		if (sign == 0 || p.isInfinity()) {
			return p.getCurve().getInfinity();
		}

		ECPoint positive = multiplyPositive(p, k.abs());
		ECPoint result = sign > 0 ? positive : positive.negate();

		return ECAlgorithms.validatePoint(result);
	}

	protected abstract ECPoint multiplyPositive(ECPoint p, BigInteger k);
}
