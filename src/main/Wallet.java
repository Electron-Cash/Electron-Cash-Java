package main;

import java.math.BigInteger;
import java.util.Enumeration;
// import java.util.ArrayList;

import org.json.me.JSONArray;
import org.json.me.JSONObject;

import electrol.java.util.HashMap;
import electrol.java.util.HashSet;
import electrol.java.util.Map;
import electrol.java.util.Set;
import electrol.java.util.Iterator;
import electrol.java.util.ArrayList;
import main.electron.Address;
import main.electron.Network;
import main.electron.Synchronizer;
import main.electron.Verifier;
import main.electron.Base58;
import main.Transaction;
import main.WalletAddress;
import main.TxIn;
import main.TxOut;
import main.Coinchooser;

public class Wallet {

	private int electrum_version = 1;
	private Storage storage = null;
	private Network network = null;
	//verifier (SPV) and synchronizer are started in start_threads
		private String txinType = "p2pkh";
		private String xpub;
		private String xprv;
		private Synchronizer synchronizer = null;
    private Verifier verifier = null;
    private int gap_limit_for_change = 6;
    private boolean use_change = true;
    private boolean multiple_change ;
    private JSONObject labels;
    private HashMap _history;
    private HashSet frozen_addresses;
		private ArrayList receivingAddresses;
		private ArrayList changeAddresses;
		private HashMap txo;
		private HashMap txi;
		private HashMap txFees;
		private HashMap prunedTxo;
		private HashMap txList;
		private HashMap transactions;
		private HashMap txAddrHist;
		private HashMap unverifiedTx;
		private HashMap verifiedTx;
		private boolean upToDate;
		private long COINBASE_MUTARITY = 100;



	public Wallet(Storage storage) throws ClassCastException, IllegalArgumentException, NullPointerException, RuntimeException, Exception {
		this.storage = storage;
		use_change = storage.get("use_change", true);
		multiple_change = storage.get("multiple_change", false);
		labels = storage.get("labels", new JSONObject());
		JSONArray frozen_addresses = storage.get("frozen_addresses", new JSONArray());
		JSONObject history = storage.get("addr_history", new JSONObject());
		_history = to_Address_dict(history);
		loadAddresses();
		loadTransactions();
		buildReverseHistory();
		// Receive requests should go here.
		unverifiedTx = new HashMap();
		verifiedTx =  new HashMap();
		JSONObject verifiedTxJSON = storage.get("verified_tx3", new JSONObject());
		Enumeration e = verifiedTxJSON.keys();
		while(e.hasMoreElements()){
			String txHash = (String) e.nextElement();
			JSONArray temp = verifiedTxJSON.optJSONArray(txHash);
			long[] currentVerifiedTx = new long[3];
			if (temp!=null) {
					for(int i=0; i<3; i++) {
						currentVerifiedTx[i] = temp.getLong(i);
					}
			}
			verifiedTx.put(txHash, currentVerifiedTx);
		}
		upToDate = false;
		checkHistory();
		JSONObject keystore = storage.get("keystore", new JSONObject());
		xpub = keystore.optString("xpub");
		xprv = keystore.optString("xprv");
	}


	private HashMap to_Address_dict(JSONObject history) throws ClassCastException, IllegalArgumentException, NullPointerException, RuntimeException, Exception {
		HashMap map = new HashMap();
		Enumeration e = history.keys();
		while(e.hasMoreElements()) {
			String key = (String)e.nextElement();
			try	{
				Address x = Address.from_string(key);
			} catch (Exception ex){
			}
			map.put(Address.from_string(key), history.optJSONArray(key));
		}
		return map;
	}

	private void loadAddresses(){
		JSONObject d = storage.get("addresses", new JSONObject());
		JSONArray receiving = d.optJSONArray("receiving");
		JSONArray changes = d.optJSONArray("change");
		receivingAddresses = new ArrayList();
		changeAddresses = new ArrayList();
		if (receiving!=null){
			try {
				for(int i = 0; i < receiving.length(); i++ ){
					receivingAddresses.add(Address.from_string(receiving.getString(i)));
				}
			} catch(Exception e){
			}
		}
		if (changes!=null){
			try {
				for(int i = 0; i < changes.length(); i++ ){
					changeAddresses.add(Address.from_string(changes.getString(i)));
				}
			} catch(Exception e){
			}
		}
	}

