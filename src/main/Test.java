package main;

/*import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
*/
public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/*try {
			certInformation("https://abc.vom-stausee.de:50002");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	/*
	 * ndnd.selfhost.eu:50002:s
electrum.epicinet.net:50002:s
VPS.hsmiths.com:50002:s
electrumx.nmdps.net:50002:s
abc.vom-stausee.de:52002:s
ruuxwv74pjxms3ws.onion:10042:s
abc.vom-stausee.de:52002:s
electron.coinucopia.io:50002:s
bch.curalle.ovh:50002:s
abc.vom-stausee.de:52002:s
499044
	 */
	/*public static void certInformation(String aURL) throws Exception{
		URL destinationURL = new URL(aURL);
		HttpsURLConnection conn = (HttpsURLConnection)destinationURL.openConnection();
		Certificate[] certs = conn.getServerCertificates();
		for (Certificate cert : certs) {
			System.out.println("Certificate is: " + cert);
			if(cert instanceof X509Certificate) {
				X509Certificate x = (X509Certificate ) cert;
				System.out.println(x.getIssuerDN());
			}
		}
		SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();	
		SSLSocket sslsocket = (SSLSocket) factory.createSocket("abc.vom-stausee.de",50002);
		sslsocket.startHandshake();
		System.out.println(sslsocket.getSession().getPeerHost());
		
	}
	private static void print_content(HttpsURLConnection con){
		if(con!=null){

			try {

				System.out.println("****** Content of the URL ********");			
				BufferedReader br = 
						new BufferedReader(
								new InputStreamReader(con.getInputStream()));

				String input;

				while ((input = br.readLine()) != null){
					System.out.println(input);
				}
				br.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}*/

}
