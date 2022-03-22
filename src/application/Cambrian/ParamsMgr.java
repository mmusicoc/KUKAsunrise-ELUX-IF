/*package application.Cambrian;

import EluxUtils.JSONmgr;

public class ParamsMgr {
	Params p;
	private JSONmgr<Params> json;
	
	public ParamsMgr() { 	// CONSTRUCTOR
	}
	
	public void init(String filename) {
		p = new Params();
		json = new JSONmgr<Params>();
		json.init(filename);
	}
	// GETTERS ---------------------------------------------------------------
	
	public void fetch() { p = json.fetchData(p); }
	
	// SETTERS ---------------------------------------------------------------
	
	public void save() { json.saveData(p); }
}
*/