package EluxUtils;

import static EluxUtils.Utils.*;
import static EluxUtils.UMath.*;

import java.io.FileWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class CSVLogger {
	private String filename;
	private boolean append;
	private String header = new String("");
	private FileWriter fw;
	
	
	public CSVLogger() { } // CONSTRUCTOR ------------------------
	
	public void init(String _filename, boolean _append) {
		this.filename = _filename;
		this.append = _append;
	}
	
	public void header(String _header) { this.header = new String(_header); }
	
	public boolean open() {
		try{
			if(append) fw = new FileWriter(FILE_ROOTPATH + filename, true);
			else fw = new FileWriter(new File(FILE_ROOTPATH + filename), false);
			if(isFileEmpty(filename) || !append) {
				if(append) padLog(filename + " is empty, creating new one.");
				fw.append(header);
			}
			return true;
		} catch (FileNotFoundException e) {
			padErr("File " + filename + " not found");
			return false;
		} catch (IOException e) {
			padErr("Error opening " + filename);
			return false;
		}
	}
	
	public boolean close(boolean log) {
		try {
			fw.flush();
			fw.close();
			if(log) padLog("Data stored to " + 
					System.getProperty("user.dir") + "\\" + filename);
			return true;
		} catch (FileNotFoundException e) {
			padErr("File " + filename + " not found");
			return false;
		} catch (IOException e) {
			padErr("Error closing " + filename); 
			return false;
		}
		
	}
	
	public boolean reset() {
		try {
			open();
			fw = new FileWriter(new File(FILE_ROOTPATH + filename), false);
			close(false);
			return true;
		} catch (FileNotFoundException e) {
			padErr("File " + filename + " not found");
			return false;
		} catch (IOException e) {
			padErr("Error closing " + filename); 
			return false;
		}
	}
	
	public boolean eol() {
		try {
			fw.append("\n");
			return true;
		} catch (IOException e) {
			padErr("Error writing to " + filename); 
			return false;
		}
	}
	
	public boolean log(String msg, boolean commaBefore) {
		try {
			if(commaBefore) fw.append(",");
			fw.append(msg);
			return true;
		} catch (IOException e) {
			padErr("Error writing to " + filename);
			return false;
		}
	}
	
	public boolean log(int value, boolean comma) {
		return log(String.valueOf(value), comma);
	}
	
	public boolean log(double value, boolean comma) {
		return log(d2s(value), comma);
	}
}