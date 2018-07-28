package electrol.main;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.java.util.ArrayList;
import electrol.java.util.HashMap;
import electrol.java.util.List;
import electrol.java.util.Map;
import electrol.util.Server;
import net.wstech2.me.httpsclient.HttpsConnectionUtils;

public class ConnectionManager extends Thread {
	private TcpConnection tcpConnection;
	private List unsentRequest;
	private Map unanswered_requests;
	private List answered_response;
	private Blockchain blockchain;
	private int tip;
	private int bad;
	private int good;
	private String mode;
	private JSONObject good_header;
	private JSONObject tip_header;
	private JSONObject bad_header;
	private int request;
	private DataOutputStream dos = null;
	private DataInputStream dis = null;

	public ConnectionManager(TcpConnection tcpConnection) {
		this.tcpConnection = tcpConnection;
		unanswered_requests = new HashMap();
		unsentRequest = new ArrayList();
		answered_response = new ArrayList();
		try {
			dis = tcpConnection.getInputStream();
			dos = tcpConnection.getOutputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
		start();
	}

	public Server getServer() {
		return tcpConnection.getServer();
	}

	public void setConnection(TcpConnection tcpConnection) {
		this.tcpConnection = tcpConnection;
	}

	public void addRequest(String method, Object[] params, int message_id) {
		unsentRequest.add(new RequestQueueItem(method, params, message_id));
	}

	public List getResponses() {
		return answered_response;
	}

	public int num_requests() {
		// Keep unanswered requests below 100'''
		int n = 100 - unanswered_requests.size();
		return Math.min(n, unsentRequest.size());
	}

	public boolean sendRequest() {
		try {
			int n = num_requests();
			StringBuffer stringBuffer = new StringBuffer();
			for (int i = 0; i < unsentRequest.size() && (i < n); i++) {
				RequestQueueItem item = (RequestQueueItem) unsentRequest.get(i);
				stringBuffer.append(item.toJsonString() + "\n");
				unanswered_requests.put(item.getId(), item);
				unsentRequest.remove(i);
			}
			dos.write(stringBuffer.toString().getBytes());
			dos.flush();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void run() {

		while (true) {

			try {

				String message = HttpsConnectionUtils.readLine(dis);
				if(message.length() != 0) {
					JSONObject response = new JSONObject(message);
					if(response.has("id")) {
						Integer id = new Integer(response.getInt("id"));
						RequestQueueItem item = (RequestQueueItem) unanswered_requests.get(id);

						if (item != null) {
							answered_response.add(new RequetResponseTuple(item, response));
							unanswered_requests.remove(id);
						} else {
							System.out.println("unknown wire id " + id);
						}
					}
					else {
						answered_response.add(new RequetResponseTuple(null, response));
					}
				}
				else {
					System.out.println("length of message is 0");
					wait();	
				}
			} catch (JSONException je) {
				je.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void setBlockchain(Blockchain blockchain) {
		this.blockchain = blockchain;
	}

	public void setTip(int tip) {
		this.tip = tip;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getMode() {
		return mode;
	}

	public void setTip_header(JSONObject tip_header) {
		this.tip_header = tip_header;
	}

	public int getTip() {
		return tip;
	}

	public int getBad() {
		return bad;
	}

	public void setBad(int bad) {
		this.bad = bad;
	}

	public JSONObject getBad_header() {
		return bad_header;
	}

	public void setBad_header(JSONObject bad_header) {
		this.bad_header = bad_header;
	}

	public Blockchain getBlockchain() {
		return blockchain;
	}

	public JSONObject getTip_header() {
		return tip_header;
	}

	public int getGood() {
		return good;
	}

	public void setGood(int good) {
		this.good = good;
	}

	public JSONObject getGood_header() {
		return good_header;
	}

	public void setGood_header(JSONObject good_header) {
		this.good_header = good_header;
	}

	public int getRequest() {
		return request;
	}

	public void setRequest(int request) {
		this.request = request;
	}

}
