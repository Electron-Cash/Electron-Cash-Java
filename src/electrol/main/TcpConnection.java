package electrol.main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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

import electrol.util.Server;
import net.wstech2.me.httpsclient.HttpsConnectionImpl;
import net.wstech2.me.httpsclient.HttpsConnectionUtils;

public class TcpConnection {

	
	private HttpsConnectionImpl connection;
	private SocketConnection connection1;

	private Server server;
	
	public TcpConnection(Server server) throws IOException {
		this.server = server;
		if (server.getProtocol().equals("s")) {
			System.out.println(server.getHost());
			connection = new HttpsConnectionImpl(server.getHost(), server.getPort());
			connection.setAllowUntrustedCertificates(true);
			connection.connectSocket();
			System.out.println("connected");
			
		} else {
			
			connection1 = (SocketConnection) Connector.open("socket://" + server.getHost() + ":" + server.getPort());	
			
	/*		outputStream = connection1.openDataOutputStream();
			inputStream = connection1.openDataInputStream();
	*/	}
		
	}
	
	public static String getSSLResponse(InputStream in, OutputStream out,
			String request) throws IOException {
		String retval = null;
		TlsClientProtocol tlsClientProtocol = new TlsClientProtocol(in, out);
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
		retval = getResponse(tlsClientProtocol.getInputStream(),
				tlsClientProtocol.getOutputStream(), request);
		tlsClientProtocol.close();
		return retval;
	}
	
	public static String getResponse(InputStream in, OutputStream out, String request)
			throws IOException {

		StringBuffer retval = new StringBuffer();
		byte[] content = new byte[100];
		out.write(request.getBytes());
		out.flush();

		int read = 0;
		while ((read = in.read(content)) != -1) {
			HttpsConnectionUtils.logDebug("Reading " + read + " bytes[ " + new String(content, 0, read)
					+ "]");
			retval.append(new String(content, 0, read));
		}

		return retval.toString();
	}

	
	public Server getServer() {
		return server;
	}
	public DataOutputStream getOutputStream() throws IOException {
		return connection.openDataOutputStream();
	}
	public DataInputStream getInputStream() throws IOException {
		return connection.openDataInputStream();
	}
	public void close() {
		try {
			if (connection != null) {
				connection.close();
			}
			if(connection1 != null){
				connection1.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
