package application.Pickit;

import static eluxLibs.Utils.*;
import eluxLibs.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.CommandInvalidException;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;

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
	
	@Override public void initialize() {
		kiwaController = (Controller) getContext().getControllers().toArray()[0];
		kiwa = (LBR) kiwaController.getDevices().toArray()[0];
		move.setJTConds(10.0);
		move.setGlobalSpeed(1);
		move.setTCP(PickitGripper, "/Flange");
		plc.askOpen();
		if(!move.PTP("/_PickIt/Scan", 1)) stop();
		pickit = new HandlerPickIt(kiwa, move);
		pickit.init("192.168.2.12", 30001);
	}

	@Override public void run() {
		int boxStatus;
		Frame pickFrame;
		padLog("Start picking sequence");
		if(!move.PTP("/_PickIt/Scan", 1)) stop();
		if(!pickit.config(3, 2, "/_PickIt/Scan", 0.25, 350, 5000)) stop();
		while (pickit.isRunning()) {
			boxStatus = pickit.getBox(true);
			
			/*int timeCounter = 0;
			if (pickit.getRemainingObj() == 0) {
				move.PTP("/_PickIt/Scan", 1);
				waitMillis(350);
				pickit.doScanForObj();
				//sleep();
			} else {
				pickit.doScanForObj();
			}
			while(!pickit.isReady()) {
				waitMillis(10);
				timeCounter += 10;
				if (timeCounter >= 6000) {
					padErr("Timeout is overdue, PickIt didn't answer");
					continue;
				}
			}
			if (pickit.hasFoundObj()) {
				//padLog(pickit.getPickFrame().toString());
				pick(pickit.getPickFrame());
				//pickit.doSendPickFrameData();
			}*/
			/*
			
			boxStatus = pickit.getBox(true);
			if (boxStatus > 0) {
				pickFrame = pickit.computePickPose();
				padLog(pickFrame.toString());
				move.setTCP(pickitGripper, "/Cylinder");
				padLog("The pick ID is " + pickit.getPickID());
				kiwa.move(ptp(pickFrame));
				//pick(pickFrame);
				sleep();
						switch (pickit.getObjType()) {
							case 1:
								move.PTP("/_PickIt/Pole_H/_4_Jig_approach_Z",1);
								move.LIN("/_PickIt/Pole_H/_5_Jig_pos",1);
								plc.openGripper();
								move.LIN("/_PickIt/Pole_H/_4_Jig_approach_Z",1);
								break;
							case 2:
								move.PTP("/_PickIt/Pole_H/_3_Approach_Z",1);
								move.LIN("/_PickIt/Pole_H",1);
								plc.openGripper();
								move.PTP("/_PickIt/Pole_H/_1_Approach_X",1);
								break;
							default: 
								break;
						}
						padLog("Placed model type " + pickit.getPickID());
						while (!pickit.isReady()) {
							waitMillis(100);
						}
						padLog("Placed model type " + pickit.getPickID());
					
					padLog("Placed model type " + pickit.getPickID());
				}*/
			}
			pickit.terminate();
	}
	
	private void pick(Frame targetFrame) {
		Frame postFrame = targetFrame.copyWithRedundancy();
		postFrame.setZ(postFrame.getZ() + 100);
		plc.openGripperAsync();
		move.setTCP(PickitGripper, "/Approach");
		move.PTP(targetFrame, 0.75);
		move.setTCP(PickitGripper, "/Cylinder");
		move.LIN(targetFrame, 0.25);
		plc.closeGripper();
		move.LIN(postFrame, 0.6);
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
