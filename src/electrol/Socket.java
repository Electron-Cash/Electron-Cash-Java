package electrol;

import java.io.IOException;

import electrol.httpsclient.HttpsConnectionImpl;
import electrol.httpsclient.HttpsConnectionUtils;

public class Socket {
	HttpsConnectionImpl connection;
	int timeout;
	public Socket(HttpsConnectionImpl connection) {
		this.connection = connection;
	}
	public void send(byte[] b) {
		try {
			connection.openOutputStream().write(b);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public byte[] recv(int i) throws IOException {
		return HttpsConnectionUtils.readLine(connection.openInputStream()).getBytes();
	}
	public int getTimeout() {
		return timeout;
	}
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	public void close() {
		try {
			connection.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
