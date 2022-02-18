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

public class _CambrianApp extends RoboticsAPIApplication {	
	@Inject	@Named("Cambrian") private Tool tool;
	xAPI__ELUX elux = new xAPI__ELUX();
	@Inject xAPI_MF	mf = elux.getMF();
	@Inject xAPI_Pad pad = elux.getPad();
	@Inject xAPI_Move move = elux.getMove();
	@Inject CambrianAPI cambrian = new CambrianAPI(elux);
	Params p = new Params();
	JSONmgr<Params> paramsMgr = new JSONmgr<Params>();
	RecipeMgr rcp = new RecipeMgr();
	UserKeys keys = new UserKeys(this);
	OEEmgr oee = elux.getOEE();
	RemoteMgr remote = new RemoteMgr();
	CSVLogger precLog = new CSVLogger();
	RecipeLUT rcpLUT = new RecipeLUT();
	
	FrameList frameList = new FrameList();
	boolean logger;
	int approachMode, idle, sniffing_pause;
	int loop_joint, target;
	int moveAns, failure;
	int trials;
	String cambrianModel, SP_pathroot, SP_path, NJ_pathroot, NJ_path;
	Frame SP_frame, NJ_frame;
	
	@Override public void initialize() {
		paramsMgr.init("CambrianParams.json");
		//paramsMgr.saveData(p);
		p = paramsMgr.fetchData(p);
		
		keys.configPadKeys();
		remote.init(p.REMOTE_FILENAME);
		setLogger(remote.getLogger());
		
		// INIT MOVE ---------------------------------------------
		move.init("/_Cambrian/_HomeLB",			// Home path
					tool, "/TCP",				// Tool, TCP
					remote.getSpeed(), 1.0,		// Relative speed and acceleration
					20.0, 5.0,					// Blending
					5.0, true,					// Collision detection (Nm), auto release
					false);						// Logging
		move.setA7Speed(1); 					// Accelerate J7 if bottleneck
		
		// INIT RECIPE -------------------------------------------
		rcp.init(pad, p.RECIPE_FILENAME, false);
		rcp.fetchAllRecipes();
		rcp.selectRecipeRCP(rcpLUT.getRecipe(925503312));
		//rcp.selectRecipeRCP("F4");
		//rcp.askPNC();
		
		SP_pathroot = "/_Cambrian/" + rcp.getActiveRCP() + "/ScanPoints/";
		NJ_pathroot = "/_Cambrian/" + rcp.getActiveRCP() + "/NominalJoints/";
			
		rcp.selectJointID(rcp.getOItemID(0));

		// INIT CAMBRIAN -----------------------------------------
		if(!cambrian.init("192.168.2.50", 4000)) stop();
		cambrianModel = "Elux_fridge_ref_bolt";
		cambrian.loadModel(cambrianModel, logger);
		//cambrian.getNewPrediction(cambrianModel);
		
		// INIT OEE & PRECISION LOGGING --------------------------
		oee.init("FRIDGE", "JOINT",
				p.TOTAL_JOINTS, p.MAX_TRIALS, 
				p.OEE_OBJ_FILENAME, 
				p.OEE_STATS_FILENAME,
				p.OEE_EVENTS_FILENAME, true); // DISABLE TRUE TO RESET OBJ
		
		precLog.init(p.PRECISION_FILENAME, true);
		precLog.header("JOINT,X,Y,Z,DIST,A,B,C,(RC),(Reason),(Date),(Time)\n");
		
		//if(pad.question("Restart all OEE & precision data?", "YES", "NO") == 0)
			resetAllOEE();
		
		// INIT PROCESS ------------------------------------------
		sniffing_pause = 500;
		loop_joint = 0;
		idle = 0;
		approachMode = 0;
		if(!move.PTPhome(1, false)) stop();
	}
	
	void sniff(int target) {
		if(logger) padLog("Leak test #" + target);
		mf.blinkRGB("B", sniffing_pause);
	}
	
	void resetAllOEE() {
		oee.resetCycle();
		oee.resetItems();
		oee.saveOEEimage(false);
		precLog.reset();
		padLog("All OEE have been zeroed.");
	}

