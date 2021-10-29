package EluxAPI;

import static EluxAPI.Utils.*;

import java.io.FileWriter;
import java.io.Serializable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class xAPI_OEE implements Serializable {
	private static final long serialVersionUID = 1L;
	private String processName;
	private char processInitial;
	private int itemAmount;
	private int maxTrials;
	private String oee_obj_filename;
	private String oee_stats_filename;
	private String oee_events_filename;
	
	private xAPI_OEEitem[] items;
	private xAPI_OEEitem cycle;
	private double prevItem, prevCycle;
	boolean itemWithPause;
	boolean cycleWithBads;
	boolean cycleWithINR;
	
	public xAPI_OEE() { } 	// CONSTRUCTOR ---------------------------------
	
	// PRINTER METHODS -----------------------------------------------------
	
	public void printStatsCycle() {
		double rate;
		String stats = new String("STATISTICS FOR " + processName + " CYCLES -----------");
		stats = (stats + "\nTOTAL cycles: " + cycle.getTotal());
		stats = (stats + "\nGOOD cycles: " + cycle.getGood());
		stats = (stats + "\nCycles with TMI: " + cycle.getTMI());
		stats = (stats + "\nCycles with NRI: " + cycle.getINR());
		
		//rate = cycle.getNRI() / (cycle.getTotal() + 0.0);
		//stats = (stats + "\n\nNRI/TOTAL: " + String.format("%,.2f", rate));
		rate = cycle.getTMI() / (cycle.getTotal() + 0.0);
		stats = (stats + "\nBAD rate: " + String.format("%,.2f",100 * rate) + "%");
		
		stats = (stats + "\n\nLAST Cycle Time: " + String.format("%,.2f",
				cycle.getLastCT() / 1000));
		stats = (stats + "\nAVG Cycle Time: " + String.format("%,.2f",
				cycle.getAvgCT() / 1000));
		
		stats = (stats + "\n---------------------------------------------------");
		System.out.println(stats);
	}
	
	public void printStatsItem(int item) {
		double rate;
		String itemName = (processName + " " + (item > 0 ? item : "TOTAL") );
		String stats = new String("STATISTICS FOR " + itemName + " ---------------");
		itemName = itemName.toLowerCase();
		stats = (stats + "\nTOTAL " + itemName + ": " + items[item].getTotal());
		stats = (stats + "\nGOOD " + itemName + ": " + items[item].getGood());
		stats = (stats + "\n   of which RFT " + itemName + ": " + items[item].getRFT());
		stats = (stats + "\n   of which RNFT " + itemName + ": " + items[item].getRNFT());
		stats = (stats + "\nNRFT " + itemName + ": " + items[item].getNRFT());
		stats = (stats + "\n   of which TMI " + itemName + ": " + items[item].getTMI());
		stats = (stats + "\n   of which RNFT " + itemName + ": " + items[item].getRNFT());
		stats = (stats + "\nNRI in " + itemName + ": " + items[item].getINR());
		
		rate = (items[item].getGood() + items[item].getINR() - items[item].getTMI() * 
				maxTrials) / (items[item].getGood() + 0.0);
		stats = (stats + "\n\nTRIALS/GOOD: " + String.format("%,.2f", rate));
		rate = items[item].getINR() / (items[item].getTotal() + 0.0);
		stats = (stats + "\nNRI/TOTAL: " + String.format("%,.2f", rate));
		rate = items[item].getTMI() / (items[item].getTotal() + 0.0);
		stats = (stats + "\nTMI rate: " + String.format("%,.2f",100 * rate) + "%");
		
		stats = (stats + "\n\nLAST Cycle Time: " + String.format("%,.2f",
				items[item].getLastCT() / 1000));
		stats = (stats + "\nAVG Cycle Time: " + String.format("%,.2f",
				items[item].getAvgCT() / 1000));
		
		stats = (stats + "\n---------------------------------------------------");
		System.out.println(stats);
	}
	
	// SETTER METHODS ----------------------------------------------------
	
	public void init(String _processName, int _processItems, int _maxTrials, 
			String _oee_obj_filename,
			String _oee_stats_filename,
			String _oee_events_filename) {
		this.processName = _processName;
		this.processInitial = Character.toUpperCase(_processName.charAt(0));
		this.itemAmount = _processItems;
		this.maxTrials = _maxTrials;
		this.oee_obj_filename = _oee_obj_filename;
		this.oee_stats_filename = _oee_stats_filename;
		this.oee_events_filename = _oee_events_filename;
		this.resetItems();
		this.resetCycle();
	}
	
	public void resetCycle() {
		cycle = new xAPI_OEEitem();
		cycle.reset();
		prevCycle = prevItem = getTimeStamp();
	}
	
	public void resetItems() {
		items = new xAPI_OEEitem[itemAmount + 1]; 		// Item 0 is aggregated data
		for (int i = 0; i <= itemAmount; i++) {
			items[i] = new xAPI_OEEitem();
			items[i].reset();
		}
		prevItem = getTimeStamp();
	}
	
	public void resetCycleTime() {
		for (int i = 0; i<= itemAmount; i++) {
			items[i].setFirstCycle();
		}
		cycle.setFirstCycle();
	}
	
	public void startCycle() { cycleWithINR = cycleWithBads = false; }
	
	public void addNewCycle() {
		double currentTime = getTimeStamp();
		double cycleTime = currentTime - prevCycle;
		prevCycle = currentTime;
		cycle.addTotal();
		if (cycleWithINR) cycle.addINR();
		if (cycleWithBads) cycle.addTMI();
		else cycle.addGood();
		cycle.setLastCT(cycleTime);
		startCycle();
	}
	
	public void addItem(int item) {
		items[item].addTotal();
		items[0].addTotal();
		if(!itemWithPause) {
			double currentTime = getTimeStamp();
			double itemTime = currentTime - prevItem;
			prevItem = currentTime;
			items[0].setLastCT(itemTime);
			items[item].setLastCT(itemTime);
		}
		itemWithPause = false;
	}
	
	public void addGood(int item) { items[item].addGood(); items[0].addGood(); 
		addItem(item); }
	public void addTMI(int item) { items[item].addTMI(); items[0].addTMI();
		cycleWithBads = true; addItem(item); logOEEevent(item, -3); }
	public void addINR(int item) { items[item].addINR(); items[0].addINR(); 
		cycleWithINR = true; }
	public void addRFT(int item) { items[item].addRFT(); items[0].addRFT(); }
	public void addRNFT(int item) { items[item].addRNFT(); items[0].addRNFT(); }
	public void addNRFT(int item) { items[item].addNRFT(); items[0].addNRFT(); }
	
	// COLD STORAGE ----------------------------------------------------
	
	public void saveOEEtoFile(xAPI_OEE oee, boolean log) {
		try {
			FileOutputStream f = new FileOutputStream(new File(oee_obj_filename));
			ObjectOutputStream o = new ObjectOutputStream(f);
			o.writeObject(oee);
			o.close();
			f.close();
			if(log) padLog("OEE data stored to " + 
					System.getProperty("user.dir") + "\\" + oee_obj_filename);
		} catch (FileNotFoundException e) {
			padErr("File not found");
		} catch (IOException e) {
			padErr("Error initializing output stream");
		}
		saveOEEtoCSV(log);
	}
	
	public xAPI_OEE restoreOEEfromFile(boolean log) {
		xAPI_OEE oee = new xAPI_OEE();
		try {
			FileInputStream fi = new FileInputStream(new File(oee_obj_filename));
			ObjectInputStream oi = new ObjectInputStream(fi);
			oee = (xAPI_OEE) oi.readObject();
			oi.close();
			fi.close();
			if(log) padLog("OEE data loaded from " + 
					System.getProperty("user.dir") + "\\" + oee_obj_filename);
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
			oee = null;
		} catch (IOException e) {
			System.out.println("Error initializing input stream");
			oee = null;
		} catch (ClassNotFoundException e) {
			oee = null;
			e.printStackTrace(); } 
		return oee;
	}
	
	public void saveOEEtoCSV(boolean log) {
		try{
			FileWriter fw = new FileWriter(new File(oee_stats_filename), false);
			// PRINT HEADER -----------------------------------------
			fw.append("METRIC,CYCLE," + processInitial + " TOTALS");
			for(int i = 1; i <= itemAmount; i++)
				logCSV(fw, String.valueOf(processInitial) + i, true);
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
			
			logCSV(fw, "PWC", false); logCSV(fw, cycle.getPWC(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getPWC(), true);
			fw.append("\n");
			
			logCSV(fw, "INR", false); logCSV(fw, cycle.getINR(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getINR(), true);
			fw.append("\n");
			
			logCSV(fw, "INF", false); logCSV(fw, cycle.getINF(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getINF(), true);
			fw.append("\n");
			
			logCSV(fw, "IUR", false); logCSV(fw, cycle.getIUR(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getIUR(), true);
			fw.append("\n");
			
			logCSV(fw, "IWC", false); logCSV(fw, cycle.getIWC(), true);
			for(int i = 0; i <= itemAmount; i++) 
				logCSV(fw, items[i].getIWC(), true);
			fw.append("\n");
			
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
	
	// EVENT LOGGER ----------------------------------------------------
	
	public String reasonCode(int reasonCode) {
		String error = new String("");
		switch(reasonCode) {
			case 0:  error = "Collision"; break;
			case -1: error = "Not reachable"; break;
			case -2: error = "Non existent"; break;
			case -3: error = "Not found"; break;
			default: error = "Other"; break;
		}
		return error;
	}
	
	public void event(int item, int event) {
		if (event != 1) logOEEevent(item, event);
		if (event == 0) itemWithPause = true;
	}
	
	public void logOEEevent(int item, int code) {
		try{
			FileWriter fw = new FileWriter(oee_events_filename, true);
			if(isFileEmpty(oee_events_filename)) {
				padLog(oee_events_filename + " is empty, creating new one.");
				fw.append("DATE,TIME," + processName + ",CODE,EVENT\n");
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