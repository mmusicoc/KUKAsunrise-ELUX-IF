package application.Cambrian;

public class Params {
	String REMOTE_FILENAME;
	String OEE_OBJ_FILENAME;
	String OEE_STATS_FILENAME;
	String OEE_EVENTS_FILENAME;
	String PRECISION_FILENAME;
	String RECIPE_FILENAME;
	double APPROACH_SPEED;
	int MAX_TRIALS;
	int FILTER_DIST_IAE; 	// mm to consider already visited (+-)
	int FILTER_DIST_INV; 	// mm to consider within filter (+-)
	int FILTER_ANG_INV;		// deg to consider valid (+-)
	double RANDOM_DIST_MIN;	// In mm
	double RANDOM_DIST_MAX;	// In mm
	int APPROACH_DIST;		// In mm
	int EXIT_DIST;			// In mm, in addition to approach dist
	int TOTAL_JOINTS;
	
	public Params() { } // CONSTRUCTOR
}