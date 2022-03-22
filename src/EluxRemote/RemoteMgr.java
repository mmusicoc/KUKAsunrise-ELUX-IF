package EluxRemote;

import EluxUtils.JSONmgr;

public class RemoteMgr {
	private Remote data;
	private JSONmgr<Remote> json;
	
	public RemoteMgr() { 	// CONSTRUCTOR
	}
	
	public void init(String filename) {
		data = new Remote();
		json = new JSONmgr<Remote>();
		json.init(filename);
	}
	// GETTERS ---------------------------------------------------------------
	
	public int getIdle() { fetch(); return data.idle; }
	public boolean getLogger() { fetch(); return data.logger; }
	public double getSpeed() { fetch(); return data.speed; }
	public double getAccel() { fetch(); return data.accel; }
	public String getProg() { fetch(); return data.prog; }
	
	public void fetch() { data = json.fetchData(data); }
	
	// SETTERS ---------------------------------------------------------------
	public void setIdle(int idle) { 
		if(data.idle != idle) {	data.idle = idle; save(); } }
	public void setLogger(boolean logger) { 
		if(data.logger != logger) { data.logger = logger; save(); } }
	public void setSpeed(double speed) { 
		if(data.speed != speed) { data.speed = speed; save(); } }
	public void setProg(String prog) { 
		if(data.prog != prog) { data.prog = prog; save(); } }
	
	public void save() { json.saveData(data); }
}