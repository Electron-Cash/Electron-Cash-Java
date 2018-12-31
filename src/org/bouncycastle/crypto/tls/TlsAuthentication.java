package org.bouncycastle.crypto.tls;

import java.io.IOException;

public interface TlsAuthentication {

	void notifyServerCertificate(Certificate serverCertificate) throws IOException;

	TlsCredentials getClientCredentials(CertificateRequest certificateRequest) throws IOException;
}
