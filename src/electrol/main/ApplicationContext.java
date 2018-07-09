package electrol.main;

public class ApplicationContext {

	private int latestBlockHeight;
    private boolean latestBlock;
    private String defaultServer;
	private TcpConnection tcpConnection;
    
	public int getLatestBlockHeight() {
		return latestBlockHeight;
	}

	public void setLatestBlockHeight(int latestBlockHeight) {
		this.latestBlockHeight = latestBlockHeight;
	}

	public boolean isLatestBlock() {
		return latestBlock;
	}

	public void setLatestBlock(boolean latestBlock) {
		this.latestBlock = latestBlock;
	}

	public String getDefaultServer() {
		return defaultServer;
	}

	public void setDefaultServer(String defaultServer) {
		this.defaultServer = defaultServer;
	}

	public TcpConnection getTcpConnection() {
		return tcpConnection;
	}

	public void setTcpConnection(TcpConnection tcpConnection) {
		this.tcpConnection = tcpConnection;
	}
	
	
}
