package EluxLogger;

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
	
	
	public CSVLogger(String filename, boolean append, char sepChar) { // CONSTRUCTOR ------------------------
		this.filename = filename;
		this.append = append;
		this.sepChar = sepChar;
	}
	
	public void setHeader(String _header) { this.header = new String(_header); }
	
	public boolean open(boolean log) {
		try{
			if(append) fw = new FileWriter(FILES_FOLDER + filename, true);
			else fw = new FileWriter(new File(FILES_FOLDER + filename), false);
			if(isFileEmpty(filename) || !append) {
				if(append && log) logmsg(filename + " is empty, creating new one.");
				fw.append(header);
			}
			return true;
		} catch (FileNotFoundException e) {
			try {
				File newFile = new File(FILES_FOLDER + filename);
				newFile.createNewFile();
				return open(log);
			} catch (IOException e1) {
				logErr("Unable to create new file \"" + FILES_FOLDER + filename + "\"");
				e1.printStackTrace();
				return false;
			}
		} catch (IOException e) {
			logErr("Error opening " + filename);
			return false;
		}
	}
	
	public boolean close(boolean log) {
		try {
			fw.flush();
			fw.close();
			if(log) logmsg("CSV stored to " + ROOT_PATH + FILES_FOLDER + filename);
			return true;
		} catch (FileNotFoundException e) {
			logErr("File " + filename + " not found");
			return false;
		} catch (IOException e) {
			logErr("Error closing " + filename); 
			return false;
		}
		
	}
	
	public boolean reset() {
		try {
			this.open(false);
			fw = new FileWriter(new File(FILES_FOLDER + filename), false);
			this.close(false);
			return true;
		} catch (FileNotFoundException e) {
			logErr("File " + filename + " not found");
			return false;
		} catch (IOException e) {
			logErr("Error closing " + filename); 
			return false;
		}
	}
	
	public boolean eol() {
		try {
			fw.append("\n");
			return true;
		} catch (IOException e) {
			logErr("Error writing to " + filename); 
			return false;
		}
	}
	
	public boolean log(String msg, boolean sepCharBefore) {
		try {
			if(sepCharBefore) fw.append(sepChar);
			fw.append(msg);
			return true;
		} catch (IOException e) {
			logErr("Error writing to " + filename);
			return false;
		}
	}
	
	public boolean log(int value, boolean sepCharBefore) {
		return log(String.valueOf(value), sepCharBefore);
	}
	
	public boolean log(double value, boolean sepCharBefore) {
		return log(d2s(value), sepCharBefore);
	}
	
	public boolean log(char value, boolean sepCharBefore) {
		return log(value + "", sepCharBefore);
	}
}