package application.Cambrian;

import static EluxUtils.Utils.*;
import static EluxUtils.UMath.*;
import EluxUtils.*;
import EluxAPI.*;
import EluxLogger.*;
import EluxOEE.*;
import EluxRemote.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class _CambrianApp extends RoboticsAPIApplication {
	public static final String PARAMS_FILENAME = "CambrianParams.json";
	public static final String REMOTE_FILENAME = "Remote.json";
	public static final String OEE_OBJ_FILENAME = "OEE_Object.txt";
	public static final String OEE_STATS_FILENAME = "OEE_Stats.csv";
	public static final String OEE_EVENTS_FILENAME = "OEE_Failure_Log.csv";
	public static final String PRECISION_FILENAME = "Cambrian_Precision_Log.csv";
	public static final String RECIPE_FILENAME = "CambrianRecipes.json";
	public static final String FRAMES_PR = "/_Cambrian";
	
	@Inject	@Named("Cambrian") private Tool tool;
	xAPI__ELUX elux = new xAPI__ELUX();
	@Inject xAPI_MF	mf = elux.getMF();
	@Inject xAPI_Pad pad = elux.getPad();
	@Inject xAPI_PLC plc = elux.getPLC();
	@Inject xAPI_Move move = elux.getMove();
	@Inject ProLogger log = elux.getLog();
	@Inject CambrianAPI cambrian = new CambrianAPI(elux);
	
	Params p = new Params();
	JSONmgr<Params> paramsMgr = new JSONmgr<Params>();
	RecipeMgrJoints rcp = new RecipeMgrJoints();
	RecipeBuilder rcpb = new RecipeBuilder(this);
	UserKeys keys = new UserKeys(this, log);
	OEEmgr oee = elux.getOEE();
	RemoteMgr remote = new RemoteMgr();
	CSVLogger precLog;
	LUTrecipe LUTrcp = new LUTrecipe();
	LUTcambrianModel LUTcm = new LUTcambrianModel();
	
	FrameList visitedJoints = new FrameList();
	boolean firstRun;
	boolean logger;
	int PNC, SN;
	String RCP;
	int idle;
	int approachMode, sniffing_pause;
	int loop_joint, jointID;
	int moveAns;
	int failure[] = new int[3];
	int trial;
	String cambrianModel;
	String RB_path, SP_pathroot, SP_path, NJ_pathroot;
	Frame OB_frame, offset2NB, SP_frame;
	Frame NJ1_frame, NJ2_frame;
	Frame targetFrame;
	
	@Override public void initialize() {
		paramsMgr.init(PARAMS_FILENAME);
		//paramsMgr.saveData(p);
		p = paramsMgr.fetchData(p);
		
		keys.configPadKeys();
		remote.init(REMOTE_FILENAME);
		
		log.newLog("RobotInit");
		setLogger(remote.getLogger());
		
		//PNC = 925501302;	// ###########################################################
		//SN = 20451651;	// ###########################################################
		
		// INIT MOVE ---------------------------------------------
		move.init(FRAMES_PR + "/_HomeLB",		// Home path
					tool, "/TCP",				// Tool, TCP
					remote.getSpeed(), 1.0,		// Relative speed and acceleration
					20.0, 5.0,					// Blending
					5.0, 0,						// Collision detection (Nm), release mode
					false);						// Logging
		move.setA7Speed(1); 					// Accelerate J7 if bottleneck
		
		if(!move.PTPhome(0.5, false)) stop();
		
		// INIT RECIPE -------------------------------------------
		rcp.init(pad, RECIPE_FILENAME, log);
		//rcp.copyRecipe("F7", "F9");
		//rcpb.copyFrames("F7", "F9");

		// INIT CAMBRIAN -----------------------------------------
		if(!cambrian.init(log)) stop();
		loadAllCambrianModels();
		
		// INIT OEE & PRECISION LOGGING --------------------------
		oee.init("FRIDGE", "JOINT",
				p.TOTAL_JOINTS, p.MAX_TRIALS, 
				OEE_OBJ_FILENAME, 
				OEE_STATS_FILENAME,
				OEE_EVENTS_FILENAME, true); // DISABLE TRUE TO RESET OBJ
		
		precLog  = new CSVLogger(PRECISION_FILENAME, true, ';');
		precLog.setHeader("Date;Time;PNC;SN;J-ID;J-T;TrN;" +
						"X(mm);Y(mm);Z(mm);DIST;A(°);B(°);C(°);" +
						"CT(s);RC;Reason\n");
		
		if(pad.question("Restart all OEE & precision data?", "YES", "NO") == 0) resetAllOEE();
		
		// INIT PROCESS ------------------------------------------
		sniffing_pause = p.sniffPause;
		loop_joint = 0;
		idle = 0; remote.setIdle(0);
		approachMode = p.approachMode;
		firstRun = true;
		if(!move.PTPhome(1, false)) stop();
		plc.fbkMissionEnded();
		plc.setDO07(true);		// Switch on light
	}
	
	@Override public void run() {		// MAIN CYCLIC PROGRAM
		while (true) {
			if(waitForNewFridge()) {
				if(true) {
					scanFridge();
					log.msg(Event.Proc, "Fridge scanned & going home", 0, true);
					if(move.PTPhome(1, false)) plc.fbkMissionEnded();
				}
			}
		}
	}
	
	boolean waitForNewFridge() {
		rcp.fetchAllRecipes();
		log.msg(Event.Proc, "Waiting for new fridge detected & identified", 0, false);
		while(!plc.loadRecipe()) { waitMillis(10);} // Wait for new fridge data available
		
		// LOAD RECIPE
		PNC = plc.getPNC();
		SN = plc.getSN();
		RCP = LUTrcp.getRecipe(PNC);
		log.newLog(PNC + "_" + SN + "_" + RCP);
		log.msg(Event.Prod, "Current PNC is " + PNC + 
							"\nCurrent SN is " + SN + 
							"\nCurrent RCP is " + RCP, 0, false);
		if(RCP.compareTo("RCP NOT FOUND") == 0) {
			log.msg(Event.Rcp, "Recipe for PNC " + PNC + " not found", 1, true);
			return false; // RECIPE NOT FOUND
		}
		rcp.selectRecipeRCP(RCP);
		RB_path = FRAMES_PR + "/Recipes/" + RCP + "/_RefBolt";
		SP_pathroot = FRAMES_PR + "/Recipes/" + RCP + "/ScanPoints/";
		NJ_pathroot = FRAMES_PR + "/Recipes/" + RCP + "/NominalJoints/";
		
		plc.fbkRecipeLoaded();
		log.msg(Event.Proc, "Recipe " + RCP + " loaded & waiting for YuMi", 0, true);
		
		while(!plc.missionStart()) { // Wait for OK from line to start
			waitMillis(10);
			if (this.checkMissionEndReq(true)) {
				log.msg(Event.Fail, "Scan aborted, fridge missed", 0, true);
				return false;
			}
		}
		plc.fbkMissionRunning();
		return true;
	}
	
	boolean checkMissionEndReq(boolean doingIntent) {
		if(plc.missionEnd()) {
			log.msg(Event.Proc, "Mission end after timeout / yumi finished / user request", 1, true);
			if(doingIntent) oee.endItem(jointID);
			endCycle();
			return true;
		}
		return false;
	}

	void logPrecision(int item, Frame offset, int outcome) {
		precLog.open(false);
		precLog.log(getDate(), false);
		precLog.log(getTime(':'), true);
		precLog.log(PNC, true);
		precLog.log(SN, true);
		precLog.log(item, true);
		precLog.log((item == 0) ? 'B' : rcp.getActiveJointType(), true);
		precLog.log((item == 0) ? 0 : trial, true);
		precLog.log(rf2s(offset, true, true), true);
		if(outcome == 1) precLog.log(oee.getCurrentItemCT(), true);
		else precLog.log("", true);
//		if(outcome!= 1) {
			precLog.log(outcome, true);
			precLog.log(oee.reason(outcome), true);
//		}
		precLog.eol();
		precLog.close(false);
	}
	
	boolean targetVisited(Frame target) {
		double dist;
		for(int i = 0; i < visitedJoints.size(); i++) {
			dist = visitedJoints.get(i).distanceTo(target);
			if(dist < p.FILTER_IAE_DIST) {
				log.msg(Event.Vision, "Distance to joint #" + rcp.getOItemID(i) + " = " + d2s(dist) +
									" too small - already visited", 0, true);
				return true;
			}
		}
		return false;
	}
	
	boolean targetFilter(Frame offset) {
		if(p.FILTER_INV_ENABLED == true) {
			if (offset.distanceTo(offset.getParent()) > p.FILTER_INV_DIST) {
				log.msg(Event.Vision, "Distance between detection and nominal is " + 
						d2s(offset.distanceTo(offset.getParent())), 0, true);
				return false;
			} else if(r2d(offset.getAlphaRad()) > p.FILTER_INV_ANG) {
				log.msg(Event.Vision, "Alpha (A) between detection and nominal is " + 
						d2s(r2d(offset.getAlphaRad())) + "°", 0, true);
				return false;
			} else if(r2d(offset.getBetaRad()) > p.FILTER_INV_ANG) {
				log.msg(Event.Vision, "Beta (B) between detection and nominal is " + 
						d2s(r2d(offset.getBetaRad())) + "°", 0, true);
				return false;
			} else if(r2d(offset.getGammaRad()) > p.FILTER_INV_ANG) {
				log.msg(Event.Vision, "Gamma (C) between detection and nominal is " + 
						d2s(r2d(offset.getAlphaRad())) + "°", 0, true);
				return false;
			} else return true;
		} else return true;		// Override filter if NJ not recorded
	}
	
	boolean selectBestPrediction(int jointID, FrameList predictions) {
		int bestFound = 0;
		String NJ_path = NJ_pathroot + "P" + jointID;
		NJ1_frame = move.p2f(NJ_path).transform(offset2NB.getTransformationFromParent());
		for(int i = 0; i < predictions.size(); i++) {
			Frame prediction = predictions.get(i).transform(rcp.getDO().invert());
			Frame offset2NJ = prediction.copyWithRedundancy(NJ1_frame);
			if(!targetVisited(prediction)) {
				if(targetFilter(offset2NJ)) {
					targetFrame = prediction;
					log.msg(Event.Vision, "Found pred #" + i + 
									"a good match for Joint ID=" + jointID, 0, false);
					return true;
				} else {
					bestFound = 2;
				}
			} else {
				bestFound = (bestFound == 2) ? 2 : 3;
				predictions.remove(i);
				i--;
			}
		}
		if (bestFound == 2) oee.addINV(jointID);
		if (bestFound == 3) oee.addIAE(jointID);
		return false;
	}
	
	
	void visitJoint(int jointID, int MJ, Frame targetFrame) {
		Frame approachFrame = offsetFrame(targetFrame,
				0,0,-p.APPROACH_DIST,0,0,0);
		
		moveAns = move.PTP(approachFrame, 1, (approachMode == 2));
		failure[MJ] += oee.checkMoveFailure(jointID, moveAns);
		
		if(approachMode == 2) {
			moveAns = move.LIN(targetFrame, p.APPROACH_SPEED, false);
			failure[MJ] += oee.checkMoveFailure(jointID, moveAns);
		}
		
		sniff(jointID); // SNIFFING PROCESS ------------------------------------
		
		if(approachMode == 2) {
			moveAns = move.LIN(approachFrame, p.APPROACH_SPEED, true);
			failure[MJ] += oee.checkMoveFailure(jointID, moveAns);
		}
		
		moveAns = move.LINREL(0, 0, -p.EXIT_DIST * (
			(rcp.getActiveIndex() == rcp.getActiveItemAmount()) ? 2 : 1), 1, true);
		failure[MJ] += oee.checkMoveFailure(jointID, moveAns);
	}
	
	int filterAndVisit(int jointID, int MJ, Frame targetFrame) {
		Frame offset2NJ = targetFrame.copyWithRedundancy(NJ1_frame);
		Frame offset2OB = targetFrame.copyWithRedundancy(OB_frame);
		if(!targetVisited(targetFrame)) {
			if(targetFilter(offset2NJ)) {
				// VISIT JOINT -------------------------------------------------
				if(approachMode != 0) visitJoint(jointID, MJ, targetFrame); 
				// CHECK SUCCESS -----------------------------------------------
				if(failure[MJ] == 0 && failure[0] == 0) {
					visitedJoints.add(targetFrame);
					log.msg(Event.Proc, "J" + jointID + " successful", 0, true);
					mf.blinkRGB("G", 250);
					logPrecision(jointID, offset2OB, 1);		// Update OB/NJ
				} else {
					log.msg(Event.Proc, "J" + jointID + " unsuccessful due to IWC/IUR", 0, true);
					logPrecision(jointID, offset2OB, failure[MJ]); // Could be >1 cause, Update OB/NJ
					INR();
				}
			} else {
				log.msg(Event.Vision, "Detection not valid(filtered)", 1, true);
				failure[MJ] += 1000;
				oee.addINV(jointID);
				logPrecision(jointID, offset2OB, 2);		// Update OB/NJ
				INR();
			}
		} else {
			log.msg(Event.Vision, "Detection already visited", 1, true);
			failure[MJ] += 10000;
			oee.addIAE(jointID);
			logPrecision(jointID, offset2OB, 3);
			INR();
		}
		return failure[MJ];
	}
	
	boolean scanJoint() {
		oee.startItem();
		trial = 1;
		SP_path = SP_pathroot + "P" + jointID;
		SP_frame = move.p2f(SP_path).transform(offset2NB.getTransformationFromParent());
		failure[1] = 1;
		failure[2] = rcp.isActiveJointMJ() ? 1 : 0;
		int MJ = rcp.getActiveJointMJ();
		
		do {	// SCAN LOOP --------------------------------------------------------
			oee.pause();
			idle = remote.getIdle();
			if(idle == 1) idle();
			setLogger(remote.getLogger());
			setSpeed(remote.getSpeed());
			move.setGlobalAccel(remote.getAccel());
			oee.resume();
			
			if (trial >= p.MAX_TRIALS + 1) {	// TOO MANY INTENTS ------------------
				if(failure[1] > 0) {
					log.msg(Event.Fail, "Skip joint #" + jointID + " after " + 
											(trial - 1) + " intents.", 0, true);
					mf.blinkRGB("R", 1000);
					oee.addTMI(jointID);
					oee.endItem(jointID);
				}
				if(failure[2] > 0) {
					log.msg(Event.Fail, "Skip joint #" + MJ + "after " + 
											(trial - 1) + " intents.", 0, true);
					mf.blinkRGB("R", 1000);
					oee.addTMI(MJ);
					oee.endItem(MJ);
				}
				
				break;
			}
			log.msg(Event.Proc, "Starting trial #" + trial, 0, false);
			failure[0] = 0;
			if (this.checkMissionEndReq(true)) return false;	// CHECK if need to free conveyor
			moveAns = move.PTP(SP_frame, 1, false); // MOVE TO ScanPoint ----------
			if (this.checkMissionEndReq(true)) return false;	// CHECK if need to free conveyor
			failure[0] += oee.checkMoveFailure(jointID, moveAns);
			log.msg(Event.Move, "Reached SP for " + rcp.getActiveJointType() + 
					rcp.getActiveJointID(), -1, true);
			if(cambrian.doScan(cambrianModel) > 0) {				
				if(failure[1] > 0 && selectBestPrediction(jointID, cambrian.getPredictFrames())) { 
					failure[1] = 0; 
					filterAndVisit(jointID, 1, targetFrame);	// VISIT J1
					if(failure[1] == 0) {
						oee.endItem(jointID);
						oee.startItem();
					}
				}
				if(rcp.isActiveJointMJ() && (failure[2] > 0) && 
						selectBestPrediction(MJ, cambrian.getPredictFrames())) {
					failure[2] = 0;
					filterAndVisit(MJ, 2, targetFrame);	// VISIT J2
					if(failure[2] == 0) {
						oee.endItem(MJ);
						oee.startItem();
					}
				}
				if((failure[0] + failure[1] + failure [2]) == 0) break;
				
			} else {
				log.msg(Event.Proc, rcp.getActiveJointType() + jointID + " not successful", 0, true);
				oee.addINF(jointID);
			}
			INR();
		} while (true);
		return true;
	}
	
	void scanFridge() {
		if (loop_joint == 0) log.msg(Event.Proc, "Fridges ready and YuMi scanning. Start mission", 0, true);
		oee.startCycle();
		visitedJoints.free();
		if(!p.scanBoltOnce || firstRun) {
			cambrianModel = LUTcm.getCambrianModel('B');
			cambrian.loadModel(cambrianModel);
			move.PTP(FRAMES_PR + "/_RBSP", 1, false);
			log.msg(Event.Proc, "Cambrian scanning for bolt...", 0, true);
			if (cambrian.doScan(cambrianModel) > 0) {
				OB_frame = cambrian.getPredictFrames().getFirst();
				offset2NB = OB_frame.copyWithRedundancy(move.p2f(RB_path));
				// IGNORE FRIDGE ROTATION TO MIN NOISE
				offset2NB.setAlphaRad(0).setBetaRad(0).setGammaRad(0);
				log.msg(Event.Vision, "Offset from nominal RefBolt is " + rf2s(offset2NB, false, false), 0, true);
				logPrecision(0, offset2NB, 1);
				firstRun = false;
			} else {
				logErr("Reference bolt not found");
				endCycle();
				return;
				//if(testMode) stop();
			}
		}
		
		for(int i = 0; i < rcp.getActiveItemAmount(); i++) {
			if (loop_joint == 0) {
				rcp.selectJointID(rcp.getOItemID(i));
				if(i == 0) {
					cambrianModel = LUTcm.getCambrianModel(rcp.getActiveJointType());
					cambrian.loadModel(cambrianModel);
				}
				jointID = rcp.getActiveJointID();
			} else {
				jointID = loop_joint;
				rcp.selectJointID(jointID);
				//i = USED_JOINTS;
			}
			
			if (this.checkMissionEndReq(false)) return;
			if(!scanJoint()) return;		// SCAN JOINT
			selectModelAtEnd(i);
		}
		endCycle();
	}
	
	void endCycle() {
		if(loop_joint == 0) {
			oee.endCycle();
			oee.saveOEEimage(false);
			move.PTPhome(1, false);
			log.msg(Event.Proc, "Robot returned to home, conveyor free", 0, true);
			plc.fbkMissionEnded();
			if(idle == 2) idle();
		}
	}
	
	void loadAllCambrianModels() {
		cambrian.loadModel(LUTcm.getCambrianModel('J'));
		cambrian.loadModel(LUTcm.getCambrianModel('U'));
		cambrian.loadModel(LUTcm.getCambrianModel('W'));
		cambrian.loadModel(LUTcm.getCambrianModel('C'));
		cambrian.loadModel(LUTcm.getCambrianModel('B'));
	}
	
	void selectModelAtEnd(int orderIndex) {
		String prevModel = cambrianModel;
		cambrianModel = LUTcm.getCambrianModel(rcp.getNextJointType(orderIndex));
		if(cambrianModel.compareTo(prevModel) != 0) 
			cambrian.loadModel(cambrianModel);
	}
	
	void INR() {
		SP_frame = randomizeFrame(move.p2f(SP_path), p.RANDOM_DIST_MIN,
														 p.RANDOM_DIST_MAX);
		trial++;
		if(trial <= p.MAX_TRIALS) log.msg(Event.Fail, 
				"Scanpoint randomized after Intent Not Right", 0, false);
		mf.blinkRGB("RB", 50);
	}
	
	void sniff(int target) {			// PROCESS
		log.msg(Event.Proc, "Leak test #" + target, 0, true);
		mf.blinkRGB("B", sniffing_pause);
	}
	
	void idle() {
		plc.setDO07(false);		// Switch off light
		move.PTPhome(1, true);
		log.msg(Event.HMI, "Robot is now in idle mode,\n" +
				" - To resume press again SLEEP or edit Remote.json\n" +
				" - To deselect program, first go to T1, then resume", 1, false);
		do {
			waitMillis(1000);
			idle = remote.getIdle();
		} while(idle != 0);
		if(idle != remote.getIdle()) remote.setIdle(idle);
		plc.setDO07(true);		// Switch on light
		log.msg(Event.HMI, "Exiting idle mode", 1, true);
	}
	
	void setLogger(boolean logger) {
		if(this.logger != logger) {
			//padLog(logger ? "Logger on" : "Logger off");
			this.logger = logger;
			move.setLogger(logger);
			remote.setLogger(logger);
			cambrian.setLogger(logger);
			oee.setLogger(logger);
			log.setPadLogger(logger);
		}
	}
	
	void setSpeed(double speed) {
		if(move.getGlobalSpeed() != speed) {
			move.setGlobalSpeed(speed, true);
			oee.resetCycleTime();
			remote.setSpeed(speed);
		}
	}
	
	void resetAllOEE() {
		oee.resetCycle();
		oee.resetItems();
		oee.saveOEEimage(false);
		precLog.reset();
		log.msg(Event.HMI, "All OEE have been zeroed.", 1, false);
	}
	
	void stop() {
		pad.info("PROGRAM STOPPED, continue to abort...");
		cambrian.end();
		getApplicationControl().halt();
		//dispose();
	}
}