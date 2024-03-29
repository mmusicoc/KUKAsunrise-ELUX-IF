package EluxOEE;

import static EluxUtils.Utils.*;
import EluxLogger.CSVLogger;

public class OEEfailureLogger {
	private String itemName;
	private CSVLogger csv;
	
	public OEEfailureLogger() { } // CONSTRUCTOR ------------------------
	
	public void init(String _itemName, String _oee_events_filename) {
		this.itemName = _itemName;
		
		this.csv = new CSVLogger(_oee_events_filename, true, ';');
		this.csv.setHeader("DATE;TIME;" + itemName + ";CODE;EVENT\n");
	}
	
	// PROCESS FAILURE MODES ----------------------------------------------
	
	public String reason(int reasonCode) {
		switch(reasonCode) {
			case   1: return "OK";	//"Successful";	
			case  -1: return "IWC"; //"Collision";
			case -10: return "IUR"; //"Un-Reachable";
			case -100:return "Path non existent";
			case   2: return "INV"; //"Non Valid (filtered)";
			case   3: return "IAE"; //"Already Executed";
			case   4: return "INF"; //"Not Found";
			case   5: return "INP"; //"Not Precise";
			case   6: return "TMI"; //"Too Many Intents";
			default: return "Other";
		}
	}
	
	// EVENT LOGGER TO CSV ---------------------------------------------------
	
	public void logFailure(int item, int code) {
		csv.open(false);
		
		csv.log(getDate(), false);
		csv.log(getTime(':'), true);
		csv.log(item, true);
		csv.log(code, true);
		csv.log(reason(code), true); 
		csv.eol();
			
		csv.close(false);
	}
}