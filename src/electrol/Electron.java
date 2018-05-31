package electrol;

import javax.microedition.lcdui.Choice;

import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import electrol.util.Config;
import main.Main;

public class Electron extends MIDlet implements CommandListener{

	private Form form;
	private String[] protocal = {"https", "http"};
	private TextField textField;
	private ChoiceGroup group;
	private Command exit, select;
	private Display display;
	
	public Electron() {
		display = Display.getDisplay(this);
		form = new Form("Call http/https url");
		group = new ChoiceGroup("Select Protocal", Choice.POPUP, protocal, null);
		select = new Command("SELECT", Command.OK, 1);
		exit = new Command("EXIT", Command.EXIT, 1);
		textField = new TextField("Please Enter Url", "", 50, 0);   
		form.append(group);
		form.append(textField);
		form.addCommand(select);
		form.addCommand(exit);
		//form.setCommandListener(this);
		Main.main();
		//electrol1.Network network = new electrol1.Network();
		//network.start();
		//block.update_size();
		System.out.println();
		//BigInteger big =new BigInteger("12", 80);

		/*try {
			lock.lock();
			new BitcoinHeadersDownload(NetworkConstants.HEADERS_URL,NetworkConstants.BitcoinMainnet.HEADERS_PATH,"header").start();
			lock.unlock();
		}catch (InterruptedException e) {
			e.printStackTrace();
		}
		*/
		
		
	}

	public void commandAction(Command c, Displayable d) {
		try {
			if (c == exit) {
				destroyApp(true); 
				notifyDestroyed();
			} else if (c == select) {
				StringItem item = new StringItem("Selected Choices = "+ group.getString(group.getSelectedIndex()),"");
				String url = "";
		
				
				form.append(item);
				display.setCurrent(d);
			}
		}
		catch (MIDletStateChangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {


	}

	protected void pauseApp() {
		// TODO Auto-generated method stub

	}

	protected void startApp() throws MIDletStateChangeException {
		// TODO Auto-generated method stub

		display.setCurrent(form);

	}

	
	
}