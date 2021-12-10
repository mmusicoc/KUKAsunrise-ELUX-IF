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
	private String oee_obj_filename;
	
	public OEEstore() { } // CONSTRUCTOR ------------------------
	
	public void init(String _oee_obj_filename) {
		this.oee_obj_filename = _oee_obj_filename;
	}
	
	public void saveOEEimage(OEEdata oee, boolean log) {
		try {
			FileOutputStream f = new FileOutputStream(
					new File(FILE_ROOTPATH + oee_obj_filename));
			ObjectOutputStream o = new ObjectOutputStream(f);
			o.writeObject(oee);
			o.close();
			f.close();
			if(log) padLog("OEE data stored to " + 
					System.getProperty("user.dir") + "\\" + oee_obj_filename);
		} catch (FileNotFoundException e) { 
			padErr("File " + oee_obj_filename + " not found");
		} catch (IOException e) { padErr("Error writing to " + oee_obj_filename); }
	}
	
	public OEEdata restoreOEEimage(boolean log) {
		try {
			FileInputStream fi = new FileInputStream(
					new File(FILE_ROOTPATH + oee_obj_filename));
			ObjectInputStream oi = new ObjectInputStream(fi);
			OEEdata oee = (OEEdata) oi.readObject();
			oi.close();
			fi.close();
			if(log) padLog("OEE data loaded from " + 
					System.getProperty("user.dir") + "\\" + oee_obj_filename);
			return oee;
		} catch (FileNotFoundException e) { 
			padErr("File " + oee_obj_filename + " not found");
		} catch (IOException e) { padErr("Error reading from " + oee_obj_filename);
		} catch (ClassNotFoundException e) { padErr("Object not found"); }
		return null;
	}
}