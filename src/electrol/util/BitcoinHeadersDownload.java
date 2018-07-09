package electrol.util;

import java.io.DataInputStream;

import java.io.IOException;

import electrol.httpsclient.HttpsConnectionImpl;
import electrol.main.Network;
import electrol.main.test.Network1;

public class BitcoinHeadersDownload  extends Thread{
	private String saveLocation;
	private Network1 network;
	
	public BitcoinHeadersDownload(String saveLocation, Network1 network) {
		this.saveLocation = saveLocation;
		this.network = network;
	}
	public void run() {
		try {
			
			System.out.println("Header downloading start");
			HttpsConnectionImpl connection = new HttpsConnectionImpl(BitcoinMainnet.HEADERS_URL,443,BitcoinMainnet.HEADERS_PATH);
			connection.setAllowUntrustedCertificates(true);
			DataInputStream inputStream = connection.openDataInputStream();
			Files.download(inputStream, saveLocation);
			System.out.println("download complete");
			network.setDownloadingHeaders(false);
			connection.close();
		}
		catch(IOException ioEx) {
			ioEx.printStackTrace();
		}
	}
}
