package org.electroncash;

import java.io.IOException;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.electroncash.security.SecureSocketConnection;
import org.electroncash.util.AddressUtil;
import org.electroncash.util.ApplicationContext;
import org.electroncash.util.Bitcoin;
import org.electroncash.util.Blockchain;
import org.electroncash.util.BlockchainsUtil;
import org.electroncash.util.Config;
import org.electroncash.util.Files;
import org.electroncash.util.Map;
import org.electroncash.util.MasterKeys;
import org.electroncash.util.Network;
import org.electroncash.util.NetworkUtil;
import org.electroncash.util.Storage;
import org.electroncash.util.Util;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public class Electron extends MIDlet implements CommandListener
{
	private Command select;
	private Command back;
	private Display display;
	private List list;
	private Storage storage;
	private Network network;
	private ApplicationContext context;
	private AlertUtil alertUtil;

	public Electron()
	{
		display = Display.getDisplay(this);
		back = new Command("Back", 2, 1);
		select = new Command("Select", 4, 2);
		list = new List("Options", 1);
		alertUtil = new AlertUtil(display, false);
		context = new ApplicationContext();
		context.setLatestBlock(false);
		context.setAlertUtil(alertUtil);
		Config config = new Config();
		context.setDefaultServer("blackie.c3-soft.com:50002:s");
		context.setConfig(config);
		
		list.append("History", null);
		list.append("Send", null);
		list.append("Receive", null);
		list.append("Seed", null);
		list.addCommand(back);
		list.addCommand(select);
		list.setCommandListener(this);
	}

	public void start(Storage storage)
	{
		this.storage = storage;
		try {
			checkOrGenerateAddress();
			network = new Network(context, storage);
			network.start();
		} catch (ClassCastException e) {
			alertUtil.sendErrorAlert(e.getMessage());
		} catch (IllegalArgumentException e) {
			alertUtil.sendErrorAlert(e.getMessage());
		} catch (NullPointerException e) {
			alertUtil.sendErrorAlert(e.getMessage());
		} catch (RuntimeException e) {
			alertUtil.sendErrorAlert(e.getMessage());
		} catch (IOException e) {
			alertUtil.sendErrorAlert(e.getMessage());
		} catch (JSONException e) {
			alertUtil.sendErrorAlert("JSONException "+e.getMessage());
		} catch (InvalidCipherTextException e) {
			alertUtil.sendErrorAlert(e.getMessage());
		}
	}

	protected void destroyApp(boolean arg0) throws MIDletStateChangeException{}

	protected void pauseApp() {}

	protected void startApp() throws MIDletStateChangeException
	{
		try
		{
			Map blockchains = BlockchainsUtil.read_blockchain();
			NetworkUtil.init_header_file((Blockchain)blockchains.get(new Integer(0)));
			SecureSocketConnection tcp = new SecureSocketConnection(NetworkUtil.deserialize_server(context.getDefaultServer()));
			context.setSocketConnection(tcp);
			boolean isExist = Files.isExist("default_wallet");
			if (isExist) {
				PasswordUI passwordUI = new PasswordUI(this, alertUtil);
				display.setCurrent(passwordUI);
			}
			else {
				SeedUI seedUI = new SeedUI(this, alertUtil);
				display.setCurrent(seedUI);
			}
		} catch (IOException e) {
			alertUtil.sendErrorAlert("IOExcption " + e.getMessage());
		}
		catch (OutOfMemoryError e) {
			alertUtil.sendErrorAlert(e.getMessage());
		} catch (NullPointerException e) {
			alertUtil.sendErrorAlert(e.getMessage());
		} catch (Exception e) {
			alertUtil.sendErrorAlert("General "+e.getMessage());
		}
	}

	public void commandAction(Command c, Displayable d)
	{
		if (c.getCommandType() == 4) {
			if (list.getSelectedIndex() == 0) {
				display.setCurrent(new HistoryCanvas(this));
			}
			if (list.getSelectedIndex() == 1) {
				if (context.isLatestBlock()) {
					alertUtil.sendInfoAlert( "Wait!!! Block verification is processing");
				}
				else {
					display.setCurrent(new PayForm(network, this, storage));
				}
			}
			if (list.getSelectedIndex() == 2) {
				display.setCurrent(new ReceiveForm(this, storage));
			}
			if (list.getSelectedIndex() == 3) {
				Alert alert = new Alert("Your Seed Is", storage.get("seed", ""), null, AlertType.INFO);
				alert.setTimeout(-2);
				display.setCurrent(alert);
			}
		}
	}

	public List getList()
	{
		return list;
	}

	public Storage getStorage() {
		return storage;
	}

	public void checkOrGenerateAddress() throws JSONException, DataLengthException, IllegalStateException, InvalidCipherTextException, IOException {
		JSONObject jsonObject = storage.get("keystore", new JSONObject());
		if (jsonObject.length() == 0) {
			String seed = storage.get("seed", "");
			byte[] bip32root = MasterKeys.get_seed_from_mnemonic(seed);
			byte[] root512 = Bitcoin.get_root512_from_bip32root(bip32root);
			byte[] kBytes = new byte[32];
			System.arraycopy(root512, 0, kBytes, 0, 32);
			byte[] cBytes = new byte[32];
			System.arraycopy(root512, 32, cBytes, 0, 32);
			String jonaldPubKey = Bitcoin.get_pubkey_from_secret(kBytes);
			String serializedXprv = Bitcoin.serializeXprv(cBytes, kBytes);
			byte[] cKbytes = Util.hexStringToByteArray(jonaldPubKey);
			String serializedXpub = Bitcoin.serializeXpub(cBytes, cKbytes);
			String final_xprv = Bitcoin.base_encode_58(serializedXprv);
			String final_xpub = Bitcoin.base_encode_58(serializedXpub);
			jsonObject.put("xprv", final_xprv);
			jsonObject.put("xpub", final_xpub);
			storage.put("keystore", jsonObject);
		}
		JSONObject addresses = storage.get("addresses", new JSONObject());
		JSONObject addr_history = storage.get("addr_history", new JSONObject());
		JSONArray change = addresses.optJSONArray("change");
		if ((change == null) || (change.length() == 0)) {
			AddressUtil addressUtil = new AddressUtil(jsonObject.getString("xpub"));
			String[] changeAddresses = addressUtil.generateLegacyAddresses(14, 1);
			change = new JSONArray();
			for (int i = 0; i < changeAddresses.length; i++) {
				change.put(changeAddresses[i]);
				addr_history.put(changeAddresses[i], new JSONArray());
			}
			addresses.put("change", change);
		}
		JSONArray receiving = addresses.optJSONArray("receiving");
		if ((receiving == null) || (receiving.length() == 0)) {
			AddressUtil addressUtil = new AddressUtil(jsonObject.getString("xpub"));
			String[] receivingAddresses = addressUtil.generateLegacyAddresses(27, 0);
			receiving = new JSONArray();
			for (int i = 0; i < receivingAddresses.length; i++) {
				receiving.put(receivingAddresses[i]);
				addr_history.put(receivingAddresses[i], new JSONArray());
			}
			addresses.put("receiving", receiving);
		}
		storage.put("addr_history", addr_history);
		storage.put("addresses", addresses);
		storage.write();
	}
}
