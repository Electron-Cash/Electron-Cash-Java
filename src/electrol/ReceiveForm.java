package electrol;


import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import electrol.main.Storage;
import electrol.util.Constants;


public class ReceiveForm extends Form{
	private TextField address;
	private TextField description;
	private TextField amount;
	private ChoiceGroup expire;
	private Command select, back  ,clear;
	
	public ReceiveForm(String title) {
		super(title);
		String[] protocal = {"1 Hour","1 Day","1 Week","Never"};
		address = new TextField("Receiving Address", "", 50, TextField.ANY);
		Storage storage = new Storage(Constants.STORAGE_PATH);
		ReceivingAddress receivingAddress = new ReceivingAddress();
		try {
			String rec = receivingAddress.get_address(storage,false);
			System.out.println(rec);
			address.setString(rec);
		}catch(Exception e) {
			e.printStackTrace();
		}
		description = new TextField("Description", "", 50, TextField.ANY);
		
		amount = new TextField("Requested Amount ", "", 10, TextField.NUMERIC);
		expire = new ChoiceGroup("Request Expires", Choice.POPUP, protocal, null);
		back=new Command("Back",Command.BACK,0);
		select=new Command("Save",Command.OK,0);
		clear  = new Command("New",Command.OK,0);
		append(address);
		append(description);
		append(amount);
		append(expire);
		addCommand(select);
		addCommand(back);
		addCommand(clear);
	}
	
}
