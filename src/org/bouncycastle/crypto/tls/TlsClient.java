package org.bouncycastle.crypto.tls;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

public interface TlsClient extends TlsPeer {
	void init(TlsClientContext context);

	TlsSession getSessionToResume();

	ProtocolVersion getClientHelloRecordLayerVersion();

	ProtocolVersion getClientVersion();

	int[] getCipherSuites();

	short[] getCompressionMethods();

	Hashtable getClientExtensions() throws IOException;

	void notifyServerVersion(ProtocolVersion selectedVersion) throws IOException;

	void notifySessionID(byte[] sessionID);

	void notifySelectedCipherSuite(int selectedCipherSuite);

	void notifySelectedCompressionMethod(short selectedCompressionMethod);

	void processServerExtensions(Hashtable serverExtensions) throws IOException;

	void processServerSupplementalData(Vector serverSupplementalData) throws IOException;

	TlsKeyExchange getKeyExchange() throws IOException;

	TlsAuthentication getAuthentication() throws IOException;

	Vector getClientSupplementalData() throws IOException;

	void notifyNewSessionTicket(NewSessionTicket newSessionTicket) throws IOException;
}
