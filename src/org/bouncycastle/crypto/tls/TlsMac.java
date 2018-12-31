package org.bouncycastle.crypto.tls;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.digests.LongDigest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.Arrays;

public class TlsMac {
	protected TlsContext context;
	protected byte[] secret;
	protected Mac mac;
	protected int digestBlockSize;
	protected int digestOverhead;
	protected int macLength;

	public TlsMac(TlsContext context, Digest digest, byte[] key, int keyOff, int keyLen) {
		this.context = context;

		KeyParameter keyParameter = new KeyParameter(key, keyOff, keyLen);

		this.secret = Arrays.clone(keyParameter.getKey());

		if (digest instanceof LongDigest) {
			this.digestBlockSize = 128;
			this.digestOverhead = 16;
		} else {
			this.digestBlockSize = 64;
			this.digestOverhead = 8;
		}

		if (TlsUtils.isSSL(context)) {
			this.mac = new SSL3Mac(digest);

			if (digest.getDigestSize() == 20) {

				this.digestOverhead = 4;
			}
		} else {
			this.mac = new HMac(digest);

		}

		this.mac.init(keyParameter);

		this.macLength = mac.getMacSize();
		if (context.getSecurityParameters().truncatedHMac) {
			this.macLength = Math.min(this.macLength, 10);
		}
	}

	public byte[] getMACSecret() {
		return this.secret;
	}

	public int getSize() {
		return macLength;
	}

	public byte[] calculateMac(long seqNo, short type, byte[] message, int offset, int length) {
		ProtocolVersion serverVersion = context.getServerVersion();
		boolean isSSL = serverVersion.isSSL();

		byte[] macHeader = new byte[isSSL ? 11 : 13];
		TlsUtils.writeUint64(seqNo, macHeader, 0);
		TlsUtils.writeUint8(type, macHeader, 8);
		if (!isSSL) {
			TlsUtils.writeVersion(serverVersion, macHeader, 9);
		}
		TlsUtils.writeUint16(length, macHeader, macHeader.length - 2);

		mac.update(macHeader, 0, macHeader.length);
		mac.update(message, offset, length);

		byte[] result = new byte[mac.getMacSize()];
		mac.doFinal(result, 0);
		return truncate(result);
	}

	public byte[] calculateMacConstantTime(long seqNo, short type, byte[] message, int offset, int length,
			int fullLength, byte[] dummyData) {

		byte[] result = calculateMac(seqNo, type, message, offset, length);

		int headerLength = TlsUtils.isSSL(context) ? 11 : 13;

		int extra = getDigestBlockCount(headerLength + fullLength) - getDigestBlockCount(headerLength + length);

		while (--extra >= 0) {
			mac.update(dummyData, 0, digestBlockSize);
		}

		mac.update(dummyData[0]);
		mac.reset();

		return result;
	}

	protected int getDigestBlockCount(int inputLength) {
		return (inputLength + digestOverhead) / digestBlockSize;
	}

	protected byte[] truncate(byte[] bs) {
		if (bs.length <= macLength) {
			return bs;
		}

		return Arrays.copyOf(bs, macLength);
	}
}
