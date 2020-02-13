package application.Pickit;

import static eluxLibs.Utils.*;
import eluxLibs.*;

import javax.inject.Inject;
import javax.inject.Named;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class PickItCalib extends RoboticsAPIApplication {
	@Inject private MediaFlangeIOGroup 			mediaFlangeIOGroup;
	
	@Inject	@Named("PickItFlange") 		private Tool flange;
	
	// Custom modularizing handler objects
	@Inject private HandlerMFio	mf = new HandlerMFio(mediaFlangeIOGroup);
	@Inject private HandlerMov move = new HandlerMov(mf);
	@Inject private HandlerPickIt pickit = new HandlerPickIt(move);
	
	@Override public void initialize() {
		move.setTCP(flange);
		pickit.init("192.168.2.12", 30001);
	}

	@Override public void run() {
		try {
			calibrate();
		} catch (InterruptedException e) {
			padErr("Unable to perform requested callibration.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Calibration function, requires 5 calibration poses to be taught-in, named Calib_pose1 to Calib_pose5
	 * @throws InterruptedException
	 */
	public void calibrate() throws InterruptedException {
		final double calibSpeed = 1;
		move.setGlobalSpeed(1);
		padLog("Starting Multi Pose Calibration ... ");
		move.PTP("/_PickIt/Calib/P1", calibSpeed);
		padLog("Calib in P1");
		pickit.doCalibration();
		move.PTP("/_PickIt/Calib/P2", calibSpeed);
		padLog("Calib in P2");
		pickit.doCalibration();		
		move.PTP("/_PickIt/Calib/P3", calibSpeed);
		padLog("Calib in P3");
		pickit.doCalibration();
		move.PTP("/_PickIt/Calib/P4", calibSpeed);
		padLog("Calib in P4");
		pickit.doCalibration();
		move.PTP("/_PickIt/Calib/P5", calibSpeed);
		padLog("Calib in P5");
		pickit.doCalibration();
		move.PTP("/_PickIt/Calib", calibSpeed);
        padLog("Finished callibration");
	}
	
	@Override public void dispose() { 
		pickit.terminate();
		super.dispose(); 
	}
}
