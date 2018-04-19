package electrol.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;

public class Files {
	public static final String path = ".electron-cash/";
	
	public static String getDefaultPath() throws IOException{
		String appPath = "file:///"+getDefaultRoot()+path;
		FileConnection con = (FileConnection) Connector.open(appPath, Connector.READ_WRITE);
		if(!con.isDirectory())
			con.mkdir();
		return appPath;
	}
	
	public static byte[] readBytes(String path, int skip, int bytes) {
		byte[] buffer = new byte[bytes];
		try {
		FileConnection con = (FileConnection) Connector.open(Files.getDefaultPath()+path, Connector.READ_WRITE);
		
		DataInputStream di = con.openDataInputStream();
		di.skip(skip);
		di.read(buffer);
		di.close();
		con.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
		return buffer;
	}
	
	public static String read(String path) {

		String fileContent = "[]";
		try {
			FileConnection fc = (FileConnection) Connector.open(getDefaultPath()+path, Connector.READ_WRITE);
			if(!fc.exists()) {
				fc.close();
				throw new IOException("File does not exist on "+path);
			}
			fileContent = Utils.readFromStream(fc.openDataInputStream());
			fc.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return fileContent;
	}

	public static String delete(String path) {

		try {
			FileConnection fc = (FileConnection) Connector.open(getDefaultPath()+path, Connector.READ_WRITE);
			if(!fc.exists()) {
				throw new IOException("File does not exist");
			}
			fc.delete();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void write(Object data, String path) {
		try {
			FileConnection con = (FileConnection) Connector.open(getDefaultPath()+path, Connector.READ_WRITE);
			if(!con.exists())
				con.create();
			DataOutputStream out = con.openDataOutputStream();
			PrintStream output = new PrintStream(out);
			output.print(data);
			output.close();
			out.close();
			con.close();
		}catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	public static OutputStream write(String path) {
		try {
			FileConnection con = (FileConnection) Connector.open(getDefaultPath()+path, Connector.READ_WRITE);
			if(!con.exists())
				con.create();
			DataOutputStream out = con.openDataOutputStream();
			return out;
		}catch (IOException e) {
			e.printStackTrace();
		} 
		return null;
	}
	
	public static void download(DataInputStream inputStream, String file) throws IOException{
		FileConnection con = (FileConnection) Connector.open(Files.getDefaultPath()+file, Connector.READ_WRITE);
		if(!con.exists()) {
			con.create();
		}
		DataOutputStream out = con.openDataOutputStream();
		int length;
		byte[] buffer = new byte[1024];
		while ((length = inputStream.read(buffer)) != -1) {
			out.write(buffer, 0, length);
		}
		out.close();
		con.close();
		inputStream.close();
	}
	
	public static boolean isExist(String path) {
		boolean exist = false;
		try {
			FileConnection con = (FileConnection) Connector.open(getDefaultPath()+path, Connector.READ_WRITE);
			exist = con.exists();
			con.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
		return exist;
	}
	
	public static long filesize(String path) {
		long size = 0 ;
		try {
			FileConnection con = (FileConnection) Connector.open(getDefaultPath()+path, Connector.READ_WRITE);
			if(con.exists()) {
				size = con.fileSize();
			}
			con.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
		return size;
	}
	//TODO - Need some more testing
	public static void append(Object data, String path) {
		try {
			FileConnection con = (FileConnection) Connector.open(getDefaultPath()+path, Connector.READ_WRITE);
			if(!con.exists())
				con.create();
			OutputStream out = con.openOutputStream(Long.MAX_VALUE);
			PrintStream output = new PrintStream(out);
			output.print(data);
			output.close();
			con.close();
		}catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	public static String getDefaultRoot() throws IOException {
		Enumeration roots = FileSystemRegistry.listRoots();
		if(roots.hasMoreElements()) {
			return roots.nextElement().toString();
		}
		else 
			throw new IOException("You don't have permission");
	}
	
	public static boolean getOrCreateDir(String dir) throws IOException{
		FileConnection con = (FileConnection) Connector.open(getDefaultPath()+dir, Connector.READ_WRITE);
		if(!con.isDirectory())
			con.mkdir();
		return true;
	}
	public static Enumeration listDir(String dir) throws IOException{
		FileConnection con = (FileConnection) Connector.open(getDefaultPath()+dir, Connector.READ_WRITE);
		if(con.isDirectory())
			return con.list();
		else
			return null;
	}
	public static Enumeration listFile(String dir) throws IOException{
		FileConnection con = (FileConnection) Connector.open(getDefaultPath()+dir, Connector.READ_WRITE);
		if(!con.isDirectory())
			return con.list();
		else
			return null;
	}
	public static Vector listFilterDir(String dir,String filterDir) throws IOException{
		Enumeration dirList = listDir(dir);
		Vector vector = new Vector();
		while(dirList.hasMoreElements()){
			String directory = dirList.nextElement().toString();
			if(directory.startsWith(filterDir)) {
				vector.addElement(directory);
			}
		}
		return vector;
	}

	public static void rename(String old_path, String path) throws IOException {
		// TODO Auto-generated method stub
		FileConnection con = (FileConnection) Connector.open(getDefaultPath()+old_path, Connector.READ_WRITE);
		con.rename(path);
		con.close();
	}
}
