package EluxOEE;

import static EluxAPI.Utils.*;

public class xOEE {
	private xOEEstore store;
	private xOEEpadPrinter padPrintOEE;
	private xOEEstatsCSV statsCSV;
	private xOEEeventLogger OEELogger;
	
	private int itemAmount;
	private xOEEdata d;
	private xOEEitem cycleFlags, itemFlags;
	private double prevCycle, prevItem;
	
	public xOEE() { } 	// CONSTRUCTOR ---------------------------------
	
	public void init(String _cycleName, String _itemName, 
						int _itemAmount, int _maxTrials, 
						String _oee_obj_filename,
						String _oee_stats_filename,
						String _oee_events_filename, boolean restore) {
		this.itemAmount = _itemAmount;
		
		this.cycleFlags = new xOEEitem();
		this.itemFlags = new xOEEitem();
		d = new xOEEdata();
		d.cycle = new xOEEitem();
		d.items = new xOEEitem[itemAmount + 1];
		for (int i = 0; i <= itemAmount; i++) d.items[i] = new xOEEitem();
				
		this.resetCycle();
		this.resetItems();

		this.store = new xOEEstore();
		this.store.init(_oee_obj_filename);
		
		if(restore) store.restoreOEEimage(true);
		
		this.padPrintOEE = new xOEEpadPrinter();
		this.padPrintOEE.init(_cycleName, _itemName,
							  _maxTrials, d.cycle, d.items);
		
		this.statsCSV = new xOEEstatsCSV();
		this.statsCSV.init(_itemName, _itemAmount, d,
						   _oee_stats_filename);
		
		this.OEELogger = new xOEEeventLogger();
		this.OEELogger.init(_itemName, _oee_events_filename);
	}
	
	// PROCESS FAILURE MODES ----------------------------------------------
	
	public int checkMove(int item, int event) {
		if (event != 1) {
			OEELogger.logEvent(item, -event);
			switch(event) {
				case  0: d.items[item].addIWC(); d.items[0].addIWC();
						 cycleFlags.addIWC(); itemFlags.addIWC();
						 padErr("Intent With Collision at joint " + item); break;
				case  1: d.items[item].addIUR(); d.items[0].addIUR();
				 		 cycleFlags.addIUR(); itemFlags.addIUR(); 
				 		 padErr("Intent Un-Reachable at joint " + item); break;
				default: padErr("Event not valid.");
			}
			return 1;
		} else return 0;
	}
	
	// RESETTER METHODS --------------------------------------------------
	
	public void resetCycle() {
		cycleFlags.reset();
		d.cycle.reset();
		prevCycle = prevItem = getTimeStamp();
	}
	
	public void resetItems() {		// Item 0 is aggregated data
		itemFlags.reset();
		for (int i = 0; i <= itemAmount; i++) d.items[i].reset();
		prevItem = getTimeStamp();
	}
	
	public void resetCycleTime() {
		for (int i = 0; i<= itemAmount; i++) {
			d.items[i].setFirstCycle();
		}
		d.cycle.setFirstCycle();
		padLog("Cycle Time stats have ben resetted."); 
	}
	
	// START/END MANAGERS ------------------------------------------------
	
	public void startCycle() { 
		cycleFlags.reset(); 
		prevCycle = prevItem = getTimeStamp();
	}
	public void startItem() { 
		itemFlags.reset(); 
		prevItem = getTimeStamp();
	}
	
	public void endCycle() {
		if(cycleFlags.getINR() == 0) d.cycle.addRFT();
		else {
			d.cycle.addNRFT();
			if(cycleFlags.getIWC() == 0 &&
			   cycleFlags.getTMI() == 0) d.cycle.addRNFT();
			
			if(cycleFlags.getTMI() > 0) d.cycle.addTMI();
			if(cycleFlags.getIUR() > 0) d.cycle.addIUR();
			if(cycleFlags.getIAE() > 0) d.cycle.addIAE();
			if(cycleFlags.getINV() > 0) d.cycle.addINV();
			if(cycleFlags.getINF() > 0) d.cycle.addINF();
			if(cycleFlags.getINP() > 0) d.cycle.addINP();
		}
		if(cycleFlags.getIWC() > 0) { d.cycle.addIWC(); d.cycle.addPWC(); }
		
		double currentTime = getTimeStamp();
		double cycleTime = currentTime - prevCycle;
		prevCycle = currentTime;
		d.cycle.setLastCT(cycleTime);
	}
	
	public void endItem(int item) {
		if(itemFlags.getINR() == 0) { d.items[item].addRFT();  d.items[0].addRFT(); } 
		else { 						  d.items[item].addNRFT(); d.items[0].addNRFT();
			if(itemFlags.getIWC() == 0 && 
			   itemFlags.getTMI() == 0) { d.items[item].addRNFT();  d.items[0].addRNFT(); }
		}
		if(itemFlags.getIWC() > 0) { d.items[item].addPWC(); d.items[0].addPWC(); }
		
		double currentTime = getTimeStamp();
		double itemTime = currentTime - prevItem;
		prevItem = currentTime;
		d.items[0].setLastCT(itemTime);
		d.items[item].setLastCT(itemTime);
	}
	
	// FAILURE NOTIFIERS ------------------------------------------------
	
	public void addTMI(int item) {
		d.items[item].addTMI(); d.items[0].addTMI();
		cycleFlags.addTMI(); itemFlags.addTMI();
		OEELogger.logEvent(item, 6); 
		statsCSV.saveOEEstats(false);
	}
	public void addIAE(int item) {
		d.items[item].addIAE(); d.items[0].addIAE(); 
		cycleFlags.addIAE(); itemFlags.addIAE();
	}
	public void addINV(int item) {
		d.items[item].addINV(); d.items[0].addINV(); 
		cycleFlags.addINV(); itemFlags.addINV();
	}
	public void addINF(int item) {
		d.items[item].addINF(); d.items[0].addINF();
		cycleFlags.addINF(); itemFlags.addINF();
	}
	public void addINP(int item) {
		d.items[item].addINP(); d.items[0].addINP();
		cycleFlags.addINP(); itemFlags.addINP();
		OEELogger.logEvent(item, 5);
	}
	
	// FILE MANAGEMENT ----------------------------------------------------
	
	public void saveOEEimage(boolean log) {
		store.saveOEEimage(d, log);
		statsCSV.saveOEEstats(log);
	}
	
	public void restoreOEEimage(boolean log) { d = store.restoreOEEimage(log);}
	
	public void printStatsCycle() { padPrintOEE.printStatsCycle(); }
	public void printStatsItem(int item) { padPrintOEE.printStatsItem(item); }
}