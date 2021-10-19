package application.ZMsumpFixation;

import static EluxAPI.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class ZMsumpFixation extends RoboticsAPIApplication {
	@Inject	@Named("ZM_Screwer") private Tool ZM_Screwer;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_MF	mf = elux.getMF();
	@Inject private xAPI_Move move = elux.getMove();
	
	@Override public void initialize() {
		move.setJTconds(15.0);
		move.setGlobalSpeed(1);
		move.setBlending(10, 5);
		move.setTool(ZM_Screwer);
		move.setTCP("/Screwer_TCP");
		move.setHome("/ZMsumpFixation/home");
		if(!move.PTPhome(1, true)) stop();
	}

	@Override public void run() {
		double timestamp;
		double speedTravel = 0.8;
		double speedApproach = 0.5;
		double speedRetract = 0.7;
		do {
			timestamp = getTimeStamp();
			move.PTP("/ZMsumpFixation/via14", speedTravel, true);
			move.LIN("/ZMsumpFixation/via13", speedApproach, false);
			//padLog("1st Screwing");
			screw();
			move.LIN("/ZMsumpFixation/via14", speedRetract, true);
			move.PTP("/ZMsumpFixation/via16", speedTravel, true);
			move.LIN("/ZMsumpFixation/via15", speedApproach, false);
			//padLog("2nd Screwing");
			screw();
			move.LIN("/ZMsumpFixation/via16", speedRetract, true);
			move.PTP("/ZMsumpFixation/via18", speedTravel, true);
			move.LIN("/ZMsumpFixation/via17", speedApproach, false);
			//padLog("3rd Screwing");
			screw();
			move.LIN("/ZMsumpFixation/via18", speedRetract, true);
			move.PTP("/ZMsumpFixation/via20", speedTravel, true);
			move.LIN("/ZMsumpFixation/via19", speedApproach, false);
			//padLog("4th Screwing");
			screw();
			move.LIN("/ZMsumpFixation/via20", speedRetract, true);
			move.PTP("/ZMsumpFixation/via22", speedTravel, true);
			move.LIN("/ZMsumpFixation/pick",  speedApproach, false);
			//padLog("Pick");
			mf.blinkRGB("B", 300);
			move.LIN("/ZMsumpFixation/via22", speedRetract, true);
			move.PTP("/ZMsumpFixation/via21", speedTravel, true);
			move.LIN("/ZMsumpFixation/place", speedApproach, false);
			//padLog("5th Screwing");
			screw();
			waitMillis(300);
			move.LIN("/ZMsumpFixation/via23", speedRetract, true);
			move.LIN("/ZMsumpFixation/via25", speedTravel, true);
			move.LIN("/ZMsumpFixation/via24", speedApproach, false);
			//padLog("6th Screwing");
			screw();
			move.LIN("/ZMsumpFixation/via25", speedRetract, true);
			move.PTP("/ZMsumpFixation/via26", speedTravel, false); // Return home
			//padLog("HOME");
			timestamp = (getTimeStamp() - timestamp) / 1000;
			padLog(timestamp);
			mf.blinkRGB("RG", 5000);
		} while (true);
	}
	
	private void screw() {
		mf.setRGB("B");
		waitMillis(2000);
	}

	/*private void takePicture() {
		waitMillis(100);
		do {
			waitMillis(10);
		} while (!plc.gripperIsIdle());
		
		plc.closeGripperAsync();
		plc.openGripperAsync();
		
		do {
			waitMillis(10);
		} while (plc.gripperIsHolding());
	}*/
	
	private void stop() {
		padLog("Program stopped");
		dispose();
	}
	
	@Override public void dispose() { 
		super.dispose(); 
	}
}
