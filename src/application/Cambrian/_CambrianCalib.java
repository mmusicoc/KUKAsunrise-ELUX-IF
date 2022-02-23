package application.Cambrian;

import static EluxUtils.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class _CambrianCalib extends RoboticsAPIApplication {
	@Inject	@Named("Cambrian") private Tool tool;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_Move move = elux.getMove();
	@Inject private xAPI_Pad pad = elux.getPad();
	@Inject private CambrianAPI cambrian = new CambrianAPI(elux);
	
	@Override public void initialize() {
		// INIT MOVE ---------------------------------------------
		move.init("/_Cambrian/_Home",			// Home path
					tool, "/TCP",				// Tool, TCP
					1, 1.0,						// Relative speed and acceleration
					20.0, 5.0,					// Blending
					15.0, 0,					// Collision detection (Nm), release mode
					false);						// Logging
				
		cambrian.init("192.168.2.50", 4000);
	}

	@Override public void run() {
		try {
			if(pad.question("Are you sure you want to recalibrate?", "YES", "NO") == 0) {
				calibrate("/_Cambrian/Calib");		// Robot mounted camera
			} else dispose();	
		} catch (InterruptedException e) {
			padErr("Unable to perform requested callibration.");
			e.printStackTrace();
		}
		pad.info("Calibration finished.");
		cambrian.end();
	}
	
	public void calibrate(String basePath) throws InterruptedException {
		move.PTPhome(1, false);
		waitMillis(1000);
		padLog("Starting Multi Pose Calibration ... ");
		cambrian.startCalibration(); // <<<<<<<<<<<<<<<<<<<<<<
		for(int i = 1; i <= 14; i++) {
			move.PTP(basePath + "/P" + i, 1, false);
			//padLog("Calib in P" + i);
			cambrian.captureCalibration(); // <<<<<<<<<<<<<<<<<<<<<
			padLog("Finished calib P" + i);
		}
		
        padLog("Finished callibration");
	}
	
	@Override public void dispose() { 
		//cambrian.terminate();
		super.dispose(); 
	}
}
