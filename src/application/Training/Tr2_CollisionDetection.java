package application.Training;

import static EluxAPI.Utils.*;
import EluxAPI.*;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Tool;
import javax.inject.Inject;
import javax.inject.Named;

public class Tr2_CollisionDetection extends RoboticsAPIApplication {
	// Standard KUKA API objects
	@Inject private LBR 					kiwa;
	@Inject private MediaFlangeIOGroup 		mfio;
	@Inject	@Named("Gripper") private Tool 	gripper;
	
	// Custom modularizing handler objects
	@Inject private API_MF	mf = new API_MF(mfio);
	@Inject private API_Pad pad = new API_Pad(mf);
	@Inject private API_Movements move = new API_Movements(mf);
	
	// Private properties - application variables
	private double relSpeed = 0.15;
	
	@Override public void initialize() {
		gripper.attachTo(kiwa.getFlange());
		double maxTorque = pad.askTorque();
		move.setJTconds(maxTorque);
		move.setGlobalSpeed(1);
	}

	@Override public void run() {
		mf.setRGB("B");
		for (;;) {
			move.PTPsafe("/_HOME/_1_Teach_LEFT", relSpeed);
			waitMillis(1500, true);
			move.PTPsafe("/_HOME/_3_Teach_RIGHT", relSpeed);
			waitMillis(1500, true);
		}
	}
}