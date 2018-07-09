package electrol.main;

import java.io.IOException;
import java.util.Date;
import java.util.Random;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.java.util.HashMap;
import electrol.java.util.HashSet;
import electrol.java.util.Iterator;
import electrol.java.util.List;
import electrol.java.util.Map;
import electrol.java.util.Queue;
import electrol.java.util.Set;
import electrol.util.BitcoinMainnet;
import electrol.util.Files;
import electrol.util.Server;



public class Network extends Thread{

	private Set disconnected_server;
	private Set connecting;
	private Random random;
	private JSONArray recent_servers;
	private Server default_server;
	private static JSONObject irc_servers = new JSONObject(); //returned by interface (list from irc)
	private String protocol = "s";
	private String default_protocol = "s";
	private Map interfaces = new HashMap(); //["Server","TcpConnection"]
	private Queue socketQueue= new Queue(10);
	private Map blockchains;
	private boolean downloadingHeaders = false;
	private TcpConnection tcpConnection;
	private volatile int message_id;
	private Date date;
	private Map sub_cache = new HashMap();
	private Map unanswered_requests = new HashMap();
	private Set subscribed_addresses = new HashSet();
	private Integer blockchain_index;
	private Map subscriptions = new HashMap();
	private Interface interfaceObj;
	private String connection_status;
	private int num_servers = 10;
	private ApplicationContext context;
	public Network(ApplicationContext context){
		this.context = context;
		blockchains = BlockchainsUtil.read_blockchain();
		connecting = new HashSet();
		disconnected_server = new HashSet();
		random = new Random();
		date = new Date();
		try {
			recent_servers = NetworkUtil.read_recent_servers();
			default_server = pick_random_server();
			startNetwork(default_server.getProtocol());
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void startNetwork(String protocol) throws JSONException {
		disconnected_server = new HashSet();
		this.protocol = protocol;
		startInterfaces();
	}


	private void startInterfaces() throws JSONException {
		startInterface(default_server);
		for(int i=0;i<num_servers-1;i++) {
			start_random_interface();
		}

	}
	private void start_random_interface() throws JSONException{

		Iterator iterator = interfaces.keySet().iterator();
		Set exclude = disconnected_server;
		while (iterator.hasNext()) {
			Server server = (Server)iterator.next();
			if(!disconnected_server.contains(server)) {
				exclude.add(server);
			}
		}
		Server server = NetworkUtil.pick_random_server(get_servers(),protocol, exclude,default_protocol);
		if(server != null) {
			startInterface(server);
		}
	}
	private void startInterface(Server server) {
		if(!connecting.contains(server) && !interfaces.containsKey(server)) {
			if(server.equals(default_server)) {
				System.out.println("connecting to "+server.toString()+" as new interface");
			}
			try {
				new Connection(server,socketQueue);
				connecting.add(server);
			}catch(Exception e) {
				disconnected_server.add(server);
			}
		}	
	}
	
	private JSONObject get_servers() throws JSONException {
		JSONObject out = BitcoinMainnet.getDefaultServers();
		if(irc_servers.length() > 0) {
			filterVersion(irc_servers);
		}
		else {
			JSONArray servers = recent_servers;
			for(int i=0;i<servers.length();i++) {
				Server server = NetworkUtil.deserialize_server(servers.getString(i));
				if(!out.has(server.getHost())) {
					JSONObject value = new JSONObject();
					value.put(server.getProtocol(), server.getPort());
					out.put(server.getHost(), value);
				}
			}
		}
		return out;
	}
	

	private void filterVersion(JSONObject irc_servers2) {
		// TODO Auto-generated method stub

	}

	public Server pick_random_server() throws JSONException {
		return NetworkUtil.pick_random_server(null, "s", new HashSet(),default_protocol);
	}

	public void run() {
		/*try {
			NetworkUtil.init_header_file(this, (Blockchain)(blockchains.get(new Integer(0))));
			while(isDownloadingHeaders()) {
				System.out.println("wait for header download");
				Thread.sleep(1000);
			}
		} catch (JSONException e1) {
			e1.printStackTrace();
		}catch(InterruptedException e) {

		}*/
		while(true) {
			try {
				maintainSockets();
				
				System.out.println("----------------wait on sock----------------------");
				waitOnSockets();
				System.out.println("----------------after wait on sock----------------------");
				//maintain_requests();
				process_pending_sends();

			}catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void waitOnSockets() throws JSONException, ClassCastException, IllegalArgumentException, NullPointerException, RuntimeException, IOException {
		Object[] arr = interfaces.values().toArray();
		for(int i=0;i<arr.length;i++) {
			Interface inter = (Interface)arr[i];
			if(inter.num_requests() > 0)
				inter.send_requests();
		}
		System.out.println("-------------------------"+arr.length);
		for(int i=0;i<arr.length;i++) {
			processResponses(((Interface)arr[i]));
		}
	} 
	private void processResponses(Interface interface1) throws JSONException, ClassCastException, IllegalArgumentException, NullPointerException, RuntimeException, IOException{
		List responses = interface1.get_responses();
		
		Iterator it = responses.iterator();
		while(it.hasNext()) {
			Set callbacks = new HashSet();
			RequetResponseTuple key = (RequetResponseTuple)it.next();
			RequestQueueItem request = key.getRequest();
			JSONObject response = key.getResponse();
			System.out.println("response "+response);
			String k = "";
			if(request != null) {

				Object[]client_req = (Object[])unanswered_requests.get(request.getId());
				unanswered_requests.remove(request.getId());
				if(client_req != null) {
					callbacks = (Set)client_req[2];
				}
				else {
					k = get_index(request.getMethod(),request.getParams());
					callbacks = (Set)subscriptions.get(k);
				}
				response.put("method", request.getMethod());
				JSONArray array = new JSONArray();
				Object[] o = request.getParams();
				for(int i=0;i< o.length;i++) {
					array.put(o[i]);
				}
				response.put("params", array);
				if("blockchain.scripthash.subscribe".equals(request.getMethod())) {
					subscribed_addresses.add(request.getParams()[0]);
				}
			}
			else {
				if(response == null) {  //# Closed remotely / misbehaving
					connection_down(interface1.getServer());
					break;
				}
				String method = response.optString("method");
				JSONArray params = response.optJSONArray("params");
				if(params == null)
					k = method;
				else
					k = method +":"+ params.get(0);
				if("blockchain.headers.subscribe".equals(method)) {
					response.put("result", params.get(0));
					response.put("params", new JSONArray());
				}
				else if("blockchain.scripthash.subscribe".equals(method)) {
					response.put("result", params.get(1));
					response.put("params", new JSONArray().put(params.get(0)));
				}
				callbacks = (Set)subscriptions.get(k);
			}
			if(request!= null && request.getMethod()!= null && request.getMethod().endsWith(".subscribe")){
				sub_cache.put(k, response);
			}
			process_response(interface1, response, callbacks);
		}
	}
	private void process_response(Interface tcpConnection, JSONObject response, Set callbacks) throws JSONException, ClassCastException, IllegalArgumentException, NullPointerException, RuntimeException, IOException {
		String error = response.optString("error");
		String method = response.optString("method");
		if("server.version".equals(method)) {
			JSONArray result = response.optJSONArray("result");
			if(result != null){
				System.out.println("Server Version "+result.getString(1));
			}
		}
		else if("blockchain.headers.subscribe".equals(method)) {
			if(error != null) {
				JSONObject result = response.optJSONObject("result");
				on_notify_header(tcpConnection, result);
			}
		}
		else if("server.peers.subscribe".equals(method)) {
			if(error != null) {
				JSONArray result = response.optJSONArray("result");
				if(result != null) {
					irc_servers = NetworkUtil.parse_servers(result);
				}
				notify("servers");
			}
		}

		else if("blockchain.block.get_chunk".equals(method)) {
			on_get_chunk(tcpConnection, response);
		}
		else if("blockchain.block.get_header".equals(method)) {
			on_get_header(tcpConnection, response);
		}
		/*Iterator it = callbacks.iterator();
		while(it.hasNext()) {
			Callback call = (Callback)it.next();
			call.call(response);
		}*/
	}

	public void setStatus(String status) throws JSONException {
		this.connection_status = status;
		notify("status");
	}
	public void on_get_chunk(Interface interface1,JSONObject response) throws JSONException {
		//Handle receiving a chunk of block headers
		String error = response.optString("error");
		String result = response.optString("result");
		JSONArray params = response.optJSONArray("params");

		if(result == null || params == null || error.length()>0){
			System.out.println("bad response");
			return;
		}
		int index = params.getInt(0);
		if(interface1.getRequest() != index) {
			return;
		}
		boolean connect = interface1.getBlockchain().connect_chunk(index, result);

		if(!connect) {
			connection_down(interface1.getServer());
			return;
		}
		if(interface1.getBlockchain().height() < interface1.getTip()) {
			request_chunk(interface1, index+1);
		}
		else {
			interface1.setRequest(0);
			interface1.setMode("default");

			System.out.println("catch up done" + interface1.getBlockchain().height());
			interface1.getBlockchain().set_catch_up(null);
			context.setLatestBlock(true);
			context.setLatestBlockHeight(interface1.getBlockchain().height());

		}
		notify("updated");
	}

	public void on_get_header(Interface interface1,JSONObject response) throws ClassCastException, IllegalArgumentException, NullPointerException, JSONException, RuntimeException, IOException {
		//Handle receiving a single block header'''
		System.out.println("On get header ");
		JSONObject header = response.optJSONObject("result");
		if(header == null) {
			connection_down(interface1.getServer());
			return;
		}
		int height = header.optInt("block_height");
		if(interface1.getRequest() != height){
			System.out.println("unsolicited header "+interface1.getRequest()+" "+height);
			connection_down(interface1.getServer());
			return;
		}
		Blockchain chain = BlockchainsUtil.check_header(header);
		int next_height = 0;
		if("backward".equals(interface1.getMode())) {
			if(chain != null) {
				System.out.println("binary search");
				interface1.setMode("binary");
				interface1.setBlockchain(chain);
				interface1.setGood(height);
				next_height = (interface1.getBad() + interface1.getGood()) / 2;

			}
			else {
				if(height == 0) {
					connection_down(interface1.getServer());
					next_height = 0;
				}
				else {
					interface1.setBad(height);
					interface1.setBad_header(header);
					int delta = interface1.getTip() - height;
					next_height = Math.max(0, interface1.getTip() - 2 * delta);
				}
			}
		}
		else if("binary".equals(interface1.getMode())) {
			if(chain != null) {
				interface1.setGood(height);
				interface1.setBlockchain(chain);
			}
			else {
				interface1.setBad(height);
				interface1.setBad_header(header);
			}
			System.out.println("good and bad "+interface1.getBad()+" "+interface1.getGood());
			if(interface1.getBad() != interface1.getGood() + 1) {
				next_height = (interface1.getBad() + interface1.getGood()) / 2;
			}
			else if(!interface1.getBlockchain().can_connect(interface1.getBad_header(), false)){
				connection_down(interface1.getServer());
				next_height = 0;
			}
			else {

				Blockchain branch = (Blockchain)blockchains.get(new Integer(interface1.getBad()));
				if(branch  != null){
					if(branch.check_header(interface1.getBad_header())) {
						System.out.println("joining chain" + interface1.getBad());
						next_height = 0;
					}
					else if(branch.parent().check_header(header)) {
						interface1.setBlockchain(branch.parent());
						next_height = 0;
					}
					else{
						System.out.println("checkpoint conflicts with existing fork"+ branch.getPath());
						// branch.write('', 0);
						branch.save_header(interface1.getBad_header());
						interface1.setMode("catch_up");
						interface1.setBlockchain(branch);
						next_height = interface1.getBad()+1;
						interface1.getBlockchain().set_catch_up(interface1.getServer());
					}
				}
				else {
					int bh = interface1.getBlockchain().height();
					next_height = 0;
					if(bh > interface1.getGood()){
						if(interface1.getBlockchain().check_header(interface1.getBad_header())){
							Blockchain b = interface1.getBlockchain().fork(interface1.getBad_header());
							blockchains.put(new Integer(interface1.getBad()),b);
							interface1.setBlockchain(b);
							System.out.println("new chain "+b.get_checkpoint());
							interface1.setMode("catch_up");
							next_height = interface1.getBad() + 1;
							interface1.getBlockchain().set_catch_up(interface1.getServer());
						}
					}
					else
					{
						if(interface1.getBlockchain().get_catch_up() == null && bh < interface1.getTip()) {
							System.out.println("catching up from "+ (bh + 1));
							interface1.setMode("catch_up");
							next_height = bh + 1;
							interface1.getBlockchain().set_catch_up(interface1.getServer());
						}
					}
				}
				notify("updated");
			}
		}
		else if("catch_up".equals(interface1.getMode())){
			boolean can_connect = interface1.getBlockchain().can_connect(header,true);
			if(can_connect) {
				interface1.getBlockchain().save_header(header);
				if(height < interface1.getTip()) {
					next_height = height + 1;
				}
				else {
					next_height = 0;
				}
			}
			else {
				//go back
				System.out.println("cannot connect " + height);
				interface1.setMode("backward");
				interface1.setBad(height);
				interface1.setBad_header(header);
				next_height =height - 1;
			}
			if(next_height == 0) {
				// exit catch_up state
				System.out.println("catch up done "+ interface1.getBlockchain().height());
				interface1.getBlockchain().set_catch_up(null);
				switch_lagging_interface();
				notify("updated");
				context.setLatestBlock(true);
			}
		}
		else {
			try {
				throw new Exception("Exception "+interface1.getMode());
			}catch(Exception e) {
				e.printStackTrace();
			}
		}

		//If not finished, get the next header
		if(next_height != 0){
			if(interface1.getMode().equals("catch_up") && interface1.getTip() > next_height + 50) {
				request_chunk(interface1, next_height / 2016);
			}
			else {

				request_header(interface1, next_height);
			}
		}
		else {
			interface1.setMode("default");
			interface1.setRequest(0);
			notify("updated");
		}
		//refresh network dialog
		notify("interfaces");
		Synchronization.getScriptHashAddresses(this);
	}
	private void request_chunk(Interface connection, int idx) {
		queue_request("blockchain.block.get_chunk", new Integer[] {new Integer(idx)}, connection);
		connection.setRequest(idx);
		connection.setRequest_time(date.getTime());
	}
	private void notify(String key) throws JSONException {
		if("status".equals(key)) {

		}else if("updated".equals(key)) {


		}
		else {
			trigger_callback(key , get_status_value(key));
		}

	}

	private void trigger_callback(String key, Object get_status_value) {


	}
	public Object get_status_value(String key) throws JSONException {
		if("status".equals(key)) {
			return connection_status;
		}
		else if("updated".equals(key)) {
			return new LocalServerTuple(get_local_height(),get_server_height());
		}
		else if("servers".equals(key)) { 
			return get_servers();
		}
		else if("interfaces".equals(key)) {
			return get_interfaces();
		}
		return null;
	}

	private Set get_interfaces() {
		return interfaces.keySet();
	}
	public void on_notify_header(Interface interface1 ,JSONObject header) throws JSONException, IOException {
		int height = header.optInt("block_height");
		if( height == 0)
			return;
		interface1.setTip_header(header);
		interface1.setTip(height);

		if(!"default".equals(interface1.getMode())){
			return;
		}

		Blockchain b = BlockchainsUtil.check_header(header);
		if(b != null) {
			interface1.setBlockchain(b);
			switch_lagging_interface();
			notify("updated");
			notify("interfaces");
			return;
		}
		b = BlockchainsUtil.can_connect(header);


		if(b != null) {
			interface1.setBlockchain(b);
			b.save_header(header);
			switch_lagging_interface();
			notify("updated");
			notify("interfaces");
			return;
		}
		Object[] obj =blockchains.values().toArray();
		int tip = Integer.MIN_VALUE;
		for(int i=0;i<obj.length;i++) {
			int tmp = ((Blockchain)obj[i]).height(); 
			if(tip < tmp) {
				tip = tmp;
			}
		}
		if(tip >=0) {
			interface1.setMode("backward");
			interface1.setBad(height);
			interface1.setBad_header(header);
			request_header(interface1, Math.min(tip, height - 1));
		}
		else {
			Blockchain chain = (Blockchain) blockchains.get(new Integer(0));
			System.out.println("catch up "+chain.get_catch_up());
			if(chain.get_catch_up() == null) {
				chain.set_catch_up(interface1); 
				interface1.setMode("catch_up");
				interface1.setBlockchain(chain);
				request_header(interface1, 0);
			}
		}
	}
	private boolean server_is_lagging() {
		int sh = get_server_height();
		if(sh ==0) {
			System.out.println("no height for main interface");
			return true;
		}
		int lh = get_local_height();
		boolean result = (lh - sh) > 1;
		if(result) {
			System.out.println(default_server +" is lagging "+sh+" vs "+lh);
		}
		return result;
	}
	public int get_local_height() {
		return blockchain().height();
	}
	public int get_server_height() {
		if(interfaceObj != null)
			return interfaceObj.getTip();
		else 
			return 0;
	}
	public Blockchain blockchain() {
		System.out.println(interfaceObj);
		System.out.println(interfaceObj.getBlockchain());
		if(interfaceObj !=null && interfaceObj.getBlockchain() != null) {
			blockchain_index = interfaceObj.getBlockchain().get_checkpoint();
		}
		System.out.println(blockchain_index);
		return (Blockchain)blockchains.get(blockchain_index);
	}
	private void switch_lagging_interface() throws JSONException {
		if(server_is_lagging()) {
			//switch to one that has the correct header (not height)
			JSONObject header = blockchain().read_header(get_local_height());
			System.out.println("header "+header);
			Iterator keys = interfaces.keySet().iterator();
			Set filtered = new HashSet();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				Interface i = (Interface)interfaces.get(key);
				if(i.getTip_header().equals(header)) {
					filtered.add(key);
				}	
			}
			if(filtered.size()>0) {
				int random = new Random().nextInt(filtered.size());
				Server choice = (Server)filtered.toArray()[random];
				switch_to_interface(choice);
			}
		}
	}

	private void connection_down(Server server) throws JSONException {
		disconnected_server.add(server);
		if(server.equals(default_server)) {
			setStatus("disconnected");
			this.interfaceObj = null;
		}
		if(interfaces.containsKey(server)) {
			//close_interface((Interface)interfaces.get(server));
			interfaces.remove(server);
			System.out.println("removed "+server.toString());
			notify("interfaces");
		}
		Iterator it = blockchains.values().iterator();
		while (it.hasNext()) {
			Blockchain b = (Blockchain) it.next();
			if(server.equals(b.get_catch_up())) {
				b.set_catch_up(null);
			}
		}
	}

	public String get_index(String method,Object[] params) {
		//   """ hashable index for subscriptions and cache"""
		if(params.length==0) {
			return method;
		}
		else {
			return method+":"+params[0].toString();
		}
	}

	/*private void maintain_requests() throws JSONException {
		Iterator it = interfaces.values().iterator();
		while (it.hasNext()) {
			Interface interface1 = (Interface) it.next();
			if(interface1.getRequest() != 0 && (date.getTime() - interface1.getRequest_time()) > 20) {
				System.out.println("blockchain request timed out");
				connection_down(interface1.getServer());
			}
		}
	}*/

	private void maintainSockets() throws JSONException {
		while(!socketQueue.isEmpty()) {
			ServerSocketTuple tuple = (ServerSocketTuple)socketQueue.remove();
			Server server = tuple.getServer();
			TcpConnection socket = tuple.getSocket();
			if(connecting.contains(server))
				connecting.remove(server);

			if(tuple.getSocket() != null) {
				new_interface(server,socket);
			}
			else {
				System.out.println("socket is null");
				connection_down(server);
			}
		}
		/*Iterator it = interfaces.values().iterator();
		while (it.hasNext()) {
			Interface type = (Interface) it.next();
			if(type.has_timed_out()) {
				System.out.println("times out");
				connection_down(type.getServer());
			}
		}*/

		if((interfaces.size() + connecting.size()) < num_servers) {
			start_random_interface();
		}
		if(tcpConnection == null) {
			if(!"connecting".equals(connection_status)) {
				Set servers = interfaces.keySet();
				if(servers.contains(default_server)) {
					servers.remove(default_server);
				}
				if(servers.size() >0) {
					int size = servers.size();
					switch_to_interface((Server)servers.toArray()[random.nextInt(size)]);
				}
			}
		}
	}

	private Set pending_sends = new HashSet();
	public void process_pending_sends() {
		// Requests needs connectivity.  If we don't have an interface,
		// we cannot process them.
		if(tcpConnection == null)
			return;

		synchronized(pending_sends) {
			Set sends = pending_sends;
			pending_sends = new HashSet();
			Iterator it = sends.iterator();
			while(it.hasNext()) {
				MessagesCallbackTuple mcTuple = (MessagesCallbackTuple)it.next();
				Iterator it1 = mcTuple.getMethodParamsTuple().iterator();
				while(it1.hasNext()) {
					MethodParamsTuple mptuple = (MethodParamsTuple)it1.next();
					JSONObject r =null;
					if(mptuple.getMethod().endsWith(".subscribe")) {
						String k = get_index(mptuple.getMethod(), mptuple.getParams());
						//add callback to list
						Set callbacks = (Set)subscriptions.get(k);
						if(!callbacks.contains(mcTuple.getCallback())) {
							callbacks.add(mcTuple.getCallback());
						}
						subscriptions.put(k,callbacks);
						//check cached response for subscriptions
						r = (JSONObject)sub_cache.get(k);
					}
					if(r != null) {
						mcTuple.getCallback().call(r);
					}
					else {
						message_id = queue_request(mptuple.getMethod(), mptuple.getParams(), null);
						unanswered_requests.put(new Integer(message_id), new Object[] {mptuple.getMethod(), mptuple.getParams(), mcTuple.getCallback()});
					}
				}
			}
		}
	}

	private void add_recent_server(String server) {
		try {
			JSONArray array = new JSONArray();
			for(int i=0;i<recent_servers.length();i++) {
				String item = recent_servers.getString(i);
				if(!server.equals(item)) {
					array.put(item);
				}
			}
			recent_servers.put(0, server);
			for(int i=0;i<array.length();i++) {
				recent_servers.put(i+1, array.getString(i));
			}
			saveRecent();
		}catch(JSONException je) {
			je.printStackTrace();
		}
	}

	public void saveRecent() {
		Files.write(recent_servers, "recent-servers");
	}

	private void new_interface(Server server, TcpConnection socket) throws JSONException {
		add_recent_server(server.toString());
		Interface interface1 = new Interface(server, socket);
		interface1.setTip(0);
		interface1.setBlockchain(null);
		interface1.setMode("default");
		interface1.setTip_header(null);
		interface1.setRequest(0);
		interface1.setServer(server);
		interfaces.put(server, interface1);
		queue_request("blockchain.headers.subscribe",new String[] {} , interface1);
		if(server.equals(default_server)) {
			switch_to_interface(server);
		}
	}

	private void switch_to_interface(Server server) throws JSONException {
		default_server = server;
		if(!interfaces.containsKey(server)) {
			interfaceObj = null;
			startInterface(server);
			return ;
		}
		Interface i = (Interface)interfaces.get(server);
		if(interfaceObj != i) {
			interfaceObj = i;
			send_subscriptions();
			setStatus("connected");
			notify("updated");
		}
	}

	private void send_subscriptions() {
		System.out.println("sending subscriptions to " + interfaceObj.getServer().toString() +" "+ unanswered_requests.size()  +" "+  subscribed_addresses.size());
		sub_cache.clear();
		Object[] requests = unanswered_requests.values().toArray();
		unanswered_requests = new HashMap();
		for(int i=0;i<requests.length;i++) {
			Object[] request = (Object[])requests[i];
			int message_id = queue_request((String)request[0], (String[])request[1], null);
			unanswered_requests.put(new Integer(message_id),request);
		}
		queue_request("server.peers.subscribe", new String[] {}, null);
		Iterator it = subscribed_addresses.iterator();
		while (it.hasNext()) {
			String type = (String) it.next();
			queue_request("blockchain.scripthash.subscribe", new String[] {type}, null);
		}
	}

	private void request_header(Interface connection, int height) {

		queue_request("blockchain.block.get_header", new Integer[] {new Integer(height)}, connection);
		connection.setRequest(height);
		connection.setRequest_time(date.getTime());

	}

	public int queue_request(String method,Object[] params,Interface interface1) {
		// If you want to queue a request on any interface it must go
		// through this function so message ids are properly tracked
		if(interface1 == null)
			interface1 = this.interfaceObj;
		interface1.queue_request(method, params, message_id);
		return ++message_id;
	}
	public void setDownloadingHeaders(boolean b) {
		this.downloadingHeaders = b;

	}
	public boolean isDownloadingHeaders() {
		return downloadingHeaders;
	}


}
