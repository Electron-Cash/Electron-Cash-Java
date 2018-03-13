package test;

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpsConnection;
import javax.microedition.io.SecurityInfo;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;

public class HttpsConnectionTest extends Thread{
	private String url;
	private Display display;
	private Form form;
	public HttpsConnectionTest(String url, Display display,Form form) {
		this.url = url;
		this.display  = display;
		this.form =form;
	}
	public void run() {

		try {
			HttpsConnection connection = (HttpsConnection) Connector.open(url);
			SecurityInfo securityInfo = connection.getSecurityInfo();
			System.out.println(securityInfo.getServerCertificate());
			connection.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			Alert a = new Alert("Exception", ioe.toString(), null, null);
			a.setTimeout(Alert.FOREVER);
			display.setCurrent(a, form);
		}
	}
}
