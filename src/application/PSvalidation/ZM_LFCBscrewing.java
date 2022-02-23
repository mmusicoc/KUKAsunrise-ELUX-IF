package application.PSvalidation;

import static EluxUtils.Utils.*;
import static EluxUtils.UMath.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class ZM_LFCBscrewing extends RoboticsAPIApplication {
	@Inject	@Named("ZMScrewDriver") private Tool tool;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_MF	mf = elux.getMF();
	@Inject xAPI_Pad pad = elux.getPad();
	@Inject private xAPI_Move move = elux.getMove();
	
	double avgCT = 0;
	double CT = 0;
	int cycleCount = 0;
	String pr = "/PSvalidation/ZM_LFCBscrewing/";
	
	@Override public void initialize() {
		move.init(pr + "via5",				// Home path
				tool, "/ScrewBit",			// Tool, TCP
				1.0, 0.4,					// Relative speed and acceleration
				10.0, 5.0,					// Blending
				40.0, 0,					// Collision detection (Nm), auto release
				false);						// Logging
		
		if(!move.PTPhome(1, true)) stop();
		pad.info("Start now");
	}
	
	private void scan() {
		mf.blinkRGB("GB", 1000);
	}
	
	private void screw() {
		mf.blinkRGB("B", 2000);
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
		double speedTravel = 0.7;
		double speedApproach = 0.35;
		double speedRetract = 0.6;
		do {
			double startTimeStamp = getTimeStamp();
			move.PTP(pr + "via1L", speedTravel, false);
			//padLog("Scan hole 1");
			scan();
			move.LIN(pr + "via1L", speedTravel, true);
			move.LIN(pr + "viaL", speedApproach, false);
			//padLog("1st Screwing");
			screw();
			move.LIN(pr + "via1L", speedRetract, true);
			move.PTP(pr + "via3", speedTravel, false);
			//padLog("Scan hole 2");
			scan();
			move.LIN(pr + "via1R", speedTravel, true);
			move.LIN(pr + "viaR", speedApproach, false);
			//padLog("2nd Screwing");
			screw();
			move.LIN(pr + "via1R", speedRetract, true);
			move.PTP(pr + "via4", speedTravel, true);
			move.PTP(pr + "via5", speedTravel, false);
			//padLog("HOME");

			endCycle(startTimeStamp);
		} while (true);
	}
	
	private void stop() {
		padLog("Program stopped");
	}
}