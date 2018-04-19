package electrol;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;

public class HttpConnectionTest extends Thread{
	private String url;
	private Display display;
	private Form form;
	public HttpConnectionTest(String url, Display display,Form form) {
		this.url = url;
		this.display  = display;
		this.form =form;
	}
	public void run() {

		try {
			HttpConnection connection = (HttpConnection) Connector.open(url);
			connection.setRequestMethod(HttpConnection.GET);
			connection.setRequestProperty("Content-Type","//text plain");
			// HTTP Response
			System.out.println("Status Line Code: " + connection.getResponseCode());
			System.out.println("Status Line Message: " + connection.getResponseMessage());	
			if(connection.getResponseCode() == HttpConnection.HTTP_OK) {
				String str;
				InputStream inputstream = connection.openInputStream();
				int length = (int) connection.getLength();
				if (length != -1)
				{
					byte incomingData[] = new byte[length];
					inputstream.read(incomingData);
					str = new String(incomingData);
				}
				else  
				{
					ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
					int ch;
					while ((ch = inputstream.read()) != -1) {
						bytestream.write(ch);
					}
					str = new String(bytestream.toByteArray());
					bytestream.close();
				}
				Alert a = new Alert("Result", str, null, null);
				a.setTimeout(Alert.FOREVER);
				display.setCurrent(a, form);

			}

			connection.close();
		}
		catch (IOException ioe) {
			
			Alert a = new Alert("Exception", ioe.getMessage(), null, null);
			a.setTimeout(Alert.FOREVER);
			display.setCurrent(a, form);
		}
	}
}
