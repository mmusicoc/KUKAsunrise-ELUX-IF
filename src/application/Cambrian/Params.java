package application.Cambrian;

public class Params {
	double APPROACH_SPEED;
	int FILTER_IAE_DIST; 	// mm to consider already visited (+-)
	boolean FILTER_INV_ENABLED;
	int FILTER_INV_DIST; 	// mm to consider within filter (+-)
	int FILTER_INV_ANG;		// deg to consider valid (+-)
	int MAX_TRIALS;
	double RANDOM_DIST_MIN;	// In mm
	double RANDOM_DIST_MAX;	// In mm
	int APPROACH_DIST;		// In mm
	int EXIT_DIST;			// In mm, in addition to approach dist
	int TOTAL_JOINTS;
	
	int approachMode;
	int sniffPause;
	boolean sandBoxMode;
	boolean scanBoltOnce;
	
	//boolean teachNominal[];
	
	public Params() { 
//		teachNominal = new boolean[12];
	} // CONSTRUCTOR
}