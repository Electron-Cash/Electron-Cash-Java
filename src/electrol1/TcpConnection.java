package electrol.main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import electrol.httpsclient.HttpsConnectionImpl;
import electrol.util.Server;

public class TcpConnection {

	
	private DataOutputStream outputStream;
	private DataInputStream inputStream;
	private HttpsConnectionImpl connection;
	private SocketConnection connection1;

	private Server server;
	
	public TcpConnection(Server server) throws IOException {
		this.server = server;
		if (server.getProtocol().equals("s")) {
			connection = new HttpsConnectionImpl(server.getHost(), server.getPort());
			connection.setAllowUntrustedCertificates(true);
			
			/*connection.setTimeout(0);*/
			connection.connectSocket();
			outputStream = connection.openDataOutputStream();
			inputStream = connection.openDataInputStream();
		} else {
			connection1 = (SocketConnection) Connector.open("socket://" + server.getHost() + ":" + server.getPort());

			outputStream = connection1.openDataOutputStream();
			inputStream = connection1.openDataInputStream();
		}
	}
	
	public Server getServer() {
		return server;
	}
	public DataOutputStream getOutputStream() {
		return outputStream;
	}
	public DataInputStream getInputStream() {
		return inputStream;
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
