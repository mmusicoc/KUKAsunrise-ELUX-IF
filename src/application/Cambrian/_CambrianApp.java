package application.Cambrian;

import static EluxAPI.Utils.*;
import EluxAPI.*;
import EluxOEE.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyEvent;

public class _CambrianApp extends RoboticsAPIApplication {
	private static final String OEE_OBJ_FILENAME = "OEE_Object.txt";
	private static final String OEE_STATS_FILENAME = "OEE_Stats.csv";
	private static final String OEE_EVENTS_FILENAME = "OEE_Event_Log.csv";
	private static final String SP_PATHROOT = "/_Cambrian/ScanPoints3/";
	private static final double APPR_SPEED = 0.4;
	private static final int JOINT_BUBBLE = 20; // mm to consider already visited
	private static final int JOINT_TRIALS = 10;
	private static final double RANDOM_MIN = 8.0;
	private static final double RANDOM_MAX = 16.0;
	private static final int APPROACH_DIST = 50;	// In mm
	private static final int EXIT_DIST = 50;		// In mm, in addition to approach dist
	private static final int TOTAL_JOINTS = 10;
	
	@Inject	@Named("Cambrian") 	private Tool 	GripperCambrian;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_MF	mf = elux.getMF();
	@Inject private xAPI_Pad pad = elux.getPad();
	@Inject private xAPI_Move move = elux.getMove();
	@Inject private xOEE oee = elux.getOEE();
	@Inject private CambrianAPI cambrian = new CambrianAPI(elux);
	//@Inject private CambrianRecipeMgr joints = new CambrianRecipeMgr();
	
	FrameList frameList;
	boolean logger, sleep, onlyApproach;
	int sniffing_pause;
	int loop_joint;
	int moveAns, unfinished;
	int target;
	String cambrianModel;
	Transformation offset;
	
	@Override public void initialize() {
		configPadKeys();
		
		oee.init("FRIDGE", "JOINT",
				TOTAL_JOINTS, JOINT_TRIALS, 
				OEE_OBJ_FILENAME, 
				OEE_STATS_FILENAME,
				OEE_EVENTS_FILENAME);
		oee = oee.restoreOEEimage(true); // DISABLE FIRST TIME
		
		// INIT MOVE ---------------------------------------------
		move.setHome("/_Cambrian/_Home");
		move.setTool(GripperCambrian);
		move.setTCP("/TCP");
		move.setGlobalSpeed(0.25, true);
		move.setBlending(20, 5);
		move.setJTconds(5.0);
		move.setReleaseAuto(true);

		// INIT CAMBRIAN -----------------------------------------
		cambrianModel = new String("Elux_weldedpipes");
		if(!cambrian.init("192.168.2.50", 4000)) dispose();
		cambrian.loadModel(cambrianModel);
		cambrian.getNewPrediction(cambrianModel);
		cambrian.setApproachDist(APPROACH_DIST);
		cambrian.setDepthOffset(0);
		
		// INIT PROCESS ------------------------------------------
		sniffing_pause = 500;
		loop_joint = 0;
		logger = false;
		sleep = false;
		onlyApproach = false;
		if(!move.PTPhome(1, false)) stop();
	}
	
	private boolean targetVisited(Frame target) {
		for(int i=0; i<frameList.size(); i++) {
			if (frameList.get(i).distanceTo(target) < JOINT_BUBBLE) return true;
		}
		return false;
	}
	
	private void sniff(int target) {
		if(logger) padLog("Leak test #" + target);
		mf.blinkRGB("B", sniffing_pause);
	}

	@Override public void run() {
		frameList = new FrameList();
		while (true) {
			if (loop_joint == 0 && logger) padLog("Start testing sequence");
			oee.startCycle();
			for(int i = 1; i <= TOTAL_JOINTS; i++) {
				if (loop_joint == 0) target = i;
				else {
					target = loop_joint;
					i = TOTAL_JOINTS;
				}
				if (target == 4) { i = target = 5; } // Ignore last joints
				if (target == 7) { i = target = 8; }
				//if (target == 10) { break; }
				int trial_counter = JOINT_TRIALS;
				String path = SP_PATHROOT + "P" + target;
				Frame SP_frame = move.toFrame(path);
				oee.startItem();
				do {
					unfinished = 0;
					if(sleep) {	// HIBERNATE -----------------------------------------------
						move.LIN("/_Cambrian/_Home", 1, true);
						padLog("Robot is in sleep mode, toggle in key button " +
								"before deselecting program");
					}
					while(sleep);
					
					if (trial_counter == 0) {	// TOO MANY INTENTS ------------------------
						padLog("Skip joint #" + target + ", too many trials.");
						mf.blinkRGB("R", 1000);
						oee.addTMI(target);
						break;
					}
					moveAns = move.PTP(SP_frame, 1, false); // MOVE TO ScanPoint -----------
					unfinished += oee.checkMove(target, moveAns);
					if(cambrian.getNewPrediction(cambrianModel)) {
						if(!targetVisited(cambrian.getTargetFrame())) {
							// PREPARE CAMBRIAN AND DIGEST ANSWER --------------------------
							Frame targetFrame = cambrian.getTargetFrame();
							//Frame approachFrame = cambrian.getApproachFrame();
							Frame approachFrame = offsetFrame(targetFrame, 0,0,-APPROACH_DIST,0,0,0);
							// VISIT JOINT -------------------------------------------------
							moveAns = move.PTP(approachFrame, 1, !onlyApproach);
							unfinished += oee.checkMove(target, moveAns);
							if(!onlyApproach) moveAns = move.LIN(targetFrame, APPR_SPEED, false);
							unfinished += oee.checkMove(target, moveAns);
							sniff(target); // SNIFFING PROCESS -----------------------------
							if(!onlyApproach) {
								moveAns = move.LIN(approachFrame, APPR_SPEED, true);
								unfinished += oee.checkMove(target, moveAns);
							}
							moveAns = move.LINREL(0, 0, -EXIT_DIST * (target == 10 ? 2 : 1), 1, true);
							unfinished += oee.checkMove(target, moveAns);
							// CHECK SUCCESS -----------------------------------------------
							if(unfinished == 0) {
								frameList.add(targetFrame);
								break;
							} else {
								if(logger) padLog("Joint #" + target + " unsuccessful, randomizing");
								SP_frame = randomizeFrame(move.toFrame(path), RANDOM_MIN, RANDOM_MAX);
								trial_counter--;
							}
						} else {
							if(logger) padLog("Detection already visited, randomizing");
							SP_frame = randomizeFrame(move.toFrame(path), RANDOM_MIN, RANDOM_MAX);
							trial_counter--;
							oee.addIAE(target);
							unfinished++;
						}
					} else {
						if(logger) padLog("Joint #" + target + " not found, randomizing");
						SP_frame = randomizeFrame(move.toFrame(path), RANDOM_MIN, RANDOM_MAX);
						trial_counter--;
						oee.addINF(target);
						unfinished++;
					}
				} while (unfinished > 0);
				oee.endItem(target);
			}
			frameList.free();
			if(loop_joint == 0) {
				move.PTPhome(1, true);
				oee.endCycle();
				oee.saveOEEimage(false);
			}
		}
	}
	
