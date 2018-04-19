package electrol;

public class ClientRequestHolder {
	private String method;
	private String[] params;
	private Synchronizer sync;
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String[] getParams() {
		return params;
	}
	public void setParams(String[] params) {
		this.params = params;
	}
	public Synchronizer getSync() {
		return sync;
	}
	public void setSync(Synchronizer sync) {
		this.sync = sync;
	}
	
}
