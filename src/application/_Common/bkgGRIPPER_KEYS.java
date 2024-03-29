package application._Common;

//import static EluxUtils.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import application.Cambrian._CambrianApp;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.uiModel.userKeys.*;
 
public class bkgGRIPPER_KEYS extends RoboticsAPICyclicBackgroundTask {
	@Inject private Plc_inputIOGroup 		plcin;
	@Inject private Plc_outputIOGroup 		plcout;
	@Inject private MediaFlangeIOGroup 		mfio;
	@Inject private xAPI_MF	mf = new xAPI_MF(mfio);
	@Inject private xAPI_PLC plc = new xAPI_PLC(mf, plcin, plcout);
	@Inject private xAPI_Pad pad = new xAPI_Pad(mf);
	
	@Override public void initialize() {
		initializeCyclic(0, 500, TimeUnit.MILLISECONDS,	CycleBehavior.BestEffort);
		configPadKeysGLOBAL();
		//padLog("App switcher started, access it pressing the key");
	}

	public void runCyclic() {
		
		
	}

	void configPadKeysGLOBAL() {
		IUserKeyListener padKeysListener = new IUserKeyListener() {
			@Override public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				switch (key.getSlot()) {
					case 0:  						// KEY - OPEN GRIPPER
						plc.openGripper();
						break;
					case 1:  						// KEY - CLOSE GRIPPER
						plc.closeGripper();
						break;
					case 2:
					try {
						getTaskManager().getTask(_CambrianApp.class).stopAllInstances();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
						break;
					case 3:
						break;
				}
			}
		};
		
		pad.keyBarSetup(padKeysListener, "GLOBAL", "OPEN gripper", "CLOSE gripper", "KILL CAMBRIAN", " ");
	}
	/*
	private void appSwitcher() {
		int promptAns = pad.question("Which program do you want to run?", "Cancel", "Sleep", "Home", "Tr1", "Tr2", "Tr3", "Tr4", "Tr5");
		padLog(promptAns);
		switch (promptAns) {
			case 0:
				break;
			case 1:
				halt();
				break;
			case 2:
				_Home appHome = new _Home();
				appHome.initialize();
				padLog("Home initialized");
				padLog("Home run");
				appHome.run();
				break;
			case 3:
//				Tr3_BasicMotions runApp3 = new Tr3_BasicMotions();
//				runApp3.initialize();
//				runApp3.run();
				break;
			default:
				break;
		}
	}
	*/
}
