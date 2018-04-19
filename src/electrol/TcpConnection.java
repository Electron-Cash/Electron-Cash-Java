package electrol;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import electrol.httpsclient.HttpsConnectionImpl;
import electrol.httpsclient.HttpsConnectionUtils;
import electrol.java.util.Queue;
import electrol.util.Server;
import electrol.util.Utils;

public class TcpConnection extends Thread {

	private HttpsConnectionImpl connection;
	private SocketConnection connection1;
	private Queue socket_queue;
	private String config_path;
	private Server serverObj;
	private boolean use_ssl = false;

	public TcpConnection(String server, Queue socket_queue, String config_path) {
		this.socket_queue = socket_queue;
		this.config_path = config_path;
		this.serverObj = Network.deserialize_server(server);
		if (serverObj.getProtocol().equals("s")) {
			use_ssl = true;
		}
	}

	public void run() {
		try {
			if (use_ssl) {
				connection = new HttpsConnectionImpl(serverObj.getHost(), serverObj.getPort());
				connection.setAllowUntrustedCertificates(true);
				connection.connectSocket();
			}
			else {
				connection1 = (SocketConnection)Connector.open("socket://"
						+ serverObj.getHost() + ":" + serverObj.getPort());
			}
			socket_queue.insert(this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void sendMessage(String message) throws IOException {
		if(use_ssl)
			connection.openOutputStream().write(message.getBytes());
		else
			connection1.openOutputStream().write(message.getBytes());
	}

	public String getMessage() throws IOException {
		if(use_ssl)
			return HttpsConnectionUtils.readLine(connection.openInputStream());
		else 
			return HttpsConnectionUtils.readLine(connection1.openInputStream());
	}
}
