package electrol.main;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.java.util.HashMap;
import electrol.java.util.HashSet;
import electrol.java.util.Iterator;
import electrol.java.util.List;
import electrol.java.util.Map;
import electrol.java.util.Set;
import electrol.main.Deserialize.Holder;
import electrol.util.Server;

public class Network extends Thread{

	private Map scripthashMap;
	private HashSet hashes;
	private Map blockchains;
	private boolean downloadingHeaders = false;
	private int message_id;
	private Map sub_cache = new HashMap();
	private Map unanswered_requests = new HashMap();
	private Set subscribed_addresses = new HashSet();
	private Integer blockchain_index;
	private Map subscriptions = new HashMap();
	private ApplicationContext context;
	private ConnectionManager connectionManager;
	private Storage storage;
	private Config config;
	private String responseString = "";
	public Network(ApplicationContext context, Storage storage){
		this.context = context;
		this.storage = storage;
		this.config = context.getConfig();
		scripthashMap = new HashMap();
		hashes = new HashSet();
		blockchains = BlockchainsUtil.read_blockchain();
		blockchain_index = (Integer)config.get("blockchain_index", new Integer(0));
		if(!blockchains.keySet().contains(blockchain_index)) {
			blockchain_index = new Integer(0);
		}
		connectionManager =  new ConnectionManager(context.getTcpConnection());
		connectionManager.setTip(0);
		connectionManager.setBlockchain(null);
		connectionManager.setMode("default");
		connectionManager.setTip_header(null);
		connectionManager.setRequest(0);

		queue_request("blockchain.headers.subscribe",new String[] {});
		queue_request("server.version", new String[] {"3.1.5","1.1"});
	}