	private void loadTransactions(){
		JSONObject txiJSON = storage.get("txi", new JSONObject());
		JSONObject txoJSON = storage.get("txo", new JSONObject());
		JSONObject txFeesJSON = storage.get("tx_fees", new JSONObject());
		JSONObject prundesTxsJSON = storage.get("pruned_txo", new JSONObject());
		JSONObject transactionsJSON = storage.get("transactions", new JSONObject());
		txi = new HashMap();
		txo = new HashMap();
		txFees = new HashMap();
		prunedTxo = new HashMap();
		transactions =new HashMap();
		if (txiJSON != null){
			Enumeration e = txiJSON.keys();
			while(e.hasMoreElements()){
				String key = (String) e.nextElement();
				try{
					txi.put(key, to_Address_dict(txiJSON.optJSONObject(key)));
				} catch (Exception ex){
				}
			}
		}
		if (txoJSON != null){
			Enumeration e = txoJSON.keys();
			while(e.hasMoreElements()){
				String key = (String) e.nextElement();
				try{
					txo.put(key, to_Address_dict(txoJSON.optJSONObject(key)));
				} catch (Exception ex){
				}
			}
		}
		if (txFeesJSON != null){
			Enumeration e = txFeesJSON.keys();
			while(e.hasMoreElements()){
				String key = (String) e.nextElement();
				try{
					txFees.put(key, txFeesJSON.getLong(key));
				} catch (Exception ex){}
			}
		}
		//Pruned? what type is it?

		if (transactionsJSON != null){
			Enumeration e = transactionsJSON.keys();
			while(e.hasMoreElements()){
				String txHash = (String) e.nextElement();
				HashMap tx = new HashMap();
				try {
					tx.put("raw", transactionsJSON.getString(txHash));
					transactions.put(txHash, tx);
				} catch (Exception ex) {}
				if ((!txo.containsKey(txHash)) &&
				 	  (!txi.containsKey(txHash)) &&
					  (!prunedTxo.containsValue(txHash))){
					transactions.remove(txHash);
				}
			}
		}
	}

	public void buildReverseHistory(){
		txAddrHist = new HashMap();
		Iterator it = _history.keySet().iterator();
		while(it.hasNext()){
			Address addr = (Address)it.next();
			try{
				JSONArray hist = (JSONArray) _history.get(addr);
				if (hist.length()!=0) {
					for(int i = 0 ; i < hist.length(); i++){
						HashSet s = new HashSet();
						String txHash = hist.getJSONArray(i).getString(0);
						if (txAddrHist.containsKey(txHash)){
							s = (HashSet) txAddrHist.get(txHash);
						}
						s.add(addr);
						txAddrHist.put(txHash,s);
					}
				}

			} catch (Exception ex){}
		}
	}

	public void checkHistory(){
		boolean save = false;
		ArrayList myAddrs = new ArrayList();
		Iterator it = _history.keySet().iterator();
		while(it.hasNext()){
			Address addr = (Address)it.next();
			if(isMine(addr)){
				myAddrs.add(addr);
			} else {
				save = true;
				_history.remove(it);
			}
		}
		it = _history.keySet().iterator();
		while(it.hasNext()){
			Address addr = (Address)it.next();
			try{
				JSONArray hist = (JSONArray) _history.get(addr);
				for(int i =0 ; i< hist.length(); i++){
					String txHash = hist.getJSONArray(i).getString(0);
					int txHeight = hist.getJSONArray(i).getInt(1);
					if (prunedTxo.containsValue(txHash) ||
							txi.containsKey(txHash) ||
							txo.containsKey(txHash)){
								continue;
							}
					HashMap tx = (HashMap) transactions.get(txHash);
					if (tx != null){
						addTransaction(txHash, tx);// TO be Done
						save = true;
					}
				}
			} catch (Exception ex){
				ex.printStackTrace();
			}
		}
		if (save){
			save_transactions(true);// To Be Done
		}
	}

