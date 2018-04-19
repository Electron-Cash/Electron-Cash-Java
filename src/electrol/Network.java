package electrol;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.java.util.HashMap;
import electrol.java.util.HashSet;
import electrol.java.util.Iterator;
import electrol.java.util.Map;
import electrol.java.util.Queue;
import electrol.java.util.Set;
import electrol.util.BitcoinHeadersDownload;
import electrol.util.BitcoinMainnet;
import electrol.util.BlockchainsUtil;
import electrol.util.Config;
import electrol.util.Constants;
import electrol.util.Files;
import electrol.util.Server;
import electrol.util.StringUtils;

public class Network extends Thread implements INetwork{
	
	private static Integer blockchain_index;
	private static int NODES_RETRY_INTERVAL = 60;
	private static	int	SERVER_RETRY_INTERVAL = 10;
	private static int COIN = 100000000;
	private static int[] FEE_TARGETS = new int[] {25, 10, 5, 2};
	
	private String default_server = "";
	private JSONObject pending_sends = new JSONObject();
	private int message_id = 0;
	private static Map irc_servers = new HashMap(); //returned by interface (list from irc)
	private String banner = "";
	private String donation_address = "";
	private long relay_fee = 0;
	private Interface interFace = null;
	private static Map interfaces = new HashMap();
	private static Set connecting = new HashSet();
	private static Set disconnected_servers;
	private Map blockchains;
	private boolean downloadingHeaders = false;
	private int num_servers = 10;
	private String protocol;
	private JSONArray recent_servers;
	private Config config = new Config();
	private Queue socket_queue = new Queue(num_servers);
	private Map unanswered_requests = new HashMap();
	private Set subscribed_addresses = new HashSet();
	private Map sub_cache = new HashMap();
	private boolean auto_connect;
	private String proxy;
	private Map subscriptions;
	private Date date;
	private long server_retry_time;
	private long nodes_retry_time;
	private String connection_status;
	
