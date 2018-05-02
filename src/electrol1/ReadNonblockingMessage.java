package electrol.main;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import electrol.httpsclient.HttpsConnectionUtils;

public class ReadNonblockingMessage extends Thread{
	
	private final int READ_TIMEOUT = 1000;
	private String result ;
	private boolean check = true;
	private long currentTime;
	Thread t;
	public ReadNonblockingMessage(InputStream stream) throws IOException,InterruptedException  {
		currentTime = new Date().getTime();
		t = Thread.currentThread();
		start();
		result =  HttpsConnectionUtils.readLine(stream);
		check = false;
		
	}
	
	public String read() {
		return result;
	}
	
	public void run() {
		while(new Date().getTime()-currentTime < READ_TIMEOUT ) {
			if(!check) {
				return;
			}
		}
		t.interrupt();
	}
}