	public void addTransaction(String txHash, HashMap transaction){
		System.out.println("we Are In transaction adding");
		HashMap tx = Transaction.deserialize((String)transaction.get("raw"));
		// This place is wierd. It can arise NullPointer Exception. Replace Later!
		boolean isCoinbase = ((String)
													 ((HashMap)
													 		((ArrayList)tx.get("inputs"))
														 .get(0))
														.get("type"))
													.equals("coinbase");
		HashMap d = new HashMap();
		txi.put(txHash, d);
		System.out.println(((ArrayList)tx.get("inputs")).size());

	}

	public ArrayList getReceiveingAddresses(){
		return receivingAddresses;
	}

	public ArrayList getChangeAddresses(){
		return changeAddresses;
	}

	public ArrayList getAddresses(){
		ArrayList result = new ArrayList();
		for(int i =0; i < receivingAddresses.size(); i++ ){
			result.add(receivingAddresses.get(i));
		}
		for(int i =0; i < changeAddresses.size(); i++ ){
			result.add(changeAddresses.get(i));
		}
		return result;
	}

	public boolean isMine(Address address){
		return receivingAddresses.contains(address)|changeAddresses.contains(address);
	}

	public JSONArray getAddressHistory(Address address) {
		return (JSONArray)_history.get(address);
	}

	public HashMap getAddrIo(Address address) {
		HashMap result = new HashMap();
		JSONArray h = getAddressHistory(address);
		HashMap received = new HashMap();
		HashMap sent = new HashMap();
		for(int i=0; i< h.length(); i++){
			JSONArray x = h.optJSONArray(i);
			String txHash = x.optString(0);
			long height = x.optLong(1);
			HashMap txos = (HashMap)txo.get(txHash);
			JSONArray txoForAddress = (JSONArray)txos.get(address);
			if (txoForAddress!=null){
				for(int j = 0; j < txoForAddress.length(); j++){
					JSONArray entity = txoForAddress.optJSONArray(j);
					if (entity != null){
						int n = entity.optInt(0);
						long v = entity.optLong(1);
						boolean isCb = entity.optBoolean(2);
						String key = txHash + ":" + n;
						HashMap temp = new HashMap();
						temp.put("height", height);
						temp.put("v", v);
						temp.put("is_cb", isCb);
						received.put(key, temp);
					}
				}
			}
			HashMap txis = (HashMap)txi.get(txHash);
			JSONArray txiForAddress = (JSONArray)txis.get(address);
			if(txiForAddress!=null){
				for(int j = 0; j < txiForAddress.length(); j++){
					JSONArray entity = txiForAddress.optJSONArray(j);
					if (entity!=null){
						String txiLocal = entity.optString(0);
						long v = entity.optLong(1);
						sent.put(txiLocal, height);
					}
				}
			}
		}
		result.put("received", received);
		result.put("sent", sent);
		return result;
	}

	public HashMap getAddrUtxo(Address address){
		HashMap addrIo = getAddrIo(address);
		HashMap coins = (HashMap) addrIo.get("received");
		HashMap spent = (HashMap) addrIo.get("sent");
		Iterator it = spent.keySet().iterator();
		while(it.hasNext()){
			String txi = (String)it.next();
			coins.remove(txi);
		}
		HashMap out = new HashMap();
		it = coins.keySet().iterator();
		while(it.hasNext()){
			String txoLocal = (String)it.next();
			HashMap txoVals = (HashMap) coins.get(txoLocal);
			if (txoVals != null){
				long txHeight = (long) txoVals.get("height");
				long value = (long) txoVals.get("v");
				boolean isCb = (boolean) txoVals.get("is_cb");
				String[] prevouts = txoLocal.split(":");
				String prevoutHash = prevouts[0];
				String prevoutN =prevouts[1];
				HashMap x = new HashMap();
				x.put("address", address);
				x.put("value", value);
				x.put("prevout_n", Long.parseLong(prevoutN));
				x.put("prevout_hash", prevoutHash);
				x.put("height", txHeight);
				x.put("coinbase", isCb);
				out.put(txoLocal, x);
			}
		}
		return out;
	}

