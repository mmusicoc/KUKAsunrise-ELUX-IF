package application.PickIt;

import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class TCPtest extends RoboticsAPIApplication {
	@Inject private MediaFlangeIOGroup 			mediaFlangeIOGroup;
	@Inject @Named("GripperPickit") private Tool GripperPickit;
	
	// Custom modularizing handler objects
	@Inject private API_MF	mf = new API_MF(mediaFlangeIOGroup);
	@Inject private API_Movements move = new API_Movements(mf);
	
	@Override public void initialize() {
		move.setHome("/_HOME/_2_Teach_CENTRAL");
		move.setJTconds(10.0);
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
