package EluxUtils;		// Valid only for non generic-based classes, 
						// if so override fetch() with TypeToken approach

import static EluxUtils.Utils.*;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

public class JSONmgr<O> {
	private String filename;
	
	public JSONmgr() { } // CONSTRUCTOR ------------------------
	
	public void init(String filename) { this.filename = filename; }
	
	public O fetchData(O data) {
		Gson gson = new Gson();
		try {
			JsonReader reader = new JsonReader(
					new FileReader(FILE_ROOTPATH + filename));
			return gson.fromJson(reader, data.getClass());
		} catch (FileNotFoundException e) {
			padErr("File " + filename + " not found");
			return null;
		} 
	}
	
	public boolean saveData(O data) {
		try {
			FileWriter fw = new FileWriter(
					new File(FILE_ROOTPATH + filename), false);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			gson.toJson(data, fw);
			fw.flush();
			fw.close();
			return true;
		} catch (FileNotFoundException e) {
			padErr("File " + filename + " not found");
			return false;
		} catch (IOException e) {
			//e.printStackTrace();
			padErr("Error writing to " + filename);
			return false;
		}
	}
	
	public void printData(O data) {
		if(data == null) padErr("Nonexistent");
		else {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String json = gson.toJson(data);
			padLog(json);
		}
	}
}