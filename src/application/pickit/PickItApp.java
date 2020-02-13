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
import com.kuka.roboticsAPI.geometricModel.Tool;

public class PickItApp extends RoboticsAPIApplication {
	private Controller kiwaController;
	private LBR kiwa;
	private HandlerPickIt pickit;
	
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	private MediaFlangeIOGroup 			mediaFlangeIOGroup;
	@Inject	@Named("PickitGripper") 	private Tool PickitGripper;
	
	// Custom modularizing handler objects
	@Inject private HandlerMFio	mf = new HandlerMFio(mediaFlangeIOGroup);
	@Inject private HandlerPLCio plc = new HandlerPLCio(mf, plcin, plcout);
	@Inject private HandlerMov move = new HandlerMov(mf);
	@Inject private HandlerPad pad = new HandlerPad(mf);
	
	int failCounterFind = 0;
	int failCounterPick = 0;
	
	@Override public void initialize() {
		kiwaController = (Controller) getContext().getControllers().toArray()[0];
		kiwa = (LBR) kiwaController.getDevices().toArray()[0];
		move.setJTConds(10.0);
		move.setGlobalSpeed(1);
		move.setHome("/_PickIt/Scan");
		move.setTCP(PickitGripper, "/Flange");
		if(!move.PTPhome(1)) stop();
		move.PTP("/_PickIt/Scan/Release", 1);
		plc.askOpen();
		move.PTPhome(1);
		pickit = new HandlerPickIt(kiwa, move);
		pickit.init("192.168.2.12", 30001);
		if(!pickit.config(3, 3, 5000)) stop();
	}

	@Override public void run() {
		padLog("Start picking sequence");
	//	if(!move.PTP("/_PickIt/Scan/S1", 1)) stop();
		scan(false);
		while (pickit.isRunning()) {
			if (pickit.getBox() > 0) {
				failCounterFind = 0;
				if (!pick(pickit.getPickFrame(), 200)) padLog("Error when picking piece.");
				else {
					move.setTCP(PickitGripper, "/Flange");
					move.PTPhome(1);
					scan(true);
					if ((pickit.getObjType() == 1 && pickit.getPickID() == 1)) {
						move.PTP("/_PickIt/PumpJig/Approach_Z",1);
						move.LIN("/_PickIt/PumpJig",0.3);
						plc.openGripper();
						move.LIN("/_PickIt/PumpJig/Approach_Z",0.8);
					} else if (pickit.getObjType() == 1 && pickit.getPickID() != 1) {
						move.PTP("/_PickIt/Pole/H_Zoffset",1);
						move.LIN("/_PickIt/Pole/H_pole",0.3);
						plc.openGripper();
						move.LIN("/_PickIt/Pole/H_Xoffset",0.8);
						move.LIN("/_PickIt/Pole/Transition_XZ",1);
					} else {
						padLog("None");
					}
					move.LINhome(1);
				}
				//while (!pickit.isReady()) { waitMillis(100); padLog("Waiting.");}
			} else {
				failCounterFind ++;
			}
			if (pickit.getRemainingObj() == 0) scan(false);
			else {
				//scan();
				pickit.doSendNextObj(); 			// If scan between single picks is not mandatory
			}
		}
		pickit.terminate();
	}
	
	private void scan(boolean async) {
		if (!async || failCounterFind > 1) move.PTPhome(1);
		if (failCounterFind == 3) {
			vibrate();
			failCounterFind = 0;
			scan(false);
		}
		if (!async) waitMillis(350);
		pickit.doScanForObj();
	}
	
	private void vibrate() {
		pad.info("Vibrate the box");
	}
	
	private boolean pick(Frame targetFrame, int offset) {
		Frame postFrame = targetFrame.copyWithRedundancy();
		postFrame.setZ(postFrame.getZ() + offset);
		plc.openGripperAsync();
		padLog("Picking model " + pickit.getObjType() + ", pickPoint "+ pickit.getPickID());
		move.setTCP(PickitGripper, "/Cylinder/Approach");
		if (!move.PTP(targetFrame, 0.75)) return false;
		move.setTCP(PickitGripper, "/Cylinder");
		if (!move.LIN(targetFrame, 0.25)) return false;
		plc.closeGripper();
		if (!move.LIN(postFrame, 0.6)) return false;
		return true;
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
