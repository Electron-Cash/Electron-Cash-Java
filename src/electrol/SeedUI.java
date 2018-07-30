package electrol;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDletStateChangeException;

import electrol.main.Seed;

public class SeedUI extends Form implements CommandListener{

	private Command exit = new Command("Exit", Command.EXIT, 1);
	private Command ok = new Command("Ok", Command.SCREEN, 2);

	private ChoiceGroup seedOptions = new ChoiceGroup("Select Seed Options", Choice.EXCLUSIVE);
	private TextField textField= new TextField("Enter Seed", "", 1000, TextField.ANY);
	private Electron electron;
	private Display display;
	public SeedUI(Electron electron) {
		super("Electron -> Seed Options");
		this.electron = electron;
		this.display = Display.getDisplay(electron);
		seedOptions.append("New Seed.", null);
		seedOptions.append("Already Have Seed.", null);
		seedOptions.setSelectedIndex(0, true);
		append(seedOptions);

		addCommand(exit);
		addCommand(ok);
		setCommandListener(this);
	}

	public void commandAction(Command cmd, Displayable d) {
		if(cmd.equals(ok)) {
			textField.setPreferredSize(100, 100);
			if(textField.getString() != null && textField.getString().length() != 0) {
				String seed = textField.getString();
				display.setCurrent(new PasswordUI(electron,seed));
			}
			else {
				if(seedOptions.getSelectedIndex() == 0){
					textField.setLabel("Your new seed is.");
					textField.setString(new Seed().generateSeed());
					textField.setConstraints(TextField.UNEDITABLE);
					append(textField);
					append("Keep your seed safe for future reference.");	
				}
				else {
					append(textField);
				}
				delete(0);
			}
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

}
