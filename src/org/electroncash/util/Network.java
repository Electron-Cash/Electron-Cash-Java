package org.electroncash.util;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.util.Arrays;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public class Network
extends Thread
{
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
	private Map checkpoint_servers_verified = new HashMap();

	private Set requested_chunks = new HashSet();
	private ApplicationContext context;
	private ConnectionManager connectionManager;
	private Storage storage;
	private Config config;
	private String responseString = "";
	private boolean verified_checkpoint = false;
	private int checkpoint_height = 540250;

	public Network(ApplicationContext context, Storage storage) throws ClassCastException, IllegalArgumentException, NullPointerException, RuntimeException, IOException { this.storage = storage;
	this.context = context;
	config = context.getConfig();
	scripthashMap = new HashMap();
	hashes = new HashSet();
	blockchains = BlockchainsUtil.read_blockchain();
	blockchain_index = ((Integer)config.get("blockchain_index", new Integer(0)));
	if (!blockchains.keySet().contains(blockchain_index)) {
		blockchain_index = new Integer(0);
	}
	connectionManager = new ConnectionManager(context.getSocketConnection(), context.getAlertUtil());
	connectionManager.setTip(0);
	connectionManager.setBlockchain(null);
	connectionManager.setTip_header(null);
	connectionManager.setRequest(0);
	connectionManager.setMode((byte)1);
	queue_request("server.version", new String[] { "3.3.2", "1.4" });
	queue_request("blockchain.headers.subscribe", new String[0]);
	}

	public void run()
	{
		try
		{
			while(true) {
				waitOnSockets();
				process_pending_sends();
				if (context.isLatestBlock()) {
					subscribeAddresses();
					get_transaction();
					varify(getTxHashHeight());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void waitOnSockets() throws Exception
	{
		if (connectionManager.num_requests() > 0) {
			connectionManager.sendRequest();
		}
		processResponses();
	}

	private void processResponses() throws Exception {
		List responses = connectionManager.getResponses();
		Iterator it = responses.iterator();
		while (it.hasNext())
		{
			RequetResponseTuple key = (RequetResponseTuple)it.next();
			RequestItem request = key.getRequest();
			JSONObject response = key.getResponse();
			String k = "";
			if (request != null) {
				unanswered_requests.remove(request.getId());
				response.put("method", request.getMethod());
				JSONArray array = new JSONArray();
				Object[] o = request.getParams();
				for (int i = 0; i < o.length; i++) {
					array.put(o[i]);
				}
				response.put("params", array);
				if ("blockchain.scripthash.subscribe".equals(request.getMethod())) {
					subscribed_addresses.add(request.getParams()[0]);
					get_history(response);
				}
				if ("blockchain.scripthash.get_history".equals(request.getMethod())) {
					get_tx(response);
				}
				if ("blockchain.transaction.get".equals(request.getMethod())) {
					get_raw_tx(response);
				}
				if ("blockchain.transaction.get_merkle".equals(request.getMethod())) {
					get_merkle(response);
				}
				if ("blockchain.transaction.broadcast".equals(request.getMethod())) {
					responseString = response.toString();
				}
			}
			else {
				String method = response.optString("method");
				JSONArray params = response.optJSONArray("params");
				if (params == null) {
					k = method;
				} else
					k = method + ":" + params.get(0);
				if ("blockchain.headers.subscribe".equals(method)) {
					response.put("result", params.get(0));
					response.put("params", new JSONArray());
				}
				else if ("blockchain.scripthash.subscribe".equals(method)) {
					response.put("result", params.get(1));
					response.put("params", new JSONArray().put(params.get(0)));
					String address = AddressUtil.getLegacyAddressByScripthash(params.getString(0));
					scripthashMap.put(response.getString("result"), address);
					queue_request("blockchain.scripthash.subscribe", new String[] { params.getString(0) });
				}
			}


			if ((request != null) && (request.getMethod() != null) && (request.getMethod().endsWith(".subscribe"))) {
				sub_cache.put(k, response);
			}
			process_response(connectionManager, request, response);
		}
	}

	private void get_merkle(JSONObject response) throws JSONException, DataLengthException, IllegalStateException, InvalidCipherTextException, IOException { Verifier verifer = new Verifier();
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

	public boolean isMine(String address)
	{
		JSONObject addresses = storage.get("addr_history", new JSONObject());
		return addresses.has(address);
	}

	private void get_raw_tx(JSONObject response) throws JSONException, DataLengthException, IllegalStateException, InvalidCipherTextException, IOException {
		JSONObject transactions = storage.get("transactions", new JSONObject());
		String transactionHash = response.getJSONArray("params").getString(0);
		transactions.put(transactionHash, response.getString("result"));
		storage.put("transactions", transactions);

		Deserialize d = Deserialize.parse(response.getString("result"));

		JSONObject txo = storage.get("txo", new JSONObject());

		JSONObject txi = storage.get("txi", new JSONObject());
		JSONObject txiHash = txi.getJSONObject(transactionHash);
		Vector inputs = d.getInputs();
		for (int i = 0; i < inputs.size(); i++) {
			RawInput input = (RawInput)inputs.elementAt(i);
			Vector v = d.script_GetOp(input.getScript());
			int[] match = { 78, 78 };
			String address = "";
			if (d.match_decoded(v, match)) {
				String xpub = BlockchainsUtil.bh2u(((Deserialize.Holder)v.elementAt(1)).getVch());
				String ripeHash = Bitcoin.ripeHash(xpub);
				address = AddressUtil.legacyAddrfromPubKeyHash(ripeHash);
			}
			if (isMine(address)) {
				JSONArray array = new JSONArray();
				array.put(input.getTxHash() + ":" + input.getTxIndex());

				if (txo.has(input.getTxHash())) {
					JSONObject txoKey = txo.getJSONObject(input.getTxHash());
					if (txoKey.length() > 0) {
						JSONArray txoArr = txoKey.getJSONArray(address);
						if (txoArr.getInt(0) == input.getTxIndex()) {
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
		for (int i = 0; i < d.getOutputCount(); i++) {
			RawOutput output = (RawOutput)outputs.elementAt(i);
			JSONArray array = new JSONArray();
			array.put(i);
			array.put(output.getAmount());
			try
			{
				String address = AddressUtil.getLegacyAddressByScripthash(output.getScript());
				if (isMine(address)) {
					txoHash.put(address, array);
				}
			}
			catch (Exception e) {
				System.out.println(outputs +" "+e.getMessage());
			}
		}


		txo.put(transactionHash, txoHash);
		storage.put("txo", txo);

		storage.write();
	}

	private void get_tx(JSONObject response) throws JSONException, DataLengthException, IllegalStateException, InvalidCipherTextException, IOException {
		String scripthashAddr = response.getJSONArray("params").getString(0);
		String legacyAddress = (String)scripthashMap.get(scripthashAddr);
		JSONObject history = storage.get("addr_history", new JSONObject());
		JSONArray itemHistory = history.getJSONArray(legacyAddress);
		JSONObject txo = storage.get("txo", new JSONObject());
		JSONObject txi = storage.get("txi", new JSONObject());
		JSONObject transactions = storage.get("transactions", new JSONObject());

		JSONArray result = response.getJSONArray("result");
		for (int i = 0; i < result.length(); i++) {
			JSONObject jsonObject = result.getJSONObject(i);
			JSONArray array = new JSONArray();
			String hash = jsonObject.getString("tx_hash");
			array.put(hash);
			array.put(jsonObject.getInt("height"));
			if (!contain(itemHistory, array)) {
				itemHistory.put(array);
			}
			hashes.add(hash);
			if (!txo.has(hash)) {
				txo.put(hash, new JSONObject());
			}
			if (!txi.has(hash)) {
				txi.put(hash, new JSONObject());
			}
			if (!transactions.has(hash)) {
				transactions.put(hash, "");
			}
		}
		storage.put("txi", txi);
		storage.put("txo", txo);
		storage.put("transactions", transactions);
		storage.put("addr_history", history);

		storage.write();
	}

	private boolean contain(JSONArray array, JSONArray item)
			throws JSONException
	{
		if (array.length() == 0) {
			return false;
		}

		for (int i = 0; i < array.length(); i++) {
			JSONArray getItem = array.getJSONArray(i);
			if (item.toString().equals(getItem.toString())) {
				return true;
			}
		}
		return false;
	}

	private void process_response(ConnectionManager connectionManager, RequestItem request, JSONObject response) throws Exception
	{
		String error = response.optString("error");
		String method = response.optString("method");
		if ("server.version".equals(method)) {
			JSONArray result = response.optJSONArray("result");
			if (result != null) {
				connectionManager.setServerVersion(result.getString(1));
			}
		}
		else if ("blockchain.headers.subscribe".equals(method)) {
			if (error != null) {
				JSONObject result = response.optJSONObject("result");
				on_notify_header(connectionManager, result);
			}
		}
		else if ("server.peers.subscribe".equals(method)) {
			if (error != null) {
				JSONArray result = response.optJSONArray("result");
				if (result != null) {
					NetworkUtil.parse_servers(result);
				}
				notify("servers");
			}

		}
		else if ("blockchain.block.headers".equals(method)) {
			on_block_headers(connectionManager, request, response);
		}
		else if ("blockchain.block.header".equals(method)) {
			on_header(connectionManager, request, response);
		}
	}

	private void on_block_headers(ConnectionManager connectionManager, RequestItem request, JSONObject response)
			throws Exception
	{
		String error = response.optString("error");
		JSONObject result = response.optJSONObject("result");
		JSONArray params = response.optJSONArray("params");
		if ((request == null) || (result == null) || (params == null) || (error.length() > 0)) {
			if (request != null) {
				short index = (short)(((Integer)request.getParams()[0]).intValue() / 252);
				requested_chunks.remove(new Short(index));
			}
			throw new Exception("Bad Request");
		}
		Object[] request_params = request.getParams();
		int request_base_height = ((Integer)request_params[0]).intValue();
		int expected_header_count = ((Integer)request_params[1]).intValue();
		short index = (short)(request_base_height / 252);




		requested_chunks.remove(new Short(index));
		String hexdata = result.getString("hex");
		int actual_header_count = hexdata.length() / 160;
		if (actual_header_count > expected_header_count) {
			throw new Exception("chunk data size incorrect expected_size=" + expected_header_count * 80 * 2 + " actual_size=" + hexdata.length());
		}
		boolean proof_was_provided = false;
		if ((result.has("root")) && (result.has("branch"))) {
			int header_height = request_base_height + actual_header_count - 1;
			int header_offset = (actual_header_count - 1) * 160;
			String header = hexdata.substring(header_offset, header_offset + 160);
			if (!validate_checkpoint_result(result.getString("root"), Util.jsonArrayToArray(result.getJSONArray("branch")), header, header_height)) {
				System.out.println("disconnecting server for incorrect checkpoint proof");
				connection_down(connectionManager.getServer(), true);
				return;
			}
			byte[] data = BlockchainsUtil.bfh(hexdata);
			try {
				BlockchainsUtil.verify_proven_chunk(request_base_height, data);
			} catch (Exception e) {
				System.out.println("disconnecting server for failed verify_proven_chunk:" + e.getMessage());
				connection_down(connectionManager.getServer(), true);
				return;
			}
			proof_was_provided = true;
		}
		else if ((request_params.length == 3) && (((Integer)request_params[2]).intValue() != 0))
		{
			connection_down(connectionManager.getServer(), false);
			return;
		}
		JSONObject jsonObject = (JSONObject)checkpoint_servers_verified.get(connectionManager.getServer());
		int verification_top_height = jsonObject.optInt("height");
		boolean was_verification_request = (verification_top_height > 0) && (request_base_height == verification_top_height - 147 + 1) && (actual_header_count == 147);

		int initial_interface_mode = connectionManager.getMode();
		Blockchain target_blockchain = null;
		if (connectionManager.getMode() == 1) {
			if (!was_verification_request) {
				System.out.println("disconnecting unverified server for sending unrelated header chunk");
				connection_down(connectionManager.getServer(), true);
				return;
			}
			if (!proof_was_provided) {
				System.out.println("disconnecting unverified server for sending verification header chunk without proof");
				connection_down(connectionManager.getServer(), true);
				return;
			}
			if (!apply_successful_verification(connectionManager, (Integer)request_params[2], result.getString("root"))) {
				return;
			}


			target_blockchain = (Blockchain)blockchains.get(new Integer(0));
		}
		else {
			target_blockchain = connectionManager.getBlockchain();
		}
		byte[] chunk_data = BlockchainsUtil.bfh(hexdata);
		byte connect_state = target_blockchain.connect_chunk(request_base_height, chunk_data, proof_was_provided);
		if (connect_state == 0) {
			System.out.println("connected chunk, height=" + request_base_height + " count=" + actual_header_count + " proof_was_provided=" + proof_was_provided);
		} else {
			if (connect_state == 2) {
				System.out.println("identified forking chunk, height=" + request_base_height + " count=" + actual_header_count);
				return;
			}

			System.out.println("discarded bad chunk, height=" + request_base_height + " count=" + actual_header_count);
			connection_down(connectionManager.getServer(), false);
		}
		System.out.println("initial_interface_mode " + initial_interface_mode);

		if (initial_interface_mode == 1) {
			_process_latest_tip(connectionManager);
			return;
		}
		if ((proof_was_provided) && (!was_verification_request)) {
			return;
		}

		if (connectionManager.getBlockchain().height() < connectionManager.getTip()) {
			request_headers(connectionManager, request_base_height + actual_header_count, 252, false);
		}
		else {
			connectionManager.setMode((byte)0);
			System.out.println("catch up done " + connectionManager.getBlockchain().height());
			connectionManager.getBlockchain().set_catch_up(null);
			notify("updated");
		}
	}

	private boolean apply_successful_verification(ConnectionManager connectionManager, Integer height, String checkpoint_root) throws JSONException
	{
		Iterator it = checkpoint_servers_verified.values().iterator();
		int count = 0;
		while (it.hasNext()) {
			JSONObject type = (JSONObject)it.next();
			if (((String)type.get("root")).length() != 0) {
				count++;
			}
		}
		String[] known_roots = new String[count];
		it = checkpoint_servers_verified.values().iterator();
		count = 0;
		while (it.hasNext()) {
			JSONObject type = (JSONObject)it.next();
			if (((String)type.get("root")).length() != 0) {
				known_roots[count] = type.getString("root");
				count++;
			}
		}

		if ((count > 0) && (checkpoint_root.equals(known_roots[0]))) {
			System.out.println("server sent inconsistent root " + checkpoint_root);
			connection_down(connectionManager.getServer(), false);
			return false;
		}
		JSONObject object = (JSONObject)checkpoint_servers_verified.get(connectionManager.getServer());
		object.put("root", checkpoint_root);



		if (!verified_checkpoint)
		{
			verified_checkpoint = true;
		}

		System.out.println("server was verified correctly");
		connectionManager.setMode((byte)0);
		return true;
	}


	private void connection_down(Server server, boolean b) {}

	private void on_header(ConnectionManager connectionManager, RequestItem request, JSONObject response)
			throws Exception
	{
		String result = response.optString("result");
		JSONObject result1 = response.optJSONObject("result");
		if ((result == null) && (result1 == null)) {
			connection_down(connectionManager.getServer(), false);
			return;
		}
		if (request == null) {
			System.out.println("disconnecting server for sending unsolicited header, no request, params= " + response.getJSONArray("params"));
			connection_down(connectionManager.getServer(), false);
			return;
		}
		int height = ((Integer)request.getParams()[0]).intValue();
		int response_height = response.getJSONArray("params").getInt(0);
		if (response_height != height) {
			System.out.println("unsolicited header " + connectionManager.getRequest() + " " + height);
			connection_down(connectionManager.getServer(), false);
			return;
		}
		boolean proof_was_provided = false;
		String hexheader = null;
		if ((result1 != null) && (result1.has("root")) && (result1.has("branch")) && (result1.has("header"))) {
			hexheader = result1.getString("header");
			if (!validate_checkpoint_result(result1.getString("root"), Util.jsonArrayToArray(result1.getJSONArray("branch")), hexheader, height)) {
				System.out.println("unprovable header height=" + height);
				connection_down(connectionManager.getServer(), false);
				return;
			}
			proof_was_provided = true;
		}
		else if (result != null) {
			hexheader = result;
		}
		else {
			System.out.println("disconnecting server for params");
			connection_down(connectionManager.getServer(), false);
			return;
		}
		JSONObject header = BlockchainsUtil.deserializeHeader(BlockchainsUtil.bfh(hexheader), height);
		Blockchain chain = BlockchainsUtil.check_header(header);
		System.out.println("mode " + connectionManager.getMode());
		int next_height = 0;
		if (connectionManager.getMode() == 2) {
			if (chain != null) {
				System.out.println("binary search");
				connectionManager.setMode((byte)5);
				connectionManager.setBlockchain(chain);
				connectionManager.setGood(height);
				next_height = (connectionManager.getBad() + connectionManager.getGood()) / 2;


			}
			else if (height <= 540250) {
				connection_down(connectionManager.getServer(), false);
				next_height = 0;
			}
			else {
				connectionManager.setBad(height);
				connectionManager.setBad_header(header);
				int delta = connectionManager.getTip() - height;
				next_height = Math.max(540250, connectionManager.getTip() - 2 * delta);
			}

		}
		else if (connectionManager.getMode() == 5) {
			if (chain != null) {
				connectionManager.setGood(height);
				connectionManager.setBlockchain(chain);
			}
			else {
				connectionManager.setBad(height);
				connectionManager.setBad_header(header);
			}
			System.out.println("good and bad " + connectionManager.getBad() + " " + connectionManager.getGood());
			if (connectionManager.getBad() != connectionManager.getGood() + 1) {
				next_height = (connectionManager.getBad() + connectionManager.getGood()) / 2;
			}
			else if (!connectionManager.getBlockchain().can_connect(connectionManager.getBad_header(), false)) {
				connection_down(connectionManager.getServer(), false);
				next_height = 0;
			}
			else
			{
				Blockchain branch = (Blockchain)blockchains.get(new Integer(connectionManager.getBad()));
				if (branch != null) {
					if (branch.check_header(connectionManager.getBad_header())) {
						System.out.println("joining chain" + connectionManager.getBad());
						next_height = 0;
					}
					else if (branch.parent().check_header(header)) {
						connectionManager.setBlockchain(branch.parent());
						next_height = 0;
					}
					else {
						System.out.println("checkpoint conflicts with existing fork" + branch.getPath());
						branch.write(new byte[0], 0, true);
						branch.save_header(connectionManager.getBad_header());
						connectionManager.setMode((byte)3);
						connectionManager.setBlockchain(branch);
						next_height = connectionManager.getBad() + 1;
						connectionManager.getBlockchain().set_catch_up(connectionManager);
					}
				}
				else {
					int bh = connectionManager.getBlockchain().height();
					next_height = 0;
					if (bh > connectionManager.getGood()) {
						if (connectionManager.getBlockchain().check_header(connectionManager.getBad_header())) {
							Blockchain b = connectionManager.getBlockchain().fork(connectionManager.getBad_header());
							blockchains.put(new Integer(connectionManager.getBad()), b);
							connectionManager.setBlockchain(b);
							System.out.println("new chain " + b.get_checkpoint());
							connectionManager.setMode((byte)3);
							next_height = connectionManager.getBad() + 1;
							connectionManager.getBlockchain().set_catch_up(connectionManager);
						}


					}
					else if ((connectionManager.getBlockchain().get_catch_up() == null) && (bh < connectionManager.getTip())) {
						System.out.println("catching up from " + (bh + 1));
						connectionManager.setMode((byte)3);
						next_height = bh + 1;
						connectionManager.getBlockchain().set_catch_up(connectionManager);
					}

				}
			}
		}
		else if (connectionManager.getMode() == 3) {
			boolean can_connect = connectionManager.getBlockchain().can_connect(header, true);
			if (can_connect) {
				connectionManager.getBlockchain().save_header(header);
				if (height < connectionManager.getTip()) {
					next_height = height + 1;
				}
				else {
					next_height = 0;
				}
			}
			else {
				System.out.println("cannot connect " + height);
				connectionManager.setMode((byte)2);
				connectionManager.setBad(height);
				connectionManager.setBad_header(header);
				next_height = height - 1;
			}
			if (next_height == 0) {
				System.out.println("catch up done " + connectionManager.getBlockchain().height());
				connectionManager.getBlockchain().set_catch_up(null);
				switch_lagging_interface();
				System.out.println("updated..................4");
				notify("updated");
				context.setLatestBlock(true);
			}
		}
		else {
			try {
				throw new Exception("Exception " + connectionManager.getMode());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}


		if (next_height != 0) {
			if ((connectionManager.getMode() == 3) && (connectionManager.getTip() > next_height)) {
				request_headers(connectionManager, next_height, 252, false);
			}
			else {
				request_header(connectionManager, next_height);
			}
		}
		else {
			connectionManager.setMode((byte)0);
			System.out.println("updated..................3");
			notify("updated");
		}
	}

	private boolean validate_checkpoint_result(String merkle_root, String[] merkle_branch, String header, int header_height) {
		byte[] received_merkle_root = Arrays.reverse(BlockchainsUtil.bfh(merkle_root));
		byte[] expected_merkle_root; 
		if ("3848ff6c001ebf78ec1a798c2002f154ace4ba6c0f0a58ccb22f66934eda7143" != null) {
			expected_merkle_root = Arrays.reverse(BlockchainsUtil.bfh("3848ff6c001ebf78ec1a798c2002f154ace4ba6c0f0a58ccb22f66934eda7143"));
		}
		else {
			expected_merkle_root = received_merkle_root;
		}

		if (!Arrays.areEqual(received_merkle_root, expected_merkle_root)) {
			return false;
		}

		byte[] header_hash = BlockchainsUtil.Hash(BlockchainsUtil.bfh(header));
		byte[][] byte_branches = new byte[merkle_branch.length][];
		for (int i = 0; i < merkle_branch.length; i++) {
			byte_branches[i] = Arrays.reverse(BlockchainsUtil.bfh(merkle_branch[i]));
		}
		byte[] proven_merkle_root = BlockchainsUtil.root_from_proof(header_hash, byte_branches, header_height);
		if (!Arrays.areEqual(proven_merkle_root, expected_merkle_root)) {
			return false;
		}
		return true;
	}

	void request_headers(ConnectionManager connectionManager, int base_height, int count, boolean silent) throws Exception {
		if (!silent) {
			System.out.println("requesting multiple consecutive headers, from " + base_height + " count " + count);
		}
		if (count > 252) {
			throw new Exception("Server does not support requesting more than 252 consecutive headers");
		}
		int top_height = base_height + count - 1;
		if (top_height > 540250) {
			if (base_height < 540250) {
				int verified_count = 540250 - base_height + 1;
				_request_headers(connectionManager, base_height, verified_count, 540250);
			}
			else {
				_request_headers(connectionManager, base_height, count, 0);
			}
		} else {
			_request_headers(connectionManager, base_height, count, 540250);
		}
	}

	private void _request_headers(ConnectionManager connectionManager, int base_height, int count, int checkpoint_height)
	{
		System.out.println("base " + base_height + " checkpoint " + checkpoint_height + " count " + count);
		Integer[] params = { new Integer(base_height), new Integer(count), new Integer(checkpoint_height) };
		queue_request("blockchain.block.headers", params);
	}

	private void notify(String key) {
		if (!"status".equals(key))
		{
			if ("updated".equals(key)) {
				System.out.println("updated...");
				context.setLatestBlock(true);
			}
		}
	}


	public void on_notify_header(ConnectionManager connectionManager, JSONObject header_dict)
			throws JSONException, IOException
	{
		if ((!header_dict.has("hex")) || (!header_dict.has("height"))) {
			connection_down(connectionManager.getServer(), false);
			return;
		}

		String header_hex = header_dict.getString("hex");
		int height = header_dict.getInt("height");
		JSONObject header = BlockchainsUtil.deserializeHeader(BlockchainsUtil.bfh(header_hex), height);

		if (height <= 540250) {
			connection_down(connectionManager.getServer(), false);
			return;
		}

		connectionManager.setTip_header(header);
		connectionManager.setTip(height);
		if (connectionManager.getMode() == 1)
		{
			request_initial_proof_and_headers(connectionManager);
			return;
		}
		_process_latest_tip(connectionManager);
	}

	private void _process_latest_tip(ConnectionManager connectionManager) throws JSONException, IOException {
		if (connectionManager.getMode() != 0) {
			return;
		}

		JSONObject header = connectionManager.getTip_header();
		int height = connectionManager.getTip();
		Blockchain b = BlockchainsUtil.check_header(header);
		if (b != null) {
			connectionManager.setBlockchain(b);
			switch_lagging_interface();
			notify("updated");
			notify("interfaces");
			return;
		}
		b = BlockchainsUtil.can_connect(header);
		if (b != null) {
			connectionManager.setBlockchain(b);
			b.save_header(header);
			switch_lagging_interface();
			notify("updated");
			notify("interfaces");
			return;
		}


		Object[] obj = blockchains.values().toArray();
		int tip = Integer.MIN_VALUE;
		StringBuffer debug = new StringBuffer("[");
		for (int i = 0; i < obj.length; i++) {
			int tmp = ((Blockchain)obj[i]).height();
			debug.append(tmp);
			if (obj.length - 1 != i) {
				debug.append(",");
			}
			if (tip < tmp) {
				tip = tmp;
			}
		}
		debug.append("]");
		if (tip > 540250) {
			System.out.println("attempt to reconcile longest chain tip=" + tip + " heights=" + debug.toString());
			connectionManager.setMode((byte)2);
			connectionManager.setBad(height);
			connectionManager.setBad_header(header);
			request_header(connectionManager, Math.min(tip, height - 1));
		}
		else {
			System.out.println("attempt to catch up tip=" + tip + " heights=" + debug);
			Blockchain chain = (Blockchain)blockchains.get(new Integer(0));
			if (chain.get_catch_up() == null) {
				chain.set_catch_up(connectionManager);
				connectionManager.setMode((byte)3);
				connectionManager.setBlockchain(chain);
				System.out.println("switching to catchup mode " + tip);
				request_header(connectionManager, 540251);
			}
			else {
				System.out.println("chain already catching up with " + chain.get_catch_up().getServer());
			}
		}
	}

	private void request_initial_proof_and_headers(ConnectionManager connectionManager) throws JSONException, IOException {
		if (checkpoint_servers_verified.containsKey(connectionManager.getServer()))
		{
			System.out.println("request_initial_proof_and_headers bypassed");
			connectionManager.setMode((byte)0);
			_process_latest_tip(connectionManager);
		}
		else {
			System.out.println("request_initial_proof_and_headers pending");



			if (checkpoint_height == 0) {
				checkpoint_height = (connectionManager.getTip() - 100);
			}
			Hashtable hashtable = new Hashtable(2);
			hashtable.put("root", "");
			hashtable.put("height", new Integer(checkpoint_height));

			JSONObject object = new JSONObject(hashtable);
			checkpoint_servers_verified.put(connectionManager.getServer(), object);

			_request_headers(connectionManager, checkpoint_height - 147 + 1, 147, checkpoint_height);
		}
	}

	private boolean server_is_lagging() {
		int sh = get_server_height();
		if (sh == 0) {
			System.out.println("no height for main interface");
			return true;
		}
		int lh = get_local_height();
		boolean result = lh - sh > 1;
		if (result) {
			System.out.println(context.getDefaultServer() + " is lagging " + sh + " vs " + lh);
		}
		return result;
	}

	public int get_local_height() {
		return blockchain().height();
	}

	public int get_server_height() {
		if (connectionManager != null)
			return connectionManager.getTip();
		return 0;
	}

	public Blockchain blockchain() {
		if ((connectionManager != null) && (connectionManager.getBlockchain() != null)) {
			blockchain_index = connectionManager.getBlockchain().get_checkpoint();
		}
		return (Blockchain)blockchains.get(blockchain_index);
	}


	private void switch_lagging_interface() {}

	public String get_index(String method, Object[] params)
	{
		if (params.length == 0) {
			return method;
		}

		return method + ":" + params[0].toString();
	}



	private Set pending_sends = new HashSet();

	public void process_pending_sends() { 
		if (context.getSocketConnection() == null) {
			return;
		}
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
						Set callbacks = (Set)subscriptions.get(k);
						if(!callbacks.contains(mcTuple.getCallback())) {
							callbacks.add(mcTuple.getCallback());
						}
						subscriptions.put(k,callbacks);
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

	private void request_header(ConnectionManager connectionManager, int height)
	{
		System.out.println("requesting header " + height);
		Integer[] params; 
		if (height > 540250) {
			params = new Integer[] { new Integer(height) };
		} else {
			params = new Integer[] { new Integer(height), new Integer(540250) };
		}
		queue_request("blockchain.block.header", params);
		connectionManager.setRequest(height);
	}

	public int queue_request(String method, Object[] params) {
		connectionManager.addRequest(method, params, message_id);
		return ++message_id;
	}

	public String queue_synch_request(String method, Object[] params) {
		connectionManager.addRequest(method, params, message_id);
		message_id += 1;
		while (responseString == "") {}
		String response = responseString;
		responseString = "";
		return response;
	}

	public void setDownloadingHeaders(boolean b)
	{
		downloadingHeaders = b;
	}

	public boolean isDownloadingHeaders() {
		return downloadingHeaders;
	}

	public void subscribeAddresses() {
		JSONObject addresses = storage.get("addr_history", new JSONObject());
		Enumeration e = addresses.keys();
		while (e.hasMoreElements()) {
			String address = (String)e.nextElement();
			if (!scripthashMap.containsValue(address)) {
				String scripthash = AddressUtil.to_scripthash_hex_from_legacy(address);

				scripthashMap.put(scripthash, address);
				queue_request("blockchain.scripthash.subscribe", new String[] { scripthash });
			}
		}
	}

	public void get_history(JSONObject respone) throws JSONException {
		String result = respone.optString("result");
		if ((result != null) && (!result.equals("null"))) {
			queue_request("blockchain.scripthash.get_history", new String[] { respone.getJSONArray("params").getString(0) });
		}
	}

	public void get_transaction() {
		JSONObject transactions = storage.get("transactions", new JSONObject());
		Enumeration e = transactions.keys();
		while (e.hasMoreElements()) {
			String key = (String)e.nextElement();
			queue_request("blockchain.transaction.get", new String[] { key });
		}
	}

	public HashMap getTxHashHeight() throws JSONException
	{
		HashMap txHashHeight = new HashMap();
		JSONObject jsonObject = storage.get("addr_history", new JSONObject());
		Enumeration e = jsonObject.keys();
		while (e.hasMoreElements()) {
			String object = (String)e.nextElement();
			JSONArray array = jsonObject.getJSONArray(object);

			if (array.length() > 0) {
				for (int i = 0; i < array.length(); i++) {
					JSONArray item = array.getJSONArray(i);
					txHashHeight.put(item.getString(0), Integer.valueOf(item.getString(1)));
				}
			}
		}
		return txHashHeight;
	}

	private boolean request_chunk(ConnectionManager connectionManage, int chunk_index) throws Exception {
		if (requested_chunks.contains(new Integer(chunk_index)))
			return false;
		requested_chunks.add(new Integer(chunk_index));

		int chunk_count = 252;
		request_headers(connectionManage, chunk_index * chunk_count, chunk_count, true);
		return true;
	}

	public void varify(HashMap hashmap) throws Exception { Iterator it = hashmap.keySet().iterator();
	while (it.hasNext()) {
		String hash = (String)it.next();
		Integer height = (Integer)hashmap.get(hash);
		JSONObject header = connectionManager.getBlockchain().read_header(height.intValue(), null);
		if ((header == null) && 
				(height.intValue() <= 540250)) {
			int index = height.intValue() / 252;
			if (request_chunk(connectionManager, index)) {
				System.out.println("verifier requesting chunk " + index + " for height " + height);
			}
		}
		else
		{
			queue_request("blockchain.transaction.get_merkle", new Object[] { hash, height });
		}
	}
	}

	public Set getSubscribedAddresses() { return subscribed_addresses; }
}