	void logPrecision(int item, Frame offset, int outcome) {
		precLog.open();
		precLog.log(item, false);
		precLog.log(rf2s(offset, true, true), true);
		if(outcome != 1) {
			precLog.log(outcome, true);
			precLog.log(oee.reason(outcome), true);
			precLog.log(getDateAndTime(), true);
		}
		precLog.eol();
		precLog.close(false);
	}
	
	void selectModelAtEnd(int orderIndex) {
		String prevModel = cambrianModel;
		cambrianModel = rcp.getNextCambrianModel(orderIndex);
		if(cambrianModel.compareTo(prevModel) != 0) 
			cambrian.loadModel(cambrianModel, false);
	}
	
	void INR() {
		SP_frame = randomizeFrame(move.toFrame(SP_path), p.RANDOM_DIST_MIN,
														 p.RANDOM_DIST_MAX);
		trials++;
		mf.blinkRGB("RB", 50);
	}
	
	boolean targetVisited(Frame target) {
		for(int i=0; i<frameList.size(); i++) {
			if (frameList.get(i).distanceTo(target) < p.FILTER_IAE_DIST) {
				if (logger) padLog("Distance to joint " + rcp.getOItemID(i) + 
									"too small, already visited");
				return true;
			}
		}
		return false;
	}
	
	boolean targetFilter(Frame offset) {
		if(p.FILTER_INV_ENABLED == true) {
			if (offset.distanceTo(offset.getParent()) > p.FILTER_INV_DIST) {
				if(logger) padLog("Distance between detection and nominal is " + 
						d2s(offset.distanceTo(offset.getParent())));
				return false;
			} else if(r2d(offset.getAlphaRad()) > p.FILTER_INV_ANG) {
				if(logger) padLog("Alpha (A) between detection and nominal is " + 
						d2s(r2d(offset.getAlphaRad())) + "�");
				return false;
			} else if(r2d(offset.getBetaRad()) > p.FILTER_INV_ANG) {
				if(logger) padLog("Beta (B) between detection and nominal is " + 
						d2s(r2d(offset.getBetaRad())) + "�");
				return false;
			} else if(r2d(offset.getGammaRad()) > p.FILTER_INV_ANG) {
				if(logger) padLog("Gamma (C) between detection and nominal is " + 
						d2s(r2d(offset.getAlphaRad())) + "�");
				return false;
			} else return true;
		} else return true;		// Override filter if NJ not recorded
	}
	
	public void scanJoint() {
		do {
			failure = 0;
			
			oee.pause();
			idle = remote.getIdle();
			if(idle == 1) idle();
			setLogger(remote.getLogger());
			setSpeed(remote.getSpeed());
			move.setGlobalAccel(remote.getAccel());
			oee.resume();
			
			if (trials == p.MAX_TRIALS) {	// TOO MANY INTENTS -----------------------
				if(logger) padLog("Skip joint #" + target + ", too many intents.");
				mf.blinkRGB("R", 1000);
				oee.addTMI(target);
				break;
			}
			moveAns = move.PTP(SP_frame, 1, false); // MOVE TO ScanPoint ----------
			failure += oee.checkMoveFailure(target, moveAns);
			if(cambrian.getNewPrediction(cambrianModel)) {
				Frame targetFrame = cambrian.getTargetFrame();
				// TRANSFORM ANSWER, GET OFFSET -----------------------------------
				targetFrame.transform(rcp.getDO().invert());
				Frame offset2NJ = targetFrame.copyWithRedundancy(NJ_frame);
				if(!targetVisited(targetFrame)) {
					if(targetFilter(offset2NJ)) {
						// VISIT JOINT -------------------------------------------------
						if(approachMode != 0) {
							Frame approachFrame = offsetFrame(targetFrame,
														0,0,-p.APPROACH_DIST,0,0,0);
							moveAns = move.PTP(approachFrame, 1, (approachMode == 2));
							failure += oee.checkMoveFailure(target, moveAns);
							if(approachMode == 2) {
								moveAns = move.LIN(targetFrame, p.APPROACH_SPEED, false);
								failure += oee.checkMoveFailure(target, moveAns);
							}
							sniff(target); // SNIFFING PROCESS -------------------------
							if(approachMode == 2) {
								moveAns = move.LIN(approachFrame, p.APPROACH_SPEED, true);
								failure += oee.checkMoveFailure(target, moveAns);
							}
							moveAns = move.LINREL(0, 0, -p.EXIT_DIST * (
									(rcp.getActiveIndex() == rcp.getActiveItemAmount()) ? 2 : 1), 1, true);
							failure += oee.checkMoveFailure(target, moveAns);
						}
						// CHECK SUCCESS -----------------------------------------------
						if(failure == 0) {
							frameList.add(targetFrame);
							if(logger) padLog("J" + target + " successful");
							logPrecision(target, offset2NJ, 1);
							break;
						} else {
							if(logger) padLog("J" + target + " unsuccessful, randomizing");
							logPrecision(target, offset2NJ, failure); // Could be >1 cause
							INR();
						}
					} else {
						if(logger) padLog("Detection not valid(filtered), randomizing");
						oee.addINV(target);
						logPrecision(target, offset2NJ, 2);
						INR();
						
					}
				} else {
					if(logger) padLog("Detection already visited, randomizing");
					oee.addIAE(target);
					logPrecision(target, offset2NJ, 3);
					INR();
				}
			} else {
				if(logger) padLog("Joint #" + target + " not found, randomizing");
				oee.addINF(target);
				INR();
			}
		} while (true);
	}

