package application.PickIt;

import static EluxAPI.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class PickItCalib extends RoboticsAPIApplication {
	private LBR kiwa;
	@Inject private MediaFlangeIOGroup 			mediaFlangeIOGroup;
	
	@Inject	@Named("GripperPickit") 		private Tool GripperPickit;
	
	// Custom modularizing handler objects
	@Inject private API_MF	mf = new API_MF(mediaFlangeIOGroup);
	@Inject private API_Movements move = new API_Movements(mf);
	@Inject private API_Pad pad = new API_Pad(mf);
	@Inject private API_PickIt pickit = new API_PickIt(kiwa);
	
	@Override public void initialize() {
		move.setTool(GripperPickit);
		move.setTCP("/Flange");
		pickit.init("192.168.2.12", 30001);
	}

	@Override public void run() {
		int ans = 0;
		try {
			ans = pad.question("Where is the camera mounted?", "Robot flange", "Static pole");
			if (ans == 0) 	calibrate("/_PickIt/Calib/Robot");		// Robot mounted camera
			else 			calibrate("/_PickIt/Calib/Static");		// Static mounted camera
		} catch (InterruptedException e) {
			padErr("Unable to perform requested callibration.");
			e.printStackTrace();
		}
	}
	
	/**
	 * Calibration function, requires 5 calibration poses to be taught-in, named Calib_pose1 to Calib_pose5
	 * @throws InterruptedException
	 */
	public void calibrate(String basePath) throws InterruptedException {
		move.setGlobalSpeed(1);
		move.PTP(basePath, 1, false);
		waitMillis(1000);
		padLog("Starting Multi Pose Calibration ... ");
		move.PTP(basePath + "/P1", 1, false);
		padLog("Calib in P1");
		pickit.doCalibration();
		move.PTP(basePath + "/P2", 1, false);
		padLog("Calib in P2");
		pickit.doCalibration();		
		move.PTP(basePath + "/P3", 1, false);
		padLog("Calib in P3");
		pickit.doCalibration();
		move.PTP(basePath + "/P4", 1, false);
		padLog("Calib in P4");
		pickit.doCalibration();
		move.PTP(basePath + "/P5", 1, false);
		padLog("Calib in P5");
		pickit.doCalibration();
		move.PTP(basePath, 1, false);
        padLog("Finished callibration");
	}
	
	@Override public void dispose() { 
		pickit.terminate();
		super.dispose(); 
	}
}
