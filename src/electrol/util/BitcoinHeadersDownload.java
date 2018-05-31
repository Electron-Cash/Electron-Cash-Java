package electrol.util;

import java.io.DataInputStream;

import java.io.IOException;

import electrol.INetwork;
import electrol.httpsclient.HttpsConnectionImpl;

public class BitcoinHeadersDownload  extends Thread{
	private String saveLocation;
	private INetwork network;
	
	public BitcoinHeadersDownload(String saveLocation, INetwork network) {
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
