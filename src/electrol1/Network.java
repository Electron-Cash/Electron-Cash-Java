package electrol1;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.INetwork;
import electrol.Version;
import electrol.java.util.ArrayList;
import electrol.java.util.HashMap;
import electrol.java.util.HashSet;
import electrol.java.util.Iterator;
import electrol.java.util.List;
import electrol.java.util.Map;
import electrol.java.util.Queue;
import electrol.java.util.Set;
import electrol.util.BitcoinHeadersDownload;
import electrol.util.BitcoinMainnet;
import electrol.util.Files;
import electrol.util.Server;
import electrol.util.StringUtils;



public class Network extends Thread implements INetwork{
	private static int[] FEE_TARGETS = new int[] {25, 10, 5, 2};
	private static int COIN = 100000000;

	private Set disconnectedServer;
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
	private int message_id;
	private Date date;
	private Map sub_cache = new HashMap();
	private Map unanswered_requests = new HashMap();
	private Set subscribed_addresses = new HashSet();
	private Config config;
	private Integer blockchain_index;
	private Map subscriptions = new HashMap();

	private String banner;

	private String donation_address;
	private int relay_fee;
	private String connection_status;

	public Network(){
		blockchains = BlockchainsUtil.read_blockchain();
		connecting = new HashSet();
		disconnectedServer = new HashSet();
		random = new Random();
		date = new Date();
		config = new Config();
		try {
			recent_servers = read_recent_servers();
			default_server = pickRandomServer(null);
			startNetwork(default_server.getProtocol(),default_server,10);
		}catch(Exception e) {
			e.printStackTrace();
		}


	}
	public void startNetwork(String protocol,Server default_server,int num_servers) throws JSONException {
		disconnectedServer = new HashSet();
		this.protocol = protocol;
		startInterfaces(default_server,num_servers);
	}


