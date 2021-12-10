package application.Cambrian;

import static EluxUtils.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class CambrianCalib extends RoboticsAPIApplication {
	@Inject	@Named("Cambrian") private Tool GripperCambrian;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_Move move = elux.getMove();
	@Inject private xAPI_Pad pad = elux.getPad();
	@Inject private CambrianAPI cambrian = new CambrianAPI(elux);
	
	@Override public void initialize() {
		move.setTool(GripperCambrian);
		move.setTCP("/TCP");
		move.setGlobalSpeed(1);
		move.setJTconds(15.0);
		move.setBlending(20, 5);
		move.setHome("/_Cambrian/_Home");
		cambrian.init("192.168.2.50", 4000);
	}

	@Override public void run() {
		try {
			calibrate("/_Cambrian/Calib");		// Robot mounted camera
		} catch (InterruptedException e) {
			padErr("Unable to perform requested callibration.");
			e.printStackTrace();
		}
		pad.info("Calibration finished.");
	}
	
	public void calibrate(String basePath) throws InterruptedException {
		move.PTPhome(1, false);
		waitMillis(1000);
		padLog("Starting Multi Pose Calibration ... ");
		cambrian.startCalibration(); // <<<<<<<<<<<<<<<<<<<<<<
		for(int i = 1; i <= 14; i++) {
			move.PTP(basePath + "/P" + i, 1, false);
			padLog("Calib in P" + i);
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
