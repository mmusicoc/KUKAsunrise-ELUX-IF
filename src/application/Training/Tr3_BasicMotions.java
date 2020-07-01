package application.Training;

import static eluxLibs.Utils.*;
import eluxLibs.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class Tr3_BasicMotions extends RoboticsAPIApplication {
	@Inject private MediaFlangeIOGroup 	mfio;
	
	@Inject @Named("PickItFlange") private Tool flange;
	
	// Custom modularizing handler objects
	@Inject private HandlerMFio	mf = new HandlerMFio(mfio);
	@Inject private HandlerMov move = new HandlerMov(mf);
	
	
	@Override public void initialize() {
		move.setHome("/_HOME/_2_Teach_CENTRAL");
		move.setJTConds(10.0);
		move.setGlobalSpeed(1);
		move.setTCP(flange);
	}

	@Override public void run() {
		double relSpeed;
		move.PTPHOMEsafe();
		for (relSpeed = 0.2; relSpeed < 1 ; relSpeed += 0.195){
			padLog("Speed is " + relSpeed + "/1");
			move.PTPsafe("/_HOME/_0_Shutoff_REST", relSpeed);
			waitMillis(1000, true);
			move.LINsafe("/_HOME/_1_Teach_LEFT", relSpeed);
			waitMillis(1000, true);
			move.CIRCsafe("/_HOME/_2_Teach_CENTRAL", "/_HOME/_3_Teach_RIGHT", relSpeed);
		}
		move.PTPHOMEsafe();
		padLog("Finished program");
		return;
	}
}