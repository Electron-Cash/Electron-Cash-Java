package electrol;

import java.util.Date;

import org.json.me.JSONObject;

import electrol.java.util.Arrays;
import electrol.java.util.HashMap;
import electrol.java.util.HashSet;
import electrol.java.util.Map;
import electrol.java.util.Set;

public class Interface {

	private String server;
	private Socket socket;
	private Blockchain blockchain;
	private Map tip_header;
	private Map bad_header;
	private int tip;
	private String mode;
	private Integer request;
	private long request_time;
	private long last_request;
	private Date date;
	private long last_ping= 0;
	private boolean closed_remotely= false;
	private Set unsent_requests;
	private Map unanswered_requests;
	private int good;
	private int bad;
	private String server_version;
	private SocketPipe pipe;
	
	public Interface(String server, Socket socket) {
		this.server = server;
		this.socket = socket;
		this.date = new Date();
		this.last_request = date.getTime();
		this.unsent_requests = new HashSet();
		this.unanswered_requests = new HashMap();
	}
	
	public long getRequest_time() {
		return request_time;
	}

	public void setRequest_time(long request_time) {
		this.request_time = request_time;
	}

	public Map getBad_header() {
		return bad_header;
	}

	public void setBad_header(Map bad_header) {
		this.bad_header = bad_header;
	}

	public int getBad() {
		return bad;
	}

	public void setBad(int bad) {
		this.bad = bad;
	}

	public int getGood() {
		return good;
	}

	public void setGood(int good) {
		this.good = good;
	}

	public void setRequestTime(long request_time) {
		this.request_time = request_time;
	}
	public Blockchain getBlockchain() {
		return blockchain;
	}
	public void setBlockchain(Blockchain blockchain) {
		this.blockchain = blockchain;
	}
	public Map getTip_header() {
		return tip_header;
	}
	public void setTip_header(Map tip_header) {
		this.tip_header = tip_header;
	}
	public int getTip() {
		return tip;
	}
	public void setTip(int tip) {
		this.tip = tip;
	}
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public Integer getRequest() {
		return request;
	}
	public void setRequest(Integer request) {
		this.request = request;
	}
	public String getServer() {
		return server;
	}
	public void setServer(String server) {
		this.server = server;
	}
	public Socket getHttpsConnectionImpl() {
		return socket;
	}
	public void setHttpsConnectionImpl(Socket socket) {
		this.socket = socket;
	}
	
	public String getServer_version() {
		return server_version;
	}

	public void setServer_version(String server_version) {
		this.server_version = server_version;
	}

	public String getHost() {
		// TODO Auto-generated method stub
		return null;
	}
	public void queue_request(String method, Object[] params, int message_id) {
		this.request_time = date.getTime();
		this.unsent_requests.add(new RequestQueueItem(method, params, message_id));
	}
	public int num_requests() {
        // Keep unanswered requests below 100
        int n = 100 - unanswered_requests.size();
        return Math.min(n, unsent_requests.size());
	}
	
	public boolean send_requests() {
	     //Sends queued requests.  Returns False on failure.
	      int n = num_requests();
	      Object[] wire_requests = Arrays.slice(unsent_requests.toArray(),0,n);
	      try {
	    	  for(int i=0;i<wire_requests.length;i++) {
	    		  RequestQueueItem item = (RequestQueueItem)wire_requests[i];
	    		  
	    	  }
	    	  Set set = new HashSet(wire_requests.length);
	    	  for(int i=0;i<wire_requests.length;i++) {
	    		  set.add(wire_requests[i]);
	    	  }
	          pipe.send_all(set);
	       }
	       catch(Exception e) {
	    	   e.printStackTrace();
	    	   return false;
	       }
	      int unset_request_count = unsent_requests.size();
	      Object[] ret=Arrays.slice(unsent_requests.toArray(), n, unset_request_count);
	      unsent_requests.clear();
	      for(int i=0;i<ret.length;i++) {
	    	  unsent_requests.add(ret[i]);
	      }
	      for(int i=0;i<wire_requests.length;i++) {
	    	   RequestQueueItem request = (RequestQueueItem)wire_requests[i];
	    	   unanswered_requests.put(request.getId(), request);
	       }
	       return true;
	}
	
	public boolean ping_required() {
        //Maintains time since last ping.  Returns True if a ping should be sent.
        long now = date.getTime();
        if(now - last_ping > 60) {
            last_ping = now;
            return true;
        }
        return false;
	}
	
	public boolean has_timed_out() {
        //Returns True if the interface has timed out.'''
        if (unanswered_requests != null && date.getTime() - request_time > 10 && pipe.idle_time() > 10) {
            System.out.println("timeout" +unanswered_requests.size());
            return true;
        }
        return false;
	}
	public HashMap get_responses() {
        /*Call if there is data available on the socket.  Returns a list of
        (request, response) pairs.  Notifications are singleton
        unsolicited responses presumably as a result of prior
        subscriptions, so request is None and there is no 'id' member.
        Otherwise it is a response, which has an 'id' member and a
        corresponding request.  If the connection was closed remotely
        or the remote server is misbehaving, a (None, None) will appear.
        */
        HashMap responses = new HashMap();
        while(true) {
        	JSONObject response;
            try {
                response = pipe.get();
            }
            catch(Exception e) {
            	e.printStackTrace();
            	break;
            }
            if(response == null) {
                closed_remotely = true;
                System.out.println("connection closed remotely");
                break;
            }
            System.out.println("<--" + response);
            int wire_id;
            try {
            	wire_id = response.getInt("id");
            	RequestQueueItem item= (RequestQueueItem)unanswered_requests.get(new Integer(wire_id));
            	if(item != null) {
            		responses.put(item, response);	
            	}
            	else {
            		System.out.println("unknown wire id "+wire_id);
            		break;
            	}
            }
            catch(Exception e) {
            	responses.put(null, response);
            }    
        }
        return responses;
	}
	public void close() {
		if(!closed_remotely) {
			socket.close();
		}
	}
}