	public Network() {
		
		date = new Date();
		Blockchain block = new Blockchain(config,1,null);
		blockchains = block.read_blockchain(config);
		blockchain_index = (Integer)config.value("blockchain_index");
		if(!blockchains.keySet().contains(blockchain_index)) {
			blockchain_index = new Integer(0);
		}
		default_server = (String)config.value("server");
		if(default_server !=null && !default_server.equals("")) {
			try {
				deserialize_server(default_server);
			}
			catch(Exception ex) {
				System.out.println("Warning: failed to parse server-string; falling back to random.");
				default_server = "";
			}
		}
		if(default_server.equals("")) {
			default_server = pickRandomServer(null, "s", new HashSet());
		}
		server_retry_time = date.getTime();
		nodes_retry_time = date.getTime();
		recent_servers = read_recent_servers();
		subscriptions = new HashMap();
		auto_connect = ((Boolean)config.get("auto_connect", Boolean.TRUE)).booleanValue();
		proxy = "";
		try {
			start_network(deserialize_server(default_server).getProtocol(),null);
		}catch(Exception e) {
			e.printStackTrace();
		}
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
	
	public String pickRandomServer(JSONObject hostmap, String protocol, Set exclude_set) {
		if(hostmap == null) {
			hostmap = BitcoinMainnet.getDefaultServers();
		}
		Object[] filter = filterProtocol(hostmap, protocol).toArray();
		String defaultServer ="";
		Set set = new HashSet();
		for(int i=0;i<filter.length;i++) {
			if(!exclude_set.contains((String)filter[i]))
				set.add((String)filter[i]);
		}
		defaultServer = set.toArray()[new Random().nextInt(set.size())].toString();
		return defaultServer;
	}
	
	public Set filterProtocol(JSONObject hostmap,String protocol){
		Set filter = new HashSet();
		Enumeration enumeration = hostmap.keys();
		try {
			while(enumeration.hasMoreElements()) {
				String key = (String)enumeration.nextElement();
				JSONObject item = hostmap.getJSONObject(key);
				if(item.has(protocol)) {
					int port = item.getInt(protocol);
					String server = serialize_server(key, port, protocol);
					filter.add(server);
				}
			}
		}catch(JSONException json) {
			json.printStackTrace();
		}
		return filter;
	}
	
	public String serialize_server(String host,int  port,String  protocol) {
		return host+":"+port+":"+protocol;
	}
	
	public void start_network(String protocol, String proxy) throws Exception{
		if(interFace != null || interfaces.size() !=0) {
			throw new Exception("Invalid");
		}
		if(!connecting.isEmpty() || !socket_queue.isEmpty()) {
			throw new Exception("Invalid");
		}
		disconnected_servers = new HashSet();
		this.protocol = protocol;
		setProxy(proxy);
		start_interfaces();
	}
	
	private void start_interfaces() {
		//start_interface(default_server);
		for(int i=0; i< num_servers ; i++) {
			System.out.println(i);
			//start_random_interface();
		}
	}

	private void start_random_interface() {
		
		Set excludeList = new HashSet();
		Object[] servers = interfaces.keySet().toArray();
		for(int i=0;i < servers.length;i++) {
			String interfaceServer = servers[i].toString();
			if(disconnected_servers.contains(interfaceServer)) {
				excludeList.add(interfaceServer);
			}
		}
		//TODO- Look how we can get this
		String prototcal = "s";
		JSONObject jsonservers = get_servers();
		String server =pickRandomServer(jsonservers, prototcal, excludeList);
		if(server != null && !server.equals("")) {
			start_interface(default_server);
		}
	}
	
	private void start_interface(String server) {
		if(!interfaces.containsKey(server) && !connecting.contains(server)) {
			if(server.equals(default_server)) {
                System.out.println("connecting to "+server+" as new interface ");
                set_status("connecting");
			}
			connecting.add(server);
			new TcpConnection(server, socket_queue, config.path()).start();
		}
	}
	
	public void init_header_file() {
		Blockchain b = (Blockchain)blockchains.get(new Integer(0));
		if(b.get_hash(0).equals(BitcoinMainnet.GENESIS)) {
			System.out.println("file found");
			downloadingHeaders = false;
			return;
		}
		
		String filename = b.getPath();
		downloadingHeaders = true;
		BitcoinHeadersDownload download = new BitcoinHeadersDownload(filename, this);
		download.start();
	}

	public boolean isRecent(String version) {
		return Integer.valueOf(version.replace('.', '0')).intValue() >= Integer.valueOf(Constants.PROTOCOL_VERSION.replace('.', '0')).intValue();
	}
	
	//TODO- Needs work
	private Map filterVersion(Map json) {
		return json;
	}
	
	private JSONObject get_servers() {
		JSONObject out = BitcoinMainnet.getDefaultServers();
		if(irc_servers.size() > 0) {
			filterVersion(irc_servers);
		}
		else {
			JSONArray servers = read_recent_servers();
			for(int i=0;i<servers.length();i++) {
				try {
					Server server = deserialize_server(servers.getString(i));
					if(!out.has(server.getHost())) {
						JSONObject value = new JSONObject();
						value.put(server.getProtocol(), server.getPort());
						out.put(server.getHost(), value);
					}
				}
			    catch (JSONException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return out;
	}


	//TODO - need implemenation
	public void setProxy(String proxy) {
		
	}
	
	public JSONObject getServer(JSONArray result) throws JSONException {
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
	
	

	

	public int maxCheckpoint() throws JSONException, IOException {	
		return Math.max(0, BitcoinMainnet.getCheckpoints().length() * 2016 - 1);
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


	public void saveRecent(JSONArray recent) {
		Files.write(recent, "recent-servers");
	}
	
	public static void notify(String key) {
		if("status".equals(key) ||  "updated".equals(key)) {
			
		}	
	}
	public boolean isDownloadingHeaders() {
		return downloadingHeaders;
	}
	public void setDownloadingHeaders(boolean downloadingHeaders) {
		this.downloadingHeaders = downloadingHeaders;
	}
	public boolean is_running() {
		return true;
	}
	public void maintain_sockets() {
		while (!socket_queue.isEmpty()) {
			ServerSocketTuple holder = (ServerSocketTuple) socket_queue.remove();
			if(connecting.contains(holder.getServer())) {
				connecting.remove(holder.getServer());
			}
			if(holder.getSocket() != null) {
				new_interface(holder.getServer(), holder.getSocket());
			}
			else {
				connection_down(holder.getServer());
			}
		}
		//Send pings and shut down stale interfaces
        // must use copy of values
		Object[] interfaceArray = interfaces.values().toArray();
		for(int i=0;i<interfaceArray.length;i++) {
			Interface inter =(Interface)interfaceArray[i];
			if(inter.has_timed_out()) {
				connection_down(inter.getServer());
			}
			else if(inter.ping_required()){
				queue_request("server.version", new String[] {Version.PACKAGE_VERSION,Version.PROTOCOL_VERSION}, inter);
			}
		}
        long now = date.getTime();
        if(interfaces.size()+connecting.size() < num_servers) {
        	start_random_interface();
        	if(now - nodes_retry_time > NODES_RETRY_INTERVAL) {
                System.out.println("network: retrying connections");
                disconnected_servers = null;
                nodes_retry_time = now;
        	}
        }
        
        // main interface
        if(!is_connected()) {
            if(auto_connect) {
                if(!is_connecting()) {
                    switch_to_random_interface();
                }
            }
            else {
                if(disconnected_servers.contains(default_server)){
                    if(now - server_retry_time > SERVER_RETRY_INTERVAL) {
                        disconnected_servers.remove(default_server);
                        server_retry_time = now;
                    }
                }
                else {
                    switch_to_interface(default_server);
                }
            }
        }
        else {
            if(config.is_fee_estimates_update_required()) {
                request_fee_estimates();
            }
        }
	}
	
	private void request_fee_estimates() {
		config.requested_fee_estimates();
		for(int i=0;i <FEE_TARGETS.length;i++) {
			queue_request("blockchain.estimatefee", new Integer[] {new Integer(i)} , null);
		}
		
	}


	private void switch_to_random_interface() {
		// TODO Auto-generated method stub
		
	}
	
	public void on_get_chunk(Interface inter,Map response) {
        //Handle receiving a chunk of block headers
        String error = (String)response.get("error");
        String result = (String)response.get("result");
        Integer[] params = (Integer[])response.get("params");
        if(result == null || params == null || error != null){
            System.out.println("bad response");
            return;
        }
        // Ignore unsolicited chunks
        int index = params[0].intValue();
        if(inter.getRequest().intValue() != index){
            return;
        }
        boolean connect = inter.getBlockchain().connect_chunk(index, result);
        // If not finished, get the next chunk
        if(!connect) {
            connection_down(inter.getServer());
            return;
        }
        if(inter.getBlockchain().height() < inter.getTip()){
            request_chunk(inter, new Integer(index+1));
        }
        else {
            inter.setRequest(new Integer(0));
            inter.setMode("default");
            System.out.println("catch up done" + inter.getBlockchain().height());
            inter.getBlockchain().set_catch_up(null);
        }
        notify("updated");
	}
	
	public void on_get_header(Interface inter,Map response) {
        //Handle receiving a single block header'''
        Map header = (Map)response.get("result");
        if(header == null) {
            System.out.println(response);
            connection_down(inter.getServer());
            return;
        }
        int height = ((Integer)header.get("block_height")).intValue();
        if(inter.getRequest().intValue() != height){
            System.out.println("unsolicited header "+inter.getRequest()+" "+height);
            connection_down(inter.getServer());
            return;
        }
        
        Blockchain chain = null ;/*BlockchainsUtil.check_header(header);*/
        Integer next_height = null;
        if("backward".equals(inter.getMode())) {
            if(chain != null) {
                System.out.println("binary search");
                inter.setMode("binary");
                inter.setBlockchain(chain);
                inter.setGood(height);
                next_height = new Integer((inter.getBad() + inter.getGood()) / 2);
            }
            else {
                if(height == 0) {
                    connection_down(inter.getServer());
                    next_height = null;
                }
                else {
                    inter.setBad(height);
                    inter.setBad_header(header);
                    int delta = inter.getTip() - height;
                    next_height = new Integer(Math.max(0, inter.getTip() - 2 * delta));
                }
            }
        }
        else if("binary".equals(inter.getMode())) {
            if(chain != null) {
                inter.setGood(height);
                inter.setBlockchain(chain);
            }
            else {
                inter.setBad(height);
                inter.setBad_header(header);
            }
            if(inter.getBad() != inter.getGood() + 1) {
                next_height = new Integer((inter.getBad() + inter.getGood()) / 2);
            }
            else if(!inter.getBlockchain().can_connect(inter.getBad_header(), false)){
                connection_down(inter.getServer());
                next_height = null;
            }
            else {
                Blockchain branch = (Blockchain)blockchains.get(new Integer(inter.getBad()));
                if(branch  != null){
                    if(branch.check_header(inter.getBad_header()) != null) {
                        System.out.println("joining chain" + inter.getBad());
                        next_height = null;
                    }
                    else if(branch.parent().check_header(header) != null) {
                        System.out.println("reorg" + inter.getBad() +" "+ inter.getTip());
                        inter.setBlockchain(branch.parent());
                        next_height = null;
                    }
                    else{
                    	System.out.println("checkpoint conflicts with existing fork"+ branch.getPath());
                       // branch.write('', 0);
                        branch.save_header(inter.getBad_header());
                        inter.setMode("catch_up");
                        inter.setBlockchain(branch);
                        next_height = new Integer(inter.getBad()+1);
                        inter.getBlockchain().set_catch_up(inter.getServer());
                    }
                }
                else {
                    int bh = inter.getBlockchain().height();
                    next_height = null;
                    if(bh > inter.getGood()){
                        if(inter.getBlockchain().check_header(inter.getBad_header()) == null){
                            Blockchain b = inter.getBlockchain().fork(inter.getBad_header());
                            blockchains.put(new Integer(inter.getBad()),b);
                            inter.setBlockchain(b);
                            System.out.println("new chain "+b.get_checkpoint());
                            inter.setMode("catch_up");
                            next_height = new Integer(inter.getBad() + 1);
                            inter.getBlockchain().set_catch_up(inter.getServer());
                        }
                    }
                    else
                    {
                        //assert bh == inter.getGood();
                        if(inter.getBlockchain().get_catch_up() == null && bh < inter.getTip()) {
                            System.out.println("catching up from "+ (bh + 1));
                            inter.setMode("catch_up");
                            next_height = new Integer(inter.getBad() + 1);
                            inter.getBlockchain().set_catch_up(inter.getServer());
                        }
                    }
                }
                notify("updated");
            }
        }
        else if("catch_up".equals(inter.getMode())){
            Blockchain can_connect = inter.getBlockchain().can_connect(header);
            if(can_connect != null) {
                inter.getBlockchain().save_header(header);
                if(height < inter.getTip()) {
                	next_height = new Integer(height + 1);
                }
                else {
                	next_height = null;
                }
            }
            else {
                //go back
                System.out.println("cannot connect " + height);
                inter.setMode("backward");
                inter.setBad(height);
                inter.setBad_header(header);
                next_height =new Integer(height - 1);
            }
            if(next_height == null) {
                // exit catch_up state
            	System.out.println("catch up done "+ inter.getBlockchain().height());
                inter.getBlockchain().set_catch_up(null);
                switch_lagging_interface();
                notify("updated");
            }
        }
        else {
        	try {
            throw new Exception("Exception "+inter.getMode());
        	}catch(Exception e) {
        		e.printStackTrace();
        	}
        }
        //If not finished, get the next header
        if(next_height != null){
            if(inter.getMode().equals("catch_up") && inter.getTip() > next_height.intValue() + 50)
                request_chunk(inter, new Integer(next_height.intValue() / 2016));
            else {
                request_header(inter, next_height);
            }
        }
        else {
            inter.setMode("default");
            inter.setRequest(null);
            notify("updated");
        }
        //refresh network dialog
        notify("interfaces");
	}
	private void request_chunk(Interface inter, Integer idx) {
        System.out.println("requesting chunk " + idx);
        queue_request("blockchain.block.get_chunk", new Integer[] {idx}, inter);
        inter.setRequest(idx);
        inter.setRequestTime(date.getTime());
	}
	
	private void request_header(Interface inter, Integer height) {
		queue_request("blockchain.block.get_header", new Integer[] {height}, inter);
		inter.setRequest(height);
		inter.setRequestTime(date.getTime());
	}
	private boolean is_connecting() {
		// TODO Auto-generated method stub
		return connection_status.equals("connecting");
	}

	public boolean is_connected() {
		return interFace != null;
	}
	
	private void connection_down(String server) {
		//A connection to server either went down, or was never made.
        //We distinguish by whether it is in self.interfaces.'''
        disconnected_servers.add(server);
        if(server.equals(default_server)) {
            set_status("disconnected");
        }
        if(interfaces.containsKey(server)) {
            close_interface((Interface)interfaces.get(server));
            notify("interfaces");
        }
        Object[] obj = blockchains.values().toArray();
        for(int i=0;i < obj.length;i++) {
        	Blockchain b = (Blockchain)obj[i];
        	if(b.get_catch_up().equals(server)) {
        		b.set_catch_up(null);
        	}
        }
 	}

	private void close_interface(Interface interface1) {
		if(interface1 != null) {
			if(interfaces.containsKey(interface1.getServer())){
				interfaces.remove(interface1.getServer());
			}
			if(interface1.getServer().equals(this.default_server)) {
				this.interFace = null;
			}
			interface1.close();
		}
	}

	private void new_interface(String server, Socket socket) {
		add_recent_server(server);
		Interface interfce = new Interface(server, socket);
		interfce.setTip(0);
		interfce.setBlockchain(null);
		interfce.setMode("default");
		interfce.setTip_header(null);
		interfce.setRequest(new Integer(0));
		interfaces.put(server, interfce);
		queue_request("blockchain.headers.subscribe",new String[] {} , interfce);
		if(server == default_server) {
			switch_to_interface(server);
		}
	}

	private void switch_to_interface(String server) {
		default_server = server;
		if(!interfaces.containsKey(server)) {
			interFace = null;
			start_interface(server);
			return ;
		}
		Interface i = (Interface)interfaces.get(server);
		if(interFace != i) {
			interFace = i;
			send_subscriptions();
			set_status("connected");
			notify("updated");
		}
	}
	
	/*public void follow_chain(Integer index) {
        Blockchain blockchain = (Blockchain)blockchains.get(index);
        if(blockchain != null) {
            blockchain_index = index;
            config.set_key("blockchain_index", index);
            Iterator iterator = interfaces.values().iterator();
            while (iterator.hasNext()) {
				Interface i = (Interface) iterator.next();
				if(i.getBlockchain().equals(blockchain)) {
					switch_to_interface(i.getServer());
					break;
				}
			}
        }
        else {
            try {
				throw new Exception("blockchain not found " + index);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        if(interFace != null) {
            String server = interFace.getServer();
            Server serverObj = deserialize_server(server);
            set_parameters(serverObj.getHost(), serverObj.getPort(), serverObj.getProtocol(), proxy, auto_connect);
        }
	}*/
	
	/*private void set_parameters(String host, int port, String protocol, String proxy, boolean auto_connect) {
		//proxy_str = serialize_proxy(proxy)
		String server = serialize_server(host, port, protocol);
		try:
            deserialize_server(serialize_server(host, port, protocol))
            if proxy:
                proxy_modes.index(proxy["mode"]) + 1
                int(proxy['port'])
        except:
            return
		config.set_key("auto_connect", new Boolean(auto_connect), false);
        //config.set_key("proxy", proxy_str, false)
        config.set_key("server", server, true);
        this.auto_connect = auto_connect;
        if(!this.proxy.equals(proxy) || this.protocol.equals(protocol) ) {
            //Restart the network defaulting to the given server
            stop_network();
            default_server = server;
            try {
				start_network(protocol, proxy);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        else if(!default_server.equals(server)) {
        	switch_to_interface(server);
        }
        else {
        	switch_lagging_interface();
            notify("updated");
        }
	}*/

	private void switch_lagging_interface() {
		 if(server_is_lagging() && auto_connect) {
			 //switch to one that has the correct header (not height)
			 Map header = blockchain().read_header(get_local_height());
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
				 String choice = (String)filtered.toArray()[random];
				 switch_to_interface(choice);
			 }
		 }
		
	}
	
	public int queue_request(String method,Object[] params,Interface interFace) {
        // If you want to queue a request on any interface it must go
        // through this function so message ids are properly tracked
        if(interFace == null)
            interFace = this.interFace;
        int message_id = this.message_id;
        this.message_id += 1;
        System.out.println(interFace.getHost() + "-->" +" "+ method +" "+ params +" "+ message_id);
        interFace.queue_request(method, params, message_id);
        return message_id;
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

	public Blockchain blockchain() {
        if(interFace !=null && interFace.getBlockchain() != null) {
            blockchain_index = interFace.getBlockchain().get_checkpoint();
        }
        return (Blockchain)blockchains.get(blockchain_index);
	}
	
	public int get_local_height() {
        return blockchain().height();
	}
	
	private void set_status(String string) {
		connection_status = string;
		
	}

	private void send_subscriptions() {
		System.out.println("sending subscriptions to " + interFace.getServer() +" "+ unanswered_requests.size()  +" "+  subscribed_addresses.size());
		sub_cache.clear();
		Object[] requests = unanswered_requests.values().toArray();
		unanswered_requests = new HashMap();
		if(interFace.ping_required()) {
			queue_request("server.version", new String[]{Version.PACKAGE_VERSION,Version.PROTOCOL_VERSION}, interFace);
		}
		for(int i=0;i<requests.length;i++) {
			
		}
	}

	public String get_index(String method,String[] params) {
	     //   """ hashable index for subscriptions and cache"""
		if(params.length==0) {
			return method;
		}
		else {
			return method+":"+params[0];
		}
	 }
	private void add_recent_server(String server) {
		try {
			JSONArray array = new JSONArray();
			for(int i=0;i<recent_servers.length();i++) {
				if(i==20) {
					break;
				}
				if(i==0)
					array.put(server);
				else {
					if(!recent_servers.getString(i).equals(server)) {
						array.put(recent_servers.getString(i));
					}	
				}	
			}
			saveRecent(array);
		}catch(JSONException je) {
			je.printStackTrace();
		}
	}

	public void wait_on_sockets() {
		if(interfaces == null) {
			try {
				Thread.sleep(100);
			}catch(InterruptedException ie) {
				ie.printStackTrace();
			}
			return;
		}
		Object[] interfacesArray = interfaces.values().toArray();
		for(int i=0; i<interfacesArray.length;i++) {
			Interface inter = (Interface)interfacesArray[i];
			inter.send_requests();
			process_responses(inter);
		}
		
	}
	public void maintain_requests() {
		Object[] interfaceArray = interfaces.values().toArray();
		for(int i=0 ;i <interfaceArray.length;i++) {
			Interface inter  = (Interface)interfaceArray[i];
			if(inter.getRequest() !=null && date.getTime() - inter.getRequest_time() > 20) {
				System.out.println("blockchain request timed out");
				connection_down(inter.getServer());
			}
		}
	}
	
	public Object get_status_value(String key) {
		if("status".equals(key)) {
			return connection_status;
		}
		else if("banner".equals(key)) {
			return banner;
		}
		else if("fee".equals(key)) {
			return config.fee_estimates;
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
		// TODO Auto-generated method stub
		return interfaces.keySet();
	}
	public void process_responses(Interface inter) {
		HashMap responses = inter.get_responses();
		Iterator it = responses.keySet().iterator();
		while(it.hasNext()) {
			RequestQueueItem item = (RequestQueueItem)it.next();
			Map response = (Map)responses.get(item);
			String k = null;
			if(item != null) {
				String index = get_index(item.getMethod(), (String[])item.getParams());
				//client requests go through self.send() with a
                //callback, are only sent to the current interface,
                //and are placed in the unanswered_requests dictionary
                ClientRequestHolder client_req = (ClientRequestHolder)unanswered_requests.get(item.getId());
                if(client_req != null) {
                	 //assert inter == interFace;
                	 //callbacks = [client_req[2]];
                }
                else {
                	//skipping callbacks
                }
                try {
	                response.put("method", item.getMethod());
	                response.put("params", item.getParams());
                }catch(Exception e) {
                	e.printStackTrace();
                }
                if("blockchain.scripthash.subscribe".equals(item.getMethod())) {
                    subscribed_addresses.add(item.getParams()[0]);
                }
			}
			else {
				 if(response == null) {  //# Closed remotely / misbehaving
					 connection_down(inter.getServer());
					 break;
				 }
					 //Rewrite response shape to match subscription request response
					 String method = (String)response.get("method");
					 String[] params = (String[])response.get("params");
					 k = get_index(method, params);
					 if("blockchain.headers.subscribe".equals(method)) {
						 response.put("result", params[0]);
						 response.put("params", new String[] {});
					 }
					 else if("blockchain.scripthash.subscribe".equals(method)) {
						 response.put("result", params[1]);
						 response.put("params", new String[] {params[0]});
					 }
				
				 //callbacks = self.subscriptions.get(k, [])
			}
			if(item.getMethod().endsWith(".subscribe")){
                sub_cache.put(k, response);
			}
			process_responses(inter, response);
		}
	}
	private void process_responses(Interface inter, Map response) {
		// TODO Auto-generated method stub
		System.out.println("<--"+ response);
        String error = (String)response.get("error");
        String method = (String)response.get("method");
        String[] params = (String[])response.get("params");

        // We handle some responses; return the rest to the client.
        if("server.version".equals(method)) {
        	String result = (String)response.get("result");
            inter.setServer_version(result);
        }
        else if("blockchain.headers.subscribe".equals(method)) {
            if(error != null) {
            	Map result = (Map)response.get("result");
                on_notify_header(inter, result);
            }
        }
        else if("server.peers.subscribe".equals(method)) {
        	if(error != null) {
        		Map result = (Map)response.get("result");
        		irc_servers = parse_servers(result);
        		notify("servers");
        	}
        }
        else if("server.banner".equals(method)) {
        	if(error != null) {
        		String result = (String)response.get("result");
                banner = result;
                notify("banner");
        	}
        }
        else if("server.donation_address".equals(method)) {
        	if(error != null) {
        		String result = (String)response.get("result");
                donation_address = result;
        	}
        }
        else if("blockchain.estimatefee".equals(method)) {
        	if(error != null) {
        		int result = ((Integer)response.get("result")).intValue();
        		if(result > 0) {
        			String i =params[0];
        			int fee = result * COIN;
        			config.update_fee_estimates(i, fee);
        			System.out.println("fee_estimates " + i +" "+ fee);
        			notify("fee");
        		}
        	}
        }
        else if("blockchain.relayfee".equals(method)) {
        	if(error != null) {
        		int result = ((Integer)response.get("result")).intValue();
        		relay_fee = result * COIN;
        		System.out.println("relayfee "+relay_fee);
        	}
        }
        else if("blockchain.block.get_chunk".equals(method)) {
        	on_get_chunk(inter, response);
        }
        else if("blockchain.block.get_header".equals(method)) {
        	on_get_header(inter, response);
        }
	}

	private Map parse_servers(Map result) {
		// TODO Auto-generated method stub
		return null;
	}

	private int get_server_height() {
		if(interFace != null)
			return interFace.getTip();
		else 
			return 0;
	}

	public void run_jobs() {
		
	}
	public void process_pending_sends() {
		
	}
    public void on_notify_header(Interface inter ,Map header) {
    	Blockchain blockchain = new Blockchain(config, 0, null);
        Integer height = (Integer)header.get("block_height");
        if( height != null)
            return;
        inter.setTip_header(header);
        inter.setTip(height.intValue());
        if("default".equals(inter.getMode())){
            return;
        }
        Blockchain b = blockchain.check_header(header);
        if(b != null) {
            inter.setBlockchain(b);
            switch_lagging_interface();
            notify("updated");
            notify("interfaces");
            return;
        }
        b = blockchain.can_connect(header);
        if(b != null) {
            inter.setBlockchain(b);
            b.save_header(header);
            switch_lagging_interface();
            notify("updated");
            notify("interfaces");
            return;
        }
        Object[] obj =blockchains.values().toArray();
        int[] arr = new int[obj.length];
        for(int i=0;i<obj.length;i++) {
        	arr[i] = ((Blockchain)obj[i]).height();
        }
        electrol.java.util.Arrays.sort(arr);
        int tip = arr[arr.length-1];
       
        if(tip >=0) {
            inter.setMode("backward");
            inter.setBad(height.intValue());
            inter.setBad_header(header);
            request_header(inter, new Integer(Math.min(tip, new Integer(height.intValue() - 1).intValue())));
        }
        else {
            Blockchain chain = (Blockchain) blockchains.get(new Integer(0));
            if(chain.get_catch_up() == null) {
                chain.set_catch_up(inter); 
                inter.setMode("catch_up");
                inter.setBlockchain(chain);
                request_header(inter, new Integer(0));
            }
        }
    }
	public void run() {
		init_header_file();
		while(is_running() && downloadingHeaders) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		while(is_running()) {
			 maintain_sockets();
	         wait_on_sockets();
	         maintain_requests();
	         run_jobs();    //Synchronizer and Verifier
	         process_pending_sends();
		}
		stop_network();
		on_stop();
	}

	private void on_stop() {
		// TODO Auto-generated method stub
		
	}

	private void stop_network() {
		// TODO Auto-generated method stub
		
	}
	class LocalServerTuple{
		private int a;
		private int b;
		public LocalServerTuple(int a, int b) {
			this.a = a;
			this.b = b;
		}
		public int getA() {
			return a;
		}
		public int getB() {
			return b;
		}
	}
}
