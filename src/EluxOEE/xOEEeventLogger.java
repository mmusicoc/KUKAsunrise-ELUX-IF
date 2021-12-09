package EluxOEE;

import static EluxAPI.Utils.*;

import EluxAPI.CSVLogger;

public class xOEEeventLogger {
	private String itemName;
	private CSVLogger csv;
	
	public xOEEeventLogger() { } // CONSTRUCTOR ------------------------
	
	public void init(String _itemName, String _oee_events_filename) {
		this.itemName = _itemName;
		
		this.csv = new CSVLogger();
		this.csv.init(_oee_events_filename, true);
		this.csv.header("DATE,TIME," + itemName + ",CODE,EVENT\n");
	}
	
	// PROCESS FAILURE MODES ----------------------------------------------
	
	public String reasonCode(int reasonCode) {
		switch(reasonCode) {
			case  0: return "Collision";
			case  1: return "Un-Reachable";
			case  2: return "Already Executed";
			case  3: return "Non Valid (filtered)";
			case  4: return "Not Found";
			case  5: return "Not Precise";
			case  6: return "Too Many Intents";
			case 10:return "Path non existent";
			default: return "Other";
		}
	}
	
	// EVENT LOGGER TO CSV ---------------------------------------------------
	
	public void logEvent(int item, int code) {
		csv.open();
		
		csv.log(getDateAndTime(), false);
		csv.log(item, true);
		csv.log(-code, true);
		csv.log(reasonCode(code), true); 
		csv.eol();
			
		csv.close(false);
	}
}