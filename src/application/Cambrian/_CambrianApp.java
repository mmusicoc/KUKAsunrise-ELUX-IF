package application.Cambrian;

import static EluxUtils.Utils.*;
import static EluxUtils.UMath.*;
import EluxUtils.*;
import EluxAPI.*;
import EluxOEE.*;
import EluxRemote.*;

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
	private static final String REMOTE_FILENAME = "Remote.json";
	private static final String OEE_OBJ_FILENAME = "OEE_Object.txt";
	private static final String OEE_STATS_FILENAME = "OEE_Stats.csv";
	private static final String OEE_EVENTS_FILENAME = "OEE_Failure_Log.csv";
	private static final String PRECISION_FILENAME = "Cambrian_Precision_Log.csv";
	private static final String RECIPE_FILENAME = "CambrianRecipes.json";
	private static final String SP_PATHROOT = "/_Cambrian/F2scanPoints/";
	private static final String NJ_PATHROOT = "/_Cambrian/F2nominalJoints/";
	private static final double APPROACH_SPEED = 0.4;
	private static final int FILTER_DIST_IAE = 15; // mm to consider already visited
	private static final int FILTER_DIST_INV = 15; // mm to consider within filter
	private static final int FILTER_ANG_INV = 15;	// deg to consider valid
	private static final int JOINT_TRIALS = 10;
	private static final double RANDOM_DIST_MIN = 8.0;
	private static final double RANDOM_DIST_MAX = 16.0;
	private static final int APPROACH_DIST = 50;	// In mm
	private static final int EXIT_DIST = 50;		// In mm, in addition to approach dist
	private static final int TOTAL_JOINTS = 10;
	private static final int[] JOINT_SEQUENCE = {1,3,4,7};
	private static final int USED_JOINTS = JOINT_SEQUENCE.length;
	
	@Inject	@Named("Cambrian") 	private Tool tool;
			private xAPI__ELUX elux = new xAPI__ELUX();
			private xAPI_MF	mf = elux.getMF();
	@Inject private xAPI_Pad pad = elux.getPad();
	@Inject private xAPI_Move move = elux.getMove();
			private OEEmgr oee = elux.getOEE();
	@Inject private CambrianAPI cambrian = new CambrianAPI(elux);
			private CambrianRecipeMgr rcp = new CambrianRecipeMgr();
	
	private RemoteMgr remote = new RemoteMgr();
	private CSVLogger precLog = new CSVLogger();
	
	FrameList frameList;
	boolean logger;
	int approachMode;
	int sleep;
	int sniffing_pause;
	int loop_joint;
	int moveAns, unfinished;
	int target;
	String cambrianModel;
	Transformation offset;
	
	@Override public void initialize() {
		configPadKeys();
		remote.init(REMOTE_FILENAME);
		remote.fetchRemoteData();
		
		// INIT MOVE ---------------------------------------------
		move.init("/_Cambrian/_Home",			// Home path
					tool, "/TCP",				// Tool, TCP
					remote.getSpeed(), 1.0,		// Relative speed and acceleration
					20.0, 5.0,					// Blending
					5.0, true,					// Collision detection (Nm), response
					false);						// Logging
		move.setA7Speed(1); 					// Accelerate J7 if bottleneck

		// INIT CAMBRIAN -----------------------------------------
		cambrianModel = new String("Elux_weldedpipes");
		if(!cambrian.init("192.168.2.50", 4000)) dispose();
		cambrian.loadModel(cambrianModel, false);
		cambrian.getNewPrediction(cambrianModel);
		cambrian.setApproachDist(APPROACH_DIST);
		cambrian.setDepthOffset(0);
		
		// INIT OEE & PRECISION LOGGING --------------------------
		oee.init("FRIDGE", "JOINT",
				TOTAL_JOINTS, JOINT_TRIALS, 
				OEE_OBJ_FILENAME, 
				OEE_STATS_FILENAME,
				OEE_EVENTS_FILENAME, true); // DISABLE TRUE TO RESET OBJ
		
		precLog.init(PRECISION_FILENAME, true);
		precLog.header("JOINT,X,Y,Z,DIST,A,B,C\n");
		
		if(pad.question("Restart all OEE & precision data?", "YES", "NO") == 0)
			resetAllOEE();
		
		// INIT PROCESS ------------------------------------------
		sniffing_pause = 500;
		loop_joint = 0;
		logger = false;
		sleep = 0;
		approachMode = 0;
		if(!move.PTPhome(1, false)) stop();
	}
	
	private void sniff(int target) {
		if(logger) padLog("Leak test #" + target);
		mf.blinkRGB("B", sniffing_pause);
	}
	
	public void resetAllOEE() {
		oee.resetCycle();
		oee.resetItems();
		oee.saveOEEimage(false);
		precLog.reset();
		padLog("All OEE have been zeroed.");
	}

	public void logPrecision(int item, Frame offset) {
		precLog.open();
		//precLog.log(getDateAndTime(), false);
		precLog.log(item, false);
		precLog.log(relFrameToString(offset, true, true), true);
		precLog.eol();
		precLog.close(false);
	}
	
	public void selectModelAtEnd() {
		//if (target == 2) cambrian.loadModel(cambrianModel = "Elux_weldedpipes", false);
		//else if (target == 7) cambrian.loadModel(cambrianModel = "Elux_crimp6", false);
	}
	
	private boolean targetVisited(Frame target) {
		for(int i=0; i<frameList.size(); i++) {
			if (frameList.get(i).distanceTo(target) < FILTER_DIST_IAE) {
				if (logger) padLog("Distance to joint " + JOINT_SEQUENCE[i] + 
									"too small, already visited");
				return true;
			}
		}
		return false;
	}
	
	public boolean targetFilter(Frame offset) {
		if (offset.distanceTo(offset.getParent()) > FILTER_DIST_INV) {
			if(logger) padLog("Distance between detection and nominal is " + 
					d2s(offset.distanceTo(offset.getParent())));
			return false;
		} else if(r2d(offset.getAlphaRad()) > FILTER_ANG_INV) {
			if(logger) padLog("Alpha (A) between detection and nominal is " + 
					d2s(r2d(offset.getAlphaRad())) + "°");
			return false;
		} else if(r2d(offset.getBetaRad()) > FILTER_ANG_INV) {
			if(logger) padLog("Beta (B) between detection and nominal is " + 
					d2s(r2d(offset.getBetaRad())) + "°");
			return false;
		} else if(r2d(offset.getGammaRad()) > FILTER_ANG_INV) {
			if(logger) padLog("Gamma (C) between detection and nominal is " + 
					d2s(r2d(offset.getAlphaRad())) + "°");
			return false;
		} else return true;
	}

	@Override public void run() {
		frameList = new FrameList();
		while (true) {
			if (loop_joint == 0 && logger) padLog("Start testing sequence");
			oee.startCycle();
			for(int i = 1; i <= USED_JOINTS; i++) {
				if (loop_joint == 0) target = JOINT_SEQUENCE[i - 1];	// Reorder joints to combine same models
				else {
					target = loop_joint;
					i = USED_JOINTS;
				}
				
				int trial_counter = JOINT_TRIALS;
				String SP_path = SP_PATHROOT + "P" + target;
				String NJ_path = NJ_PATHROOT + "P" + target;
				Frame SP_frame = move.toFrame(SP_path);
				oee.startItem();
				do {
					unfinished = 0;
					oee.pause();
					if(sleep == 1) sleep();
					remote.checkIdle();
					oee.resume();
					
					if (trial_counter == 0) {	// TOO MANY INTENTS ------------------------
						if(logger) padLog("Skip joint #" + target + ", too many intents.");
						mf.blinkRGB("R", 1000);
						oee.addTMI(target);
						selectModelAtEnd();
						break;
					}
					moveAns = move.PTP(SP_frame, 1, false); // MOVE TO ScanPoint -----------
					unfinished += oee.checkMove(target, moveAns);
					if(cambrian.getNewPrediction(cambrianModel)) {
						Frame targetFrame = cambrian.getTargetFrame();
						
						// PRECISION TRACKING ----------------------------------------------
						Frame nominalJoint = move.toFrame(NJ_path);
						Frame relativeJoint = targetFrame.copyWithRedundancy(nominalJoint);
						logPrecision(target, relativeJoint);
						
						// TRANSFORM ANSWER ------------------------------------------------
						if(target == 4 || target == 7) {
							targetFrame.transform(Transformation.ofDeg(0,0,0,-90,0,0));
						}
						
						if(!targetVisited(targetFrame)) {
							if(targetFilter(relativeJoint)) {
								// VISIT JOINT -------------------------------------------------
								if(approachMode != 0) {
									Frame approachFrame = offsetFrame(targetFrame, 0,0,-APPROACH_DIST,0,0,0);
									moveAns = move.PTP(approachFrame, 1, (approachMode == 2));
									unfinished += oee.checkMove(target, moveAns);
									if(approachMode == 2) {
										moveAns = move.LIN(targetFrame, APPROACH_SPEED, false);
										unfinished += oee.checkMove(target, moveAns);
									}
									sniff(target); // SNIFFING PROCESS -------------------------
									if(approachMode == 2) {
										moveAns = move.LIN(approachFrame, APPROACH_SPEED, true);
										unfinished += oee.checkMove(target, moveAns);
									}
									moveAns = move.LINREL(0, 0, -EXIT_DIST * ((target == 
											JOINT_SEQUENCE[USED_JOINTS - 1]) ? 2 : 1), 1, true);
									unfinished += oee.checkMove(target, moveAns);
								}
								// CHECK SUCCESS -----------------------------------------------
								if(unfinished == 0) {
									frameList.add(targetFrame);
									selectModelAtEnd();
									break;
								} else {
									if(logger) padLog("Joint #" + target + " unsuccessful, randomizing");
									SP_frame = randomizeFrame(move.toFrame(SP_path), RANDOM_DIST_MIN, RANDOM_DIST_MAX);
									trial_counter--;
								}
							} else {
								if(logger) padLog("Detection not valid(filtered), randomizing");
								SP_frame = randomizeFrame(move.toFrame(SP_path), RANDOM_DIST_MIN, RANDOM_DIST_MAX);
								trial_counter--;
								unfinished++;
								oee.addINV(target);
								mf.blinkRGB("RB", 50);
								
							}
						} else {
							if(logger) padLog("Detection already visited, randomizing");
							SP_frame = randomizeFrame(move.toFrame(SP_path), RANDOM_DIST_MIN, RANDOM_DIST_MAX);
							trial_counter--;
							unfinished++;
							oee.addIAE(target);
							mf.blinkRGB("RB", 50);
						}
					} else {
						if(logger) padLog("Joint #" + target + " not found, randomizing");
						SP_frame = randomizeFrame(move.toFrame(SP_path), RANDOM_DIST_MIN, RANDOM_DIST_MAX);
						trial_counter--;
						unfinished++;
						oee.addINF(target);
						mf.blinkRGB("RB", 50);
					}
				} while (unfinished > 0);
				oee.endItem(target);
			}
			frameList.free();
			if(loop_joint == 0) {
				oee.endCycle();
				oee.saveOEEimage(false);
				//move.PTPhome(1, true);
				if(sleep == 2) sleep();
			}
		}
	}
	
	private void sleep() {
		move.PTPhome(1, true);
		padLog("Robot is now in sleep mode,\n" +
				" - To resume press again SLEEP key\n" +
				" - To deselect program, first go to T1, then resume");
		do {
			waitMillis(1000);
		} while(sleep != 0);
	}
	
	private void stop() {
		padErr("Program stopped");
		waitMillis(2000);
	}
	
	private void configPadKeys() { // BUTTONS --------------------------------------------------------
		IUserKeyListener padKeysListener1 = new IUserKeyListener() {
			@Override public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (event == UserKeyEvent.KeyDown) {
					switch (key.getSlot()) {
						case 0:  						// KEY - OEE DATA
							switch (pad.question("Manage / print OEE data", "CANCEL", "Save OEE data",
									"Restore OEE data", "Reset OEE to 0", "Reset CT",
									"Print Fridges", "Print Joint Sum", "Print 1 joint", "Print ALL")) {
								case 0: break;
								case 1: oee.saveOEEimage(true); break;
								case 2: oee.restoreOEEimage(true); break;
								case 3: resetAllOEE();
										break;
								case 4: oee.resetCycleTime();
										oee.saveOEEimage(true);
										break;
								case 5: oee.printStatsCycle(); break;
								case 6: oee.printStatsItem(0); break;
								case 7: oee.printStatsItem(pad.question("Which joint do you want to view?",
										"1","2","3","4","5","6","7","8", "9", "10") + 1); break;
								case 8: oee.printStatsCycle();
										for(int i = 0; i <= 10; i++) oee.printStatsItem(i); break;
							}
							break;
						case 1:							// KEY - RECORD INP
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
						case 2: 						// KEY - SLEEP
							if(sleep == 0) switch(pad.question("Sleep before next...",
									"CANC", "Joint", "Fridge cycle")) {
								case 0: break;
								case 1: padLog("Robot will pause before next joint.");
										sleep = 1; break;
								case 2: padLog("Robot will pause before next fridge cycle.");
										sleep = 2; break;
							} else {
								padLog("Robot will resume operations.");
								sleep = 0;
							}
							break;
						case 3:  						// KEY - OTHER
							switch(pad.question("Select option", "CANC", "Choose joint",
												"Speed", "Sniff pause","Approach mode", 
												(logger?"Disable":"Enable") + " Logger")) {
								case 0: break;
								case 1: 
									int prev_loop = loop_joint;
									loop_joint = pad.question("Which joint do you want to test?",
											"Loop ALL","1","2","3","4","5","6","7","8","9","10","11");
									if (loop_joint == 0 && prev_loop != 0) {
										padLog("Looping all from now");
										oee.startCycle();
									}
									else padLog("Looping joint #" + loop_joint);
									break;
								case 2:
									double speed = move.getGlobalSpeed();
									double newSpeed = pad.askSpeed();
									if (newSpeed != speed) {
										move.setGlobalSpeed(newSpeed, true);
										oee.resetCycleTime();
										remote.setSpeed(newSpeed);
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
									approachMode = pad.question("Select operation mode",
											"Just scan", "Scan + Approach", "Scan + approach + test");
									break;
								case 5:
									logger = !logger;
									padLog(logger ? "Logger on" : "Logger off");
									break;
							}
							break;
					}
				}
			}
		};
		pad.keyBarSetup(padKeysListener1, "SNIFFER", "OEE", "INP", "SLEEP", "OTHER");
	}
}
