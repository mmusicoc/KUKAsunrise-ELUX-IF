package EluxOEE;

import java.io.Serializable;
	
public class OEEitem implements Serializable {
	private static final long serialVersionUID = 3L;
	
	private int total;	// = GOOD + BAD
	private int good;	// = RFT + RNFT
	private int bad;	// = TMI + PWC
	private int TMI;	// = NRFT - RNFT = Too Many Intents
	private int PWC;	// Process/item with collision
	
	private int RFT;	// Right First Time
	private int RNFT;	// Right Not First Time
	private int NRFT;	// Not Right First Time
	
	// Intents Not Right	
	private int INR;	// = IWC + IUR + IAE + INV + INF + INP
	private int IWC;	// -1 - Intent With Collision
	private int IUR;	// -2 - Intent Un-Reachable (path planner error)
	private int IAE;	// -3 - Intent Already Executed (visited)
	private int INV;	// -4 - Intent Non Valid (filtered)
	private int INF;	// -5 - Intent Not Found
	private int INP;	// -6 - Intent Not Precise
	
	boolean firstCycle;
	private double lastCT;
	private double avgCT;

	public OEEitem() {
		this.reset();
	}
	
	// GETTER METHODS -------------------------------------
	public int getTotal()	{ return total; }
	public int getGood()	{ return good; }
	public int getBad() 	{ return bad; }
	public int getTMI() 	{ return TMI; }
	public int getPWC() 	{ return PWC; }
	
	public int getRFT() 	{ return RFT; }
	public int getRNFT() 	{ return RNFT; }
	public int getNRFT() 	{ return NRFT; }
	
	public int getINR() 	{ return INR; }
	public int getIWC() 	{ return IWC; }
	public int getIUR() 	{ return IUR; }
	public int getIAE() 	{ return IAE; }
	public int getINV() 	{ return INV; }
	public int getINF() 	{ return INF; }
	public int getINP()		{ return INP; }
	
	public double getLastCT() { return lastCT; }
	public double getAvgCT()  { return avgCT; }
	
	// SETTER METHODS -------------------------------------
	public void reset() {
		total = good = bad = TMI = PWC = 0;
		RFT = RNFT = NRFT = 0;
		INR = IWC = IUR = IAE = INV = INF = INP = 0;
		
		firstCycle = true;
		lastCT = avgCT = 0.0;
	}
	
	public void addTMI() 	{ TMI++; bad++; total++;}
	public void addPWC()	{ PWC++; bad++; total++;}
	
	public void addRFT() 	{ RFT++;  good++; total++;}
	public void addRNFT() 	{ RNFT++; good++; total++;}
	public void addNRFT() 	{ NRFT++; }
	
	public void addIWC() 	{ IWC++; INR++; }
	public void addIUR() 	{ IUR++; INR++; }
	public void addIAE() 	{ IAE++; INR++; }
	public void addINV() 	{ INV++; INR++; }
	public void addINF() 	{ INF++; INR++; }
	public void addINP()	{ INP++; INR++; }
	
	public void setFirstCycle() { firstCycle = true; }
	public void setLastCT(double _lastCT) { 
		lastCT = _lastCT;
		if (firstCycle) { avgCT = lastCT; firstCycle = false; }
		else avgCT = (avgCT * (total - 1) + lastCT) / (total + 0.0);
	}
}