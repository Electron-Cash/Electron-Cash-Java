package electrol.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public class Utils {

	private static Date date = new Date();
	
	public static JSONArray getJsonArray(InputStream inputStream) throws JSONException, IOException {
		return new JSONArray(readFromStream(inputStream));
	}
	public static JSONObject getJsonObject(InputStream inputStream) throws JSONException, IOException {
		return new JSONObject(readFromStream(inputStream));
	}

	public static String readFromStream(InputStream inputStream) throws IOException{
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length;
		while ((length = inputStream.read(buffer)) != -1) {
			
			result.write(buffer, 0, length);
		}
		result.close();
		inputStream.close();
		return result.toString();
	}
	public static String read(InputStream inputStream, byte[] buffer, int timeoutMillis) throws IOException{
		 int bufferOffset = 0;
		 
		long maxTimeMillis = System.currentTimeMillis() + timeoutMillis;
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		int length;
		while ((length = inputStream.read(buffer)) != -1) {
			
			result.write(buffer, 0, length);
		}
		result.close();
		
		/* while (System.currentTimeMillis() < maxTimeMillis && bufferOffset < b.length) {
	         int readLength = java.lang.Math.min(is.available(),b.length-bufferOffset);
	         // can alternatively use bufferedReader, guarded by isReady():
	         int readResult = is.read(b, bufferOffset, readLength);
	         if (readResult == -1) break;
	         bufferOffset += readResult;
	     }
	     return bufferOffset;*/
		
		return result.toString();
	}
	public static electrol.SocketPipe SocketPipe() {
		return null;
	}
}
