package application.Cambrian;

import static EluxAPI.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyEvent;

public class CambrianApp extends RoboticsAPIApplication {
	
	private static final String OEE_OBJ_FILENAME = "OEE_Object.txt";
	private static final String OEE_STATS_FILENAME = "OEE_Stats.csv";
	private static final String OEE_EVENTS_FILENAME = "OEE_Event_Log.csv";
	private static final String SP_PATHROOT = "/_Cambrian/ScanPoints/";
	private static final double APPR_SPEED = 0.4;
	private static final double EXIT_SPEED = 0.4;
	private static final int JOINT_BUBBLE = 20; // mm to consider already visited
	private static final int RANDOM_RANGE = 20;
	private static final int JOINT_TRIALS = 10;
	private static final int APPROACH_DIST = 50;	// In mm
	private static final int EXIT_DIST = 100;		// In mm
	private static final int TOTAL_JOINTS = 10;
	
	@Inject	@Named("Cambrian") 	private Tool 	GripperCambrian;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_MF	mf = elux.getMF();
	@Inject private xAPI_Pad pad = elux.getPad();
	@Inject private xAPI_OEE oee = elux.getOEE();
	@Inject private xAPI_Move move = elux.getMove();
	@Inject private xAPI_Cambrian cambrian = new xAPI_Cambrian(elux);
	
	FrameList frameList;
	boolean logger, sleep, onlyApproach;
	int sniffing_pause;
	int loop_joint;
	int success;
	String cambrianModel;
	Transformation offset;
	
