package EluxAPI;

import static EluxAPI.Utils.*;
import static EluxAPI.Utils_math.*;

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
	
	public void open() {
		try{
			if(append) fw = new FileWriter(filename, true);
			else fw = new FileWriter(new File(filename), false);
			if(isFileEmpty(filename) || !append) {
				if(append) padLog(filename + " is empty, creating new one.");
				fw.append(header);
			}
		} catch (FileNotFoundException e) {
			padErr("File " + filename + " not found");
		} catch (IOException e) {
			padErr("Error opening " + filename); }
	}
	
	public void close(boolean log) {
		if(fw != null) try {
			fw.flush();
			fw.close();
			if(log) padLog("Data stored to " + 
					System.getProperty("user.dir") + "\\" + filename);
		} catch (FileNotFoundException e) {
			padErr("File " + filename + " not found");
		} catch (IOException e) {
			padErr("Error closing " + filename); }
		
	}
	
	public boolean eol() {
		try{
			fw.append("\n");
			return true;
		} catch (IOException e) {
			padErr("Error writing to " + filename); return false;}
	}
	
	public boolean log(String msg, boolean commaBefore) {
		try{
			if(commaBefore) fw.append(",");
			fw.append(msg);
			return true;
		} catch (IOException e) {
			padErr("Error writing to " + filename); return false;}
	}
	
	public boolean log(int value, boolean comma) {
		return log(String.valueOf(value), comma);
	}
	
	public boolean log(double value, boolean comma) {
		return log(d2s(value), comma);
	}
}