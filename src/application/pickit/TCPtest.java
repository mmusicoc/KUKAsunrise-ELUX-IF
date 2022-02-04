package application.PickIt;

import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class TCPtest extends RoboticsAPIApplication {
	@Inject @Named("GripperPickit") private Tool GripperPickit;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_Move move = elux.getMove();
	
	@Override public void initialize() {
		move.setHome("/_HOME/_2_Teach_CENTRAL");
		move.setMaxTorque(10.0);
		move.setGlobalSpeed(1);
		move.setTool(GripperPickit);
		move.setTCP("/Flange");
		move.PTPhome(0.5, false);
		move.setTCP("/Cylinder");
	}

	@Override public void run() {
		move.setTCP("/Cylinder");
		move.LIN("/_UTILS/TestTCP/Left", 1, false);
		move.LIN("/_UTILS/TestTCP/Right", 1, false);
		move.LIN("/_UTILS/TestTCP", 1, false);
		move.setTCP("/Approach");
		move.LIN("/_UTILS/TestTCP", 1, false);
		move.LIN("/_UTILS/TestTCP/Left", 1, false);
		move.LIN("/_UTILS/TestTCP/Right", 1, false);
		move.LIN("/_UTILS/TestTCP", 1, false);
	}
}