	private void startInterfaces(Server defaultServer, int num_servers) throws JSONException {
		startInterface(defaultServer);
		for(int i=0;i<num_servers-1;i++) {
			start_random_interface();
		}

	}
	private void start_random_interface() throws JSONException{
		JSONObject servers = get_servers();
		Enumeration enumeration = servers.keys();
		while (enumeration.hasMoreElements()) {
			String key = (String) enumeration.nextElement();
			if (disconnectedServer.contains(key) || interfaces.containsKey(key)) {
				servers.remove(key);
			}
		}
		Server server = pickRandomServer(servers);
		if(server != null) {
			startInterface(server);
		}
	}
	private void startInterface(Server defaultServer) {
		if(!connecting.contains(defaultServer) && !interfaces.containsKey(defaultServer)) {
			if(defaultServer.equals(default_server)) {
				System.out.println("connecting to "+defaultServer.toString()+" as new interface");
			}
			connecting.add(defaultServer);
		}
		new Connection(defaultServer,socketQueue);
	}
	public static Server deserialize_server(String server_str){
		String[] split = StringUtils.split(server_str,":");
		String host = split[0];
		int port = 0;
		String protocol = split[2];
		try {
			port = Integer.parseInt(split[1]);
			if(!(protocol.equals("s") || protocol.equals("t"))) {
				throw new Exception("Invalid protocol");
			}
		}
		catch(NumberFormatException nme) {
			throw new NumberFormatException();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		return new Server(host, port, protocol);
	}
	private JSONObject get_servers() throws JSONException {
		JSONObject out = BitcoinMainnet.getDefaultServers();
		if(irc_servers.length() > 0) {
			filterVersion(irc_servers);
		}
		else {
			JSONArray servers = read_recent_servers();
			for(int i=0;i<servers.length();i++) {
				Server server = deserialize_server(servers.getString(i));
				if(!out.has(server.getHost())) {
					JSONObject value = new JSONObject();
					value.put(server.getProtocol(), server.getPort());
					out.put(server.getHost(), value);
				}

			}
		}
		return out;
	}
	public JSONObject parse_servers(JSONArray result) throws JSONException {
		JSONObject servers = new JSONObject();
		for(int i=0;i< result.length();i++) {
			JSONObject out = new JSONObject();
			JSONArray array = result.getJSONArray(i);
			String host = array.getString(1);
			String pruning_level="-";
			String version="";
			if(array.length() > 2) {
				JSONArray child = array.getJSONArray(2);
				for(int j=0; j<child.length() ;j++) {
					String item = child.getString(j);
					if(item.startsWith("s") || item.startsWith("t")) {
						String protocol = item.substring(0, 1);
						String port = item.substring(1);
						if(port.trim().equals("")) {
							port = BitcoinMainnet.getDefaultPorts().get(protocol).toString();
						}
						out.put(protocol, port);
					}
					else if(item.startsWith("v")) {
						version = item.substring(1);
					}
					else if(item.startsWith("p")) {
						pruning_level= item.substring(1);
						if(pruning_level.trim().equals("")) {
							pruning_level = "0";
						}

					}
				}
			}
			out.put("pruning",pruning_level);
			out.put("version",version);
			servers.put(host, out);
		}
		return servers;
	}

	private void filterVersion(JSONObject irc_servers2) {
		// TODO Auto-generated method stub

	}
	public Server pickRandomServer(JSONObject default_servers) throws JSONException {
		if(default_servers == null) {
			default_servers = BitcoinMainnet.getDefaultServers();
		}

		List filter = new ArrayList();
		Enumeration enumeration = default_servers.keys();
		while(enumeration.hasMoreElements()) {
			String server = (String)enumeration.nextElement();
			JSONObject item = default_servers.getJSONObject(server);
			if(item.has(default_protocol)) {
				int port = item.getInt(default_protocol);
				filter.add(new Server(server, port, default_protocol));
			}
		}
		return (Server)filter.get(random.nextInt(filter.size()));
	}

	public JSONArray read_recent_servers() {
		try {
			String json = Files.read("recent-servers");
			if(json != null)
				return new JSONArray(json);
		}
		catch(JSONException jsonEx) {
			jsonEx.printStackTrace();
		} 
		return new JSONArray();
	}
	public void init_header_file() throws JSONException {
		Blockchain b = (Blockchain)blockchains.get(new Integer(0));
		if(b.get_hash(0).equals(BitcoinMainnet.GENESIS)) {
			setDownloadingHeaders(false);
			return;
		}

		String filename = b.getPath();
		setDownloadingHeaders(true);
		BitcoinHeadersDownload download = new BitcoinHeadersDownload(filename, this);
		download.start();
	}

	public void run() {

		while(true) {
			try {
				init_header_file();
				while(isDownloadingHeaders()) {
					System.out.println("wait for header download");
					try {
						Thread.sleep(1000);
					}catch (Exception e) {}
				}
				maintainSockets();
				waitOnSockets();

			}catch(Exception e) {
				e.printStackTrace();
			}
		}


	}

	private void waitOnSockets() throws JSONException, ClassCastException, IllegalArgumentException, NullPointerException, RuntimeException, IOException {
		Iterator it = interfaces.values().iterator();
		while (it.hasNext()) {
			TcpConnection type = (TcpConnection) it.next();
			type.send_requests();
			processResponses(type);

		}
	}
	private void processResponses(TcpConnection connection) throws JSONException, ClassCastException, IllegalArgumentException, NullPointerException, RuntimeException, IOException{
		List responses = connection.get_responses();
		System.out.println("responses size "+responses.size());
		Iterator it = responses.iterator();
		while(it.hasNext()) {
			Set callbacks = new HashSet();
			RequetResponseTuple key = (RequetResponseTuple)it.next();
			RequestQueueItem request = key.getRequest();
			JSONObject response = key.getResponse();
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
					connection_down(connection.getServer());
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
			process_response(tcpConnection, response, callbacks);
		}
	}
	private void process_response(TcpConnection tcpConnection, JSONObject response, Set callbacks) throws JSONException, ClassCastException, IllegalArgumentException, NullPointerException, RuntimeException, IOException {
		String error = response.optString("error");
		String method = response.optString("method");
		JSONArray params = response.optJSONArray("params");

		// We handle some responses; return the rest to the client.
		if("server.version".equals(method)) {
			JSONArray result = response.optJSONArray("result");
			if(result != null){
				tcpConnection.setServer_version(result.getString(1));
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
					irc_servers = parse_servers(result);
				}
				notify("servers");
			}
		}
		else if("server.banner".equals(method)) {
			if(error != null) {
				String result = response.optString("result");
				if(result != null)
					banner = result;
				notify("banner");
			}
		}
		else if("server.donation_address".equals(method)) {
			if(error != null) {
				String result = response.optString("result");
				if(result != null)
					donation_address = result;
			}
		}
		else if("blockchain.estimatefee".equals(method)) {
			if(error != null) {
				String result = response.optString("result");
				if(result != null && result.length() > 0) {
					String i = params.optString(0);
					double d = Double.parseDouble(result) * COIN;
					int fee = (int)d;
					config.update_fee_estimates(i, new Integer(fee));
				}
			}
		}
		else if("blockchain.relayfee".equals(method)) {
			if(error != null) {
				String result = response.optString("result");
				if(result != null && result.length() > 0) {
					double d = Double.parseDouble(result) * COIN;
					relay_fee = (int)d;
				}
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
	public void on_get_chunk(TcpConnection connection,JSONObject response) throws JSONException {
		//Handle receiving a chunk of block headers
		String error = response.optString("error");
		String result = response.optString("result");
		JSONArray params = response.optJSONArray("params");
		if(result == null || params == null || error != null){
			System.out.println("bad response");
			return;
		}
		int index = params.getInt(0);
		if(connection.getRequest().intValue() != index) {
			return;
		}
		boolean connect = connection.getBlockchain().connect_chunk(index, result);
		if(!connect) {
			connection_down(connection.getServer());
			return;
		}
		if(connection.getBlockchain().height() < connection.getTip()) {
			request_chunk(connection, new Integer(index+1));
		}
		else {
			connection.setRequest(new Integer(0));
			connection.setMode("default");
			System.out.println("catch up done" + connection.getBlockchain().height());
			connection.getBlockchain().set_catch_up(null);
		}
		notify("updated");
	}

	public void on_get_header(TcpConnection connection,JSONObject response) throws ClassCastException, IllegalArgumentException, NullPointerException, JSONException, RuntimeException, IOException {
		//Handle receiving a single block header'''
		JSONObject header = response.optJSONObject("result");
		System.out.println("get header "+header);
		if(header == null) {
			System.out.println("response "+response);
			connection_down(connection.getServer());
			return;
		}
		int height = header.optInt("block_height");
		if(connection.getRequest().intValue() != height){
			System.out.println("unsolicited header "+connection.getRequest()+" "+height);
			connection_down(connection.getServer());
			return;
		}
		System.out.println("connection mode "+connection.getMode()+" "+header);
		Blockchain chain = BlockchainsUtil.check_header(blockchains,header);
		int next_height = 0;
		if("backward".equals(connection.getMode())) {
			if(chain != null) {
				System.out.println("binary search");
				connection.setMode("binary");
				connection.setBlockchain(chain);
				connection.setGood(height);
				next_height = (connection.getBad() + connection.getGood()) / 2;
			}
			else {
				if(height == 0) {
					connection_down(connection.getServer());
					next_height = 0;
				}
				else {
					tcpConnection.setBad(height);
					tcpConnection.setBad_header(header);
					int delta = tcpConnection.getTip() - height;
					next_height = Math.max(0, connection.getTip() - 2 * delta);
				}
			}
		}
		else if("binary".equals(connection.getMode())) {
			if(chain != null) {
				connection.setGood(height);
				connection.setBlockchain(chain);
			}
			else {
				connection.setBad(height);
				connection.setBad_header(header);
			}
			if(connection.getBad() != connection.getGood() + 1) {
				next_height = (connection.getBad() + connection.getGood()) / 2;
			}
			else if(!connection.getBlockchain().can_connect(connection.getBad_header(), false)){
				connection_down(connection.getServer());
				next_height = 0;
			}
			else {
				Blockchain branch = (Blockchain)blockchains.get(new Integer(connection.getBad()));
				if(branch  != null){
					if(branch.check_header(connection.getBad_header())) {
						System.out.println("joining chain" + connection.getBad());
						next_height = 0;
					}
					else if(branch.parent().check_header(header)) {
						System.out.println("reorg" + connection.getBad() +" "+ connection.getTip());
						connection.setBlockchain(branch.parent());
						next_height = 0;
					}
					else{
						System.out.println("checkpoint conflicts with existing fork"+ branch.getPath());
						// branch.write('', 0);
						branch.save_header(connection.getBad_header());
						connection.setMode("catch_up");
						connection.setBlockchain(branch);
						next_height = connection.getBad()+1;
						connection.getBlockchain().set_catch_up(connection.getServer());
					}
				}
				else {
					int bh = connection.getBlockchain().height();
					next_height = 0;
					if(bh > connection.getGood()){
						if(connection.getBlockchain().check_header(connection.getBad_header())){
							Blockchain parent =  connection.getBlockchain();
							Blockchain b = connection.getBlockchain().fork(parent,connection.getBad_header());
							blockchains.put(new Integer(connection.getBad()),b);
							connection.setBlockchain(b);
							System.out.println("new chain "+b.get_checkpoint());
							connection.setMode("catch_up");
							next_height = connection.getBad() + 1;
							connection.getBlockchain().set_catch_up(connection.getServer());
						}
					}
					else
					{
						//assert bh == inter.getGood();
						if(connection.getBlockchain().get_catch_up() == null && bh < connection.getTip()) {
							System.out.println("catching up from "+ (bh + 1));
							connection.setMode("catch_up");
							next_height = connection.getBad() + 1;
							connection.getBlockchain().set_catch_up(connection.getServer());
						}
					}
				}
				notify("updated");
			}
		}
		else if("catch_up".equals(connection.getMode())){
			Blockchain can_connect = connection.getBlockchain().can_connect(header);
			if(can_connect != null) {
				connection.getBlockchain().save_header(header);
				if(height < connection.getTip()) {
					next_height = height + 1;
				}
				else {
					next_height = 0;
				}
			}
			else {
				//go back
				System.out.println("cannot connect " + height);
				connection.setMode("backward");
				connection.setBad(height);
				connection.setBad_header(header);
				next_height =height - 1;
			}
			if(next_height == 0) {
				// exit catch_up state
				System.out.println("catch up done "+ connection.getBlockchain().height());
				connection.getBlockchain().set_catch_up(null);
				switch_lagging_interface();
				notify("updated");
			}
		}
		else {
			try {
				throw new Exception("Exception "+connection.getMode());
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("next height "+next_height);
		//If not finished, get the next header
		if(next_height != 0){
			if(connection.getMode().equals("catch_up") && connection.getTip() > next_height + 50)
				request_chunk(connection, new Integer(next_height / 2016));
			else {
				request_header(connection, new Integer(next_height));
			}
		}
		else {
			connection.setMode("default");
			connection.setRequest(null);
			notify("updated");
		}
		//refresh network dialog
		notify("interfaces");
	}
	private void request_chunk(TcpConnection connection, Integer idx) {
		System.out.println("requesting chunk " + idx);
		queue_request("blockchain.block.get_chunk", new Integer[] {idx}, connection);
		connection.setRequest(idx);
		connection.setRequestTime(date.getTime());
	}
	private void notify(String key) throws JSONException {
		if("status".equals(key) || "updated".equals(key)) {
			trigger_callback(key, null);
		}
		else {
			trigger_callback(key , get_status_value(key));
		}

	}

	private void trigger_callback(String key, Object get_status_value) {
		// TODO Auto-generated method stub

	}
	public Object get_status_value(String key) throws JSONException {
		if("status".equals(key)) {
			return connection_status;
		}
		else if("banner".equals(key)) {
			return banner;
		}
		else if("fee".equals(key)) {
			return config.get_fee_estimates();
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
	public void on_notify_header(TcpConnection tcpConnection ,JSONObject header) throws JSONException, IOException {
		int height = header.optInt("block_height");
		if( height == 0)
			return;
		tcpConnection.setTip_header(header);
		tcpConnection.setTip(height);
		
		if(!"default".equals(tcpConnection.getMode())){
			return;
		}
		Blockchain b = BlockchainsUtil.check_header(blockchains,header);
		
		if(b != null) {
			tcpConnection.setBlockchain(b);
			switch_lagging_interface();
			notify("updated");
			notify("interfaces");
			return;
		}
		b = BlockchainsUtil.can_connect(blockchains,header);
		if(b != null) {
			tcpConnection.setBlockchain(b);
			b.save_header(header);
			switch_lagging_interface();
			notify("updated");
			notify("interfaces");
			return;
		}
		Object[] obj =blockchains.values().toArray();
		int max = Integer.MAX_VALUE;
		for(int i=0;i<obj.length;i++) {
			int tmp = ((Blockchain)obj[i]).height(); 
			if(max > tmp) {
				max = tmp;
			}
		}
		int tip = max;
		if(tip >=0) {
			tcpConnection.setMode("backward");
			tcpConnection.setBad(height);
			tcpConnection.setBad_header(header);
			request_header(tcpConnection, new Integer(Math.min(tip, new Integer(height - 1).intValue())));
		}
		else {
			Blockchain chain = (Blockchain) blockchains.get(new Integer(0));
			if(chain.get_catch_up() == null) {
				chain.set_catch_up(tcpConnection); 
				tcpConnection.setMode("catch_up");
				tcpConnection.setBlockchain(chain);
				request_header(tcpConnection, new Integer(0));
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
	private int get_server_height() {
		if(tcpConnection != null)
			return tcpConnection.getTip();
		else 
			return 0;
	}
	public Blockchain blockchain() {
		if(tcpConnection !=null && tcpConnection.getBlockchain() != null) {
			blockchain_index = tcpConnection.getBlockchain().get_checkpoint();
		}
		return (Blockchain)blockchains.get(blockchain_index);
	}
	private void switch_lagging_interface() throws JSONException {
		if(server_is_lagging()) {
			//switch to one that has the correct header (not height)
			JSONObject header = blockchain().read_header(get_local_height());
			Iterator keys = interfaces.keySet().iterator();
			Set filtered = new HashSet();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				TcpConnection i = (TcpConnection)interfaces.get(key);
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
		disconnectedServer.add(server);
		if(server.equals(default_server)) {
			setStatus("disconnected");
		}
		if(interfaces.containsKey(server)) {
			close_interface((TcpConnection)interfaces.get(server));
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

	private void close_interface(TcpConnection tcpConnection) {
		if(tcpConnection != null) {
			if(interfaces.containsKey(tcpConnection.getServer())) {
				interfaces.remove(tcpConnection.getServer());
			}
			if(tcpConnection.getServer().equals(default_server)) {
				this.tcpConnection = null;
			}
			tcpConnection.close();
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

	private void maintainSockets() throws JSONException {
		while(!socketQueue.isEmpty()) {
			ServerSocketTuple tuple = (ServerSocketTuple)socketQueue.remove();
			if(tuple.getServer() != null)
				connecting.remove(tuple.getServer());
			if(tuple.getSocket() != null) {
				new_interface(tuple);
			}
			else {
				connection_down(tuple.getServer());
			}
		}
		Iterator it = interfaces.values().iterator();
		while (it.hasNext()) {
			TcpConnection type = (TcpConnection) it.next();
			if(type.ping_required()) {
				queue_request("server.version", new String[] {Version.PACKAGE_VERSION,Version.PROTOCOL_VERSION}, type);
			}

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
		else {
			if(config.is_fee_estimates_update_required()) {
				request_fee_estimates();
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

	private void new_interface(ServerSocketTuple tuple) throws JSONException {
		TcpConnection connection = tuple.getSocket();
		Server server = tuple.getServer();
		add_recent_server(server.toString());
		connection.setTip(0);
		connection.setBlockchain(null);
		connection.setMode("default");
		connection.setTip_header(null);
		connection.setRequest(new Integer(0));
		interfaces.put(server, connection);
		queue_request("blockchain.headers.subscribe",new String[] {} , connection);
		if(server.equals(default_server)) {
			switch_to_interface(server);
		}
	}

	private void switch_to_interface(Server server) throws JSONException {
		default_server = server;
		if(!interfaces.containsKey(server)) {
			tcpConnection = null;
			startInterface(server);
			return ;
		}
		TcpConnection i = (TcpConnection)interfaces.get(server);
		if(tcpConnection != i) {
			tcpConnection = i;
			send_subscriptions();
			setStatus("connected");
			notify("updated");
		}
	}

	private void send_subscriptions() {
		System.out.println("sending subscriptions to " + tcpConnection.getServer().toString() +" "+ unanswered_requests.size()  +" "+  subscribed_addresses.size());
		sub_cache.clear();
		Object[] requests = unanswered_requests.values().toArray();
		unanswered_requests = new HashMap();
		if(tcpConnection.ping_required()) {
			queue_request("server.version", new String[]{Version.PACKAGE_VERSION,Version.PROTOCOL_VERSION}, tcpConnection);
		}
		for(int i=0;i<requests.length;i++) {
			Object[] request = (Object[])requests[i];
			int message_id = queue_request((String)request[0], (String[])request[1], null);
			unanswered_requests.put(new Integer(message_id),request);
		}
		queue_request("server.banner", new String[] {}, null);
		queue_request("server.donation_address", new String[] {}, null);
		queue_request("server.peers.subscribe", new String[] {}, null);
		request_fee_estimates();
		queue_request("blockchain.relayfee", new String[] {}, null);
		Iterator it = subscribed_addresses.iterator();
		while (it.hasNext()) {
			String type = (String) it.next();
			queue_request("blockchain.scripthash.subscribe", new String[] {type}, null);
		}
	}
	private void request_fee_estimates() {
		config.requested_fee_estimates();
		for(int i=0;i<FEE_TARGETS.length;i++)
			queue_request("blockchain.estimatefee", new Integer[] {new Integer(i)}, null);
	}
	private void request_header(TcpConnection connection, Integer height) {
		queue_request("blockchain.block.get_header", new Integer[] {height}, connection);
		connection.setRequest(height);
		connection.setRequestTime(date.getTime());
	}

	public int queue_request(String method,Object[] params,TcpConnection tcpConnection) {
		// If you want to queue a request on any interface it must go
		// through this function so message ids are properly tracked
		if(tcpConnection == null)
			tcpConnection = this.tcpConnection;
		int message_id = this.message_id;
		this.message_id += 1;
		tcpConnection.queue_request(method, params, message_id);
		return message_id;
	}

	public void setDownloadingHeaders(boolean b) {
		this.downloadingHeaders = b;

	}
	public boolean isDownloadingHeaders() {
		return downloadingHeaders;
	}


}