	@Override public void run() {
		while (true) {
			if (loop_joint == 0 && logger) padLog("Start testing sequence");
			//rcp.fetchAllRecipes();
			oee.startCycle();
			cambrianModel = "Elux_fridge_ref_bolt";
			cambrian.loadModel(cambrianModel, logger);
			move.PTP("/_Cambrian/" + rcp.getActiveRCP() + "/ScanPoints/RBSP", 1, false);
			if(logger) padLog("Scanning for bolt...");
			if(cambrian.getNewPrediction(cambrianModel)) {
				Frame observedBolt = cambrian.getTargetFrame();
				Frame offset2NB = observedBolt.copyWithRedundancy(
						move.toFrame("/_Cambrian/" + rcp.getActiveRCP() + "/_RefBolt"));
				offset2NB.setAlphaRad(0).setBetaRad(0).setGammaRad(0);
				if(logger) padLog("Offset from nominal RefBolt is " + rf2s(offset2NB, false, false));
				
				rcp.selectJointID(rcp.getOItemID(0));
				cambrianModel = rcp.getCurrentCambrianModel();
				cambrian.loadModel(cambrianModel, logger);
				
				for(int i = 0; i < rcp.getActiveItemAmount(); i++) {
					oee.startItem();
					if (loop_joint == 0) {
						rcp.selectJointID(rcp.getOItemID(i));
						target = rcp.getJointID();
					} else {
						target = loop_joint;
						rcp.selectJointID(target);
						//i = USED_JOINTS;
					}
					
					trials = 0;
					SP_path = SP_pathroot + "P" + target;
					NJ_path = NJ_pathroot + "P" + target;
					SP_frame = move.toFrame(SP_path).transform(offset2NB.getTransformationFromParent());
					NJ_frame = move.toFrame(NJ_path).transform(offset2NB.getTransformationFromParent());
					
					scanJoint();
					
					oee.endItem(target);
					selectModelAtEnd(i);
				}
			} else padErr("Fridge too distant from nominal position, reference bolt not found");
			
			frameList.free();
			if(loop_joint == 0) {
				oee.endCycle();
				oee.saveOEEimage(false);
				//move.PTPhome(1, true);
				if(idle == 2) idle();
			}
		}
	}
	
	private void idle() {
		move.PTPhome(1, true);
		padLog("Robot is now in idle mode,\n" +
				" - To resume press again SLEEP or edit Remote.json\n" +
				" - To deselect program, first go to T1, then resume");
		do {
			waitMillis(1000);
			idle = remote.getIdle();
		} while(idle != 0);
		if(idle != remote.getIdle()) remote.setIdle(idle);
		padLog("Resuming operations...");
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
		getApplicationControl().halt();
		dispose();
	}
}