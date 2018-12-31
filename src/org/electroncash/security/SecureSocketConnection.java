package org.electroncash.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import org.bouncycastle.crypto.tls.Certificate;
import org.bouncycastle.crypto.tls.CertificateRequest;
import org.bouncycastle.crypto.tls.DefaultTlsClient;
import org.bouncycastle.crypto.tls.TlsAuthentication;
import org.bouncycastle.crypto.tls.TlsClientProtocol;
import org.bouncycastle.crypto.tls.TlsCredentials;
import org.electroncash.util.Server;

public class SecureSocketConnection
{
	static final char CR = '\r';
	static final char LF = '\n';
	private Server server;
	private TlsClientProtocol tlsClientProtocol;
	private boolean isConnected = false;
	
	public SecureSocketConnection(Server server) throws IOException {
		this.server = server;
		SocketConnection socket = (SocketConnection)Connector.open("socket://" + server.getHost() + ":" + server.getPort(), Connector.READ_WRITE, true);
		tlsClientProtocol = new TlsClientProtocol(socket.openInputStream(), socket.openOutputStream());
		DefaultTlsClient tlsClient = new DefaultTlsClient() {

			public TlsAuthentication getAuthentication() throws IOException {
				return new TlsAuthentication() {

					public void notifyServerCertificate(
							Certificate serverCertificate) throws IOException {
					}

					public TlsCredentials getClientCredentials(
							CertificateRequest certificateRequest)
							throws IOException {
						return null;
					}
				};
			}
         };
        tlsClientProtocol.connect(tlsClient);
        isConnected = true;
	}
	
	public boolean isConnected() { return isConnected; }
	
	public Server getServer() { return server;}

	public OutputStream getOutputStream() throws IOException { return tlsClientProtocol.getOutputStream(); }


	public InputStream getInputStream() throws IOException { return tlsClientProtocol.getInputStream(); }

	public void close() {
		if (tlsClientProtocol != null) {
			try {
				tlsClientProtocol.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String readLine(InputStream in) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		int c = 0;
		while ((c = in.read()) != -1 && c != LF) {
			if (c != CR) {
				bytes.write(c);
			}
		} 
		return new String(bytes.toByteArray());
	}
}
