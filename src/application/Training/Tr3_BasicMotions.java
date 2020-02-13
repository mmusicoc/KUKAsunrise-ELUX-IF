package application.Training;

import static eluxLibs.Utils.*;
import eluxLibs.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Tool;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

public class Tr3_BasicMotions extends RoboticsAPIApplication {
	@Inject	private LBR 	kiwa;
	@Inject private MediaFlangeIOGroup 	mfio;
	@Inject private HandlerMFio	mf = new HandlerMFio(mfio);
	@Inject private HandlerMov move = new HandlerMov(mf);
	@Inject	@Named("Gripper") private Tool 	gripper;
	
	@Override public void initialize() {
		gripper.attachTo(kiwa.getFlange());
		move.setHome("/_HOME/_2_Teach_CENTRAL");
		move.setJTConds(10.0);
	}

	@Override public void run() {
		padLog("Move PTP home");
		double relSpeed = 0.2;
		kiwa.move(ptpHome().setJointVelocityRel(relSpeed));
		for (relSpeed = 0.2; relSpeed < 1 ; relSpeed += 0.195){
			padLog("Speed is " + relSpeed + "/1");
			move.PTPwithJTConds("/_HOME/_0_Shutoff_REST", relSpeed);
			waitMillis(1000, true);
			move.LINwithJTConds("/_HOME/_1_Teach_LEFT", relSpeed);
			waitMillis(1000, true);
			move.CIRCwithJTConds("/_HOME/_2_Teach_CENTRAL", "/_HOME/_3_Teach_RIGHT", relSpeed);
		}
		kiwa.move(ptpHome());
		padLog("Finished program");
		return;
	}
}