	@Override public void initialize() {
		configPadKeys();
		
		oee.init("JOINT", TOTAL_JOINTS, JOINT_TRIALS, 
				OEE_OBJ_FILENAME, 
				OEE_STATS_FILENAME,
				OEE_EVENTS_FILENAME);
		//oee = oee.restoreOEEfromFile(true); // DISABLE FIRST TIME
		
		// Init move
		move.setHome("/_Cambrian/_Home");
		move.setTool(GripperCambrian);
		move.setTCP("/TCP");
		move.setGlobalSpeed(0.25, true);
		move.setBlending(20, 5);
		move.setJTconds(5.0);
		move.setReleaseAuto(true);

		// Init Cambrian
		cambrianModel = new String("Elux_weldedpipes");
		if(!cambrian.init("192.168.2.50", 4000)) dispose();
		cambrian.loadModel(cambrianModel);
		cambrian.getNewPrediction(cambrianModel);
		cambrian.setApproachDist(APPROACH_DIST);
		cambrian.setDepthOffset(0);
		
		sniffing_pause = 500;
		loop_joint = 0;
		logger = false;
		sleep = false;
		onlyApproach = true;
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
		int target;
		while (true) {
			if (loop_joint == 0 && logger) padLog("Start testing sequence");
			for(int i = 1; i <= TOTAL_JOINTS; i++) {
				if (loop_joint == 0) target = i;
				else {
					target = loop_joint;
					i = TOTAL_JOINTS;
				}
				if (target == 9) { target = TOTAL_JOINTS; break;} // Ignore last joints
				int trial_counter = JOINT_TRIALS;
				String path = SP_PATHROOT + "P" + target;
				Frame SP_frame = move.toFrame(path);
				while (true) {
					if (trial_counter == 0) {
						padLog("Skip joint #" + target + ", too many trials.");
						mf.blinkRGB("R", 500);
						oee.addTMI(target);
						break;
					}
					success = move.PTP(SP_frame, 1, false);
					oee.event(target, success);
					if(cambrian.getNewPrediction(cambrianModel) && 
							!targetVisited(cambrian.getTargetFrame())) {
						
						// PREPARE CAMBRIAN AND DIGEST ANSWER --------------------------
						Frame targetFrame = cambrian.getTargetFrame();
						Frame approachFrame = cambrian.getApproachFrame();
						/*
						if (target == 8) {
							cambrianModel = "Elux_e";
							cambrian.loadModel(cambrianModel); 
						}
						if (target == 9){
							offset = Transformation.ofDeg(-20, 0, 0, 0, 0, 0);
							targetFrame.transform(offset);
						}
						if (target == 10) {
							cambrianModel = "Elux_weldedpipes_6";
							cambrian.loadModel(cambrianModel);
							offset = Transformation.ofDeg(0, 0, 0, -90, -10, 0);
							targetFrame.transform(offset);
						}
						*/
						// VISIT JOINT -------------------------------------------------
						success = move.PTP(approachFrame, 1, !onlyApproach);
						oee.event(target, success);
						if(!onlyApproach) success = move.LIN(targetFrame, APPR_SPEED, false);
						oee.event(target, success);
						sniff(target); // SNIFFING PROCESS ----------------------------
						if(!onlyApproach) success = move.LINREL(0, 0, -EXIT_DIST  * 
								(target == 10 ? 2 : 1), EXIT_SPEED, true);
						else success = move.LINREL(0, 0, -(EXIT_DIST - APPROACH_DIST) * 
								(target == 10 ? 2 : 1), EXIT_SPEED, true);
						oee.event(target, success);
						frameList.add(cambrian.getTargetFrame().copy());
						if (trial_counter == JOINT_TRIALS) {
							oee.addRFT(target);
						} else oee.addRNFT(target);
						break;
					}
					else {
						if (logger) padLog("Joint #" + target + " not found, randomizing");
						if (trial_counter == JOINT_TRIALS) oee.addNRFT(target);
						SP_frame = move.randomizeFrame(path, RANDOM_RANGE);
						trial_counter--;
						oee.addINR(target);
					}
					if(sleep) {
						move.LIN("/_Cambrian/_Home", 1, true);
						padLog("Robot is in sleep mode, toggle in key button " +
								"before deselecting program");
					}
					while(sleep);
				}
			}
			frameList.free();
			if(loop_joint == 0) {
				move.PTPhome(1, true);
				oee.addNewCycle();
				oee.saveOEEtoFile(oee, false);
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
									"Restore OEE data", "Reset OEE to 0", "Reset Cycle Time",
									"Print Cycles data", "Print Joint Sum", "Print 1 joint")) {
								case 0: break;
								case 1: oee.saveOEEtoFile(oee, true); break;
								case 2: oee = oee.restoreOEEfromFile(true); break;
								case 3: oee.resetCycle(); oee.resetItems(); break;
								case 4: oee.resetCycleTime(); break;
								case 5: oee.printStatsCycle(); break;
								case 6: oee.printStatsItem(0); break;
								case 7: oee.printStatsItem(pad.question("Which joint do you want " +
										"to view?","1","2","3","4","5","6","7","8") + 1); break;
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
						case 2:  						// KEY - SLEEP
							sleep = !sleep;
							if(sleep) padLog("Robot will pause before next test");
							break;
						case 3:							// KEY - SPEED
							double speed = move.getGlobalSpeed();
							double newSpeed = pad.askSpeed();
							if (newSpeed != speed) {
								move.setGlobalSpeed(newSpeed, true);
								oee.resetCycleTime();
							}
							else padLog("Speed didn't change, still " + 
									String.format("%,.0f", speed * 100) + "%");
							break;
					}
				}
			}
		};
		pad.keyBarSetup(padKeysListener1, "SNIFFER 1", "OEE", "Joint loop", "Sleep toggle", "Speed");
		
		IUserKeyListener padKeysListener2 = new IUserKeyListener() {
			@Override public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (event == UserKeyEvent.KeyDown) {
					switch (key.getSlot()) {
						case 0:  						// KEY - LOG
							logger = !logger;
							padLog("Logger switched " + logger);
							break;
						case 1: 						// KEY - SNIFF PAUSE
							if(pad.question("Sniffing Pause", "True - 3s", "Test - 0.5s") == 0)
								sniffing_pause = 3000;
							else sniffing_pause = 500;
							break;
						case 2:  						// KEY - APPROACH
							onlyApproach = !onlyApproach;
							padLog("Robot will just reach the approach point " +
									"to avoid crash: " + onlyApproach);
							break;
						case 3:							// KEY - 
							break;
					}
				}
			}
		};
		pad.keyBarSetup(padKeysListener2, "SNIFFER 2", "Log", "Sniff pause", "Approach", " ");
	}
	
	private void stop() {
		padLog("Program stopped");
		waitMillis(2000);
		//dispose();
	}
}
