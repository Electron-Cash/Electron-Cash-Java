package electrol;


import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.main.AddressUtil;
import electrol.main.ApplicationContext;
import electrol.main.Main;
import electrol.main.Network;
import electrol.main.NetworkUtil;
import electrol.main.Seed;
import electrol.main.Storage;
import electrol.main.Synchronization;
import electrol.main.TcpConnection;
import electrol.main.Wallet;
import electrol.main.test.Network1;
import electrol.util.Constants;



public class Electron extends MIDlet implements CommandListener{

	private Command select, back;
	private Display display;
	private List list; 
	Network network;
	private boolean update = false;
	private ApplicationContext context;
	
	public Electron() {
		display = Display.getDisplay(this);
		context = new ApplicationContext();
		context.setLatestBlock(false);
		
		try {
			checkOrGenerateAddress();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		back=new Command("Back",Command.BACK,0);
		select=new Command("Select",Command.OK,0);
		list = new List("Options", List.EXCLUSIVE);
		list.append("History", null);
		list.append("Send", null);
		list.append("Receive", null);
		list.append("Addresses", null);
		list.append("Coins", null);
		list.addCommand(back);
		list.addCommand(select);
		list.setCommandListener(this);
		display.setCurrent(list);
		ApplicationContext context = new ApplicationContext();
		context.setDefaultServer("165.227.202.193:50002:s");
		
		try {
		TcpConnection tcp = new TcpConnection(NetworkUtil.deserialize_server(context.getDefaultServer()));
		
		context.setTcpConnection(tcp);
		
		Network1 network = new Network1(context,new Storage(Constants.STORAGE_PATH));
		network.start();
		while(!context.isLatestBlock()) {
		}
		network.subscribeAddresses();
		network.get_transaction();
		}catch(Exception e) {
			e.printStackTrace();
		}		
		
	}
	
	public void checkOrGenerateAddress() throws JSONException {
		Storage storage = new Storage(Constants.STORAGE_PATH);
		JSONObject jsonObject = storage.get("keystore", new JSONObject());
		if(jsonObject.length() == 0) {
			Seed seed = new Seed();
			String test = seed.testSeed();
			jsonObject.put("seed", test);
			Main main = new Main();
			String keys[] = main.getKeys(test);
			jsonObject.put("xprv", keys[0]);
			jsonObject.put("xpub", keys[1]);
			storage.put("keystore", jsonObject);
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
				display.setCurrent(new PayForm("Send",network));
			}
			if(list.getSelectedIndex() == 2) {
				display.setCurrent(new ReceiveForm("Receive"));
			}
			if(list.getSelectedIndex() == 3) {
				display.setCurrent(new AddressesCanvas());
			}
			if(list.getSelectedIndex() == 4) {
				display.setCurrent(new CoinCanvas());
			}
		}
	}
	
	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {

	}

	protected void pauseApp() {

	}

	protected void startApp() throws MIDletStateChangeException {
		display.setCurrent(list);

	}
	
}