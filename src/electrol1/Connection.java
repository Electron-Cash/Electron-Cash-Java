package electrol.main;

import java.io.IOException;

import electrol.java.util.Queue;
import electrol.util.Server;

public class Connection extends Thread{	
	private Server defaultServer;
	
	private Queue queue = null;
	
	public Connection(Server defaultServer, Queue queue) {
		this.queue = queue;
		this.defaultServer = defaultServer;
		start();
	}
	public void run() {
		try {
			TcpConnection connection = new TcpConnection(defaultServer);
			queue.insert(new ServerSocketTuple(defaultServer,connection));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
