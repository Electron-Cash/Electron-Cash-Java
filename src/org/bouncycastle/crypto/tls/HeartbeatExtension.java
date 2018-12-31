package org.bouncycastle.crypto.tls;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HeartbeatExtension {
	protected short mode;

	public HeartbeatExtension(short mode) {
		if (!HeartbeatMode.isValid(mode)) {
			throw new IllegalArgumentException("'mode' is not a valid HeartbeatMode value");
		}

		this.mode = mode;
	}

	public short getMode() {
		return mode;
	}

	public void encode(OutputStream output) throws IOException {
		TlsUtils.writeUint8(mode, output);
	}

	public static HeartbeatExtension parse(InputStream input) throws IOException {
		short mode = TlsUtils.readUint8(input);
		if (!HeartbeatMode.isValid(mode)) {
			throw new TlsFatalAlert(AlertDescription.illegal_parameter);
		}

		return new HeartbeatExtension(mode);
	}
}
