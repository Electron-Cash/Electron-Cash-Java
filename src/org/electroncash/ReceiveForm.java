package org.electroncash;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import org.electroncash.util.ReceivingAddress;
import org.electroncash.util.Storage;

public class ReceiveForm extends Form implements CommandListener
{
  private TextField address;
  private Command back;
  private Electron electron;
  
  public ReceiveForm(Electron electron, Storage storage)
  {
    super("Electron -> Receiving Addresss");
    this.electron = electron;
    address = new TextField("Receiving Address", "", 50, 0);
    ReceivingAddress receivingAddress = new ReceivingAddress();
    try {
      String rec = receivingAddress.get_address(storage, false);
      address.setString(rec);
    } catch (Exception e) {
      e.printStackTrace();
    }
    back = new Command("Back", 2, 0);
    append(address);
    addCommand(back);
    setCommandListener(this);
  }
  
  public void commandAction(Command cmd, Displayable arg1) {
    if (cmd.equals(back)) {
      Display.getDisplay(electron).setCurrent(electron.getList());
    }
  }
}
