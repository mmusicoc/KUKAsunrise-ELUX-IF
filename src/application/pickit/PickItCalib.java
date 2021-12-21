package application.PickIt;

import static EluxUtils.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class PickItCalib extends RoboticsAPIApplication {
	@Inject	@Named("GripperPickit") 		private Tool GripperPickit;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_Move move = elux.getMove();
	@Inject private xAPI_Pad pad = elux.getPad();
	@Inject private xAPI_PickIt pickit = new xAPI_PickIt(elux.getRobot());
	
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
