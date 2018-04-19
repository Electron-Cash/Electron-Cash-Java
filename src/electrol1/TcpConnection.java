package electrol1;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.httpsclient.HttpsConnectionImpl;
import electrol.java.util.ArrayList;
import electrol.java.util.HashMap;
import electrol.java.util.List;
import electrol.java.util.Map;
import electrol.util.Server;
import electrol.util.Utils;

public class TcpConnection {

	private Date date;
	private long last_ping = 0;
	private Server server;
	private long requestTime = 0;
	private List unsent_requests = new ArrayList();
	private Map unanswered_requests = new HashMap();
	private boolean closed_remotely = false;
	private Integer request;
	private Blockchain blockchain;
	private int tip;
	private int bad;
	private int good;
	private String mode;
	private JSONObject good_header;
	private JSONObject tip_header;
	private JSONObject bad_header;
	private OutputStream outputStream;
	private DataInputStream inputStream;
	private String server_version;
	boolean check = true;
	private HttpsConnectionImpl connection;
	private SocketConnection connection1;

	public TcpConnection(Server server) throws IOException {
		date = new Date();
		this.server = server;
		if (server.getProtocol().equals("s")) {
			connection = new HttpsConnectionImpl(server.getHost(), server.getPort());
			connection.setAllowUntrustedCertificates(true);

			connection.setTimeout(10);
			connection.connectSocket();
			if (connection != null) {
				outputStream = connection.openOutputStream();
				inputStream = connection.openDataInputStream();
			}

		} else {
			connection1 = (SocketConnection) Connector.open("socket://" + server.getHost() + ":" + server.getPort());

			outputStream = connection1.openOutputStream();
			inputStream = connection1.openDataInputStream();
		}
	}

	public String getHost() {
		return server.toString();
	}

	public boolean ping_required() {
		// Maintains time since last ping. Returns True if a ping should be sent.
		long now = date.getTime();
		if (now - last_ping > 60) {
			last_ping = now;
			return true;
		}
		return false;
	}

	public void sendMessage(String message) throws IOException, InterruptedException {
		outputStream.write(message.getBytes());
	}

	public String getMessage() throws IOException {
		return Utils.read(inputStream,new byte[8],5000);
		
	}

	public void queue_request(String method, Object[] params, int message_id) {
		this.requestTime = date.getTime();
		this.unsent_requests.add(new RequestQueueItem(method, params, message_id));
	}

	public boolean send_requests() {
		// Keep unanswered requests below 100
		int n = Math.min(100 - unanswered_requests.size(), unsent_requests.size());
		// Sends queued requests. Returns False on failure.
		StringBuffer buffer = new StringBuffer();
		try {
			for (int i = 0; i < unsent_requests.size(); i++) {
				if (i < n) {
					RequestQueueItem item = (RequestQueueItem) unsent_requests.get(i);
					buffer.append(item.toJson().toString() + "\n");
					unanswered_requests.put(item.getId(), item);
					unsent_requests.remove(i);
				}
			}
			sendMessage(buffer.toString());
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public List get_responses() throws JSONException {
		/*
		 * Call if there is data available on the socket. Returns a list of (request,
		 * response) pairs. Notifications are singleton unsolicited responses presumably
		 * as a result of prior subscriptions, so request is None and there is no 'id'
		 * member. Otherwise it is a response, which has an 'id' member and a
		 * corresponding request. If the connection was closed remotely or the remote
		 * server is misbehaving, a (None, None) will appear.
		 */
		final List responses = new ArrayList();

		while (true) {

			String message = null;
			try {
				message = getMessage();
				System.out.println("response <-- " + message);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (message == null) {
				closed_remotely = true;
				System.out.println("connection closed remotely");
				break;
			}
			
		
			JSONObject response = null;
			try {
				response = new JSONObject(message);
			} catch (JSONException je) {
				responses.add(new RequetResponseTuple(null, null));
				break;
			}
			int wire_id = response.optInt("id");
			if (wire_id == 0) {
				responses.add(new RequetResponseTuple(null, response));
			} else {
				Integer id = new Integer(wire_id);
				RequestQueueItem item = (RequestQueueItem) unanswered_requests.get(id);

				if (item != null) {
					responses.add(new RequetResponseTuple(item, response));
					unanswered_requests.remove(id);
				} else {
					System.out.println("unknown wire id " + wire_id);
					break;
				}
			}
		}
		return responses;
	}

	public void setRequestTime(long request_time) {
		this.requestTime = request_time;
	}

	public void setRequest(Integer request) {
		this.request = request;
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

	public Server getServer() {
		// TODO Auto-generated method stub
		return server;
	}

	public void setServer_version(String result) {
		this.server_version = result;

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

	public Integer getRequest() {
		return request;
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

	public void close() {
		try {
			if (server.getProtocol().equals("s")) {
				connection.close();
			} else {
				connection1.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
