package application.DWvision;

import static EluxAPI.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class DWvisionPOC extends RoboticsAPIApplication {
	
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	@Inject private MediaFlangeIOGroup 			mediaFlangeIOGroup;
	@Inject	@Named("GripperCognex") 	private Tool GripperCognex;
	
	// Custom modularizing handler objects
	@Inject private API_MF	mf = new API_MF(mediaFlangeIOGroup);
	@Inject private API_PLC plc = new API_PLC(mf, plcin, plcout);
	@Inject private API_Movements move = new API_Movements(mf);
	//@Inject private HandlerPad pad = new HandlerPad(mf);
	
	@Override public void initialize() {
		move.setJTconds(15.0);
		move.setGlobalSpeed(1);
		move.setBlending(20, 5);
		move.setTool(GripperCognex);
		move.setTCP("/POV");
		move.setHome("/_DWvision/_HOME");
		if(!move.PTPhome(1, true)) stop();
	}

	@Override public void run() {
		do {
			padLog("-----Start picture acquisition-----");
			move.PTP("/_DWvision/P01_C28/Approach", 1, true);
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
		} while (true);
	}

	private void takePicture() {
		waitMillis(100);
		do {
			waitMillis(10);
		} while (!plc.gripperIsIdle());
		
		plc.closeGripperAsync();
		plc.openGripperAsync();
		
		do {
			waitMillis(10);
		} while (plc.gripperIsHolding());
	}
	
	private void stop() {
		padLog("Program stopped");
		dispose();
	}
	
	@Override public void dispose() { 
		super.dispose(); 
	}
}
