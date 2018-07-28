package electrol.main;

public class VerifiedTx {
	private String hash;
	private int height;
	private int timestamp;
	private int pos;
	
	public VerifiedTx(String hash, int height, int timestamp, int pos) {
		this.hash = hash;
		this.height = height;
		this.timestamp = timestamp;
		this.pos = pos;
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	public int getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}
	public int getPos() {
		return pos;
	}
	public void setPos(int pos) {
		this.pos = pos;
	}
	
}
