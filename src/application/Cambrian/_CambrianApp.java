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
	private static final String PRECISION_FILENAME = "Cambrian_Precision_Log.csv";
	private static final String SP_PATHROOT = "/_Cambrian/ScanPoints3/";
	private static final String NJ_PATHROOT = "/_Cambrian/NominalJoints3/";
	private static final double APPR_SPEED = 0.4;
	private static final int EXCL_BUBBLE = 15; // mm to consider already visited
	private static final int INCL_BUBBLE = 15; // mm to consider within filter
	private static final int JOINT_TRIALS = 10;
	private static final double RANDOM_MIN = 8.0;
	private static final double RANDOM_MAX = 16.0;
	private static final int APPROACH_DIST = 50;	// In mm
	private static final int EXIT_DIST = 50;		// In mm, in addition to approach dist
	private static final int TOTAL_JOINTS = 10;
	private static final int[] JOINT_SEQUENCE = {1,2,3,5,6,8,9,10}; //,4,7
	private static final int USED_JOINTS = JOINT_SEQUENCE.length;
	
	@Inject	@Named("Cambrian") 	private Tool 	GripperCambrian;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_MF	mf = elux.getMF();
	@Inject private xAPI_Pad pad = elux.getPad();
	@Inject private xAPI_Move move = elux.getMove();
	@Inject private xOEE oee = elux.getOEE();
	@Inject private CambrianAPI cambrian = new CambrianAPI(elux);
	//@Inject private CambrianRecipeMgr joints = new CambrianRecipeMgr();
	
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
		
		oee.init("FRIDGE", "JOINT",
				TOTAL_JOINTS, JOINT_TRIALS, 
				OEE_OBJ_FILENAME, 
				OEE_STATS_FILENAME,
				OEE_EVENTS_FILENAME, true);
		//oee.restoreOEEimage(true); // DISABLE FIRST TIME
		
		precLog.init(PRECISION_FILENAME, true);
		precLog.header("JOINT,X,Y,Z,DIST,A,B,C\n");
		
		// INIT MOVE ---------------------------------------------
		move.setHome("/_Cambrian/_Home");
		move.setTool(GripperCambrian);
		move.setTCP("/TCP");
		move.setGlobalSpeed(0.25, true);
		move.setA7Speed(1); // Accelerate J7 if bottleneck
		move.setBlending(20, 5);
		move.setJTconds(5.0);
		move.setReleaseAuto(true);
		move.log(false);

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
		sleep = 0;
		approachMode = 0;
		if(!move.PTPhome(1, false)) stop();
	}
	
	private void sniff(int target) {
		if(logger) padLog("Leak test #" + target);
		mf.blinkRGB("B", sniffing_pause);
	}
	

	public void logPrecision(int item, Frame offset) {
		precLog.open();
		//precLog.log(getDateAndTime(), false);
		precLog.log(item, false);
		precLog.log(relFrameToString(offset, true, true), true);
		precLog.eol();
		precLog.close(false);
	}
	
	private boolean targetVisited(Frame target) {
		for(int i=0; i<frameList.size(); i++) {
			if (frameList.get(i).distanceTo(target) < EXCL_BUBBLE) {
				if (logger) padLog("Distance to joint " + JOINT_SEQUENCE[i] + 
									"too small, already visited");
				return true;
			}
		}
		return false;
	}
	
	public boolean targetFilter(Frame nominal, Frame target) {
		if (target.distanceTo(nominal) < INCL_BUBBLE) return true;
		else {
			if(logger) padLog("Distance between detection and nominal is " + 
								target.distanceTo(nominal));
			return false;
		}
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
				//if (target == 4) { i = target = 5; } // Ignore last joints
				//if (target == 7) { i = target = 8; }
				//if (target == 10) { break; }
				
				/*if (target == 1) {
					cambrianModel = "Elux_weldedpipes";
					cambrian.loadModel(cambrianModel);
				}
				
				if (target == 4) {
					cambrianModel = "Elux_crimp6";
					cambrian.loadModel(cambrianModel);
				}
				*/
				
				int trial_counter = JOINT_TRIALS;
				String SP_path = SP_PATHROOT + "P" + target;
				String NJ_path = NJ_PATHROOT + "P" + target;
				Frame SP_frame = move.toFrame(SP_path);
				oee.startItem();
				do {
					unfinished = 0;
					if(sleep == 1) sleep();
					
					if (trial_counter == 0) {	// TOO MANY INTENTS ------------------------
						if(logger) padLog("Skip joint #" + target + ", too many intents.");
						mf.blinkRGB("R", 1000);
						oee.addTMI(target);
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
							if(targetFilter(nominalJoint, targetFrame)) {
								// VISIT JOINT -------------------------------------------------
								if(approachMode != 0) {
									Frame approachFrame = offsetFrame(targetFrame, 0,0,-APPROACH_DIST,0,0,0);
									moveAns = move.PTP(approachFrame, 1, (approachMode == 2));
									unfinished += oee.checkMove(target, moveAns);
									if(approachMode == 2) {
										moveAns = move.LIN(targetFrame, APPR_SPEED, false);
										unfinished += oee.checkMove(target, moveAns);
									}
									sniff(target); // SNIFFING PROCESS -------------------------
									if(approachMode == 2) {
										moveAns = move.LIN(approachFrame, APPR_SPEED, true);
										unfinished += oee.checkMove(target, moveAns);
									}
									moveAns = move.LINREL(0, 0, -EXIT_DIST * ((target == 
											JOINT_SEQUENCE[USED_JOINTS - 1]) ? 2 : 1), 1, true);
									unfinished += oee.checkMove(target, moveAns);
								}
								// CHECK SUCCESS -----------------------------------------------
								if(unfinished == 0) {
									frameList.add(targetFrame);
									break;
								} else {
									if(logger) padLog("Joint #" + target + " unsuccessful, randomizing");
									SP_frame = randomizeFrame(move.toFrame(SP_path), RANDOM_MIN, RANDOM_MAX);
									trial_counter--;
								}
							} else {
								if(logger) padLog("Detection not valid(filtered), randomizing");
								SP_frame = randomizeFrame(move.toFrame(SP_path), RANDOM_MIN, RANDOM_MAX);
								trial_counter--;
								unfinished++;
								oee.addINV(target);
								mf.blinkRGB("RB", 50);
								
							}
						} else {
							if(logger) padLog("Detection already visited, randomizing");
							SP_frame = randomizeFrame(move.toFrame(SP_path), RANDOM_MIN, RANDOM_MAX);
							trial_counter--;
							unfinished++;
							oee.addIAE(target);
							mf.blinkRGB("RB", 50);
						}
					} else {
						if(logger) padLog("Joint #" + target + " not found, randomizing");
						SP_frame = randomizeFrame(move.toFrame(SP_path), RANDOM_MIN, RANDOM_MAX);
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
				move.PTPhome(1, true);
				if(sleep == 2) sleep();
			}
		}
	}
	
	private void sleep() {
		move.PTPhome(1, true);
		do {
			sleep = pad.question("Robot is in sleep mode, resume?\n" +
					"NOTE: To deselect program, first go to T1, then resume",
					"YES", "NO");
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
								case 3: oee.resetCycle(); padLog("Cycle stats have been zeroed.");
										oee.resetItems(); padLog("Items stats have been zeroed.");
										oee.saveOEEimage(true);
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
