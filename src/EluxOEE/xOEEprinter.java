package EluxOEE;

import static EluxAPI.Utils.*;

import java.io.FileWriter;
import java.io.Serializable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class xOEEprinter implements Serializable {
	private static final long serialVersionUID = 3L;
	private String cycleName;
	private String itemName;
	private char itemInitial;
	private int itemAmount;
	private int maxTrials;
	private String oee_stats_filename;
	private String oee_events_filename;
	
	private xOEEitem cycle;
	private xOEEitem[] items;
	
	public xOEEprinter() { } // CONSTRUCTOR ------------------------
	
	public void init(String _cycleName, String _itemName, 
						int _itemAmount, int _maxTrials, 
						xOEEitem _cycle, xOEEitem[] _items,
						String _oee_stats_filename,
						String _oee_events_filename) {
		this.cycleName = _cycleName;
		this.itemName = _itemName;
		this.itemInitial = Character.toUpperCase(itemName.charAt(0));
		this.itemAmount = _itemAmount;
		this.maxTrials = _maxTrials;
		this.cycle = _cycle;
		this.items = _items;
		this.oee_stats_filename = _oee_stats_filename;
		this.oee_events_filename = _oee_events_filename;
	}
	
	// PROCESS FAILURE MODES ----------------------------------------------
	
	public String reasonCode(int reasonCode) {
		switch(reasonCode) {
			case 0:  return "Collision";
			case -1: return "Un-Reachable";
			case -2: return "Already Executed";
			case -3: return "Non Valid (filtered)";
			case -4: return "Not Found";
			case -5: return "Too Many Intents";
			case -10:return "Path non existent";
			default: return "Other";
		}
	}
	
	// PRINT STATS TO SMARTPAD --------------------------------------------
	
	public void printStatsCycle() {
		double rate;
		String cycleID = cycleName.toLowerCase() + "s";
		String stats = new String("STATISTICS FOR " + cycleName + "S -----------");
		stats = (stats + "\nTOTAL " + cycleID + ": " + cycle.getTotal());
		stats = (stats + "\nGOOD " + cycleID + ": " + cycle.getGood());
		cycleID = cycleID.substring(0,1).toUpperCase() + cycleID.substring(1);
		stats = (stats + "\n" + cycleID + " with bad: " + cycle.getTMI());
		stats = (stats + "\n   of which " + cycleID + " with TMI: " + cycle.getTMI());
		stats = (stats + "\n   of which " + cycleID + " with PWC: " + cycle.getPWC());
		

		rate = cycle.getBad() / (cycle.getTotal() + 0.0);
		stats = (stats + "\nBAD rate: " + String.format("%,.2f",100 * rate) + "%");
		
		stats = (stats + "\n\nLAST Cycle Time: " + String.format("%,.2f",
				cycle.getLastCT() / 1000));
		stats = (stats + "\nAVG Cycle Time: " + String.format("%,.2f",
				cycle.getAvgCT() / 1000));
		
		stats = (stats + "\n---------------------------------------------------");
		padLog(stats);
	}
	
	public void printStatsItem(int item) {
		double rate;
		String itemID = (itemName + " " + (item > 0 ? item : "TOTAL") );
		String stats = new String("STATISTICS FOR " + itemID + " ---------------");
		itemID = itemID.toLowerCase();
		stats = (stats + "\nTOTAL " + itemID + ": " + items[item].getTotal());
		stats = (stats + "\nGOOD " + itemID + ": " + items[item].getGood());
		stats = (stats + "\n   of which RFT " + itemID + ": " + items[item].getRFT());
		stats = (stats + "\n   of which RNFT " + itemID + ": " + items[item].getRNFT());
		stats = (stats + "\nBAD " + itemID + ": " + items[item].getBad());
		stats = (stats + "\n   of which TMI " + itemID + ": " + items[item].getTMI());
		stats = (stats + "\n   of which PWC " + itemID + ": " + items[item].getPWC());
		stats = (stats + "\nNRFT " + itemID + ": " + items[item].getNRFT());
		
		stats = (stats + "\nINTENT STATISTICS -----------------------------------");
		stats = (stats + "\nIntents Not Right in " + itemID + ": " + items[item].getINR());
		stats = (stats + "\n   of which With Collision: " + items[item].getIWC());
		stats = (stats + "\n   of which Un-Reachable: " + items[item].getIUR());
		stats = (stats + "\n   of which Already Executed: " + items[item].getIAE());
		stats = (stats + "\n   of which Non Valid: " + items[item].getINV());
		stats = (stats + "\n   of which Not Found: " + items[item].getINF());
		
		stats = (stats + "\nSUCCESS RATES -----------------------------------");
		rate = (items[item].getGood() + items[item].getINR() - items[item].getTMI() * 
				maxTrials - items[item].getPWC()) / (items[item].getGood() + 0.0);
		stats = (stats + "\n\nTRIALS/GOOD: " + String.format("%,.2f", rate));
		rate = items[item].getINR() / (items[item].getTotal() + 0.0);
		stats = (stats + "\nNRI/TOTAL: " + String.format("%,.2f", rate));
		rate = items[item].getTMI() / (items[item].getTotal() + 0.0);
		stats = (stats + "\nTMI/TOTAL: " + String.format("%,.2f",100 * rate) + "%");
		rate = items[item].getPWC() / (items[item].getTotal() + 0.0);
		stats = (stats + "\nPWC/TOTAL: " + String.format("%,.2f",100 * rate) + "%");
		
		stats = (stats + "\nCYCLE TIME -----------------------------------");
		stats = (stats + "\n\nLAST Cycle Time: " + String.format("%,.2f",
				items[item].getLastCT() / 1000));
		stats = (stats + "\nAVG Cycle Time: " + String.format("%,.2f",
				items[item].getAvgCT() / 1000));
		
		stats = (stats + "\n---------------------------------------------------");
		padLog(stats);
	}
	
	// PRINT STATS TO SUMMARY CSV ------------------------------------------
	
	public void saveOEEtoCSV(boolean log) {
		try{
			FileWriter fw = new FileWriter(new File(oee_stats_filename), false);
			// PRINT HEADER -----------------------------------------
			fw.append("METRIC,CYCLE," + itemInitial + " TOTALS");
			for(int i = 1; i <= itemAmount; i++)
				logCSV(fw, String.valueOf(itemInitial) + i, true);
			fw.append("\n");
			
			// PRINT METRICS ---------------------------------------
			logCSV(fw, "TOTAL", false); logCSV(fw, cycle.getTotal(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getTotal(), true);
			fw.append("\n");
			
			logCSV(fw, "GOOD", false); logCSV(fw, cycle.getGood(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getGood(), true);
			fw.append("\n");
			
			logCSV(fw, "BAD", false); logCSV(fw, cycle.getBad(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getBad(), true);
			fw.append("\n");
			
			logCSV(fw, "TMI", false); logCSV(fw, cycle.getTMI(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getTMI(), true);
			fw.append("\n");
			
			logCSV(fw, "PWC", false); logCSV(fw, cycle.getPWC(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getPWC(), true);
			fw.append("\n");
			// --------------------------------
			logCSV(fw, "RFT", false); logCSV(fw, cycle.getRFT(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getRFT(), true);
			fw.append("\n");
			
			logCSV(fw, "RNFT", false); logCSV(fw, cycle.getRNFT(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getRNFT(), true);
			fw.append("\n");
			
			logCSV(fw, "NRFT", false); logCSV(fw, cycle.getNRFT(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getNRFT(), true);
			fw.append("\n");
			// --------------------------------
			logCSV(fw, "INR", false); logCSV(fw, cycle.getINR(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getINR(), true);
			fw.append("\n");
			
			logCSV(fw, "IWC", false); logCSV(fw, cycle.getIWC(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getIWC(), true);
			fw.append("\n");
			
			logCSV(fw, "IUR", false); logCSV(fw, cycle.getIUR(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getIUR(), true);
			fw.append("\n");
			
			logCSV(fw, "IAE", false); logCSV(fw, cycle.getIAE(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getIAE(), true);
			fw.append("\n");

			logCSV(fw, "INV", false); logCSV(fw, cycle.getINV(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getINV(), true);
			fw.append("\n");
			
			logCSV(fw, "INF", false); logCSV(fw, cycle.getINF(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getINF(), true);
			fw.append("\n");
			// --------------------------------
			logCSV(fw, "CT", false); logCSV(fw, cycle.getAvgCT(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getAvgCT(), true);
			fw.append("\n");
			
			// CLOSE STREAM -------------------------------------------------
			fw.flush();
			fw.close();
			if(log) padLog("OEE metrics stored to " + 
					System.getProperty("user.dir") + "\\" + oee_stats_filename);
		} catch (FileNotFoundException e) {
			padErr("File not found");
		} catch (IOException e) {
			padErr("Error initializing output stream"); }
	}
	
	// EVENT LOGGER TO CSV ---------------------------------------------------
	
	public void logOEEevent(int item, int code) {
		try{
			FileWriter fw = new FileWriter(oee_events_filename, true);
			if(isFileEmpty(oee_events_filename)) {
				padLog(oee_events_filename + " is empty, creating new one.");
				fw.append("DATE,TIME," + itemName + ",CODE,EVENT\n");
			}
			logCSV(fw, getDateAndTime(), false);
			logCSV(fw, item, true);
			logCSV(fw, code, true);
			logCSV(fw, reasonCode(code), true); 
			fw.append("\n");
			
			fw.flush();
			fw.close();
		} catch (FileNotFoundException e) {
			padErr("File not found");
		} catch (IOException e) {
			padErr("Error initializing output stream"); }
		saveOEEtoCSV(false);
	}
	
	private boolean logCSV(FileWriter fw, String msg, boolean comma) {
		try{
			if(comma) fw.append(",");
			fw.append(msg);
			return true;
		} catch (IOException e) {
			padErr("Error initializing output stream"); return false;}
	}
	
	private boolean logCSV(FileWriter fw, int value, boolean comma) {
		return logCSV(fw, String.valueOf(value), comma);
	}
	
	private boolean logCSV(FileWriter fw, double value, boolean comma) {
		return logCSV(fw, String.format("%,.2f",value), comma);
	}
}