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
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.CommandInvalidException;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;

public class PickItApp extends RoboticsAPIApplication {
	private LBR kiwa;
	private HandlerPickIt pickit;
	
	
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	private MediaFlangeIOGroup 			mediaFlangeIOGroup;
	@Inject	@Named("PickItFlange") 		private Tool flange;
	@Inject	@Named("PickItGripper") 	private Tool gripper;
	
	// Custom modularizing handler objects
	@Inject private HandlerMFio	mf = new HandlerMFio(mediaFlangeIOGroup);
	@Inject private HandlerPLCio plc = new HandlerPLCio(mf, plcin, plcout);
	@Inject private HandlerMov move = new HandlerMov(mf);
	@Inject private HandlerPad pad = new HandlerPad(mf);
	
	@Override public void initialize() {
		move.setTCP(flange);
		pickit = new HandlerPickIt(move);
		pickit.init("192.168.2.12", 30001);
	}

	@Override public void run() {
		Frame pickFrame;
		padLog("Start picking sequence");
		if(!move.PTP("/_PickIt/Scan", 0.25)) sleep();
		if(!pickit.config(3, 2, "/_PickIt/Scan", 0.25, 350, 3000)) sleep();
		padLog("Here1");
		while (pickit.isRunning()) {
			padLog("Here");
			pickit.getBox(true);
			Transformation pickF = pickit.getPickFrame();
			padLog("Received next Pick-it object ...");
			sleep();
								
				if (pickit.hasFoundObj()) {
					Transformation pickit_pose = pickit.getPickFrame();
		        	Frame base_T_current_ee =  kiwa.getCurrentCartesianPosition(kiwa.getFlange()).copy();
					Frame pick_pose = pickit.computePickPose(base_T_current_ee, pickit_pose);
					Frame pre_pick_pose = pickit.computePrePickPose(pick_pose);
					pickit.doSendPickFrame();
					waitMillis(150);
					padLog("The pick ID is " + pickit.getPickID());
					try {
						kiwa.move(ptp(pre_pick_pose));
						kiwa.move(lin(pick_pose));
						plc.closeGripper();
						kiwa.move(lin(pre_pick_pose));
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
					} catch (CommandInvalidException e) {
						System.out.println("Unable to move to object, going to next detection ...");
					}
					padLog("Placed model type " + pickit.getPickID());
				}
			}
			pickit.terminate();
	}
	
	@Override public void dispose() { 
		pickit.terminate();
		super.dispose(); 
	}
}
