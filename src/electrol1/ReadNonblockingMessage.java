package electrol1;

import java.io.IOException;
import java.io.InputStream;

import electrol.httpsclient.HttpsConnectionUtils;

public class ReadNonblockingMessage extends Thread{
	
	private Thread thread;
	
	public ReadNonblockingMessage() {
		//start();
	}
	
	public String read(InputStream is) throws IOException {
		try {
			
			thread = Thread.currentThread();
			System.out.println("name "+thread.getName());
			return HttpsConnectionUtils.readLine(is);
		}
		catch (Exception e) {
			System.out.println("returning ");
			e.printStackTrace();
			return "";
		}
	}
	
	public void run() {
		try {
			System.out.println("name1 "+getName());
			sleep(10000);
			if(thread.isAlive())
				thread.interrupt();
		} catch (InterruptedException e) {
			
		}
	}
}
