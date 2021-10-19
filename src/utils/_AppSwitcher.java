package utils;

import static EluxAPI.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;

public class _AppSwitcher extends RoboticsAPIApplication {
	@Inject private MediaFlangeIOGroup	mfio;
	@Inject private xAPI_MF	mf = new xAPI_MF(mfio);
	
	private enum States {cancel, sleep, home, app1, app2, app3, app4, app5, app6};
	private States state;
	
	@Override 	public void initialize() { 
		padLog("App switcher started, access it pressing the key");
		
	}

	@Override
	public void run() {
		//padLog("Move PTP to HOME-REST position");
		mf.setRGB("G");
		switch (state) {
			case sleep:
				
		}
		
		mf.setRGB("OFF");
		padLog("Finished program.");
		return;
	}
}