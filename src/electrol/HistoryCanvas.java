package electrol;

import java.util.Date;

import java.util.Enumeration;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.java.util.Arrays;
import electrol.main.Storage;
import electrol.util.Util;

public class HistoryCanvas extends Canvas implements CommandListener {
	private ScrollableTable table = null;
	private String[] hash;
	private String url_prefix = "https://www.blockchair.com/bitcoin-cash/transaction/";
	private Electron electron;
	private Command back;

	public HistoryCanvas(Electron electron) {
		back = new Command("Back", Command.BACK, 0);
		this.electron = electron;
		try {
			this.table = new ScrollableTable(get_history(), getWidth() - 20, getHeight() - 20);
		} catch (JSONException je) {
			je.printStackTrace();
		}
		addCommand(back);
		setCommandListener(this);
	}

	protected void keyPressed(int key) {
		int keyCode = getGameAction(key);

		if (keyCode == FIRE) {
			if (table.currentRow != 0) {
				String url = url_prefix + hash[table.currentRow - 1];
				Display.getDisplay(electron).setCurrent(new Alert("Blockchair Url", url, null, AlertType.CONFIRMATION));
			}
		} else {
			if (keyCode == Canvas.UP || keyCode == Canvas.DOWN) {
				table.keyPressed(keyCode);
				repaint();
			}
		}
	}

	public String[][] get_history() throws JSONException {
		Storage storage = electron.storage();
		JSONObject object = storage.get("verified_tx3", new JSONObject());
		Enumeration e = object.keys();
		int[] keys = new int[object.length()];
		int i = 0;
		JSONObject processor = new JSONObject();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();

			JSONArray array = object.getJSONArray(key);
			keys[i] = array.optInt(1);
			processor.put(key, array);
			i++;
		}
		Arrays.sort(keys);
		String[][] data = new String[object.length() + 1][3];
		hash = new String[keys.length];
		data[0] = new String[] { "Date", "Amount", "Balance" };
		Date date = new Date();
		float amount = 0;
		for (int k = 0; k < object.length(); k++) {
			e = processor.keys();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();

				JSONArray array = processor.getJSONArray(key);
				long tmpTime = array.optLong(1);
				if (keys[k] == tmpTime) {
					hash[k] = key;
					date.setTime(tmpTime * 1000);
					float tx_delta = get_tx_delta(storage, key, array.optInt(2)) / 200;
					amount += tx_delta;
					data[i - k][0] = date.toString();
					data[i - k][1] = String.valueOf(tx_delta);
					data[i - k][2] = String.valueOf(Util.round(amount, 1));
					break;
				}
			}
		}
		return data;
	}

	public float get_tx_delta(Storage store, String hash, int amt) throws JSONException {
		JSONObject txi = store.get("txi", new JSONObject());
		JSONObject txo = store.get("txo", new JSONObject());
		JSONObject txiOut = txi.getJSONObject(hash);
		Enumeration e = txiOut.keys();
		int inAmount = 0;
		int outAmount = 0;
		while (e.hasMoreElements()) {
			JSONArray array = txiOut.getJSONArray(e.nextElement().toString());
			for (int i = 0; i < array.length(); i++) {
				// JSONArray nested = array.getJSONArray(i);
				inAmount += array.optInt(1);
			}
		}
		JSONObject txoOut = txo.getJSONObject(hash);
		Enumeration e1 = txoOut.keys();
		while (e1.hasMoreElements()) {
			JSONArray array = txoOut.getJSONArray(e1.nextElement().toString());
			for (int i = 0; i < array.length(); i++) {
				// JSONArray nested = array.getJSONArray(i);
				outAmount += array.optInt(1);
			}
		}
		return outAmount - inAmount;
	}

	protected void paint(Graphics g) {
		g.setColor(0xffffff);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.translate(10, 10);
		table.paint(g);
		g.translate(-10, -10);
	}

	public void commandAction(Command cmd, Displayable arg1) {

		if (cmd.equals(back)) {
			Display.getDisplay(electron).setCurrent(electron.getList());
		}
	}
}