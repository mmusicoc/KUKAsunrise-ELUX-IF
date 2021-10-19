package application.Cambrian;

import static EluxAPI.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class CambrianCalib extends RoboticsAPIApplication {
	private LBR kiwa;
	@Inject private MediaFlangeIOGroup 			mediaFlangeIOGroup;
	
	@Inject	@Named("Cambrian") 		private Tool GripperCambrian;
	
	// Custom modularizing handler objects
	@Inject private API_MF	mf = new API_MF(mediaFlangeIOGroup);
	@Inject private API_Movements move = new API_Movements(mf);
//	@Inject private API_Pad pad = new API_Pad(mf);
	@Inject private API_Cambrian cambrian = new API_Cambrian(kiwa);
	
	@Override public void initialize() {
		move.setTool(GripperCambrian);
		move.setTCP("/TCP");
		move.setGlobalSpeed(1);
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
		while(true);
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