	public ArrayList getUtxos(ArrayList domain, boolean excludeFrozen,
														boolean mature, boolean confirmedOnly){
		ArrayList coins = new ArrayList();
		if(domain == null){
			domain = getAddresses();
		}
		if(excludeFrozen && (frozen_addresses!= null)){
			Iterator it = frozen_addresses.iterator();
			while(it.hasNext()){
				// Excluding of frozen should go here
			}
		}
		for(int i = 0; i < domain.size(); i++){
			Address addr = (Address) domain.get(i);
			HashMap utxos = getAddrUtxo(addr);
			Iterator it = utxos.keySet().iterator();
			while(it.hasNext()){
				String txHash = (String)it.next();
				HashMap	entity = (HashMap) utxos.get(txHash);
				if(entity != null) {
					long height = (long) entity.get("height");
					boolean coinbase = (boolean) entity.get("coinbase");
					if (confirmedOnly && (height <= 0)){
						continue;
					}
					if (mature && coinbase &&
							((height + COINBASE_MUTARITY)> getLocalHeight())){
						continue;
					}
					coins.add(entity);
				}
			}

		}
		return coins;
	}

	public ArrayList getSpendableCoins(ArrayList domain, HashMap config, boolean isInvoice){
		boolean confirmedOnly = false;
		try {
			confirmedOnly = (boolean)config.get("confirmed_only");
		} catch (Exception e) {};
		if (isInvoice) {
			confirmedOnly = true;
		}
		return getUtxos(domain, true, true, confirmedOnly);
	}

	public long getLocalHeight(){
		//Reimplement later! It's a STUB
		return (long) 600000;
	}

	public String getTxinType(Address address){
		return txinType;
	}

	public HashMap getAddressIndex(Address address){
		HashMap result = new HashMap();
		int receivingIndex = receivingAddresses.indexOf(address);
		int changeIndex = changeAddresses.indexOf(address);
		if (receivingIndex>=0){
			result.put("isChange", false);
			result.put("index", receivingIndex);
		} else if (changeIndex >=0) {
			result.put("isChange", true);
			result.put("index", changeIndex);
		}
		return result;
	}


	public String get_xpubkey(boolean isChange, int number){
		//Keystore type
		String result = "ff";
		try {
			byte[] tempo = Base58.decode_check(xpub);
			result += Util.bytesToHex(tempo);
		} catch (Exception ex){
			ex.printStackTrace();
		}
		// Is change?
		if(isChange){
			result += "0100";
		} else {
			result += "0000";
		}
		String numberString = String.format("%04X", number);
		result = result + numberString.substring(2,4) + numberString.substring(0,2);
		return result;
	}

	public void addInputSigInfo(HashMap txin, Address address){
		HashMap derivation = getAddressIndex(address);
		String xPubkey = get_xpubkey((boolean)derivation.get("isChange") , (int)derivation.get("index"));
		txin.put("x_pubkeys", new String[]{xPubkey});
		txin.put("num_sig", 1);
		txin.put("signatures", new ArrayList());

	}

	public void addInputInfo(HashMap txin){
		Address address = (Address) txin.get("address");
		if (isMine(address)){
			txin.put("type", getTxinType(address));
			HashMap rs = getAddrIo(address);
			HashMap received = (HashMap) rs.get("received");
			String key = (String)txin.get("prevout_hash")+":"+(long)txin.get("prevout_n");
			HashMap item = (HashMap) received.get(key);
			long value = (long) item.get("v");
			addInputSigInfo(txin, address);
		}
	}

	public int getNumTx(Address address){
		return ((JSONArray)getAddressHistory(address)).length();
	}

