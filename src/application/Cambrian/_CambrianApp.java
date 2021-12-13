package application.Cambrian;

import static EluxUtils.Utils.*;
import static EluxUtils.UMath.*;
import static application.Cambrian._CambrianParams.*;
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

public class _CambrianApp extends RoboticsAPIApplication {	
	@Inject	@Named("Cambrian") 	private Tool tool;
	xAPI__ELUX elux = new xAPI__ELUX();
	@Inject xAPI_MF	mf = elux.getMF();
	@Inject xAPI_Pad pad = elux.getPad();
	@Inject xAPI_Move move = elux.getMove();
	@Inject CambrianAPI cambrian = new CambrianAPI(elux);
	CambrianRecipeMgr rcp = new CambrianRecipeMgr();
	CambrianKeys keys = new CambrianKeys(this);
	OEEmgr oee = elux.getOEE();
	RemoteMgr remote = new RemoteMgr();
	CSVLogger precLog = new CSVLogger();
	
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
		keys.configPadKeys();
		remote.init(REMOTE_FILENAME);
		remote.fetchRemoteData();
		//initLoggerSocket("192.168.2.11", 4001);
		
		// INIT MOVE ---------------------------------------------
		move.init("/_Cambrian/_Home",			// Home path
					tool, "/TCP",				// Tool, TCP
					remote.getSpeed(), 1.0,		// Relative speed and acceleration
					20.0, 5.0,					// Blending
					5.0, true,					// Collision detection (Nm), response
					false);						// Logging
		move.setA7Speed(1); 					// Accelerate J7 if bottleneck
		
		// INIT MOVE ---------------------------------------------
		rcp.init(pad, RECIPE_FILENAME, false);
		rcp.fetchAllRecipes();
		rcp.selectRecipePNC("F2");
		rcp.selectJointID(rcp.getItem(0));

		// INIT CAMBRIAN -----------------------------------------
		//cambrianModel = new String("Elux_weldedpipes");
		if(!cambrian.init("192.168.2.50", 4000)) stop();
		cambrianModel = rcp.getModel();
		cambrian.loadModel(cambrianModel, false);
		//cambrian.getNewPrediction(cambrianModel);
		
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
		precLog.log(rf2s(offset, true, true), true);
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
			rcp.fetchAllRecipes();
			oee.startCycle();
			for(int i = 0; i < rcp.getItemsAmount(); i++) {
				oee.startItem();
				if (loop_joint == 0) {
					rcp.selectJointID(rcp.getItem(i));
					target = rcp.getJointID();
				} else {
					target = loop_joint;
					i = USED_JOINTS;
				}
				
				int trial_counter = JOINT_TRIALS;
				String SP_path = SP_PATHROOT + "P" + target;
				String NJ_path = NJ_PATHROOT + "P" + target;
				Frame SP_frame = move.toFrame(SP_path);
				do {
					unfinished = 0;
					oee.pause();
					if(sleep == 1) sleep();
					remote.checkIdle();
					setLogger(remote.getLogger());
					setSpeed(remote.getSpeed());
					move.setGlobalAccel(remote.getAccel());
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
						// TRANSFORM ANSWER ------------------------------------------------
						targetFrame.transform(rcp.getDC().invert());
						
						// PRECISION TRACKING ----------------------------------------------
						Frame nominalJoint = move.toFrame(NJ_path);
						Frame relativeJoint = targetFrame.copyWithRedundancy(nominalJoint);
						logPrecision(target, relativeJoint);
						
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
	
	void setLogger(boolean logger) {
		if(this.logger != logger) {
			padLog(logger ? "Logger on" : "Logger off");
			this.logger = logger;
			move.setLogger(logger);
			rcp.setLogger(logger);
			remote.setLogger(logger);
		}
	}
	
	void setSpeed(double speed) {
		if(move.getGlobalSpeed() != speed) {
			move.setGlobalSpeed(speed, true);
			oee.resetCycleTime();
			remote.setSpeed(speed);
		}
	}
	
	private void stop() {
		padErr("Program stopped");
		waitMillis(2000);
		dispose();
	}
}