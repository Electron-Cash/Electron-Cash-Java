package electrol.main;

public class LocalServerTuple {
	private int local_height;
	private int server_height;
	public LocalServerTuple(int local_height, int server_height) {
		this.local_height = local_height;
		this.server_height = server_height;
	}
	
	public int getLocal_height() {
		return local_height;
	}
	
	public int getServer_height() {
		return server_height;
	}

}
