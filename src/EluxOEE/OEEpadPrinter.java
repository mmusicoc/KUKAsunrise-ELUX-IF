package EluxOEE;

import static EluxUtils.Utils.*;

public class OEEpadPrinter {
	private String cycleName;
	private String itemName;
	private int maxTrials;
	
	private OEEitem cycle;
	private OEEitem[] items;
	
	public OEEpadPrinter() { } // CONSTRUCTOR ------------------------
	
	public void init(String _cycleName, String _itemName, int _maxTrials, 
						OEEitem _cycle, OEEitem[] _items) {
		this.cycleName = _cycleName;
		this.itemName = _itemName;
		this.maxTrials = _maxTrials;
		this.cycle = _cycle;
		this.items = _items;
	}
	
	public void printStatsCycle() {
		double rate;
		String cycleID = cycleName.toLowerCase() + "s";
		String stats = new String("STATISTICS FOR " + cycleName + "S -----------------");
		stats = (stats + "\nTOTAL " + cycleID + ": " + cycle.getTotal());
		stats = (stats + "\nGOOD " + cycleID + ": " + cycle.getGood());
		cycleID = cycleID.substring(0,1).toUpperCase() + cycleID.substring(1);
		stats = (stats + "\n" + cycleID + " with BAD: " + cycle.getTMI());
		stats = (stats + "\n   of which " + cycleID + " with TMI: " + cycle.getTMI());
		stats = (stats + "\n   of which " + cycleID + " with PWC: " + cycle.getPWC());
		

		rate = cycle.getBad() / (cycle.getTotal() + 0.0);
		stats = (stats + "\nBAD rate: " + String.format("%,.2f",100 * rate) + "%");
		
		stats = (stats + "\n\nLAST Cycle Time: " + String.format("%,.2f",
				cycle.getLastCT() / 1000));
		stats = (stats + "\nAVG Cycle Time: " + String.format("%,.2f",
				cycle.getAvgCT() / 1000));
		
		stats = (stats + "\n---------------------------------------------------");
		logmsg(stats);
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
		
		stats = (stats + "\n\nINTENT STATISTICS ----------------------------");
		stats = (stats + "\nIntents Not Right in " + itemID + ": " + items[item].getINR());
		stats = (stats + "\n   of which With Collision: " + items[item].getIWC());
		stats = (stats + "\n   of which Un-Reachable: " + items[item].getIUR());
		stats = (stats + "\n   of which Already Executed: " + items[item].getIAE());
		stats = (stats + "\n   of which Non Valid: " + items[item].getINV());
		stats = (stats + "\n   of which Not Found: " + items[item].getINF());
		stats = (stats + "\n   of which Not Precise: " + items[item].getINP());
		
		stats = (stats + "\n\nSUCCESS RATES -----------------------------");
		rate = (items[item].getGood() + items[item].getINR() - items[item].getTMI() * 
				maxTrials - items[item].getPWC()) / (items[item].getGood() + 0.0);
		stats = (stats + "\nTRIALS/GOOD: " + String.format("%,.2f", rate));
		rate = items[item].getINR() / (items[item].getTotal() + 0.0);
		stats = (stats + "\nNRI/TOTAL: " + String.format("%,.2f", rate));
		rate = items[item].getTMI() / (items[item].getTotal() + 0.0);
		stats = (stats + "\nTMI/TOTAL: " + String.format("%,.2f",100 * rate) + "%");
		rate = items[item].getPWC() / (items[item].getTotal() + 0.0);
		stats = (stats + "\nPWC/TOTAL: " + String.format("%,.2f",100 * rate) + "%");
		
		stats = (stats + "\n\nCYCLE TIME -----------------------------------");
		stats = (stats + "\nLAST Cycle Time: " + String.format("%,.2f",
				items[item].getLastCT() / 1000));
		stats = (stats + "\nAVG Cycle Time: " + String.format("%,.2f",
				items[item].getAvgCT() / 1000));
		
		stats = (stats + "\n---------------------------------------------------");
		logmsg(stats);
	}
	
	public void printStatsBad(int itemAmount) {
		String stats = new String("STATISTICS FOR BAD " + cycleName + 
				"S & " + itemName + "S ----");
		stats = stats + "\n" + cycleName + "S: " + 
								cycle.getBad() + "/" + cycle.getTotal() + " bad";
		for(int i = 1; i <= itemAmount; i++) 
			if(items[i].getBad() > 0) 
				stats = stats + "\n" + itemName + " " + i + ": " + 
								items[i].getBad() + "/" + items[i].getTotal() + " bad";
		stats = (stats + "\n--------------------------------------------------------------");
		logmsg(stats);
	}
}