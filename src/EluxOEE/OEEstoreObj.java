package EluxOEE;

import static EluxUtils.Utils.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class OEEstoreObj {
	private String filename;
	
	public OEEstoreObj() { } // CONSTRUCTOR ------------------------
	
	public void init(String filename) {
		this.filename = filename;
	}
	
	public void saveOEEimage(OEEdata oee, boolean log) {
		try {
			FileOutputStream f = new FileOutputStream(
					new File(FILES_FOLDER + filename));
			ObjectOutputStream o = new ObjectOutputStream(f);
			o.writeObject(oee);
			o.close();
			f.close();
			if(log) logmsg("OEE data object stored to " +
								ROOT_PATH + FILES_FOLDER + filename);
		} catch (FileNotFoundException e) { 
			logErr("File " + filename + " not found");
		} catch (IOException e) { logErr("Error writing to " + filename); }
	}
	
	public OEEdata restoreOEEimage(boolean log) {
		try {
			FileInputStream fi = new FileInputStream(
					new File(FILES_FOLDER + filename));
			ObjectInputStream oi = new ObjectInputStream(fi);
			OEEdata oee = (OEEdata) oi.readObject();
			oi.close();
			fi.close();
			if(log) logmsg("OEE data loaded from " +
								ROOT_PATH + FILES_FOLDER + filename);
			return oee;
		} catch (FileNotFoundException e) { 
			logErr("File " + filename + " not found");
		} catch (IOException e) { logErr("Error reading from " + filename);
		} catch (ClassNotFoundException e) { logErr("Object not found"); }
		return null;
	}
}