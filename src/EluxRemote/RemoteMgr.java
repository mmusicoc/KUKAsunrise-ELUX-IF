package EluxRemote;

import static EluxUtils.Utils.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

public class RemoteMgr {
	protected String filename;
	protected Remote data;
	
	public RemoteMgr() { 	// CONSTRUCTOR
	}
	
	public void init(String _filename) {
		this.filename = _filename;
		data = new Remote();
	}
	// GETTERS ---------------------------------------------------------------
	
	public void checkIdle() {
		if(fetchRemoteData() && data.getIdle()) {
			padLog("Remote has enabled idle mode, set FALSE to resume");
			while(data.getIdle()) {
				waitMillis(1000);
				fetchRemoteData();
			}
			padLog("Remote has resumed operations");
		}
	}
	public double getSpeed() { return data.getSpeed(); }
	public String getProg() { return data.getProg(); }
	
	public boolean fetchRemoteData() {
		Gson gson = new Gson();
		try {
			JsonReader reader = new JsonReader(
					new FileReader(FILE_ROOTPATH + filename));
			data = gson.fromJson(reader, Remote.class);
			return true;
		} catch (FileNotFoundException e) {
			padErr("File " + filename + " not found");
			return false;
		} 
	}
	
	// SETTERS ---------------------------------------------------------------
	public void setIdle(boolean idle) { data.setIdle(idle); }
	public void setSpeed(double speed) { data.setSpeed(speed); }
	public void setProg(String prog) { data.setProg(prog); }
	
	public void saveCurrentData() {
		try {
			FileWriter fw = new FileWriter(
					new File(FILE_ROOTPATH + filename), false);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			gson.toJson(data, fw);
			fw.flush();
			fw.close();
		} catch (FileNotFoundException e) {
			padErr("File " + filename + " not found");
		} catch (IOException e) {
			//e.printStackTrace();
			padErr("Error writing to " + filename);
		}
	}
}