package application.Pickit;

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
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class PickItApp extends RoboticsAPIApplication {
	private Controller kiwaController;
	private LBR kiwa;
	
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	@Inject private MediaFlangeIOGroup 			mediaFlangeIOGroup;
	@Inject	@Named("PickitGripper") 	private Tool PickitGripper;
	
	// Custom modularizing handler objects
	@Inject private HandlerMFio	mf = new HandlerMFio(mediaFlangeIOGroup);
	@Inject private HandlerPLCio plc = new HandlerPLCio(mf, plcin, plcout);
	@Inject private HandlerMov move = new HandlerMov(mf);
	@Inject private HandlerPad pad = new HandlerPad(mf);
	@Inject private HandlerPickIt pickit = new HandlerPickIt(kiwa);
	
	int failCounterFind = 0;
	int failCounterPick = 0;
	boolean rot180 = false;
	
	@Override public void initialize() {
		kiwaController = (Controller) getContext().getControllers().toArray()[0];
		kiwa = (LBR) kiwaController.getDevices().toArray()[0];
		move.setJTConds(10.0);
		move.setGlobalSpeed(1);
		move.setTCP(PickitGripper, "/Cylinder");
		move.setHome("/_PickIt/Scan");
		if(!move.PTPhome(1)) stop();
		move.PTP("/_PickIt/Scan/Release", 1);
		plc.askOpen();
		move.PTPhome(1);
		pickit.init("192.168.2.12", 30001);
		if(!pickit.config(3, 3, 5000)) stop();
	}

	@Override public void run() {
		padLog("Start picking sequence");
		int box = 0;
		scan(false);
		while (pickit.isRunning()) {
			box = pickit.getBox();
			if (box > 0) {
				failCounterFind = 0;
				if (!pick(pickit.getPickFrame(), 200)) {
					padLog("Error when picking piece.");
					failCounterPick ++;
				} else {
					move.setTCP(PickitGripper, "/Cylinder");
					move.PTPhome(1);
					scan(true);
					if ((pickit.getObjType() == 1 && pickit.getPickID() == 1)) {
						move.PTP("/_PickIt/PumpJig/Approach_Z",1);
						move.LIN("/_PickIt/PumpJig",0.3);
						plc.openGripper();
						move.LIN("/_PickIt/PumpJig/Approach_Z",0.8);
					} else if (pickit.getObjType() == 1 && pickit.getPickID() == 2) {
						move.PTP("/_PickIt/Pole/H_pole/H_Zoffset",1);
						move.LIN("/_PickIt/Pole/H_pole",0.3);
						plc.openGripper();
						move.LIN("/_PickIt/Pole/H_pole/H_Xoffset",0.8);
						reorientate();
						move.LIN("/_PickIt/Pole/H_pole",0.3);
						plc.closeGripper();
						move.LIN("/_PickIt/Pole/H_pole/H_Zoffset",1);
						move.LIN("/_PickIt/PumpJig/Approach_Z",1);
						move.LIN("/_PickIt/PumpJig",0.3);
						plc.openGripper();
						move.LIN("/_PickIt/PumpJig/Approach_Z",0.8);
					} else if (pickit.getObjType() == 1 && pickit.getPickID() == 3) {
						
					} else {
						padLog("None");
					}
					move.LINhome(1);
				}
				while (!pickit.isReady()) { waitMillis(100); padLog("Waiting.");}
			} else if (box == -2){
				failCounterFind ++;
				scan(false);
			} else if (box == -3) {
				pad.info("Fill the box, Pick-It detected it empty!");
				scan(false);
			}
			if (pickit.getRemainingObj() == 0) scan(false);
			else {
				//scan();							// If scan is mandatory after every pick and cannot be performed while robot is away from the POV
				//pickit.doSendNextObj(); 			// If scan between single picks is not mandatory
			}
		}
		pickit.terminate();
	}
	
	private void scan(boolean async) {
		if (!async || failCounterFind > 1) {
			move.setTCP(PickitGripper, "/Cylinder");
			move.PTPhome(1);
		}
		if (failCounterFind == 3) {
			vibrate();
			failCounterFind = 0;
			scan(false);
		}
		if (!async) waitMillis(350);
		pickit.doScanForObj();
	}
	
	private boolean pick(Frame targetFrame, int offset) {
		Frame postFrame = targetFrame.copyWithRedundancy();
		postFrame.setZ(postFrame.getZ() + offset);
		plc.openGripperAsync();
		padLog("Picking model " + pickit.getObjType() + ", pickPoint "+ pickit.getPickID());
		padLog("Rotation around Z is: " + pickit.getZrot());
		move.setTCP(PickitGripper, "/Cylinder/Approach");
		if (!move.PTP(targetFrame, 0.75)) {
			padLog("Position not reachable");
			move.setTCP(PickitGripper, "/Cylinder/Approach/Rot180");
			padLog("Trying inverted");
			if (!move.PTP(targetFrame, 0.75)) return false;
			else rot180 = true;
		}
		if (!rot180) {
			move.setTCP(PickitGripper, "/Cylinder");
			if (!move.LIN(targetFrame, 0.25)) return false;
		} else {
			move.setTCP(PickitGripper, "/Cylinder/Rot180");
			if (!move.LIN(targetFrame, 0.25)) return false;
		}
		plc.closeGripper();
		if (!rot180) {
			move.setTCP(PickitGripper, "/Cylinder");
			if (!move.LIN(postFrame, 0.6)) return false;
		} else {
			move.setTCP(PickitGripper, "/Cylinder/Rot180");
			postFrame.setAlphaRad(postFrame.getAlphaRad() + deg2rad(180));
			if (!move.LIN(postFrame, 0.6)) return false;
		}
		return true;
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
