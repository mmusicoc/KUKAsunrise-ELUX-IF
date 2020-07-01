package utils;

import eluxLibs.*;

import javax.inject.Inject;
import javax.inject.Named;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class TCPtest extends RoboticsAPIApplication {
	@Inject private MediaFlangeIOGroup 			mediaFlangeIOGroup;
	@Inject @Named("PickitGripper") private Tool pickitGripper;
	
	// Custom modularizing handler objects
	@Inject private HandlerMFio	mf = new HandlerMFio(mediaFlangeIOGroup);
	@Inject private HandlerMov move = new HandlerMov(mf);
	
	@Override public void initialize() {
		move.setHome("/_HOME/_2_Teach_CENTRAL");
		move.setJTConds(10.0);
		move.setGlobalSpeed(1);
		move.setTCP(pickitGripper, "/Flange");
		move.PTPhome(0.5);
		move.setTCP(pickitGripper, "/Cylinder");
	}

	@Override public void run() {
		move.setTCP(pickitGripper, "/Cylinder");
		move.LIN("/_UTILS/TestTCP/Left", 1, false);
		move.LIN("/_UTILS/TestTCP/Right", 1, false);
		move.LIN("/_UTILS/TestTCP", 1, false);
		move.setTCP(pickitGripper, "/Approach");
		move.LIN("/_UTILS/TestTCP", 1, false);
		move.LIN("/_UTILS/TestTCP/Left", 1, false);
		move.LIN("/_UTILS/TestTCP/Right", 1, false);
		move.LIN("/_UTILS/TestTCP", 1, false);
	}
}
