package org.bouncycastle.math.ec;

import org.electroncash.util.BigInteger;

class ZTauElement {

	public final BigInteger u;

	public final BigInteger v;

	public ZTauElement(BigInteger u, BigInteger v) {
		this.u = u;
		this.v = v;
	}
}
