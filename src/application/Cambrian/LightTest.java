package application.Cambrian;

import static EluxUtils.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class LightTest extends RoboticsAPIApplication {
	
	@Inject	@Named("Cambrian") private Tool tool;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_Move move = elux.getMove();
	@Inject xAPI_MF	mf = elux.getMF();

	String cambrianModel = "Elux_fridge_ref_bolt";
	
	@Override public void initialize() {
		move.init("/_Cambrian/_HomeLB",		// Home path
				tool, "/TCP",				// Tool, TCP
				1.0, 1.0,					// Relative speed and acceleration
				20.0, 5.0,					// Blending
				15.0, 0,					// Collision detection (Nm), release mode
				false);						// Logging
		move.PTPhome(1, false);
	}

	@Override public void run() {
		move.PTP("/_Cambrian/_HomeLB2", 1, false);
		padLog("LB2");
		mf.blinkRGB("G", 500);
		move.PTP("/_Cambrian/_HomeLB", 1, false);
		padLog("LB");
		mf.blinkRGB("R", 500);
	}
}