	public void run() {
		try {
			NetworkUtil.init_header_file(this, (Blockchain)(blockchains.get(new Integer(0))));
			while(isDownloadingHeaders()) {
				Thread.sleep(1000);
			}
		} catch (JSONException e1) {
			e1.printStackTrace();
		}catch(InterruptedException e) {

		}
		while(true) {
			try {
				waitOnSockets();
				//maintain_requests();
				process_pending_sends();
				if(context.isLatestBlock()) {
					subscribeAddresses();
					get_transaction();
					varify(getTxHashHeight());
				}
				
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void waitOnSockets() throws JSONException, ClassCastException, IllegalArgumentException, NullPointerException, RuntimeException, IOException {
		if(connectionManager.num_requests() > 0) {
			connectionManager.sendRequest();
		}
		processResponses();

	} 
	private void processResponses() throws JSONException, ClassCastException, IllegalArgumentException, NullPointerException, RuntimeException, IOException{
		List responses = connectionManager.getResponses();

		Iterator it = responses.iterator();
		while(it.hasNext()) {
			Set callbacks = new HashSet();
			RequetResponseTuple key = (RequetResponseTuple)it.next();
			RequestQueueItem request = key.getRequest();
			JSONObject response = key.getResponse();
			String k = "";
			if(request != null) {
				unanswered_requests.remove(request.getId());
				response.put("method", request.getMethod());
				JSONArray array = new JSONArray();
				Object[] o = request.getParams();
				for(int i=0;i< o.length;i++) {
					array.put(o[i]);
				}
				response.put("params", array);
				if("blockchain.scripthash.subscribe".equals(request.getMethod())) {
					subscribed_addresses.add(request.getParams()[0]);
					get_history(response);
				}
				if("blockchain.scripthash.get_history".equals(request.getMethod())) {
					get_tx(response);
				}
				if("blockchain.transaction.get".equals(request.getMethod())) {
					get_raw_tx(response);
				}
				if("blockchain.transaction.get_merkle".equals(request.getMethod())) {
					get_merkle(response);
				}
				if("blockchain.transaction.broadcast".equals(request.getMethod())) {
					responseString = response.toString();
				}
			}
			else {
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
					String address = AddressUtil.getLegacyAddressByScripthash(params.getString(0));
					scripthashMap.put(response.getString("result"), address);
					queue_request("blockchain.scripthash.subscribe", new String[] {params.getString(0)});

				}
				callbacks = (Set)subscriptions.get(k);
			}
			if(request!= null && request.getMethod()!= null && request.getMethod().endsWith(".subscribe")){
				sub_cache.put(k, response);
			}
			process_response( response, callbacks);
			connectionManager.getResponses().remove(key);
		}
	}
	private void get_merkle(JSONObject response) throws JSONException {
		Verifier verifer = new Verifier();
		VerifiedTx verified = verifer.verify_merkle(response, this);
		JSONObject verifiedTxs = storage.get("verified_tx3", new JSONObject());
		JSONArray array = new JSONArray();
		array.put(verified.getHeight());
		array.put(verified.getTimestamp());
		array.put(verified.getPos());
		verifiedTxs.put(verified.getHash(), array);
		storage.put("verified_tx3", verifiedTxs);
		storage.write();

	}

	public boolean isMine(String address) {
		JSONObject addresses =	storage.get("addr_history", new JSONObject());
		return addresses.has(address);
	}

	private void get_raw_tx(JSONObject response) throws JSONException {
		JSONObject transactions = storage.get("transactions", new JSONObject());
		String transactionHash = response.getJSONArray("params").getString(0);
		transactions.put(transactionHash, response.getString("result"));
		storage.put("transactions", transactions);

		Deserialize d = Deserialize.parse(response.getString("result"));

		JSONObject txo = storage.get("txo", new JSONObject());

		JSONObject txi = storage.get("txi", new JSONObject());
		JSONObject txiHash = txi.getJSONObject(transactionHash);
		Vector inputs = d.getInputs();
		for(int i=0;i< inputs.size();i++) {
			RawInput input = (RawInput)inputs.elementAt(i);
			Vector v = d.script_GetOp(input.getScript());
			int[] match = new int[] { AddressUtil.OP_PUSHDATA4, AddressUtil.OP_PUSHDATA4 };
			String address = "";
			if(d.match_decoded(v, match)) {
				String xpub = BlockchainsUtil.bh2u(((Holder)v.elementAt(1)).getVch());
				String ripeHash = Bitcoin.ripeHash(xpub);
				address= AddressUtil.legacyAddrfromPubKeyHash(ripeHash);
			}
			if(isMine(address)) {
				JSONArray array = new JSONArray();
				array.put(input.getTxHash()+":"+input.getTxIndex());

				if(txo.has(input.getTxHash())) {
					JSONObject txoKey = txo.getJSONObject(input.getTxHash());
					if(txoKey.length() > 0) {
						JSONArray txoArr = txoKey.getJSONArray(address);
						if(txoArr.getInt(0) == input.getTxIndex()) {
							array.put(txoArr.getInt(1));
						}
					}
				}
				txiHash.put(address, array);
			}
		}
		txi.put(transactionHash, txiHash);
		storage.put("txi", txi);

		JSONObject txoHash = txo.getJSONObject(transactionHash);

		Vector outputs = d.getOutputs();
		for(int i=0; i< d.getOutputCount();i++) {
			RawOutput output = (RawOutput)outputs.elementAt(i);
			JSONArray array = new JSONArray();
			array.put(i);
			array.put(output.getAmount());

			try {
				String address = AddressUtil.getLegacyAddressByScripthash(output.getScript());
				if(isMine(address)) {
					txoHash.put(address, array);
				}
			}
			catch(Exception e) {
				System.out.println(outputs);
			}
		}

		txo.put(transactionHash, txoHash);
		storage.put("txo", txo);

		storage.write();
	}	

	private void get_tx(JSONObject response) throws JSONException {
		String scripthashAddr = response.getJSONArray("params").getString(0);
		String legacyAddress = (String)scripthashMap.get(scripthashAddr);
		JSONObject history = storage.get("addr_history", new JSONObject());
		JSONArray itemHistory = history.getJSONArray(legacyAddress);
		JSONObject txo = storage.get("txo", new JSONObject());
		JSONObject txi = storage.get("txi", new JSONObject());
		JSONObject transactions = storage.get("transactions", new JSONObject());

		JSONArray result = response.getJSONArray("result");
		for(int i=0;i<result.length();i++) {
			JSONObject jsonObject = result.getJSONObject(i);
			JSONArray array = new JSONArray();
			String hash = jsonObject.getString("tx_hash");
			array.put(hash);
			array.put(jsonObject.getInt("height"));
			if(!contain(itemHistory, array)) {
				itemHistory.put(array);
			}
			hashes.add(hash);
			if(!txo.has(hash)) {
				txo.put(hash, new JSONObject());
			}
			if(!txi.has(hash)) {
				txi.put(hash, new JSONObject());
			}
			if(!transactions.has(hash)) {
				transactions.put(hash, "");
			}
		}
		storage.put("txi", txi);
		storage.put("txo", txo);
		storage.put("transactions", transactions);
		storage.put("addr_history", history);

		storage.write();
	}



	private boolean contain(JSONArray array, JSONArray item) throws JSONException {
		if(array.length() == 0) {
			return false;
		}
		else {
			for(int i=0;i<array.length();i++) {
				JSONArray getItem = array.getJSONArray(i);
				if(item.toString().equals(getItem.toString())) {
					return true;
				}
			}
			return false;
		}
	}

	private void process_response( JSONObject response, Set callbacks) throws JSONException, ClassCastException, IllegalArgumentException, NullPointerException, RuntimeException, IOException {
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
				on_notify_header(result);
			}
		}
		else if("server.peers.subscribe".equals(method)) {
			if(error != null) {
				JSONArray result = response.optJSONArray("result");
				if(result != null) {
					NetworkUtil.parse_servers(result);
				}
				notify("servers");
			}
		}

		else if("blockchain.block.get_chunk".equals(method)) {
			on_get_chunk( response);
		}
		else if("blockchain.block.get_header".equals(method)) {
			on_get_header(response);
		}
		/*Iterator it = callbacks.iterator();
		while(it.hasNext()) {
			Callback call = (Callback)it.next();
			call.call(response);
		}*/
	}