	public Transaction makeUnsignedTransaction(ArrayList inputs, ArrayList outputs,
																 HashMap config, long fixedFee, Address changeAddr)
																 throws Exception {
		//Here should come the part to spend the maximum. Skip it for now
		if (inputs.size() == 0) {
			throw new Exception("Not enough funds");
		}
		long feePerKb = 0;
		try {
			feePerKb = (long) config.get("fee_per_kb");
		} catch (Exception ex) {}
		// if ((fixedFee == 0 ) && feePerKb == 0){
		// 	throw new Exception("Dynamic fee estimates not available");
		// }

		for(int i = 0; i < inputs.size(); i++){
			HashMap txin = (HashMap) inputs.get(i);
			addInputInfo(txin);
		}
		//Change Addreses
		Address[] changeAddrs = new Address[1];
		if (changeAddr != null){
			changeAddrs[0] = changeAddr;
		} else {
			ArrayList addrs = new ArrayList();
			int initial = 0;
			if (changeAddresses.size() > gap_limit_for_change){
				initial = changeAddresses.size() - gap_limit_for_change;
			}
			for (int i = initial; i < changeAddresses.size(); i++ ){
				addrs.add(changeAddresses.get(i));
			}
			if (use_change && (addrs.size()>0)){
				ArrayList indexes = new ArrayList();
				for(int i = 0; i < addrs.size();i++){
					Address addr = (Address)addrs.get(i);
					if (getNumTx(addr) == 0){
						indexes.add(i);
					}
				}
				if (indexes.size() > 0){
					int randIndex = (int) indexes.get((int)(Math.random()*indexes.size()));
					changeAddrs[0] = (Address)addrs.get(randIndex);
				}
			} else {
				changeAddrs[0]= (Address)((HashMap)inputs.get(0)).get("address");
			}
		}
		HashSet txins = new HashSet();
		for(int i =0; i< inputs.size(); i++){
			HashMap input	= (HashMap) inputs.get(i);
			Address addr = (Address)input.get("address");
			boolean isChange = receivingAddresses.indexOf(addr) < 0;
			String addrStr = addr.toString((byte)1);
			int height = (int)((long)input.get("height"));
			String[] pubkeys = (String[])input.get("x_pubkeys");
			String prevout_hash = (String)input.get("prevout_hash");
			int prevout_n = (int)((long)input.get("prevout_n"));
			BigInteger amount = BigInteger.valueOf((long) input.get("value"));
			WalletAddress wAddr = new WalletAddress(addrStr, height, isChange, pubkeys[0]);
			TxIn txin = new TxIn(wAddr, prevout_hash, prevout_n, amount);
			txins.add(txin);
		}
		HashSet txouts = new HashSet();
		for(int i=0; i< outputs.size(); i++){
			HashMap output = (HashMap) outputs.get(i);
			String addrStr = ((Address)output.get("address")).toString((byte)1);
			BigInteger amount = BigInteger.valueOf((long)output.get("value"));
			TxOut txout = new TxOut(addrStr, amount);
			txouts.add(txout);
		}
		HashSet changes = new HashSet();
		changes.add(((Address)changeAddrs[0]).toString((byte)1));
		// Coinchooser coinchooser = new Coinchooser();
		Transaction tx = null;
		try {
			tx = new Coinchooser().make_tx(txins, txouts, changes);
		} catch (Exception ex){
			ex.printStackTrace();
		}
		return tx;
	}



	public String mktx(ArrayList outputs, String password,
											HashMap config, long fee,
											Address changeAddr, ArrayList domain){
		// get spendable coins
		ArrayList coins = getSpendableCoins(domain, config, false);
		Transaction tx = null;
		String result = "";
		try{
			tx = makeUnsignedTransaction(coins, outputs, config, fee, changeAddr);
			result = tx.Generate(xprv);
		} catch (Exception e){
			e.printStackTrace();
		}
		return result;
	}

	public int relayFee(Network network) {
		int relay_fee = 0;
		if(network != null)
			relay_fee = network.getRelay_fee();
		if(relay_fee <= 5000 ) {
			return 5000;
		}
		if(relay_fee >= 50000) {
			return 50000;
		}
		return relay_fee;

	}

	private boolean up_to_date;

	public String getXPRV() {

		//NEED TO IMPLEMENT.  FOR NOW JUST RETURN PLACEHOLDER VALUE.
		return xprv;
		// return "xprv9s21ZrQH143K3NZogWLRFKUKWTuQdoQFtUEZVbkb1NqQZPdG81n6uy1tAvUQc1AqiB3HfkFWZD2eT6EwqSfUzaETnZG2wDpdrFQqsoS9EVp";

	}

	public static BigInteger getLocalBlockHeight() {

		return new BigInteger("525914");
	}

	public boolean is_up_to_date() {
		return up_to_date;
	}

	public void set_up_to_date(boolean up_to_date) {
		this.up_to_date = up_to_date;
		if(up_to_date) {
			save_transactions(true);
		}
	}

	private void save_transactions(boolean b) {
		// TODO Auto-generated method stub

	}

	public Set get_address_history(String addr) {
		// TODO Auto-generated method stub
		return null;
	}

	public JSONObject transactions() {
		return new JSONObject();
	}

	public void receive_tx_callback(String tx_hash, Transaction tx, int tx_height) {
		// TODO Auto-generated method stub

	}


} // end class
