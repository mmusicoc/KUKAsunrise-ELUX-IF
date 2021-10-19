package application.Training;

import static EluxAPI.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class Tr3_BasicMotions extends RoboticsAPIApplication {
	@Inject @Named("Gripper") private Tool flange;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_Move move = elux.getMove();
	
	@Override public void initialize() {
		move.setHome("/_HOME/_2_Teach_CENTRAL");
		move.setJTconds(10.0);
		move.setGlobalSpeed(1);
		move.setTool(flange);
	}

	@Override public void run() {
		double relSpeed;
		move.PTPhome(1, false);
		for (relSpeed = 0.2; relSpeed < 1 ; relSpeed += 0.195){
			padLog("Speed is " + relSpeed + "/1");
			move.PTP("/_HOME/_0_Shutoff_REST", relSpeed, false);
			waitMillis(1000, true);
			move.LIN("/_HOME/_1_Teach_LEFT", relSpeed, false);
			waitMillis(1000, true);
			move.CIRC("/_HOME/_2_Teach_CENTRAL", "/_HOME/_3_Teach_RIGHT", relSpeed, false);
		}
		move.PTPhome(1, false);
		padLog("Finished program");
		return;
	}
}