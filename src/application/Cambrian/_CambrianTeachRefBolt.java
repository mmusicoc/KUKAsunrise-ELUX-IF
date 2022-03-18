package application.Cambrian;

import static EluxUtils.Utils.*;
import static EluxUtils.UMath.*;
import EluxAPI.*;
import EluxLogger.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.persistenceModel.IPersistenceEngine;
import com.kuka.roboticsAPI.persistenceModel.XmlApplicationDataSource;

public class _CambrianTeachRefBolt extends RoboticsAPIApplication {
	
	@Inject	@Named("Cambrian") private Tool tool;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_Move move = elux.getMove();
	@Inject private xAPI_Pad pad = elux.getPad();
	@Inject private CambrianAPI cambrian = new CambrianAPI(elux);

	String cambrianModel = "Elux_fridge_ref_bolt";
	String RCP = "F7";
	
	@Override public void initialize() {
		move.init("/_Cambrian/_HomeLB",		// Home path
				tool, "/TCP",				// Tool, TCP
				1.0, 1.0,					// Relative speed and acceleration
				20.0, 5.0,					// Blending
				15.0, 0,					// Collision detection (Nm), release mode
				false);						// Logging
		move.PTPhome(1, false);
		cambrian.init(new ProLogger());
	}

	@Override public void run() {
		move.PTP("/_Cambrian/_RBSP", 1, false);
		if(cambrian.doScan(cambrianModel) > 0) {			
			updateFrame("/_Cambrian/Recipes/" + RCP + "/_RefBolt", cambrian.getPredictFrames().getFirst());
			pad.info("Bolt location frame has been updated");
		}
		else pad.info("Cambrian didnd't provide a prediction!");
		cambrian.end();
		logmsg("Ending app, put on T1 and deselect app");
		//getApplicationControl().halt();
	}
	
	public void updateFrame(String path, Frame newFrame) {
		Frame rebasedFrame = newFrame.copyWithRedundancy(move.p2f(path).getParent());
		logmsg("The bolt is located at: " + rf2s(rebasedFrame, false, false));
		final IPersistenceEngine engine = this.getContext().getEngine(IPersistenceEngine.class);
		final XmlApplicationDataSource framesData = (XmlApplicationDataSource) engine.getDefaultDataSource();
		framesData.open();
		framesData.changeFrameTransformation(getApplicationData().getFrame(path),
											rebasedFrame.getTransformationFromParent());
		framesData.save();
		framesData.saveFile();
		logmsg("Updated app data");
	}
}