package EluxOEE;

import static EluxAPI.Utils.*;

import java.io.Serializable;

public class xOEE implements Serializable {
	private static final long serialVersionUID = 1L;
	private xOEEprinter print = new xOEEprinter();
	private xOEEstore store = new xOEEstore();
	private int itemAmount;
	
	private xOEEitem cycleFlags = new xOEEitem();
	private xOEEitem itemFlags = new xOEEitem();
	private xOEEitem cycle = new xOEEitem();
	private xOEEitem[] items;
	private double prevCycle, prevItem;
	
	public xOEE() { } 	// CONSTRUCTOR ---------------------------------
	
	public void init(String _cycleName, String _itemName, 
						int _itemAmount, int _maxTrials, 
						String oee_obj_filename,
						String oee_stats_filename,
						String oee_events_filename) {
		this.itemAmount = _itemAmount;
		this.resetItems();
		this.resetCycle();
		this.print.init(_cycleName, _itemName,
						_itemAmount, _maxTrials,
						cycle, items,
						oee_stats_filename,
						oee_events_filename);
		this.store.init(oee_obj_filename);
	}
	
	// PROCESS FAILURE MODES ----------------------------------------------
	
	public int checkMove(int item, int event) {
		if (event != 1) {
			print.logOEEevent(item, event);
			switch(event) {
				case -1: items[item].addIUR(); items[0].addIUR();
						 cycleFlags.addIUR(); itemFlags.addIUR(); break;
				case  0: items[item].addIWC(); items[0].addIWC();
						 cycleFlags.addIWC(); itemFlags.addIWC(); break;
				default: padLog("Event not valid.");
			}
			return 1;
		} else return 0;
	}
	
	// RESETTER METHODS --------------------------------------------------
	
	public void resetCycle() {
		cycleFlags.reset();
		cycle.reset();
		prevCycle = prevItem = getTimeStamp();
	}
	
	public void resetItems() {		// Item 0 is aggregated data
		itemFlags.reset();
		items = new xOEEitem[itemAmount + 1];
		for (int i = 0; i <= itemAmount; i++) {
			items[i] = new xOEEitem();
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
	
	// START/END MANAGERS ------------------------------------------------
	
	public void startCycle() { cycleFlags.reset(); }
	public void startItem() { itemFlags.reset(); }
	
	public void endCycle() {
		if(cycleFlags.getTMI() > 0) cycle.addTMI();
		if(cycleFlags.getIUR() > 0) cycle.addIUR();
		if(cycleFlags.getIAE() > 0) cycle.addIAE();
		if(cycleFlags.getINV() > 0) cycle.addINV();
		if(cycleFlags.getINF() > 0) cycle.addINF();
		if(cycleFlags.getIWC() > 0) cycle.addIWC();
		else {
			double currentTime = getTimeStamp();
			double cycleTime = currentTime - prevCycle;
			prevCycle = currentTime;
			cycle.setLastCT(cycleTime);
		}
	}
	
	public void endItem(int item) {
		if(itemFlags.getINR() == 0) { items[item].addRFT();  items[0].addRFT(); } 
		else { 						  items[item].addNRFT(); items[0].addNRFT();
			if(itemFlags.getIWC() == 0 && 
			   itemFlags.getTMI() == 0) { items[item].addRNFT();  items[0].addRNFT(); }
		}
		if(itemFlags.getIWC() > 0) { items[item].addPWC(); items[0].addPWC(); }
		else {
			double currentTime = getTimeStamp();
			double itemTime = currentTime - prevItem;
			prevItem = currentTime;
			items[0].setLastCT(itemTime);
			items[item].setLastCT(itemTime);
		}
	}
	
	public void addTMI(int item) {  items[item].addTMI(); items[0].addTMI();
									cycleFlags.addTMI(); itemFlags.addTMI();
									print.logOEEevent(item, -5); }
	
	public void addIAE(int item) {	items[item].addIAE(); items[0].addIAE(); 
									cycleFlags.addIAE(); itemFlags.addIAE();}
	public void addINV(int item) {  items[item].addINV(); items[0].addINV(); 
									cycleFlags.addINV(); itemFlags.addINV();}
	public void addINF(int item) {  items[item].addINF(); items[0].addINF(); 
									cycleFlags.addINF(); itemFlags.addINF();}
	
	// FILE MANAGEMENT ----------------------------------------------------
	
	public void saveOEEimage(boolean log) {
		store.saveOEEimage(this, log);
		print.saveOEEtoCSV(log);
	}
	
	public xOEE restoreOEEimage(boolean log) {return store.restoreOEEimage(log);}
	
	public void printStatsCycle() { print.printStatsCycle(); }
	public void printStatsItem(int item) { print.printStatsItem(item); }
}