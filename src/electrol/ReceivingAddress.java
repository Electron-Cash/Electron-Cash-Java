package electrol;

import java.nio.ByteBuffer;
import java.util.Enumeration;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

import electrol.java.util.HashSet;
import electrol.java.util.Set;
import electrol.main.AddressUtil;
import electrol.main.Bitcoin;
import electrol.main.Storage;
import electrol.util.Util;


public class ReceivingAddress {
	private static final int COMMAND_LEN = 12;
	public static final int MAX_SIZE = 0x02000000;
	
	
	public String get_address(Storage storage, boolean type) throws JSONException{
		String addressType;
		if(type) {
			addressType = "change";
		}
		else {
			addressType = "receiving";
		}
		JSONObject object = storage.get("addresses", new JSONObject());
		
		
		JSONArray addrJson = object.getJSONArray(addressType);
		JSONObject txi = storage.get("txi",new JSONObject());
		Set txiSet = getTx(txi); 
		JSONObject txo = storage.get("txo",new JSONObject());
		Set txoSet = getTx(txo);
		AddressUtil addressUtil = new AddressUtil(storage.get("keystore", new JSONObject()).getString("xpub"));
		for(int i=0;i< addrJson.length();i++) {
			String addrString = addrJson.getString(i);
			if(txiSet.contains(addrString) || txoSet.contains(addrString)){
				continue;
			}
			else {
				return addressUtil.generateCashAddrFromLegacyAddr(addrJson.getString(i));
			}
		}
		int n = addrJson.length();  // generating addresses for (n+1) if n = 5 then it will generate address 6 for for (0 to 5)
		String legacyAddress = addressUtil.generateOneLegacyAddress(n, type ? AddressUtil.ADDRESS_TYPE_CHANGE : AddressUtil.ADDRESS_TYPE_RECEIVING);
		addrJson.put(legacyAddress);
		object.put(addressType, addrJson);
		storage.put("addresses", object);
		storage.write();
		return get_address(storage, type);
	}
	
	
	public void deserializePayload(BitcoinPacketHeader header, ByteBuffer in) throws Exception {
        byte[] payloadBytes = new byte[header.size];
        in.get(payloadBytes, 0, header.size);

        // Verify the checksum.
        byte[] hash;
        hash = Bitcoin.doubleHash(payloadBytes);
        if (header.checksum[0] != hash[0] || header.checksum[1] != hash[1] ||
                header.checksum[2] != hash[2] || header.checksum[3] != hash[3]) {
            throw new Exception("Checksum failed to verify, actual " +Util.bytesToHex(hash));
        }

        
    }
	
	public static class BitcoinPacketHeader {
        /** The largest number of bytes that a header can represent */
        public static final int HEADER_LENGTH = COMMAND_LEN + 4 + 4;

        public final byte[] header;
        public final String command;
        public final int size;
        public final byte[] checksum;

        public BitcoinPacketHeader(ByteBuffer in) throws Exception {
            header = new byte[HEADER_LENGTH];
            in.get(header, 0, header.length);

            int cursor = 0;

            // The command is a NULL terminated string, unless the command fills all twelve bytes
            // in which case the termination is implicit.
            for (; header[cursor] != 0 && cursor < COMMAND_LEN; cursor++) ;
            byte[] commandBytes = new byte[cursor];
            System.arraycopy(header, 0, commandBytes, 0, cursor);
            command = new String(commandBytes,"ascii");
            cursor = COMMAND_LEN;

            size = (int) readUint32(header, cursor);
            cursor += 4;

            if (size > MAX_SIZE || size < 0)
                throw new Exception("Message size too large: " + size);

            // Old clients don't send the checksum.
            checksum = new byte[4];
            // Note that the size read above includes the checksum bytes.
            System.arraycopy(header, cursor, checksum, 0, 4);
            cursor += 4;
        }
    }
	public Set getTx(JSONObject tx) throws JSONException {
		Enumeration e = tx.keys();
		Set txSet = new HashSet();
		while(e.hasMoreElements()) {
			String txHash =(String)e.nextElement();
			JSONObject txAddresses = tx.getJSONObject(txHash);
			if(txAddresses.length() > 0) {
				Enumeration txA = txAddresses.keys();
				while(txA.hasMoreElements()) {
					txSet.add((String)txA.nextElement());
				}
			}
		}
		return txSet;
	}

	 public static long readUint32(byte[] bytes, int offset) {
         return (bytes[offset] & 0xffl) |
                 ((bytes[offset + 1] & 0xffl) << 8) |
                 ((bytes[offset + 2] & 0xffl) << 16) |
                 ((bytes[offset + 3] & 0xffl) << 24);
     }
	public void seekPastMagicBytes(ByteBuffer in) throws Exception {
        int magicCursor = 3;  // Which byte of the magic we're looking for currently.
        while (true) {
            byte b = in.get();
            // We're looking for a run of bytes that is the same as the packet magic but we want to ignore partial
            // magics that aren't complete. So we keep track of where we're up to with magicCursor.
            byte expectedByte = (byte)(0xFF >>> (magicCursor * 8));
            if (b == expectedByte) {
                magicCursor--;
                if (magicCursor < 0) {
                    // We found the magic sequence.
                    return;
                } else {
                    // We still have further to go to find the next message.
                }
            } else {
                magicCursor = 3;
            }
        }
    }
}
