package org.electroncash;

import java.io.IOException;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDletStateChangeException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.electroncash.util.Storage;
import org.json.me.JSONException;

public class PasswordUI extends Form implements CommandListener
{
	private Command exit = new Command("Exit", 7, 1);
	private Command set = new Command("Set", 1, 2);
	private Command ok = new Command("Ok", 1, 3);
	private TextField password = new TextField("Password", "", 20, 65536);
	private TextField cpassword = new TextField("Confirm Password", "", 20, 65536);
	private Electron electron;
	private Display display;
	private String seed;
	private AlertUtil alertUtil;

	public PasswordUI(Electron electron, String seed, AlertUtil alertUtil) { 
		super("Set Password");
		this.electron = electron;
		this.seed = seed;
		this.alertUtil = alertUtil;
		display = Display.getDisplay(electron);
		append(password);
		append(cpassword);
		addCommand(exit);
		addCommand(set);
		setCommandListener(this);
	}

	public PasswordUI(Electron electron, AlertUtil alertUtil) { 
		super("Enter Password");
		this.electron = electron;
		this.alertUtil = alertUtil;
		display = Display.getDisplay(electron);
		append(password);
		addCommand(exit);
		addCommand(ok);
		setCommandListener(this);
	}

	public void commandAction(Command cmd, Displayable d) { if (cmd.equals(set)) {
		String passwordText = password.getString();
		String cpasswordText = cpassword.getString();
		if (passwordText.length() < 6) {
			alertUtil.sendErrorAlert(this, "Password minimum Length is 6");
		}
		else if (!passwordText.equals(cpasswordText)) {
			alertUtil.sendErrorAlert(this, "Password and Confirm password are not equals.");

		}
		else if (setStorageInfo(passwordText)) {
			display.setCurrent(electron.getList());
		}
	}

	if (cmd.equals(ok)) {
		String passwordText = password.getString();
		if (setStorageInfo(passwordText)) {
			display.setCurrent(electron.getList());
		}
	}

	if (cmd.equals(exit)) {
		try {
			electron.destroyApp(false);
		} catch (MIDletStateChangeException e) {
			alertUtil.sendErrorAlert(this, e.getMessage());
		}
	}
	}

	private boolean setStorageInfo(String password)
	{
		try {
			Storage storage = new Storage("default_wallet", password, true);
			if ((seed != null) && (seed.length() > 0)) {
				storage.put("seed", seed);
				storage.write();
			}
			electron.start(storage);

			return true;
		} catch (InvalidCipherTextException e) {
			alertUtil.sendErrorAlert(this, "Password Is not correct " + e.getMessage());
			return false;
		} catch (IOException e) {
			alertUtil.sendErrorAlert(this, "IOException " + e.getMessage());
			return false;
		} catch (JSONException e) {
			alertUtil.sendErrorAlert(this, "JSON Exception " + e.getMessage());
			return false;
		} catch (Exception e) {
			alertUtil.sendErrorAlert(this, "Exception " + e.getMessage()); }
		return false;
	}
}
