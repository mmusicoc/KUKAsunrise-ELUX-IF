package EluxOEE;

import static EluxUtils.Utils.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class OEEstore {
	private String filename;
	
	public OEEstore() { } // CONSTRUCTOR ------------------------
	
	public void init(String filename) {
		this.filename = filename;
	}
	
	public void saveOEEimage(OEEdata oee, boolean log) {
		try {
			FileOutputStream f = new FileOutputStream(
					new File(FILE_ROOTPATH + filename));
			ObjectOutputStream o = new ObjectOutputStream(f);
			o.writeObject(oee);
			o.close();
			f.close();
			if(log) padLog("OEE data stored to " + System.getProperty("user.dir") + 
								"\\" + FILE_ROOTPATH + filename);
		} catch (FileNotFoundException e) { 
			padErr("File " + filename + " not found");
		} catch (IOException e) { padErr("Error writing to " + filename); }
	}
	
	public OEEdata restoreOEEimage(boolean log) {
		try {
			FileInputStream fi = new FileInputStream(
					new File(FILE_ROOTPATH + filename));
			ObjectInputStream oi = new ObjectInputStream(fi);
			OEEdata oee = (OEEdata) oi.readObject();
			oi.close();
			fi.close();
			if(log) padLog("OEE data loaded from " + System.getProperty("user.dir") + 
								"\\" + FILE_ROOTPATH + filename);
			return oee;
		} catch (FileNotFoundException e) { 
			padErr("File " + filename + " not found");
		} catch (IOException e) { padErr("Error reading from " + filename);
		} catch (ClassNotFoundException e) { padErr("Object not found"); }
		return null;
	}
}