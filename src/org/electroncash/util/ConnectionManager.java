package org.electroncash.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.electroncash.AlertUtil;
import org.electroncash.security.SecureSocketConnection;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public class ConnectionManager
extends Thread
{
	public static final byte MODE_DEFAULT = 0;
	public static final byte MODE_VERIFICATION = 1;
	public static final byte MODE_BACKWARD = 2;
	public static final byte MODE_CATCH_UP = 3;
	public static final byte MODE_BINARY = 5;
	private SecureSocketConnection tcpConnection;
	private List unsentRequest;
	private Map unanswered_requests;
	private List answered_response;
	private Blockchain blockchain;
	private int tip;
	private int bad;
	private int good;
	private byte mode;
	private JSONObject good_header;
	private JSONObject tip_header;
	private JSONObject bad_header;
	private int request;
	private OutputStream dos = null;
	private InputStream dis = null;
	private String serverVersion;
	private AlertUtil alertUtil;

	public ConnectionManager(SecureSocketConnection tcpConnection, AlertUtil alertUtil) {
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
		this.alertUtil = alertUtil;
		start();
	}

	public Server getServer() {
		return tcpConnection.getServer();
	}

	public void setConnection(SecureSocketConnection tcpConnection) {
		this.tcpConnection = tcpConnection;
	}

	public void addRequest(String method, Object[] params, int message_id) {
		unsentRequest.add(new RequestItem(method, params, message_id));
	}

	public List getResponses() {
		List responses = new ArrayList(answered_response);
		answered_response.clear();
		return responses;
	}

	public int num_requests()
	{
		int n = 100 - unanswered_requests.size();
		return Math.min(n, unsentRequest.size());
	}

	public boolean sendRequest() {
		try {
			int n = num_requests();
			StringBuffer stringBuffer = new StringBuffer();
			for (int i = 0; (i < unsentRequest.size()) && (i < n); i++) {
				RequestItem item = (RequestItem)unsentRequest.get(i);
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

	public void run()
	{
		while (true)
		{
			try
			{
				
				String message = SecureSocketConnection.readLine(dis);
				if (message.length() != 0) {
					JSONObject response = new JSONObject(message);
					if (response.has("id")) {
						Integer id = new Integer(response.getInt("id"));
						RequestItem item = (RequestItem)unanswered_requests.get(id);
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
					sleep(1000L);
				}
			} catch (JSONException je) {
				System.out.println("json exception");
				alertUtil.sendErrorAlert(je.getMessage());
			} catch (IOException e) {
				System.out.println("io exception");
				alertUtil.sendErrorAlert(e.getMessage());
			} catch (InterruptedException e) {
				System.out.println("intrupted excption");
				alertUtil.sendErrorAlert(e.getMessage());
			}
			catch (OutOfMemoryError e) {
				System.out.println("outofmemoryexception");
				alertUtil.sendErrorAlert(e.getMessage());
			}
			catch (Exception e) {
				alertUtil.sendErrorAlert(e.getMessage());
			}
		}
	}

	public void setBlockchain(Blockchain blockchain)
	{
		this.blockchain = blockchain;
	}

	public void setTip(int tip) {
		this.tip = tip;
	}

	public void setMode(byte mode) {
		this.mode = mode;
	}

	public byte getMode() {
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

	public String getServerVersion() {
		return serverVersion;
	}

	public void setServerVersion(String serverVersion) {
		this.serverVersion = serverVersion;
	}
}
