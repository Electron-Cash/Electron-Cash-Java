package electrol.main;

import org.json.me.JSONArray;
import org.json.me.JSONObject;

import electrol.util.Constants;

public class Synchronization {

	private static boolean sync = false;

	/*public static void getScriptHashAddresses(Network network) {
		if(!sync) {
			System.out.println("-----------subscribing");
			Storage storage = new Storage(Constants.STORAGE_PATH);
			JSONObject addresses = storage.get("addresses", new JSONObject());
			JSONArray change = addresses.optJSONArray("change");
			subscribe(network, change);
			JSONArray receiving = addresses.optJSONArray("receiving");
			subscribe(network, receiving);
			sync = true;
		}
	}
	public static void subscribe(Network network, JSONArray address_array) {
		for(int i=0;i< address_array.length() ; i++) {
			String address  = address_array.optString(i);
			if(address != null) {
				String cashaddr = AddressUtil.generateCashAddrFromLegacyAddr(address);
				String scripthash = AddressUtil.to_scripthash_hex_from_cashaddr("bitcoincash:"+cashaddr);
				System.out.println(scripthash);
				network.queue_request("blockchain.scripthash.subscribe", new String[] {scripthash}, null);
			}
		}
	}*/
}
