package electrol;


import java.io.IOException;
import java.io.InputStream;
import javax.microedition.io.HttpsConnection;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;

import electrol.httpsclient.HttpsConnectionImpl;
import electrol.util.Utils;

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

		System.out.println("url "+url);
		try {
			HttpsConnectionImpl connection=new HttpsConnectionImpl(url);
			connection.setAllowUntrustedCertificates(true);
			connection.setRequestMethod(HttpsConnectionImpl.REQUEST_METHOD_GET);
			
			InputStream inputStream = connection.openInputStream();
			String str = Utils.readFromStream(inputStream);
			System.out.println(str);
			connection.close();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
