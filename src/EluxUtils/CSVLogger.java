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
	private char sepChar;
	private String header = new String("");
	private FileWriter fw;
	
	
	public CSVLogger() { } // CONSTRUCTOR ------------------------
	
	public void init(String filename, boolean append, char sepChar) {
		this.filename = filename;
		this.append = append;
		this.sepChar = sepChar;
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
			if(log) padLog("Data stored to " + System.getProperty("user.dir") + 
								"\\" + FILE_ROOTPATH + filename);
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
	
	public boolean log(String msg, boolean sepCharBefore) {
		try {
			if(sepCharBefore) fw.append(sepChar);
			fw.append(msg);
			return true;
		} catch (IOException e) {
			padErr("Error writing to " + filename);
			return false;
		}
	}
	
	public boolean log(int value, boolean sepCharBefore) {
		return log(String.valueOf(value), sepCharBefore);
	}
	
	public boolean log(double value, boolean sepCharBefore) {
		return log(d2s(value), sepCharBefore);
	}
}