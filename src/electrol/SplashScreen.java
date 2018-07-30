package electrol;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

import org.json.me.JSONObject;
import electrol.main.Storage;

public class SplashScreen extends Form implements Runnable{
	private Storage storage;
	private Electron electron;
	private StringItem stringItem;
	public SplashScreen(Electron electron, Storage storage) {
		super("Wait...");
		this.storage = storage;
		this.electron = electron;
		this.stringItem = new StringItem("Synching wallet with blockchain.  This can take a few minutes, please wait...", "");
		append(stringItem);
	}

	public void run() {
		JSONObject jsonObject = storage.get("addr_history",new JSONObject());
		while (electron.getNetwork() == null || jsonObject.length() != electron.getNetwork().getSubscribedAddresses().size() || electron.getNetwork().isDownloadingHeaders()) 
		{}
		Display.getDisplay(electron).setCurrent(electron.getList());
	}

}
