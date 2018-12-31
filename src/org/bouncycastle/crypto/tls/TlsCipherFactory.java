package org.bouncycastle.crypto.tls;

import java.io.IOException;

public interface TlsCipherFactory {

	TlsCipher createCipher(TlsContext context, int encryptionAlgorithm, int macAlgorithm) throws IOException;
}
