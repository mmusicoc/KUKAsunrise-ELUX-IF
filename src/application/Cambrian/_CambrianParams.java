package application.Cambrian;

public class _CambrianParams {
	static final String REMOTE_FILENAME = "Remote.json";
	static final String OEE_OBJ_FILENAME = "OEE_Object.txt";
	static final String OEE_STATS_FILENAME = "OEE_Stats.csv";
	static final String OEE_EVENTS_FILENAME = "OEE_Failure_Log.csv";
	static final String PRECISION_FILENAME = "Cambrian_Precision_Log.csv";
	static final String RECIPE_FILENAME = "CambrianRecipes.json";
	static final String SP_PATHROOT = "/_Cambrian/F2scanPoints/";
	static final String NJ_PATHROOT = "/_Cambrian/F2nominalJoints/";
	static final double APPROACH_SPEED = 0.4;
	static final int FILTER_DIST_IAE = 15; // mm to consider already visited
	static final int FILTER_DIST_INV = 15; // mm to consider within filter
	static final int FILTER_ANG_INV = 15;	// deg to consider valid
	static final int JOINT_TRIALS = 10;
	static final double RANDOM_DIST_MIN = 8.0;
	static final double RANDOM_DIST_MAX = 16.0;
	static final int APPROACH_DIST = 50;	// In mm
	static final int EXIT_DIST = 50;		// In mm, in addition to approach dist
	static final int TOTAL_JOINTS = 10;
	static final int[] JOINT_SEQUENCE = {1,3,4,7};
	static final int USED_JOINTS = JOINT_SEQUENCE.length;
}