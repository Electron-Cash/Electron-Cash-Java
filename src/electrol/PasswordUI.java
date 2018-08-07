package electrol;

import java.io.IOException;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDletStateChangeException;

import org.bouncycastle.crypto.InvalidCipherTextException;
import electrol.main.Storage;
import electrol.util.Constants;

public class PasswordUI extends Form implements CommandListener{

	private Command exit = new Command("Exit", Command.EXIT, 1);
	private Command set = new Command("Set", Command.SCREEN, 2);
	private Command ok = new Command("Ok", Command.SCREEN, 3);
	private TextField password = new TextField("Password", "", 20, TextField.PASSWORD);
	private TextField cpassword = new TextField("Confirm Password", "", 20, TextField.PASSWORD);
	private Electron electron;
	private Display display;
	private String seed;
	public PasswordUI(Electron electron, String seed) {
		super("Set Password");
		this.electron = electron;
		this.seed = seed;
		display = Display.getDisplay(electron);
		append(password);
		append(cpassword);
		addCommand(exit);
		addCommand(set);
		setCommandListener(this);
	}
	public PasswordUI(Electron electron) {
		super("Enter Password");
		this.electron = electron;
		display = Display.getDisplay(electron);
		append(password);
		addCommand(exit);
		addCommand(ok);
		setCommandListener(this);
	}
	public void commandAction(Command cmd, Displayable d) {
		if(cmd.equals(set)) {
			String passwordText = password.getString();
			String cpasswordText = cpassword.getString();
			if(passwordText.length() < 6) {
				sendErrorAlert("Password minimum Length is 6");
			}
			else if(!passwordText.equals(cpasswordText)) {
				sendErrorAlert("Password and Confirm password are not equals.");
			}
			//TODO - Add more password validation
			else {
				setStorageInfo(passwordText);
			}
		}
		if(cmd.equals(ok)) {
		
			String passwordText = password.getString();
			setStorageInfo(passwordText);
		}
		if(cmd.equals(exit)) {
			try {
				electron.destroyApp(false);
			} catch (MIDletStateChangeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	private void sendErrorAlert(String message) {
		Alert alert = new Alert("Error",message, null, AlertType.ERROR);
		alert.setTimeout(Alert.FOREVER);
		display.setCurrent(alert, this);
	}
	
	private boolean setStorageInfo(String password) {
		try {
			
			Storage storage = new Storage(Constants.STORAGE_PATH, password);
			//SplashScreen splashScreen = new SplashScreen(electron, storage);
			//display.setCurrent(splashScreen);
			if(seed != null && seed.length() > 0) {
				storage.put("seed", seed);
				storage.write();
			}
			electron.setStorage(storage);
			//Thread t = new Thread(splashScreen);
			//t.start();
			electron.start();
			
			return true;
		} catch (InvalidCipherTextException e) {
			sendErrorAlert("Password Is not correct");
			return false;
		} catch (IOException e) {
			sendErrorAlert(e.getMessage());
			return false;
		}
	}
}