	private void configPadKeys() { // BUTTONS --------------------------------------------------------
		IUserKeyListener padKeysListener1 = new IUserKeyListener() {
			@Override public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (event == UserKeyEvent.KeyDown) {
					switch (key.getSlot()) {
						case 0:  						// KEY - OEE DATA
							switch (pad.question("Manage / print OEE data", "CANCEL", "Save OEE data",
									"Restore OEE data", "Reset OEE to 0", "Reset CT",
									"Print Fridges", "Print Joint Sum", "Print 1 joint", "Print ALL joints")) {
								case 0: break;
								case 1: oee.saveOEEimage(true); break;
								case 2: oee = oee.restoreOEEimage(true); break;
								case 3: oee.resetCycle(); padLog("Cycle stats have been zeroed.");
										oee.resetItems(); padLog("Items stats have been zeroed.");
								case 4: oee.resetCycleTime();
										oee.saveOEEimage(true);
										break;
								case 5: oee.printStatsCycle(); break;
								case 6: oee.printStatsItem(0); break;
								case 7: oee.printStatsItem(pad.question("Which joint do you want to view?",
										"1","2","3","4","5","6","7","8", "9", "10") + 1); break;
								case 8: for(int i = 0; i <= 10; i++) oee.printStatsItem(i);
							}
							break;
						case 1: 						// KEY - JOINT
							int prev_loop = loop_joint;
							loop_joint = pad.question("Which joint do you want to test?",
									"Loop all","1","2","3","4","5","6","7","8");
							if (loop_joint == 0 && prev_loop != 0) {
								padLog("Looping all from now");
								oee.startCycle();
							}
							else padLog("Looping joint #" + loop_joint);
							break;
						case 2:							// KEY - SPEED
							switch(pad.question("Where do you want to record a precision failure?",
									"CANC", "This joint", "Previous Joint")) {
								case 0: break;
								case 1:
									oee.addINP(target);
									unfinished++;
									padLog("Intent Not Precise Recorded for joint " + target);
									break;
								case 2:
									oee.addINP(target - 1);
									unfinished++;
									padLog("Intent Not Precise Recorded for joint " + (target - 1));
									break;
							} break;
						case 3:  						// KEY - OTHER
							switch(pad.question("Select option", "CANC", "Toggle Sleep flag",
												"Speed", "Sniff pause",
												(onlyApproach?"Approach joint":"Go into joint"), 
												(logger?"Disable":"Enable" + " Logger"))) {
								case 0: break;
								case 1: 
									sleep = !sleep;
									if(sleep) padLog("Robot will pause before next test.");
									else padLog("Robot will not pause.");
									break;
								case 2:
									double speed = move.getGlobalSpeed();
									double newSpeed = pad.askSpeed();
									if (newSpeed != speed) {
										move.setGlobalSpeed(newSpeed, true);
										oee.resetCycleTime();
									}
									else padLog("Speed didn't change, still " + 
											String.format("%,.0f", speed * 100) + "%");
									break;
								case 3:
									if(pad.question("Sniffing Pause", "True - 3s", "Test - 0.5s") == 0)
										sniffing_pause = 3000;
									else sniffing_pause = 500;
									padLog("Sniffing pause is now " + sniffing_pause + "ms.");
									break;
								case 4:
									onlyApproach = !onlyApproach;
									padLog((onlyApproach?"Robot will just reach the approach point.":
										"Robot will go into joints for sniffing."));
									break;
								case 5:
									logger = !logger;
									padLog("Logger switched " + logger);
									break;
							}
					}
				}
			}
		};
		pad.keyBarSetup(padKeysListener1, "SNIFFER", "OEE", "Joint loop", "INP", "Other");
	}
	
	private void stop() {
		padLog("Program stopped");
		waitMillis(2000);
		//dispose();
	}
}
