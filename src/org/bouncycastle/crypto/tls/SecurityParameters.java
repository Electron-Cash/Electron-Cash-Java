package org.bouncycastle.crypto.tls;

import org.bouncycastle.util.Arrays;

public class SecurityParameters {
	int entity = -1;
	int cipherSuite = -1;
	short compressionAlgorithm = -1;
	int prfAlgorithm = -1;
	int verifyDataLength = -1;
	byte[] masterSecret = null;
	byte[] clientRandom = null;
	byte[] serverRandom = null;

	short maxFragmentLength = -1;
	boolean truncatedHMac = false;
	boolean encryptThenMAC = false;

	void copySessionParametersFrom(SecurityParameters other) {
		this.entity = other.entity;
		this.cipherSuite = other.cipherSuite;
		this.compressionAlgorithm = other.compressionAlgorithm;
		this.prfAlgorithm = other.prfAlgorithm;
		this.verifyDataLength = other.verifyDataLength;
		this.masterSecret = Arrays.clone(other.masterSecret);
	}

	void clear() {
		if (this.masterSecret != null) {
			Arrays.fill(this.masterSecret, (byte) 0);
			this.masterSecret = null;
		}
	}

	public int getEntity() {
		return entity;
	}

	public int getCipherSuite() {
		return cipherSuite;
	}

	public short getCompressionAlgorithm() {
		return compressionAlgorithm;
	}

	public int getPrfAlgorithm() {
		return prfAlgorithm;
	}

	public int getVerifyDataLength() {
		return verifyDataLength;
	}

	public byte[] getMasterSecret() {
		return masterSecret;
	}

	public byte[] getClientRandom() {
		return clientRandom;
	}

	public byte[] getServerRandom() {
		return serverRandom;
	}
}
