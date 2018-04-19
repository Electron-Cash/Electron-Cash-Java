package electrol1;

import electrol.util.Server;

public class ServerSocketTuple {
	private Server server;
	private TcpConnection socket;
	
	public ServerSocketTuple(Server server, TcpConnection tcpConnection) {
		this.server = server;
		this.socket = tcpConnection;
	}
	public Server getServer() {
		return server;
	}
	public void setServer(Server server) {
		this.server = server;
	}
	public TcpConnection getSocket() {
		return socket;
	}
	public void setSocket(TcpConnection socket) {
		this.socket = socket;
	}
}
