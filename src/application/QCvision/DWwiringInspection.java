package application.QCvision;

import static EluxUtils.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class DWwiringInspection extends RoboticsAPIApplication {
	@Inject	@Named("GripperCognex") 	private Tool GripperCognex;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_PLC plc = elux.getPLC();
	@Inject private xAPI_Move move = elux.getMove();
	
	@Override public void initialize() {
		move.setMaxTorque(15.0);
		move.setGlobalSpeed(1);
		move.setBlending(20, 5);
		move.setTool(GripperCognex);
		move.setTCP("/POV");
		move.setHome("/_DWvision/_HOME");
		if(!move.PTPhome(1, true)) stop();
	}

	@Override public void run() {
		do {
			logmsg("-----Start picture acquisition-----");
			move.PTP("/_DWvision/P01_C28/Approach", 1, true);
			move.PTP("/_DWvision/P01_C28", 1, false);
			logmsg("Controls C28");
			takePicture();
			move.PTP("/_DWvision/P02_C7_C15/Approach", 1, true);
			move.PTP("/_DWvision/P02_C7_C15", 1, false);
			logmsg("Controls C7, C15");
			takePicture();
			move.PTP("/_DWvision/P03_C17A", 1, false);
			logmsg("Controls C17A");
			takePicture();
			move.PTP("/_DWvision/P04_C17B", 1, false);
			logmsg("Controls C17B");
			takePicture();
			move.PTP("/_DWvision/P05_C12_C14_C16", 1, false);
			logmsg("Controls C12, C14, C16");
			takePicture();
			move.PTP("/_DWvision/P06_C11_C13_C29", 1, false);
			logmsg("Controls C11, C13, C29");
			takePicture();
			move.LIN("/_DWvision/P07_C24", 1, false);
			logmsg("Controls C24");
			takePicture();
			move.PTP("/_DWvision/P08_C6_C9/Approach", 1, true);
			move.PTP("/_DWvision/P08_C6_C9", 1, false);
			logmsg("Controls C6, C9");
			takePicture();
			move.PTP("/_DWvision/P09_C2", 1, false);
			logmsg("Controls C2");
			takePicture();
			move.PTP("/_DWvision/P10_C5", 1, false);
			logmsg("Controls C5");
			takePicture();
			move.PTP("/_DWvision/P11_C8_C10", 1, false);
			logmsg("Controls C8, C10");
			takePicture();
			move.LIN("/_DWvision/P12_C4_lin", 1, false);
			logmsg("Controls C4");
			takePicture();
			move.PTP("/_DWvision/P13_C26", 1, false);
			logmsg("Controls C26");
			takePicture();
			move.PTP("/_DWvision/P14_C3", 1, false);
			logmsg("Controls C3");
			takePicture();
			move.PTP("/_DWvision/P15_C1", 1, false);
			logmsg("Controls C1");
			takePicture();
			move.PTP("/_DWvision/P16_C20/Approach", 1, true);	
			move.PTP("/_DWvision/P16_C20", 1, false);
			logmsg("Controls C20");
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
		logmsg("Program stopped");
		dispose();
	}
	
	@Override public void dispose() { 
		super.dispose(); 
	}
}
