package electrol;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import org.bouncycastle.util.Arrays;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.java.util.Map;
import electrol.main.AddressUtil;
import electrol.main.ApplicationContext;
import electrol.main.Bitcoin;
import electrol.main.Blockchain;
import electrol.main.BlockchainsUtil;
import electrol.main.Config;
import electrol.main.MasterKeys;
import electrol.main.Network;
import electrol.main.NetworkUtil;
import electrol.main.Storage;
import electrol.main.TcpConnection;
import electrol.util.Constants;
import electrol.util.Files;
import electrol.util.Util;



public class Electron extends MIDlet implements CommandListener{

	private Command select, back;
	private Display display;
	private List list; 
	private Network network;
	private boolean update = false;
	private ApplicationContext context;
	private Storage storage;
	public Electron() {
		display = Display.getDisplay(this);
		back=new Command("Back",Command.BACK,1);
		select=new Command("Select",Command.OK,2);
		list = new List("Options", List.EXCLUSIVE);
		list.append("History", null);
		list.append("Send", null);
		list.append("Receive", null);
		list.append("Seed", null);
		list.addCommand(back);
		list.addCommand(select);
		list.setCommandListener(this);

	}

	public void start() {
		
		context = new ApplicationContext();
		context.setLatestBlock(false);	

		Config config = new Config();
		context.setDefaultServer("165.227.202.193:50002:s");
		context.setConfig(config);
		try {
			checkOrGenerateAddress();
			Map blockchains = BlockchainsUtil.read_blockchain();
			System.out.println("starting download");
			NetworkUtil.init_header_file((Blockchain)(blockchains.get(new Integer(0))));
			alert("check or generate address");
			/*
			TcpConnection tcp = new TcpConnection(NetworkUtil.deserialize_server(context.getDefaultServer()));
			context.setTcpConnection(tcp);
			network = new Network(context,storage);
			network.start();*/
			
		}catch(Exception e) {
			alert(e.getMessage());
		}		
	}

	

	

	public void checkOrGenerateAddress() throws JSONException, Exception {
		JSONObject jsonObject = storage.get("keystore", new JSONObject());
		if(jsonObject.length() == 0) {
			String seed = storage.get("seed", "");
			MasterKeys masterKeys = new MasterKeys();
			byte[] bip32root = masterKeys.get_seed_from_mnemonic(seed);
			byte[] root512 = Bitcoin.get_root512_from_bip32root(bip32root);

			byte[] kBytes = Arrays.copyOfRange(root512, 0, 32);
			byte[] cBytes = Arrays.copyOfRange(root512, 32, 64);
			String jonaldPubKey = Bitcoin.get_pubkey_from_secret(kBytes);
			String serializedXprv = Bitcoin.serializeXprv(cBytes, kBytes);
			byte[] cKbytes = Util.hexStringToByteArray(jonaldPubKey);
			String serializedXpub = Bitcoin.serializeXpub(cBytes, cKbytes);
			String final_xprv = Bitcoin.base_encode_58(serializedXprv);
			String final_xpub = Bitcoin.base_encode_58(serializedXpub);
			jsonObject.put("xprv", final_xprv);
			jsonObject.put("xpub", final_xpub);
		}
		JSONObject addresses = storage.get("addresses", new JSONObject());
		JSONObject addr_history = storage.get("addr_history", new JSONObject());
		JSONArray change = addresses.optJSONArray("change");
		if(change== null || change.length() == 0) {
			AddressUtil addressUtil = new AddressUtil(jsonObject.getString("xpub"));
			String[] changeAddresses = addressUtil.generateLegacyAddresses(14, AddressUtil.ADDRESS_TYPE_CHANGE);
			change = new JSONArray();
			for(int i =0 ;i<changeAddresses.length;i++) {
				change.put(changeAddresses[i]);
				addr_history.put(changeAddresses[i], new JSONArray());
			}

			addresses.put("change", change);
			storage.put("addresses", addresses);
		}
		JSONArray receiving = addresses.optJSONArray("receiving");
		if(receiving== null || receiving.length() == 0) {
			AddressUtil addressUtil = new AddressUtil(jsonObject.getString("xpub"));
			String[] receivingAddresses = addressUtil.generateLegacyAddresses(27, AddressUtil.ADDRESS_TYPE_RECEIVING);
			receiving = new JSONArray();
			for(int i =0 ;i<receivingAddresses.length;i++) {
				receiving.put(receivingAddresses[i]);
				addr_history.put(receivingAddresses[i], new JSONArray());
			}
			addresses.put("receiving", receiving);
			storage.put("addr_history", addr_history);
			storage.put("addresses", addresses);
		}
		storage.write();
	}

	public void setUpdate(boolean update) {
		this.update = update;
	}
	public void commandAction(Command c, Displayable d) {
		if(c.getCommandType() == Command.OK) {
			if(list.getSelectedIndex() == 0) {
				display.setCurrent(new HistoryCanvas(this));
			}
			if(list.getSelectedIndex() == 1) {
				if(context.isLatestBlock()) {
					alert("Wait!!! Block verification is processing");
				}
				else {
					display.setCurrent(new PayForm(network,this));
				}
			}
			if(list.getSelectedIndex() == 2) {
				display.setCurrent(new ReceiveForm(this, storage));
			}
			if(list.getSelectedIndex() == 3) {			
				Alert alert = new Alert("Your Seed Is", storage.get("seed", "") , null, AlertType.INFO);
				alert.setTimeout(Alert.FOREVER);
				display.setCurrent(alert);
			}
		}
	}

	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {

	}

	protected void pauseApp() {

	}

	protected void startApp() throws MIDletStateChangeException {
		boolean isExist = Files.isExist(Constants.STORAGE_PATH);
		if(isExist) {
			PasswordUI passwordUI = new PasswordUI(this);
			display.setCurrent(passwordUI);
		}
		else {
			SeedUI seedUI = new SeedUI(this);
			display.setCurrent(seedUI);
		}
	}

	private void alert(String alertMsg) {
		Alert alert = new Alert("Error",alertMsg , null, AlertType.ERROR);
		alert.setTimeout(1000);
		display.setCurrent(alert);
	}

	public List getList() {
		return list;
	}

	public Storage storage() {
		return storage;
	}
	public void setStorage(Storage storage) {
		this.storage = storage;
	}
	
	public Network getNetwork() {
		return network;
	}
}