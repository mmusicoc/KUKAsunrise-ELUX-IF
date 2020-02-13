package backgroundTask;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;

public class UserPLC extends RoboticsAPICyclicBackgroundTask {
	@Inject		private Controller			kukaController;
	@Inject 	private Plc_inputIOGroup 	plcin;
	@Inject 	private Plc_outputIOGroup 	plcout;
	@Inject 	private MediaFlangeIOGroup 	mfio;

	@Override
	public void initialize() {
		initializeCyclic(0, 500, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
	}

	@Override
	public void runCyclic() {
		/*
		try {
			if (plcout.getMission_Run() == false){
				plcout.setMission_IndexFBK(plcin.getMission_Index());
			}
			if (mfio.getOutputX3Pin1() == false){ 	//DataLock substitute
				ThreadUtil.milliSleep(20);
			}
			plcout.setLife_BitFBK(plcin.getLife_bit());
		} catch (Exception e) {						// TODO Auto-generated catch block
			e.printStackTrace();
			ThreadUtil.milliSleep(100);
		}
		try {
			if (plcin.getApp_Auto() == false){		// open and close gripper using Green button
				mfio.setLEDGreen(false);
				if (plcin.getPinza_Idle()){
					if (mfio.getUserButton()){
						plcout.setPinza_Chiudi(true);
						ThreadUtil.milliSleep(50);
						plcout.setPinza_Chiudi(false);
					}
				} else {
					if (mfio.getUserButton()){				
						plcout.setPinza_Apri(true);
						ThreadUtil.milliSleep(50);
						plcout.setPinza_Apri(false);
					}
				}
			}
			else {
				if(mfio.getLEDRed( == false){
					mfio.setLEDGreen(true);
				} else{
					mfio.setLEDGreen(false);
				}
			}
		} catch (Exception e) {						// TODO Auto-generated catch block
			e.printStackTrace();
			ThreadUtil.milliSleep(150);
		}
		*/
	}
}