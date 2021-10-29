package EluxAPI;

import java.io.Serializable;

// Item/cycle metrics ------------------
// TOTAL = GOOD + BAD
// GOOD = RFT + RNFT
// BAD = TMI + PWC
// TMI = NRFT - RNFT = Too Many Intents

// RFT = Right First Time
// NRFT = Not Right First Time
// RNFT = Right Not First Time
// PWC = Process/item With Collision

// Intent metrics ----------------------
// INR = INF + IUR + IWC = Intent Not Right
// INF = Intent Not Found
// IUR = Intent Un-Reachable
// IWC = Intent With Collision
	
public class xAPI_OEEitem implements Serializable {
	private static final long serialVersionUID = 2L;
	
	private int total;	// = GOOD + BAD
	private int good;	// = RFT + RNFT
	private int bad;	// = TMI + PWC
	private int TMI;	// = NRFT - RNFT = Too Many Intents
	
	private int RFT;	// Right First Time
	private int RNFT;	// Right Not First Time
	private int NRFT;	// Not Right First Time
	private int PWC;	// Process/item with collision
	
	private int INR;	// = INF + IUR + IWC + IAE + INV = Intent not right
	private int INF;	// Intent not found
	private int IUR;	// Intent Un-Reachable
	private int IWC;	// Intent with collision
	private int IAE;	// Intent already executed
	private int INV;	// Intent non valid
	
	boolean firstCycle;
	private double lastCT;
	private double avgCT;

	public xAPI_OEEitem() {
		this.reset();
	}
	
	// GETTER METHODS -------------------------------------
	public int getTotal() 	{ return total; }
	public int getGood() 	{ return good; }
	public int getBad() 	{ return bad; }
	public int getTMI() 	{ return TMI; }
	
	public int getRFT() 	{ return RFT; }
	public int getRNFT() 	{ return RNFT; }
	public int getNRFT() 	{ return NRFT; }
	public int getPWC() 	{ return PWC; }
	
	public int getINR() 	{ return INR; }
	public int getINF() 	{ return INF; }
	public int getIUR() 	{ return IUR; }
	public int getIWC() 	{ return IWC; }
	public int getIAE() 	{ return IAE; }
	public int getINV() 	{ return INV; }
	
	public double getLastCT() { return lastCT; }
	public double getAvgCT()  { return avgCT; }
	
	// SETTER METHODS -------------------------------------
	public void reset() {
		total = good = bad = TMI = 0;
		RFT = RNFT = NRFT = PWC = 0;
		INR = INF = IUR = IWC = IAE = INV = 0;
		
		firstCycle = true;
		lastCT = avgCT = 0.0;
	}
	
	public void addTotal() 	{ total++; }
	public void addGood() 	{ good++; }
	public void addBad() 	{ bad++; }
	public void addTMI() 	{ TMI++; }
	
	public void addRFT() 	{ RFT++;  good++; }
	public void addRNFT() 	{ RNFT++; good++; }
	public void addNRFT() 	{ NRFT++; }
	public void addPWC()	{ PWC++; }
	
	public void addINR() 	{ INR++; }
	public void addINF() 	{ INF++; }
	public void addIUR() 	{ IUR++; }
	public void addIWC() 	{ IWC++; }
	public void addIAE() 	{ IAE++; }
	public void addINV() 	{ INV++; }
	
	public void setFirstCycle() { firstCycle = true; }
	public void setLastCT(double _lastCT) { 
		lastCT = _lastCT;
		if (firstCycle) { avgCT = lastCT; firstCycle = false; }
		else avgCT = (avgCT * (total - 1) + lastCT) 
								/ (total + 0.0);
	}
}