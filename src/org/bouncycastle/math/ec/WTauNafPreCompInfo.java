package org.bouncycastle.math.ec;

public class WTauNafPreCompInfo implements PreCompInfo {

	protected ECPoint.F2m[] preComp = null;

	public ECPoint.F2m[] getPreComp() {
		return preComp;
	}

	public void setPreComp(ECPoint.F2m[] preComp) {
		this.preComp = preComp;
	}
}