	public void on_get_chunk(JSONObject response) throws JSONException {
		//Handle receiving a chunk of block headers
		String error = response.optString("error");
		String result = response.optString("result");
		JSONArray params = response.optJSONArray("params");

		if(result == null || params == null || error.length()>0){
			System.out.println("bad response");
			return;
		}
		int index = params.getInt(0);
		/*if(connectionManager.getRequest() != index) {
			return;
		}*/
		boolean connect = connectionManager.getBlockchain().connect_chunk(index, result);

		if(!connect) {
			connection_down(connectionManager.getServer());
			return;
		}
		if(connectionManager.getBlockchain().height() < connectionManager.getTip()) {
			request_chunk(index+1);
		}
		else {
			connectionManager.setRequest(0);
			connectionManager.setMode("default");

			System.out.println("catch up done" + connectionManager.getBlockchain().height());
			connectionManager.getBlockchain().set_catch_up(null);
			context.setLatestBlock(true);
			context.setLatestBlockHeight(connectionManager.getBlockchain().height());

		}
		notify("updated");
	}

	public void on_get_header(JSONObject response) throws ClassCastException, IllegalArgumentException, NullPointerException, JSONException, RuntimeException, IOException {
		//Handle receiving a single block header'''
		System.out.println("On get header ");
		JSONObject header = response.optJSONObject("result");
		if(header == null) {
			connection_down(connectionManager.getServer());
			return;
		}
		int height = header.optInt("block_height");
		if(connectionManager.getRequest() != height){
			System.out.println("unsolicited header "+connectionManager.getRequest()+" "+height);
			connection_down(connectionManager.getServer());
			return;
		}
		Blockchain chain = BlockchainsUtil.check_header(header);
		int next_height = 0;
		System.out.println("connecting with backward"+connectionManager.getMode());
		if("backward".equals(connectionManager.getMode())) {
			if(chain != null) {
				System.out.println("binary search");
				connectionManager.setMode("binary");
				connectionManager.setBlockchain(chain);
				connectionManager.setGood(height);
				next_height = (connectionManager.getBad() + connectionManager.getGood()) / 2;

			}
			else {
				if(height == 0) {
					connection_down(connectionManager.getServer());
					next_height = 0;
				}
				else {
					connectionManager.setBad(height);
					connectionManager.setBad_header(header);
					int delta = connectionManager.getTip() - height;
					next_height = Math.max(0, connectionManager.getTip() - 2 * delta);
				}
			}
		}
		else if("binary".equals(connectionManager.getMode())) {
			if(chain != null) {
				connectionManager.setGood(height);
				connectionManager.setBlockchain(chain);
			}
			else {
				connectionManager.setBad(height);
				connectionManager.setBad_header(header);
			}
			System.out.println("good and bad "+connectionManager.getBad()+" "+connectionManager.getGood());
			if(connectionManager.getBad() != connectionManager.getGood() + 1) {
				next_height = (connectionManager.getBad() + connectionManager.getGood()) / 2;
			}
			else if(!connectionManager.getBlockchain().can_connect(connectionManager.getBad_header(), false)){
				connection_down(connectionManager.getServer());
				next_height = 0;
			}
			else {

				Blockchain branch = (Blockchain)blockchains.get(new Integer(connectionManager.getBad()));
				if(branch  != null){
					if(branch.check_header(connectionManager.getBad_header())) {
						System.out.println("joining chain" + connectionManager.getBad());
						next_height = 0;
					}
					else if(branch.parent().check_header(header)) {
						connectionManager.setBlockchain(branch.parent());
						next_height = 0;
					}
					else{
						System.out.println("checkpoint conflicts with existing fork"+ branch.getPath());
						// branch.write('', 0);
						branch.save_header(connectionManager.getBad_header());
						connectionManager.setMode("catch_up");
						connectionManager.setBlockchain(branch);
						next_height = connectionManager.getBad()+1;
						connectionManager.getBlockchain().set_catch_up(connectionManager.getServer());
					}
				}
				else {
					int bh = connectionManager.getBlockchain().height();
					next_height = 0;
					if(bh > connectionManager.getGood()){
						if(connectionManager.getBlockchain().check_header(connectionManager.getBad_header())){
							Blockchain b = connectionManager.getBlockchain().fork(connectionManager.getBad_header());
							blockchains.put(new Integer(connectionManager.getBad()),b);
							connectionManager.setBlockchain(b);
							System.out.println("new chain "+b.get_checkpoint());
							connectionManager.setMode("catch_up");
							next_height = connectionManager.getBad() + 1;
							connectionManager.getBlockchain().set_catch_up(connectionManager.getServer());
						}
					}
					else
					{
						if(connectionManager.getBlockchain().get_catch_up() == null && bh < connectionManager.getTip()) {
							System.out.println("catching up from "+ (bh + 1));
							connectionManager.setMode("catch_up");
							next_height = bh + 1;
							connectionManager.getBlockchain().set_catch_up(connectionManager.getServer());
						}
					}
				}
				notify("updated");
			}
		}
		else if("catch_up".equals(connectionManager.getMode())){
			boolean can_connect = connectionManager.getBlockchain().can_connect(header,true);
			if(can_connect) {
				connectionManager.getBlockchain().save_header(header);
				if(height < connectionManager.getTip()) {
					next_height = height + 1;
				}
				else {
					next_height = 0;
				}
			}
			else {
				//go back
				System.out.println("cannot connect " + height);
				connectionManager.setMode("backward");
				connectionManager.setBad(height);
				connectionManager.setBad_header(header);
				next_height =height - 1;
			}
			if(next_height == 0) {
				// exit catch_up state
				System.out.println("catch up done "+ connectionManager.getBlockchain().height());
				connectionManager.getBlockchain().set_catch_up(null);
				switch_lagging_interface();
				notify("updated");
				context.setLatestBlock(true);
			}
		}
		else {
			try {
				throw new Exception("Exception "+connectionManager.getMode());
			}catch(Exception e) {
				e.printStackTrace();
			}
		}

		//If not finished, get the next header
		if(next_height != 0){
			if(connectionManager.getMode().equals("catch_up") && connectionManager.getTip() > next_height + 50) {
				request_chunk(next_height / 2016);
			}
			else {

				request_header(next_height);
			}
		}
		else {
			connectionManager.setMode("default");
			connectionManager.setRequest(0);
			notify("updated");
		}
		//refresh network dialog

	}
	private void request_chunk(int idx) {
		queue_request("blockchain.block.get_chunk", new Integer[] {new Integer(idx)});
		connectionManager.setRequest(idx);
	}
	private void notify(String key) throws JSONException {
		if("status".equals(key)) {

		}else if("updated".equals(key)) {


		}
		else {
		}

	}

