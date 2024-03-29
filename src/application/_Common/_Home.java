package application._Common;

import static EluxUtils.Utils.*;
import EluxAPI.*;
import javax.inject.Inject;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

public class _Home extends RoboticsAPIApplication {
	@Inject	private LBR 				kiwa;
	@Inject private MediaFlangeIOGroup	mfio;
	@Inject private xAPI_MF	mf = new xAPI_MF(mfio);
			private double 				relSpeed;
			private int					promptAns;
			
	@Override public void initialize() {
		relSpeed = 0.25;
		promptAns = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, 
						"Where do you want to go? Collision detection is NOT enabled for this operation", 
						"Shutoff REST", "Teach LEFT", "Teach CENTRAL", "Teach RIGHT", "VERTICAL", "HORIZONTAL");
	}

	@Override public void run() {
		//padLog("Move PTP to HOME-REST position");
		mf.setRGB("G");
		switch (promptAns) {
			case 0:
				logmsg("Moving to Shutoff REST");
				kiwa.move(ptp( 	Math.toRadians(90),	// A1
								Math.toRadians(-36),// A2
								Math.toRadians(0),	// A3
								Math.toRadians(-90),// A4
								Math.toRadians(0),	// A5
								Math.toRadians(90),	// A6
								Math.toRadians(0))	// A6
							.setJointVelocityRel(relSpeed)); break;
			case 1:
				logmsg("Moving to Teach LEFT");
				kiwa.move(ptp( 	Math.toRadians(65),	// A1
								Math.toRadians(45),	// A2
								Math.toRadians(0),	// A3
								Math.toRadians(-75),// A4
								Math.toRadians(0),	// A5
								Math.toRadians(60),	// A6
								Math.toRadians(-25))// A6
							.setJointVelocityRel(relSpeed)); break;
			case 2:
				logmsg("Moving to Teach CENTRAL");
				kiwa.move(ptp( 	Math.toRadians(90),	// A1
								Math.toRadians(45),	// A2
								Math.toRadians(0),	// A3
								Math.toRadians(-45),// A4
								Math.toRadians(0),	// A5
								Math.toRadians(90),	// A6
								Math.toRadians(0))	// A6
							.setJointVelocityRel(relSpeed)); 
				break;
			case 3:
				logmsg("Moving to Teach RIGHT");
				kiwa.move(ptp( 	Math.toRadians(115),// A1
								Math.toRadians(45),	// A2
								Math.toRadians(0),	// A3
								Math.toRadians(-75),// A4
								Math.toRadians(0),	// A5
								Math.toRadians(60),	// A6
								Math.toRadians(25))	// A6
							.setJointVelocityRel(relSpeed)); break;
			case 4:
				logmsg("Moving to VERTICAL");
				kiwa.move(ptp( 	Math.toRadians(0),	// A1
								Math.toRadians(0),	// A2
								Math.toRadians(0),	// A3
								Math.toRadians(0),	// A4
								Math.toRadians(0),	// A5
								Math.toRadians(0),	// A6
								Math.toRadians(0))	// A6
							.setJointVelocityRel(relSpeed)); break;
			case 5:
				logmsg("Moving to HORIZONTAL");
				kiwa.move(ptp( 	Math.toRadians(90),	// A1
								Math.toRadians(90),	// A2
								Math.toRadians(0),	// A3
								Math.toRadians(0),	// A4
								Math.toRadians(0),	// A5
								Math.toRadians(0),	// A6
								Math.toRadians(-90))// A6
							.setJointVelocityRel(relSpeed)); break;
		}
		
		mf.setRGB("OFF");
		logmsg("Finished program.");
		return;
	}
}