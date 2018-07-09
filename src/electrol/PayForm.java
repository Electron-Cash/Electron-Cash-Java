package electrol;
import java.util.Enumeration;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.java.util.HashSet;
import electrol.java.util.Set;
import electrol.main.AddressUtil;
import electrol.main.Bitcoin;
import electrol.main.Coinchooser;
import electrol.main.Network;
import electrol.main.Storage;
import electrol.main.Transaction;
import electrol.main.TxIn;
import electrol.main.TxOut;
import electrol.main.WalletAddress;
import electrol.util.BigInteger;
import electrol.util.Constants;



public class PayForm extends Form implements CommandListener{
	public static final String SEPARATOR = ":";

	private TextField address;
	private TextField amount;
	private Command pay, back  ,clear;
	private Network network;
	
	public PayForm(String title, Network network) {
		super(title);
		address = new TextField("Pay To", "", 50, TextField.ANY);
		address.setString("qz5yu37zud465eqkmxcdmlvtuppykq7nm5a50weg9v");
		//address.setString("qzqpvnj8ct3kh2nyzmvmqq8a30syyjcr60wsqnm52tn0t");
		amount = new TextField("Amount   ", "", 10, TextField.NUMERIC);
		back=new Command("Back",Command.BACK,0);
		pay=new Command("Send",Command.OK,0);
		clear  = new Command("Clear",Command.CANCEL,0);
		append(address);
		append(amount);
		addCommand(pay);
		addCommand(back);
		addCommand(clear);
		setCommandListener(this);
		this.network = network;
	}
	public void commandAction(Command arg, Displayable arg1) {
		if(AddressUtil.addressIsValid(address.getString())) {
			
			try {
				Storage storage = new Storage(Constants.STORAGE_PATH);
				TxOut out = new TxOut(address.getString(), new BigInteger(amount.getString()+"00"));
				Set sout = new HashSet();
				sout.add(out);
				ReceivingAddress receivingAddress = new ReceivingAddress();
				Set changeSet = new HashSet();
				changeSet.add(receivingAddress.get_address(storage,true));
				Set txin = get_txin(storage);
				Transaction tx = new Coinchooser().make_tx(txin, sout, changeSet);
				String txHash = tx.Generate();
				network.queue_request("blockchain.transaction.broadcast", new String[] {txHash}, null);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println(amount.getString());
		
	}
	
	public Set get_txin(Storage storage) throws JSONException {
		Set set = new HashSet();
		JSONObject verified_tx3 = storage.get("verified_tx3",new JSONObject());
		JSONObject txo = storage.get("txo",new JSONObject());
		JSONObject txi = storage.get("txi",new JSONObject());
		Enumeration e1 = txi.keys();
		Set txiKeySet = new HashSet();
		while(e1.hasMoreElements()) {
			String key = (String)e1.nextElement();
			JSONObject obj = txi.getJSONObject(key);
			Enumeration e2 = obj.keys();
			while(e2.hasMoreElements()) {
				txiKeySet.add((String)e2.nextElement());
			}
		}
			
		JSONObject addresses = storage.get("addresses", new JSONObject());
		JSONObject keystore = storage.get("keystore", new JSONObject());
		
		JSONArray changeArray = addresses.getJSONArray("change");
		JSONArray receivingArray = addresses.getJSONArray("receiving");
		
		String masterXPUB = keystore.getString("xpub");
		Enumeration tx = verified_tx3.keys();
		while(tx.hasMoreElements()) {
			String key = (String)tx.nextElement();
			JSONObject txoElements = txo.getJSONObject(key);
			Enumeration e = txoElements.keys();
			while(e.hasMoreElements()) {
				String add = (String)e.nextElement();
				if(!txiKeySet.contains(add)) {
					JSONArray array1 = txoElements.getJSONArray(add);
					for(int i=0;i < array1.length();i++) {
						JSONArray nestedArray = array1.getJSONArray(i);
						boolean change = true;
						int index = getIndex(changeArray, add);
						if(index == -1) {
							change = false;
						}
						if(!change) {
							index = getIndex(receivingArray, add);
						}
						String pubkey = Bitcoin.fetchPublicKey(masterXPUB, AddressUtil.ADDRESS_TYPE_CHANGE, index);
						
						WalletAddress walletAddress1 = new WalletAddress(add, index,change, pubkey);
						TxIn txin = new TxIn(walletAddress1, key, nestedArray.optInt(0),new BigInteger(nestedArray.optString(1)) );					
						set.add(txin);
					}					
				}
			}
		}
		return set;
	}
	
	public int getIndex(JSONArray addresses , String address) {
		for(int i =0 ;i < addresses.length(); i++) {
			if(addresses.optString(i).equals(address)) {
				return i;
			}
		}
		return -1;
	}
}
