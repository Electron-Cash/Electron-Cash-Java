package electrol.main;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.java.util.ArrayList;
import electrol.java.util.HashMap;
import electrol.java.util.List;
import electrol.java.util.Map;
import electrol.util.Server;

public class Interface {

	private Map unanswered_requests = new HashMap();
	private List unsent_requests = new ArrayList();
	private boolean closed_remotely = false;
	private long request_time = 0;
	private Date date;
	private TcpConnection connection;
	private Blockchain blockchain;
	private int tip;
	private int bad;
	private int good;
	private String mode;
	private JSONObject good_header;
	private JSONObject tip_header;
	private JSONObject bad_header;
	private int request;
	private Server server;
	private long recv_time;

	public Interface(Server server, TcpConnection connection) {
		date = new Date();
		this.server = server;
		this.connection = connection;
	}


	public void queue_request(String method, Object[] params, int message_id) {
		request_time = date.getTime();
		unsent_requests.add(new RequestQueueItem(method, params, message_id));
	}
	
	public int num_requests() {
		//Keep unanswered requests below 100'''
		int n = 100 - unanswered_requests.size();
		return Math.min(n, unsent_requests.size());
	}

	public boolean send_requests() {
		
		// Sends queued requests. Returns False on failure.
		
		try {
			int n = num_requests();
			JSONArray array = new JSONArray();
			for (int i = 0; i < unsent_requests.size(); i++) {
				if (i < n) {

					RequestQueueItem item = (RequestQueueItem) unsent_requests.get(i);
					array.put(item.toJsonString());
					unanswered_requests.put(item.getId(), item);
					unsent_requests.remove(i);
				}
			}
			System.out.println("request "+array.toString());
			OutputStream os = connection.getOutputStream();
			os.write(array.toString().getBytes());
			os.flush();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public List get_responses(){
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

			try {
				
				ReadNonblockingMessage read = new ReadNonblockingMessage(connection.getInputStream());
				String message = read.read();
				System.out.println("message "+message);
				if (message == null) {
					closed_remotely = true;
					System.out.println("connection closed remotely");
					break;
				}
				if(message.length() == 0) {
					break;
				}
				JSONObject response = new JSONObject(message);
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
			catch (JSONException je) {
				responses.add(new RequetResponseTuple(null, null));
				break;
			} catch (IOException e) {
				e.printStackTrace();
				break;
			} catch (InterruptedException e) {
				try {
					connection = new TcpConnection(getServer());
					break;
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		return responses;
	}

	/*public boolean has_timed_out() {
		//Returns True if the interface has timed out.'''
		if(unanswered_requests.size() > 0 && (date.getTime() - request_time) > 10
				&& date.getTime() -recv_time > 10) {
			System.out.println("timeout "+ unanswered_requests.size());
			return true;
		}
		return false;
	}*/

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

	public boolean getClosed_Remotely() {
		return closed_remotely;
	}

	public void setRequest_time(long request_time) {
		this.request_time = request_time;
	}

	public long getRequest_time() {
		return request_time;
	}
	public void setRequest(int request) {
		this.request = request;
	}

	public int getRequest() {
		return request;
	}

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public void setConnection(TcpConnection connection) {
		this.connection = connection;
	}

	public void close() {
		if(!closed_remotely) {
			connection.close();
		}	
	}
}
