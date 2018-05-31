package electrol.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Vector;

import electrol.java.util.ArrayList;
import electrol.java.util.Iterator;

public class Files {
	public static final String path = ".electron-cash/";

	public static String getDefaultPath() throws IOException{

		String p = "/home/yurkazaytsev/testing/"+path;
		File f = new File(p);
		if(!f.isDirectory())
			f.mkdir();
		return p;
	}

	public static byte[] readBytes(String path, int skip, int bytes) {
		byte[] buffer = new byte[bytes];
		try {
		FileInputStream fis = new FileInputStream(getDefaultPath()+path);

		DataInputStream di = new DataInputStream(fis);
		di.skip(skip);
		di.read(buffer);
		di.close();
		fis.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
		return buffer;
	}

	public static String read(String path) {

		String fileContent = "[]";
		try {
			File f = new File(getDefaultPath()+path);
			if(!f.exists()) {
				throw new IOException("File does not exist on "+path);
			}
			FileInputStream fis = new FileInputStream(f);
			DataInputStream di = new DataInputStream(fis);

			fileContent = Utils.readFromStream(di);
			di.close();
			fis.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return fileContent;
	}

	public static String delete(String path) {

		try {
			File f = new File(getDefaultPath()+path);
			if(!f.exists()) {
				throw new IOException("File does not exist");
			}
			f.delete();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}


	public static void write(Object data, String path) {
		try {
			File f = new File(getDefaultPath()+path);
			FileOutputStream out = new FileOutputStream(f);

			if(!f.exists())
				f.createNewFile();
			PrintStream output = new PrintStream(out);
			output.print(data);
			output.close();
			out.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static OutputStream write(String path) {
		try {
			File f = new File(getDefaultPath()+path);
			FileOutputStream out = new FileOutputStream(f);

			if(!f.exists())
				f.createNewFile();
			return out;
		}catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void download(DataInputStream inputStream, String file) throws IOException{
		File f = new File(getDefaultPath()+file);
		if(!f.exists())
			f.createNewFile();

		FileOutputStream out = new FileOutputStream(f);

		int length;
		byte[] buffer = new byte[1024];
		while ((length = inputStream.read(buffer)) != -1) {
			out.write(buffer, 0, length);
		}
		out.close();
		inputStream.close();
	}

	public static boolean isExist(String path) {

			try {
				File f = new File(getDefaultPath()+path);
				return f.exists();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
	}

	public static long filesize(String path) {
		try {
			File f = new File(getDefaultPath()+path);
			return f.length();
		}catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	//TODO - Need some more testing
	public static void append(Object data, String path) {
		try {
			File f = new File(getDefaultPath()+path);
			if(!f.exists())
				f.createNewFile();
			FileOutputStream out = new FileOutputStream(f);
			PrintStream output = new PrintStream(out);
			output.print(data);
			output.close();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*public static String getDefaultRoot() throws IOException {
		Enumeration roots =File.listRoots();
		if(roots.hasMoreElements()) {
			return roots.nextElement().toString();
		}
		else
			throw new IOException("You don't have permission");
	}*/

	public static boolean getOrCreateDir(String dir) throws IOException{
		File f = new File(getDefaultPath()+dir);
		if(!f.exists())
			f.mkdir();
		return true;
	}
	public static ArrayList listDir(String dir) throws IOException{
		File f = new File(getDefaultPath()+dir);
		ArrayList list = new ArrayList();
		if(f.isDirectory()) {
			for(File f1:f.listFiles()) {
				list.add(f1.isDirectory());
			}
		}
		return list;
	}
	public static ArrayList listFile(String dir) throws IOException{
		File f = new File(getDefaultPath()+dir);
		ArrayList list = new ArrayList();
		if(f.isDirectory()) {
			for(File f1:f.listFiles()) {
				list.add(f1.isDirectory());
			}
		}
		return list;
	}
	public static Vector listFilterDir(String dir,String filterDir) throws IOException{
		Iterator dirList = listDir(dir).iterator();
		Vector vector = new Vector();
		while(dirList.hasNext()){
			String directory = dirList.next().toString();
			if(directory.startsWith(filterDir)) {
				vector.addElement(directory);
			}
		}
		return vector;
	}

	public static void rename(String old_path, String path) throws IOException {
		// TODO Auto-generated method stub
		File f = new File(getDefaultPath()+old_path);
		f.renameTo(new File(getDefaultPath()+path));
	}
}
