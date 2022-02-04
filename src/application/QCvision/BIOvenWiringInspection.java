package application.QCvision;

import static EluxUtils.UMath.d2s;
import static EluxUtils.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class BIOvenWiringInspection extends RoboticsAPIApplication {
	@Inject	@Named("IDScamera") 	private Tool tool;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_MF	mf = elux.getMF();
	@Inject xAPI_Pad pad = elux.getPad();
	@Inject private xAPI_Move move = elux.getMove();
	
	double avgCT = 0;
	double CT = 0;
	int cycleCount = 0;
	String pr = "/_QCvision/OvenWI/";		// PathRoot: parent frame of all app points
	
	@Override public void initialize() {
		move.init(pr + "HOME",				// Home path
				tool, "/OvenWI",			// Tool, TCP
				1.0, 0.4,					// Relative speed and acceleration
				10.0, 5.0,					// Blending
				40.0, true,					// Collision detection (Nm), auto release
				false);						// Logging
		
		if(!move.PTPhome(1, true)) stop();
		pad.info("Start now");	// Banner to manually trigger app start after homing
	}
	
	private void takePicture() {
		waitMillis(100);
		pad.info("Scan");
		mf.blinkRGB("B", 1500);
	}
	
	private void endCycle(double prevTimeStamp) {
		cycleCount++;
		CT = (getTimeStamp() - prevTimeStamp) / 1000;
		if (cycleCount > 1) avgCT = (avgCT * (cycleCount - 1) + CT) / cycleCount;
		else avgCT = CT;
		padLog("Cycles: " + cycleCount + ", Last CT: " + d2s(CT) + ", Avg CT: " + d2s(avgCT));
		mf.blinkRGB("RG", 4500);
	}

	@Override public void run() {
		do {
			double startTimeStamp = getTimeStamp();
			padLog("-----Start picture acquisition-----");
			move.PTP(pr + "pointname", 1, true);
			move.PTP("/_DWvision/P01_C28", 1, false);
			padLog("Controls C28");
			takePicture();
			move.PTP("/_DWvision/P02_C7_C15/Approach", 1, true);
			move.PTP("/_DWvision/P02_C7_C15", 1, false);
			padLog("Controls C7, C15");
			takePicture();
			move.PTP("/_DWvision/P03_C17A", 1, false);
			padLog("Controls C17A");
			takePicture();
			move.PTP("/_DWvision/P04_C17B", 1, false);
			padLog("Controls C17B");
			takePicture();
			move.PTP("/_DWvision/P05_C12_C14_C16", 1, false);
			padLog("Controls C12, C14, C16");
			takePicture();
			move.PTP("/_DWvision/P06_C11_C13_C29", 1, false);
			padLog("Controls C11, C13, C29");
			takePicture();
			move.LIN("/_DWvision/P07_C24", 1, false);
			padLog("Controls C24");
			takePicture();
			move.PTP("/_DWvision/P08_C6_C9/Approach", 1, true);
			move.PTP("/_DWvision/P08_C6_C9", 1, false);
			padLog("Controls C6, C9");
			takePicture();
			move.PTP("/_DWvision/P09_C2", 1, false);
			padLog("Controls C2");
			takePicture();
			move.PTP("/_DWvision/P10_C5", 1, false);
			padLog("Controls C5");
			takePicture();
			move.PTP("/_DWvision/P11_C8_C10", 1, false);
			padLog("Controls C8, C10");
			takePicture();
			move.LIN("/_DWvision/P12_C4_lin", 1, false);
			padLog("Controls C4");
			takePicture();
			move.PTP("/_DWvision/P13_C26", 1, false);
			padLog("Controls C26");
			takePicture();
			move.PTP("/_DWvision/P14_C3", 1, false);
			padLog("Controls C3");
			takePicture();
			move.PTP("/_DWvision/P15_C1", 1, false);
			padLog("Controls C1");
			takePicture();
			move.PTP("/_DWvision/P16_C20/Approach", 1, true);	
			move.PTP("/_DWvision/P16_C20", 1, false);
			padLog("Controls C20");
			takePicture();
			move.PTP("/_DWvision/_HOME", 1, false);
			
			endCycle(startTimeStamp);
		} while (true);
	}

	private void stop() {
		padLog("Program stopped");
		dispose();
	}
	
	@Override public void dispose() { 
		super.dispose(); 
	}
}
