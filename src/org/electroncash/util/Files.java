package org.electroncash.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

public class Files
{
	public static final String path = "electron-cash/";

	public Files() {}

	public static String getDefaultPath() throws IOException
	{
		String appPath = getDefaultRoot() + "electron-cash/";
		FileConnection con = (FileConnection)Connector.open(appPath, Connector.READ_WRITE);
		if (!con.isDirectory())
			con.mkdir();
		return appPath;
	}

	public static byte[] readBytes(String path, int skip, int bytes) throws IOException {
		byte[] buffer = new byte[bytes];
		FileConnection con = (FileConnection)Connector.open(getDefaultPath() + path, Connector.READ_WRITE);

		DataInputStream di = con.openDataInputStream();
		di.skip(skip);
		di.read(buffer);
		di.close();
		con.close();
		return buffer;
	}

	public static byte[] read(String path) throws IOException {
		FileConnection fc = (FileConnection)Connector.open(getDefaultPath() + path, Connector.READ_WRITE);
		if (!fc.exists()) {
			fc.close();
			throw new IOException("File does not exist on " + path);
		}
		byte[] fileContent = Util.readFromStream(fc.openDataInputStream());
		fc.close();
		return fileContent;
	}

	public static void delete(String path) throws IOException {
		FileConnection fc = (FileConnection)Connector.open(getDefaultPath() + path, Connector.READ_WRITE);
		if (!fc.exists()) {
			throw new IOException("File does not exist");
		}
		fc.delete();
	}

	public static void write(Object data, String path) throws IOException
	{
		FileConnection con = (FileConnection)Connector.open(getDefaultPath() + path, Connector.READ_WRITE);
		if (con.exists()) {
			con.delete();
		}
		con.create();
		DataOutputStream out = con.openDataOutputStream();
		PrintStream output = new PrintStream(out);
		output.print(data);
		output.close();
		out.close();
		con.close();
	}

	public static void write(byte[] data, String path) throws IOException
	{
		FileConnection con = (FileConnection) Connector.open(getDefaultPath()+path, Connector.READ_WRITE);

		if(con.exists()) {
			con.delete();
		}
		con.create();
		DataOutputStream out = con.openDataOutputStream();
		out.write(data);
		out.close();
		con.close();
	}

	public static void download(java.io.InputStream inputStream, String file) throws IOException
	{
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

	public static boolean isExist(String path) throws IOException {
		FileConnection con = (FileConnection)Connector.open(getDefaultPath() + path, Connector.READ_WRITE);
		boolean exist = con.exists();
		con.close();
		return exist;
	}

	public static long filesize(String path) throws IOException {
		long size = 0L;
		FileConnection con = (FileConnection)Connector.open(getDefaultPath() + path, Connector.READ_WRITE);
		if (con.exists()) {
			size = con.fileSize();
		}
		con.close();
		return size;
	}

	public static void append(Object data, String path) throws IOException { 
		FileConnection con = (FileConnection)Connector.open(getDefaultPath() + path, Connector.READ_WRITE);
		if (!con.exists())
			con.create();
		java.io.OutputStream out = con.openOutputStream(Long.MAX_VALUE);
		PrintStream output = new PrintStream(out);
		output.print(data);
		output.close();
		con.close();
	}

	public static String getDefaultRoot() throws IOException
	{
		String memoryCard = System.getProperty("fileconn.dir.memorycard");
		if (memoryCard != null) {
			return memoryCard;
		}
		Enumeration roots = javax.microedition.io.file.FileSystemRegistry.listRoots();
		if (roots.hasMoreElements()) {
			return "file:///" + roots.nextElement().toString();
		}
		throw new IOException("You don't have permission");
	}

	public static boolean getOrCreateDir(String dir) throws IOException {
		FileConnection con = (FileConnection)Connector.open(getDefaultPath() + dir, Connector.READ_WRITE);
		if (!con.isDirectory())
			con.mkdir();
		return true;
	}

	public static Enumeration listDir(String dir) throws IOException { 
		FileConnection con = (FileConnection)Connector.open(getDefaultPath() + dir, Connector.READ_WRITE);
		if (con.isDirectory()) {
			return con.list();
		}
		return null;
	}

	public static Enumeration listFile(String dir) throws IOException { 
		FileConnection con = (FileConnection)Connector.open(getDefaultPath() + dir, Connector.READ_WRITE);
		if (!con.isDirectory())
			return con.list();
		return null;
	}

	public static Vector listFilterDir(String dir, String filterDir) throws IOException { 
		Enumeration dirList = listDir(dir);
		Vector vector = new Vector();
		while (dirList.hasMoreElements()) {
			String directory = dirList.nextElement().toString();
			if (directory.startsWith(filterDir)) {
				vector.addElement(directory);
			}
		}
		return vector;
	}

	public static void rename(String old_path, String path) throws IOException {
		FileConnection con = (FileConnection)Connector.open(getDefaultPath() + old_path, Connector.READ_WRITE);
		con.rename(path);
		con.close();
	}
}
