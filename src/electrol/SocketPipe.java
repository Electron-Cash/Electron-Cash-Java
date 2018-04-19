package electrol;

import java.util.Date;

import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.java.util.Arrays;
import electrol.java.util.Iterator;
import electrol.java.util.Set;

public class SocketPipe {
	private Socket socket;
	private Date date;
	private long recv_time;
	private byte[] message;
	public SocketPipe(Socket impl) {
		this.socket = impl;
		date = new Date();
		impl.setTimeout(100);
		recv_time = date.getTime();
	}
	
	public void set_timeout(int timeout) {
		socket.setTimeout(timeout);;
	}
	public long idle_time() {
        return date.getTime() - recv_time;
	}
	private Tuple parse_json(byte[] message) {
		int n = -1;
		for(int i=0;i<message.length;i++) {
			if('\n' == message[i]) {
				n = i;
			}
		}
		if(n == -1) {
			return new Tuple(null, message);
		}
		JSONObject object;
		try {
			object = new JSONObject(new String(Arrays.slice(message, 0, n)));		
		}
		catch(JSONException e) {
			e.printStackTrace();
			object =null;
		}
		return new Tuple(object,Arrays.slice(message, n+1, message.length));
	}
	public JSONObject get() {
        while(true) {
        	byte[] data = null;
            Tuple parsedResult = parse_json(message);
            this.message = parsedResult.getMessage();
            JSONObject response = parsedResult.getResponse(); 
            if(response != null) {
                return response;
            }
            try {
                data = socket.recv(1024);
            }
            catch(Exception e) {
            	//not handling any specific error
            	e.printStackTrace();
            }
            if(data == null) { //Connection closed remotely
                return null;
            }   		
            message = org.bouncycastle.util.Arrays.concatenate(message , data);
            recv_time = date.getTime();
        }
	}
	public void send(RequestQueueItem item) {
		String out = item.toJson().toString()+"\n";
		_send(out.getBytes());
	}
	public void send_all(Set requests) {
		Iterator it = requests.iterator();
		StringBuffer out = new StringBuffer();
		while(it.hasNext()) {
			RequestQueueItem item=(RequestQueueItem)it.next();
			out.append(item.toJson().toString()+"\n");
		}
		_send(out.toString().getBytes());
	}
	public void _send(byte[] out) {
        while(out.length>0) {
            try {
                 socket.send(out);
                 //no need of python extra codes
                // int sent = out.length;
                // out = Arrays.slice(out, sent , out.length);
            }
            catch(Exception e) {
            	e.printStackTrace();
            	try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            }
        }
	}
	class Tuple{
		byte[] message;
		JSONObject response;
		public Tuple(JSONObject object, byte[] message) {
			this.response = object;
			this.message = message;
		}
		public byte[] getMessage() {
			return message;
		}
		public void setMessage(byte[] message) {
			this.message = message;
		}
		public JSONObject getResponse() {
			return response;
		}
		public void setResponse(JSONObject response) {
			this.response = response;
		}
		
	}
}
