package EluxAPI;

import static EluxAPI.Utils.*;

import javax.inject.Inject;
import java.io.Serializable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

// TOTAL = GOOD + BAD
// GOOD = RFT + RNFT
// BAD = NRFT - RNFT

public class API_OEE implements Serializable {
	private static final long serialVersionUID = 1L;
	private String processName;
	private int itemAmount;
	private int maxTrials;
	private ProcessItem[] items;
	private ProcessItem cycle;
	private double prevItem, prevCycle;
	boolean cycleWithFails;
	boolean cycleWithBads;
	
	// Constructor
	@Inject public API_OEE() { }
	
	// GETTER METHODS -----------------------------------------------------
	
	public void printStats(int item) {
		if(item == -1) statsCycle(); 
		else statsItem(item);
	}
	
	private void statsCycle() {
		double rate;
		String stats = new String("STATISTICS FOR " + processName + " CYCLES -----------");
		stats = (stats + "\nTOTAL cycles: " + cycle.getTotal());
		stats = (stats + "\nGOOD cycles: " + cycle.getGood());
		stats = (stats + "\nCycles with BAD: " + cycle.getBad());
		stats = (stats + "\nCycles with FAILS: " + cycle.getFail());
		
		rate = cycle.getFail() / (cycle.getTotal() + 0.0);
		stats = (stats + "\n\nFAILS/TOTAL: " + String.format("%,.2f", rate));
		rate = cycle.getBad() / (cycle.getTotal() + 0.0);
		stats = (stats + "\nBAD rate: " + String.format("%,.2f",100 * rate) + "%");
		
		stats = (stats + "\n\nLAST Cycle Time: " + String.format("%,.2f",
				cycle.getLastCycleTime() / 1000));
		stats = (stats + "\nAVG Cycle Time: " + String.format("%,.2f",
				cycle.getAvgCycleTime() / 1000));
		
		stats = (stats + "\n---------------------------------------------------");
		padLog(stats);
	}
	
	private void statsItem(int item) {
		double rate;
		String itemName = (processName + " " + (item > 0 ? item : "TOTAL") );
		String stats = new String("STATISTICS FOR " + itemName + " ---------------");
		itemName = itemName.toLowerCase();
		stats = (stats + "\nTOTAL " + itemName + ": " + items[item].getTotal());
		stats = (stats + "\nGOOD " + itemName + ": " + items[item].getGood());
		stats = (stats + "\n   of which RFT " + itemName + ": " + items[item].getRFT());
		stats = (stats + "\n   of which RNFT " + itemName + ": " + items[item].getRNFT());
		stats = (stats + "\nNRFT " + itemName + ": " + items[item].getNRFT());
		stats = (stats + "\n   of which BAD " + itemName + ": " + items[item].getBad());
		stats = (stats + "\n   of which RNFT " + itemName + ": " + items[item].getRNFT());
		stats = (stats + "\nFAILS in " + itemName + ": " + items[item].getFail());
		
		rate = (items[item].getGood() + items[item].getFail() - items[item].getBad() * 
				maxTrials) / (items[item].getGood() + 0.0);
		stats = (stats + "\n\nTRIALS/GOOD: " + String.format("%,.2f", rate));
		rate = items[item].getFail() / (items[item].getTotal() + 0.0);
		stats = (stats + "\nFAILS/TOTAL: " + String.format("%,.2f", rate));
		rate = items[item].getBad() / (items[item].getTotal() + 0.0);
		stats = (stats + "\nBAD rate: " + String.format("%,.2f",100 * rate) + "%");
		
		stats = (stats + "\n\nLAST Cycle Time: " + String.format("%,.2f",
				items[item].getLastCycleTime() / 1000));
		stats = (stats + "\nAVG Cycle Time: " + String.format("%,.2f",
				items[item].getAvgCycleTime() / 1000));
		
		stats = (stats + "\n---------------------------------------------------");
		padLog(stats);
	}
	
	// SETTER METHODS ----------------------------------------------------
	
	public void init(String _processName, int _processItems, int _maxTrials) {
		this.processName = _processName;
		this.itemAmount = _processItems;
		this.maxTrials = _maxTrials;
		this.resetItems();
		this.resetCycle();
	}
	
	public void resetCycle() {
		cycle = new ProcessItem();
		cycle.reset();
		prevCycle = prevItem = getTimeStamp();
	}
	
