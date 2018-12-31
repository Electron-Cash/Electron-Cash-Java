package org.bouncycastle.util;

public interface Memoable {

	public Memoable copy();

	public void reset(Memoable other);
}
