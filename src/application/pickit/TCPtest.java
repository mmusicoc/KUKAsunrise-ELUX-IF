package application.Pickit;

import eluxLibs.*;

import javax.inject.Inject;
import javax.inject.Named;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class TCPtest extends RoboticsAPIApplication {
	@Inject private MediaFlangeIOGroup 			mediaFlangeIOGroup;
	
	@Inject @Named("PickItFlange") private Tool flange;
	@Inject @Named("PickItGripper") private Tool gripper;
	
	// Custom modularizing handler objects
	@Inject private HandlerMFio	mf = new HandlerMFio(mediaFlangeIOGroup);
	@Inject private HandlerMov move = new HandlerMov(mf);
	
	@Override public void initialize() {
		move.setHome("/_HOME/_2_Teach_CENTRAL");
		move.setJTConds(10.0);
		move.setGlobalSpeed(1);
		move.setTCP(flange);
		move.PTPHOME(0.5);
		move.setTCP(gripper);
	}

	@Override public void run() {
		move.LIN("/_PickIt/TestTCP/Left", 0.5);
		move.LIN("/_PickIt/TestTCP/Right", 0.5);
		move.LIN("/_PickIt/TestTCP", 0.5);
	}
}
