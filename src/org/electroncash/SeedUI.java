package org.electroncash;

import java.io.IOException;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDletStateChangeException;

import org.electroncash.util.Seed;


public class SeedUI extends Form implements CommandListener
{
  private Command exit = new Command("Exit", 7, 1);
  private Command ok = new Command("Ok", 1, 2);
  
  private ChoiceGroup seedOptions = new ChoiceGroup("Select Seed Options", 1);
  private TextField textField = new TextField("Enter Seed", "", 1000, 0);
  private Electron electron;
  private Display display;
  private AlertUtil alertUtil;
  
  public SeedUI(Electron electron, AlertUtil alertUtil) { super("Electron -> Seed Options");
    this.electron = electron;
    display = Display.getDisplay(electron);
    this.alertUtil = alertUtil;
    seedOptions.append("New Seed.", null);
    seedOptions.append("Already Have Seed.", null);
    seedOptions.setSelectedIndex(0, true);
    append(seedOptions);
    
    addCommand(exit);
    addCommand(ok);
    setCommandListener(this);
  }
  
  public void commandAction(Command cmd, Displayable d) {
    if (cmd.equals(ok)) {
      textField.setPreferredSize(100, 100);
      if ((textField.getString() != null) && (textField.getString().length() != 0)) {
        String seed = textField.getString();
        display.setCurrent(new PasswordUI(electron, seed, alertUtil));
      }
      else {
        if (seedOptions.getSelectedIndex() == 0) {
          textField.setLabel("Your new seed is.");
          try {
            textField.setString(new Seed().generateSeed());
          } catch (IOException e) {
            alertUtil.sendErrorAlert(this, "IOException " + e.getMessage());
          } catch (Exception e) {
            alertUtil.sendErrorAlert(this, "Exception " + e.getMessage());
          }
          textField.setConstraints(131072);
          append(textField);
          append("Keep your seed safe for future reference.");
        }
        else {
          append(textField);
        }
        delete(0);
      }
    }
    if (cmd.equals(exit)) {
      try {
        electron.destroyApp(false);
      } catch (MIDletStateChangeException e) {
        alertUtil.sendErrorAlert(this, "MidletStateChangeException " + e.getMessage());
      }
    }
  }
}
