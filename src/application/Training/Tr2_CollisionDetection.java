package application.Training;

import static EluxUtils.Utils.*;
import EluxAPI.*;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;
import javax.inject.Inject;
import javax.inject.Named;

public class Tr2_CollisionDetection extends RoboticsAPIApplication {
	@Inject	@Named("Gripper") private Tool gripper;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_MF	mf = elux.getMF();
	@Inject private xAPI_Pad pad = elux.getPad();
	@Inject private xAPI_Move move = elux.getMove();
	
	// Private properties - application variables
	private double relSpeed = 0.15;
	
	@Override public void initialize() {
		move.setTool(gripper);
		double maxTorque = pad.askTorque();
		move.setJTconds(maxTorque);
		move.setGlobalSpeed(1);
	}

	@Override public void run() {
		mf.setRGB("B");
		for (;;) {
			move.PTP("/_HOME/_1_Teach_LEFT", relSpeed, false);
			waitMillis(1500, true);
			move.PTP("/_HOME/_3_Teach_RIGHT", relSpeed, false);
			waitMillis(1500, true);
		}
	}
}