	public void on_notify_header(JSONObject header) throws JSONException, IOException {
		int height = header.optInt("block_height");
		if( height == 0)
			return;
		connectionManager.setTip_header(header);
		connectionManager.setTip(height);

		if(!"default".equals(connectionManager.getMode())){
			return;
		}

		Blockchain b = BlockchainsUtil.check_header(header);
		System.out.println("old b is "+b);
		if(b != null) {
			connectionManager.setBlockchain(b);
			switch_lagging_interface();
			context.setLatestBlock(true);
			return;
		}
		b = BlockchainsUtil.can_connect(header);

		System.out.println("new b is b"+b);
		if(b != null) {
			connectionManager.setBlockchain(b);
			b.save_header(header);
			switch_lagging_interface();
			notify("updated");
			notify("interfaces");
			context.setLatestBlock(true);
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
			connectionManager.setMode("backward");
			connectionManager.setBad(height);
			connectionManager.setBad_header(header);
			request_header(Math.min(tip, height - 1));
		}
		else {
			Blockchain chain = (Blockchain) blockchains.get(new Integer(0));
			System.out.println("catch up "+chain.get_catch_up());
			if(chain.get_catch_up() == null) {
				chain.set_catch_up(connectionManager); 
				connectionManager.setMode("catch_up");
				connectionManager.setBlockchain(chain);
				request_header(0);
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
			System.out.println(context.getDefaultServer() +" is lagging "+sh+" vs "+lh);
		}
		return result;
	}
	
	public int get_local_height() {
		return blockchain().height();
	}

	public int get_server_height() {
		if(connectionManager != null)
			return connectionManager.getTip();
		else 
			return 0;
	}

	public Blockchain blockchain() {
		System.out.println(connectionManager.getBlockchain());
		if(connectionManager !=null && connectionManager.getBlockchain() != null) {
			blockchain_index = connectionManager.getBlockchain().get_checkpoint();
		}
		System.out.println("blockchain index"+blockchain_index);
		return (Blockchain)blockchains.get(blockchain_index);
	}

	//need to check how we can implement this
	private void switch_lagging_interface() throws JSONException {

		/*if(server_is_lagging()) {
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
		}*/
	}

	private void connection_down(Server server) throws JSONException {
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
			if(connectionManager.getRequest() != 0 && (date.getTime() - connectionManager.getRequest_time()) > 20) {
				System.out.println("blockchain request timed out");
				connection_down(connectionManager.getServer());
			}
		}
	}*/

