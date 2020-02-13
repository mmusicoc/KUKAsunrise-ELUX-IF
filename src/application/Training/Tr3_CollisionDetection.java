package application.Training;

import static utils.Utils.*;
import utils.HandlerMFio;
import utils.HandlerMov;
import utils.HandlerPad;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class Tr3_CollisionDetection extends RoboticsAPIApplication {
	// Standard KUKA API objects
	@Inject private LBR 					kiwa;
	@Inject private MediaFlangeIOGroup 		mfio;
	@Inject	@Named("Pinza") private Tool 	Gripper;
	
	// Custom modularizing handler objects
	@Inject private HandlerMFio	mf = new HandlerMFio(mfio);
	@Inject private HandlerMov move = new HandlerMov(mf);
	@Inject private HandlerPad pad = new HandlerPad(mf);
	
	// Private properties - application variables
	private double relSpeed = 0.15;
	
	@Override public void initialize() {
		Gripper.attachTo(kiwa.getFlange());
		double maxTorque = pad.askTorque();
		move.setJTConds(maxTorque);
	}

	@Override public void run() {
		mf.setRGB("B");
		for (;;) {
			move.PTPwithJTConds("/_HOME/_1_Teach_LEFT", relSpeed);
			waitMillis(1500, true);
			move.PTPwithJTConds("/_HOME/_3_Teach_RIGHT", relSpeed);
			waitMillis(1500, true);
		}
	}
}