package application.PSvalidation;

import static EluxUtils.Utils.*;
import static EluxUtils.UMath.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class ZMsumpFixation extends RoboticsAPIApplication {
	@Inject	@Named("ZMSumpFixation") private Tool tool;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_MF	mf = elux.getMF();
	@Inject private xAPI_Move move = elux.getMove();
	
	double avgCT = 0;
	double CT = 0;
	int cycleCount = 0;
	
	String pr = "/PSvalidation/ZMsumpFixation/";
	
	@Override public void initialize() {
		
		move.init(pr + "home",				// Home path
				tool, "/ScrewerTCP",			// Tool, TCP
				1.0, 1.0,					// Relative speed and acceleration
				10.0, 5.0,					// Blending
				40.0, 0,					// Collision detection (Nm), release
				false);						// Loggingmove.setJTconds(40);
		if(!move.PTPhome(1, true)) stop();
	}
	
	private void screw() {
		mf.setRGB("B");
		waitMillis(1500);
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
		double speedTravel = 0.8;
		double speedApproach = 0.5;
		double speedRetract = 0.7;
		do {
			double startTimeStamp = getTimeStamp();
			move.PTP(pr + "via14", speedTravel, true);
			move.LIN(pr + "via13", speedApproach, false);
			//padLog("1st Screwing");
			screw();
			move.LIN(pr + "via14", speedRetract, true);
			move.PTP(pr + "via16", speedTravel, true);
			move.LIN(pr + "via15", speedApproach, false);
			//padLog("2nd Screwing");
			screw();
			move.LIN(pr + "via16", speedRetract, true);
			move.PTP(pr + "via18", speedTravel, true);
			move.LIN(pr + "via17", speedApproach, false);
			//padLog("3rd Screwing");
			screw();
			move.LIN(pr + "via18", speedRetract, true);
			move.PTP(pr + "via20", speedTravel, true);
			move.LIN(pr + "via19", speedApproach, false);
			//padLog("4th Screwing");
			screw();
			move.LIN(pr + "via20", speedRetract, true);
			move.PTP(pr + "via22", speedTravel, true);
			move.LIN(pr + "pick",  speedApproach, false);
			//padLog("Pick");
			mf.blinkRGB("B", 300);
			move.LIN(pr + "via22", speedRetract, true);
			move.PTP(pr + "via21", speedTravel, true);
			move.LIN(pr + "place", speedApproach, false);
			//padLog("5th Screwing");
			screw();
			waitMillis(300);
			move.LIN(pr + "via23", speedRetract, true);
			move.LIN(pr + "via25", speedTravel, true);
			move.LIN(pr + "via24", speedApproach, false);
			//padLog("6th Screwing");
			screw();
			move.LIN(pr + "via25", speedRetract, true);
			move.PTP(pr + "via26", speedTravel, false); // Return home
			//padLog("HOME");
			
			endCycle(startTimeStamp);
		} while (true);
	}
	
	private void stop() {
		padLog("Program stopped");
	}
}