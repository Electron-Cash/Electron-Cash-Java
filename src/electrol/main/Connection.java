package electrol.main;

import electrol.java.util.Queue;
import electrol.util.Server;

public class Connection{	
	
	public Connection(Server server, Queue queue) throws Exception {
		TcpConnection connection = new TcpConnection(server);
		queue.insert(new ServerSocketTuple(server,connection));
	}
	
}
