package application.PickIt;

import static eluxLibs.Utils.*;
import eluxLibs.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class PickItApp extends RoboticsAPIApplication {
	private Controller kiwaController;
	private LBR kiwa;
	
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	@Inject private MediaFlangeIOGroup 			mediaFlangeIOGroup;
	@Inject	@Named("GripperPickit") 	private Tool GripperPickit;
	
	// Custom modularizing handler objects
	@Inject private HandlerMFio	mf = new HandlerMFio(mediaFlangeIOGroup);
	@Inject private HandlerPLCio plc = new HandlerPLCio(mf, plcin, plcout);
	@Inject private HandlerMov move = new HandlerMov(mf);
	@Inject private HandlerPad pad = new HandlerPad(mf);
	@Inject private HandlerPickIt pickit = new HandlerPickIt(kiwa);
	
	int failCounter = -1;
	boolean rot180 = false;
	
	@Override public void initialize() {
		kiwaController = (Controller) getContext().getControllers().toArray()[0];
		kiwa = (LBR) kiwaController.getDevices().toArray()[0];
		move.setJTconds(15.0);
		move.setGlobalSpeed(1);
		move.setBlending(20, 5);
		move.setTool(GripperPickit);
		move.setTCP("/Cylinder");
		move.setHome("/_PickIt/Scan");
		if(!move.PTPhome(1, false)) stop();
		if(plc.gripperIsHolding()) {
			move.PTP("/_PickIt/Scan/Release", 1, false);
			plc.askOpen();
			move.PTPhome(1, false);
		}
		pickit.init("192.168.2.12", 30001);
		if(!pickit.config(3, 3, 5000)) stop();
	}

	@Override public void run() {
		padLog("Start picking sequence");
		int box = 0;
		int pick = 0;
		scan(false);
		while (pickit.isRunning()) {
			box = pickit.getBox();
			if (box > 0) {
				padLog("Found " + box + " objects, picking model " + pickit.getObjType() + ", pickPoint "+ pickit.getPickID());
				pick = pick(pickit.getPickFrame());
				if (pick == 0) {
					padLog("Object found unreachable by robot, polling next one");
					if(pickit.getRemainingObj() > 0) pickit.doSendNextObj();
					else scan(false);
				} else if (pick == -1) {
					padLog("Gripper collided with unexpected object");
					scan(false);
				} else if (pick == -2) {
					padLog("Object couldn't be exctracted from box");
					scan(false);
				} else {
					failCounter = 0;
					move.setTCP("/Cylinder");
					move.PTPhome(1, false);
					scan(true);
					place();
					move.LINhome(1);
				}
				// while (!pickit.isReady()) { waitMillis(100); padLog("Waiting.");}
			} else if (box == -2){
				failCounter ++;
				scan(false);
			} else if (box == -3) {
				failCounter ++;
				pad.info("Fill the box, Pick-It detected it empty!");
				scan(false);
			}
		}
		pickit.terminate();
	}
	
	private void scan(boolean async) {
		if (!async) failCounter ++;
		if (!async || failCounter > 1) {
			move.setTCP("/Cylinder");
			move.PTPhome(1, false);
		}
		if (failCounter >= 3) {
			vibrate();
			failCounter = -1;
			scan(false);
		}
		if (!async) waitMillis(350);
		pickit.doScanForObj();
	}
	
	private int pick(Frame targetFrame) {
		Frame postFrame = targetFrame.copyWithRedundancy();
		postFrame.setZ(300 - (postFrame.getY() - 400) * 0.6);
		plc.openGripperAsync();
		move.setTCP("/Cylinder/Approach");
		if (!move.PTP(targetFrame, 0.75, false)) return 0;
		if (pickit.getObjType() == 1 && (pickit.getPickID() == 1 || pickit.getPickID() == 2)) {
			move.setTCP("/Cylinder");
		} else move.setTCP("/Cylinder/End");
		if (!move.LINsafe(targetFrame, 0.25)) return -1;
		plc.closeGripper();
		if (!move.LIN(postFrame, 0.6, false)) {
			plc.openGripper();
			move.setTCP("/Cylinder/Approach");
			move.LIN(targetFrame, 0.6, false);
			return -2;
		}
		return 1;
	}
	
	private void place() {
		if ((pickit.getObjType() == 1 && pickit.getPickID() == 1)) {
			move.PTP("/_PickIt/PumpJig/Approach_Z",1, true);
			move.LIN("/_PickIt/PumpJig",0.3, false);
			plc.openGripper();
			move.LIN("/_PickIt/PumpJig/Approach_Z",0.8, true);
		} else if (pickit.getObjType() == 1 && pickit.getPickID() == 2) {
			move.PTP("/_PickIt/Pole/H_pole/H_Zoffset",1, true);
			move.LIN("/_PickIt/Pole/H_pole",0.3, false);
			plc.openGripper();
			move.LIN("/_PickIt/Pole/H_pole/H_Xoffset",0.8, false);
			reorientate();
			move.LIN("/_PickIt/Pole/H_pole",0.3, false);
			plc.closeGripper();
			move.LIN("/_PickIt/Pole/H_pole/H_Zoffset",1, true);
			move.LIN("/_PickIt/PumpJig/Approach_Z",1, true);
			move.LIN("/_PickIt/PumpJig",0.3, false);
			plc.openGripper();
			move.LIN("/_PickIt/PumpJig/Approach_Z",0.8, true);
		} else if (pickit.getObjType() == 3 && pickit.getPickID() == 1) {
			move.setTCP("/Cylinder/End");
			move.PTP("/_PickIt/Pole/V_Zoffset",1, true);
			move.LIN("/_PickIt/Pole", 0.3, false);
			plc.openGripper();
			move.LIN("/_PickIt/Pole/V_Zoffset",0.8, true);
		} else {
			padLog("None");
		}
	}
	
	private void reorientate() {
		pad.info("Reorientate the piece in the pole for " + pickit.getZrot() + " deg." +
					"\n\nIn a final implementation, a servomotor could be used to rotate the required angle");
	}
	
	private void vibrate() {
		pad.info("Vibrate the box");
	}
	
	private void stop() {
		padLog("Program stopped");
		dispose();
	}
	
	@Override public void dispose() { 
		pickit.terminate();
		super.dispose(); 
	}
}
