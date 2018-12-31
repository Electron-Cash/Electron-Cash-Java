package org.bouncycastle.crypto.tls;

import java.io.IOException;

public interface TlsPeer {

	boolean shouldUseGMTUnixTime();

	void notifySecureRenegotiation(boolean secureNegotiation) throws IOException;

	TlsCompression getCompression() throws IOException;

	TlsCipher getCipher() throws IOException;

	void notifyAlertRaised(short alertLevel, short alertDescription, String message, Exception cause);

	void notifyAlertReceived(short alertLevel, short alertDescription);

	void notifyHandshakeComplete() throws IOException;
}
