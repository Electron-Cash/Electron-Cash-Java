package org.electroncash.util;

import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Random;
import javax.microedition.io.file.FileConnection;
import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;

public class NetworkUtil
{
  public NetworkUtil() {}
  
  public static Server deserialize_server(String server_str)
  {
    String[] split = StringUtils.split(server_str, ":");
    String host = split[0];
    int port = 0;
    String protocol = split[2];
    try {
      port = Integer.parseInt(split[1]);
      if ((!protocol.equals("s")) && (!protocol.equals("t"))) {
        throw new Exception("Invalid protocol");
      }
    }
    catch (NumberFormatException nme) {
      throw new NumberFormatException();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    

    return new Server(host, port, protocol);
  }
  
  public static JSONObject parse_servers(JSONArray result) throws JSONException {
    JSONObject servers = new JSONObject();
    for (int i = 0; i < result.length(); i++) {
      JSONObject out = new JSONObject();
      JSONArray array = result.getJSONArray(i);
      String host = array.getString(1);
      String pruning_level = "-";
      String version = "";
      if (array.length() > 2) {
        JSONArray child = array.getJSONArray(2);
        for (int j = 0; j < child.length(); j++) {
          String item = child.getString(j);
          if ((item.startsWith("s")) || (item.startsWith("t"))) {
            String protocol = item.substring(0, 1);
            String port = item.substring(1);
            if (port.trim().equals("")) {
              port = BitcoinMainnet.getDefaultPorts().get(protocol).toString();
            }
            out.put(protocol, port);
          }
          else if (item.startsWith("v")) {
            version = item.substring(1);
          }
          else if (item.startsWith("p")) {
            pruning_level = item.substring(1);
            if (pruning_level.trim().equals("")) {
              pruning_level = "0";
            }
          }
        }
      }
      
      out.put("pruning", pruning_level);
      out.put("version", version);
      servers.put(host, out);
    }
    return servers;
  }
  
  public static void init_header_file(Blockchain b) throws JSONException, java.io.IOException {
    String filename = Files.getDefaultPath() + b.getPath();
    FileConnection connection = (FileConnection)javax.microedition.io.Connector.open(filename, 3);
    int length = 43220080;
    if ((!connection.exists()) || (connection.fileSize() < length)) {
      connection.create();
      OutputStream dos = connection.openOutputStream();
      int size = 65536;
      for (int i = 0; i < length / size; i++) {
        dos.write(new byte[size], 0, size);
      }
      dos.write(new byte['籰'], 0, 31856);
    }
    connection.close();
    
    b.update_size();
  }
  
  public static Server pick_random_server(JSONObject default_servers, String protocol, Set exclude, String default_protocol) throws JSONException {
    if (default_servers == null) {
      default_servers = BitcoinMainnet.getDefaultServers();
    }
    
    List eligible = new ArrayList();
    Enumeration enumeration = default_servers.keys();
    while (enumeration.hasMoreElements()) {
      String server = (String)enumeration.nextElement();
      JSONObject item = default_servers.getJSONObject(server);
      if (item.has(protocol)) {
        int port = item.getInt(protocol);
        Server sObject = new Server(server, port, default_protocol);
        if (!exclude.contains(sObject)) {
          eligible.add(sObject);
        }
      }
    }
    
    return (Server)eligible.get(new Random().nextInt(eligible.size()));
  }
  
  public static JSONArray read_recent_servers() throws JSONException, java.io.IOException {
    if (!Files.isExist("recent-servers"))
      return new JSONArray();
    String json = new String(Files.read("recent-servers"));
    return new JSONArray(json);
  }
}
