package electrol;
import java.io.IOException;

import java.util.Enumeration;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import org.bouncycastle.crypto.InvalidCipherTextException;
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
	private TextField secret;
	private Command pay, back  ,clear;
	private Network network;
	private Display display;
	private Electron electron;
	public PayForm(Network network, Electron electron) {
		super("Send");
		this.electron = electron;
		display = Display.getDisplay(electron);
		address = new TextField("Pay To", "", 50, TextField.ANY);
		amount = new TextField("Amount   ", "", 10, TextField.NUMERIC);
		secret = new TextField("Password   ", "", 10, TextField.PASSWORD);
		back=new Command("Back",Command.BACK,0);
		pay=new Command("Send",Command.OK,0);
		
		clear  = new Command("Clear",Command.CANCEL,0);
		append(address);
		append(amount);
		append(secret);
		addCommand(pay);
		addCommand(back);
		addCommand(clear);
		setCommandListener(this);
		this.network = network;
	}
	public void commandAction(Command arg, Displayable displayable) {
		if(arg.equals(pay)) {
			if(pay()) {
				Alert alert = new Alert("Success", "Amount Processed Successfully!!!", null, AlertType.CONFIRMATION);
				alert.setTimeout(Alert.FOREVER);
				display.setCurrent(alert, electron.getList());
			}
		}
		else if(arg.equals(back)) {
			display.setCurrent(electron.getList());
		}
		else {
			address.setString("");
			amount.setString("");
			secret.setString("");
		}
	}
	
	public boolean pay() {
		if(AddressUtil.addressIsValid(address.getString())) {
			try {
				
				Storage storage = new Storage(Constants.STORAGE_PATH, secret.getString());
				TxOut out = new TxOut(address.getString(), new BigInteger(amount.getString()+"00"));
				Set sout = new HashSet();
				sout.add(out);
				ReceivingAddress receivingAddress = new ReceivingAddress();
				Set changeSet = new HashSet();
				changeSet.add(receivingAddress.get_address(storage,true));
				Set txin = get_txin(storage);
				Transaction tx = new Coinchooser(storage).make_tx(txin, sout, changeSet);
				String txHash = tx.Generate();
				String response = network.queue_synch_request("blockchain.transaction.broadcast", new String[] {txHash});
				infoAlert(response);
				return true;
			} catch (JSONException e) {
				alert("Error: "+e.getMessage());
				return false;
			} catch (InvalidCipherTextException e) {
				alert("Password is not correct");
				return false;
			} catch (IOException e) {
				alert("Error: "+e.getMessage());
				return false;
			} catch (Exception e) {
				alert("Error: "+e.getMessage());
				return false;
			} 
		}
		else {
			alert("Invalid Receiver Address!!!");
			return false;
		}
	}
	
	private void infoAlert(String alertMsg) {
		Alert alert = new Alert("Response",alertMsg , null, AlertType.CONFIRMATION);
		alert.setTimeout(Alert.FOREVER);
		display.setCurrent(alert, this);
	}
	
	private void alert(String alertMsg) {
		Alert alert = new Alert("Error",alertMsg , null, AlertType.ERROR);
		alert.setTimeout(Alert.FOREVER);
		display.setCurrent(alert, this);
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
					JSONArray nestedArray = txoElements.getJSONArray(add);
					//System.out.println(array1);
					//for(int i=0;i < array1.length();i++) {
						//JSONArray nestedArray = array1.getJSONArray(i);
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
					//}					
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