	private Set pending_sends = new HashSet();
	public void process_pending_sends() {
		// Requests needs connectivity.  If we don't have an interface,
		// we cannot process them.
		if(context.getTcpConnection() == null)
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
						message_id = queue_request(mptuple.getMethod(), mptuple.getParams());
						unanswered_requests.put(new Integer(message_id), new Object[] {mptuple.getMethod(), mptuple.getParams(), mcTuple.getCallback()});
					}
				}
			}
		}
	}

	private void send_subscriptions() {
		System.out.println("sending subscriptions to " + connectionManager.getServer().toString() +" "+ unanswered_requests.size()  +" "+  subscribed_addresses.size());
		sub_cache.clear();
		Object[] requests = unanswered_requests.values().toArray();
		unanswered_requests = new HashMap();
		for(int i=0;i<requests.length;i++) {
			Object[] request = (Object[])requests[i];
			int message_id = queue_request((String)request[0], (String[])request[1]);
			unanswered_requests.put(new Integer(message_id),request);
		}
		queue_request("server.peers.subscribe", new String[] {});
		Iterator it = subscribed_addresses.iterator();
		while (it.hasNext()) {
			String type = (String) it.next();
			queue_request("blockchain.scripthash.subscribe", new String[] {type});
		}
	}

	private void request_header(int height) {
		queue_request("blockchain.block.get_header", new Integer[] {new Integer(height)});
		connectionManager.setRequest(height);
	}

	public int queue_request(String method,Object[] params) {
		connectionManager.addRequest(method, params, message_id);
		return ++message_id;
	}
	
	public String queue_synch_request(String method,Object[] params) {
		connectionManager.addRequest(method, params, message_id);
		++message_id;
		while(responseString == "") {}
		String response = responseString;
		responseString = "";
		return response;
	}
	

	public void setDownloadingHeaders(boolean b) {
		this.downloadingHeaders = b;
	}

	public boolean isDownloadingHeaders() {
		return downloadingHeaders;
	}

	public void subscribeAddresses() {
		JSONObject addresses = storage.get("addr_history", new JSONObject());
		Enumeration e = addresses.keys();
		while(e.hasMoreElements()) {
			String address = (String)e.nextElement();
			String scripthash = AddressUtil.to_scripthash_hex_from_legacy(address);
			scripthashMap.put(scripthash, address);
			queue_request("blockchain.scripthash.subscribe", new String[] {scripthash});
		}
	}

	public void get_history(JSONObject respone) throws JSONException {
		String result = respone.optString("result");
		if(result != null && !result.equals("null")) {
			queue_request("blockchain.scripthash.get_history", new String[] {respone.getJSONArray("params").getString(0)});
		}
	}

	public void get_transaction() {
		JSONObject transactions = storage.get("transactions", new JSONObject());
		Enumeration e = transactions.keys();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			queue_request("blockchain.transaction.get", new String[] {key});
		}

	}
	
	public HashMap getTxHashHeight() throws JSONException {
		HashMap txHashHeight = new HashMap();
		JSONObject jsonObject = storage.get("addr_history", new JSONObject());
		Enumeration e =jsonObject.keys();
		while (e.hasMoreElements()) {
			String object = (String) e.nextElement();
			JSONArray array = jsonObject.getJSONArray(object);
			if(array.length() > 0) {
				for(int i=0;i<array.length();i++) {
					JSONArray item = array.getJSONArray(i);

					txHashHeight.put(item.getString(0), item.getString(1));
				}
			}
		}
		return txHashHeight;
	} 
	
	public void varify(HashMap hashmap) {
		Iterator it = hashmap.keySet().iterator();
		while(it.hasNext()) {
			String hash = (String)it.next();
			String height = (String) hashmap.get(hash);

			queue_request("blockchain.transaction.get_merkle", new Object[] {hash, Integer.valueOf(height)});
		}
	}
	
	public Set getSubscribedAddresses() {
		return subscribed_addresses;
	}
}
