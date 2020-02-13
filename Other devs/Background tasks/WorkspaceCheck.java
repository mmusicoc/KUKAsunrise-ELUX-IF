package backgroundTask;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;

public class WorkspaceCheck extends RoboticsAPICyclicBackgroundTask {
	@Inject	private Controller 				kukaController;
	@Inject private Plc_inputIOGroup 		plcin;
	@Inject private Plc_outputIOGroup 		plcout;
	@Inject private MediaFlangeIOGroup 		mfio;
	private Frame actPos;
	private LBR lbr;
	
	@Override
	public void initialize() {
		initializeCyclic(0, 500, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
		lbr = (LBR) getDevice(kukaController,"LBR_iiwa_14_R820_1");
	}

	@Override
	public void runCyclic() {
		/*
		try {
			actPos = lbr.getCurrentCartesianPosition(lbr.getFlange());
			if ((actPos.getX() < -340)&&(actPos.getX() > -350)){
				if ((actPos.getY() < 550)&&(actPos.getY() > 540)){
					if ((actPos.getZ() < 370)&&(actPos.getZ() > 330)){
						plcout.setRobot_InHome(true);
						mfio.setLEDBlue(true);
						mfio.setLEDRed(false);
					}
				}
			} else{
				plcout.setRobot_InHome(false);
				mfio.setLEDBlue(false);
			}
			
			if (plcin.getApp_Reset()){
			    try {
					getTaskManager().getTask(RobotApplication.class).stopAllInstances();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				plcout.setApp_ResetDone(true);
				while (plcin.getApp_Reset() == true){
					ThreadUtil.milliSleep(50);
				}
				plcout.setApp_ResetDone(false);	
			}
		} catch (Exception e) {					// TODO Auto-generated catch block
			e.printStackTrace();
			ThreadUtil.milliSleep(200);
		}
		*/
	}
}