	public void resetItems() {
		items = new ProcessItem[itemAmount + 2]; 		// Item 0 is aggregated data
		for (int i = 0; i <= (itemAmount); i++) {
			items[i] = new ProcessItem();
			items[i].reset();
		}
		prevItem = getTimeStamp();
	}
	
	public void startCycle() { cycleWithFails = cycleWithBads = false; }
	
	public void addNewCycle() {
		double currentTime = getTimeStamp();
		double cycleTime = currentTime - prevCycle;
		prevCycle = currentTime;
		cycle.addItem();
		if (cycleWithFails) cycle.addFail();
		if (cycleWithBads) cycle.addBad();
		else cycle.addGood();
		cycle.setLastCycleTime(cycleTime);
		startCycle();
	}
	
	public void addItem(int item) {
		items[item].addItem();
		items[0].addItem();
		double currentTime = getTimeStamp();
		double itemTime = currentTime - prevItem;
		prevItem = currentTime;
		items[0].setLastCycleTime(itemTime);
		items[item].setLastCycleTime(itemTime);
		
	}
	
	public void addGood(int item) { items[item].addGood(); items[0].addGood(); }
	public void addBad(int item) { items[item].addBad(); items[0].addBad();
		cycleWithBads = true; }
	public void addFail(int item) { items[item].addFail(); items[0].addFail(); 
		cycleWithFails = true; }
	public void addRFT(int item) { items[item].addRFT(); items[0].addRFT(); }
	public void addRNFT(int item) { items[item].addRNFT(); items[0].addRNFT(); }
	public void addNRFT(int item) { items[item].addNRFT(); items[0].addNRFT(); }

	// COLD STORAGE ----------------------------------------------------
	
	public void saveOEEtoFile(API_OEE oee, String filename) {
		try {
			FileOutputStream f = new FileOutputStream(new File("OEE.txt"));
			ObjectOutputStream o = new ObjectOutputStream(f);
			o.writeObject(oee);
			o.close();
			f.close();
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
		} catch (IOException e) {
			System.out.println("Error initializing stream");
		}
	}
	
	public API_OEE restoreOEEfromFile(String filename) {
		API_OEE oee;
		try {
			FileInputStream fi = new FileInputStream(new File("OEE.txt"));
			ObjectInputStream oi = new ObjectInputStream(fi);
			oee = (API_OEE) oi.readObject();
			padLog(oee.toString());
			oi.close();
			fi.close();

		} catch (FileNotFoundException e) {
			System.out.println("File not found");
			oee = null;
		} catch (IOException e) {
			System.out.println("Error initializing stream");
			oee = null;
		} catch (ClassNotFoundException e) {
			oee = null;
			e.printStackTrace();
		} 
		return oee;
	}

}

class ProcessItem {		//---------------------------------------------------------------
	private int total;	// = GOOD + BAD
	private int good;	// = RFT + RNFT
	private int bad;	// = NRFT - RNFT
	private int fail;
	private int RFT;	// Right First Time
	private int RNFT;	// Right Not First Time
	private int NRFT;	// Not Right First Time
	
	private double lastCycleTime;
	private double avgCycleTime;

	public ProcessItem() {
		this.reset();
	}
	
	// Getter methods
	public int getTotal() 	{ return total; }
	public int getGood() 	{ return good; }
	public int getBad() 	{ return bad; }
	public int getFail() 	{ return fail; }
	public int getRFT() 	{ return RFT; }
	public int getRNFT() 	{ return RNFT; }
	public int getNRFT() 	{ return NRFT; }
	
	public double getLastCycleTime() { return lastCycleTime; }
	public double getAvgCycleTime()  { return avgCycleTime; }
	
	// Setter methods
	public void reset() {
		total = good = bad = fail = RFT = RNFT = NRFT = 0;
		lastCycleTime = avgCycleTime = 0.0;
	}
	
	public void addItem() 	{ total++; }
	public void addGood() 	{ good++; }
	public void addBad() 	{ bad++; }
	public void addFail() 	{ fail++; }
	public void addRFT() 	{ RFT++; }
	public void addRNFT() 	{ RNFT++; }
	public void addNRFT() 	{ NRFT++; }
	
	public void setLastCycleTime(double _lastCycleTime) { 
		lastCycleTime = _lastCycleTime;
		avgCycleTime = (avgCycleTime * (total - 1) + lastCycleTime) / (total + 0.0);
	}
}

class StoreOEE {
	public StoreOEE() {
	}
	
	
}