package EluxOEE;

import EluxUtils.CSVLogger;

public class OEEstatsCSV {
	private String itemName;
	private char itemInitial;
	private int itemAmount;
	
	private OEEdata d;
	private CSVLogger csv;
	
	public OEEstatsCSV() { } // CONSTRUCTOR ------------------------
	
	public void init(String _itemName, int _itemAmount, 
						OEEdata _d,
						String _oee_stats_filename) {
		this.itemName = _itemName;
		this.itemInitial = Character.toUpperCase(itemName.charAt(0));
		this.itemAmount = _itemAmount;
		this.d = _d;
		
		this.csv = new CSVLogger();
		this.csv.init(_oee_stats_filename, false, ';');
	}
	
	// PRINT STATS TO SUMMARY CSV ------------------------------------------
	
	public void saveOEEstats(boolean log) {
		csv.open();
		
		// PRINT HEADER -----------------------------------------
		csv.log("METRIC;CYCLE;" + itemInitial + " TOT", false);
		for(int i = 1; i <= itemAmount; i++)
			csv.log(String.valueOf(itemInitial) + i, true);
		csv.eol();
		
		// PRINT METRICS ---------------------------------------
		csv.log("TOTAL;" + d.cycle.getTotal(), false);
		for(int i = 0; i <= itemAmount; i++) 
			csv.log(d.items[i].getTotal(), true);
		csv.eol();
		
		csv.log("GOOD;" + d.cycle.getGood(), false);
		for(int i = 0; i <= itemAmount; i++) 
			csv.log(d.items[i].getGood(), true);
		csv.eol();
		
		csv.log("RFT;" + d.cycle.getRFT(), false);
		for(int i = 0; i <= itemAmount; i++) 
			csv.log(d.items[i].getRFT(), true);
		csv.eol();
		
		csv.log("RNFT;" + d.cycle.getRNFT(), false);
		for(int i = 0; i <= itemAmount; i++) 
			csv.log(d.items[i].getRNFT(), true);
		csv.eol();
		
		csv.log("BAD;" + d.cycle.getBad(), false);
		for(int i = 0; i <= itemAmount; i++) 
			csv.log(d.items[i].getBad(), true);
		csv.eol();
		
		csv.log("TMI;" + d.cycle.getTMI(), false);
		for(int i = 0; i <= itemAmount; i++) 
			csv.log(d.items[i].getTMI(), true);
		csv.eol();
		
		csv.log("PWC;" + d.cycle.getPWC(), false);
		for(int i = 0; i <= itemAmount; i++) 
			csv.log(d.items[i].getPWC(), true);
		csv.eol();
		
		csv.log("NRFT;" + d.cycle.getNRFT(), false);
		for(int i = 0; i <= itemAmount; i++) 
			csv.log(d.items[i].getNRFT(), true);
		csv.eol();
		
		// ----------------- INTENTS
		csv.log("INR;" + d.cycle.getINR(), false);
		for(int i = 0; i <= itemAmount; i++) 
			csv.log(d.items[i].getINR(), true);
		csv.eol();
		
		csv.log("IWC;" + d.cycle.getIWC(), false);
		for(int i = 0; i <= itemAmount; i++) 
			csv.log(d.items[i].getIWC(), true);
		csv.eol();
		
		csv.log("IUR;" + d.cycle.getIUR(), false);
		for(int i = 0; i <= itemAmount; i++) 
			csv.log(d.items[i].getIUR(), true);
		csv.eol();
		
		csv.log("IAE;" + d.cycle.getIAE(), false);
		for(int i = 0; i <= itemAmount; i++) 
			csv.log(d.items[i].getIAE(), true);
		csv.eol();
	
		csv.log("INV;" + d.cycle.getINV(), false);
		for(int i = 0; i <= itemAmount; i++) 
			csv.log(d.items[i].getINV(), true);
		csv.eol();
		
		csv.log("INF;" + d.cycle.getINF(), false);
		for(int i = 0; i <= itemAmount; i++) 
			csv.log(d.items[i].getINF(), true);
		csv.eol();
		
		csv.log("INP;" + d.cycle.getINP(), false);
		for(int i = 0; i <= itemAmount; i++) 
			csv.log(d.items[i].getINP(), true);
		csv.eol();
		
		// -------------- CYCLE TIME
		csv.log("CT", false);
		csv.log(d.cycle.getAvgCT() / 1000.0, true);
		for(int i = 0; i <= itemAmount; i++)
			csv.log(d.items[i].getAvgCT() / 1000.0, true);
		csv.eol();
		
		csv.close(log);
	}
}