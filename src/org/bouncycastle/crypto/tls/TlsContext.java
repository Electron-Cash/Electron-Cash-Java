package org.bouncycastle.crypto.tls;

import org.bouncycastle.crypto.prng.RandomGenerator;
import org.electroncash.security.SecureRandom;

public interface TlsContext {
	RandomGenerator getNonceRandomGenerator();

	SecureRandom getSecureRandom();

	SecurityParameters getSecurityParameters();

	boolean isServer();

	ProtocolVersion getClientVersion();

	ProtocolVersion getServerVersion();

	TlsSession getResumableSession();

	Object getUserObject();

	void setUserObject(Object userObject);

	byte[] exportKeyingMaterial(String asciiLabel, byte[] context_value, int length);
}
