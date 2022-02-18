package application._Common;

import static EluxUtils.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
//import com.kuka.roboticsAPI.uiModel.userKeys.*;

public class TestIO extends RoboticsAPIApplication {
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject xAPI_MF	mf = elux.getMF();
//	@Inject private xAPI_Pad pad = elux.getPad();
//	@Inject private xAPI_PLC plc = elux.getPLC();
	
	boolean DI[] = {false, false, false, false};
	
	@Override public void initialize() {
		mf.setRGB("OFF");
	}

	@Override public void run() {
		while(true) {
	//		if (scanInputs()) mf.blinkRGB("G", 1000);
			waitMillis(10);
		}
	}
	/*
	public boolean scanInputs() {
		boolean aux = false;
		boolean change = false;
		
		aux = plc.getDIF10();
		if(aux != DI[0]) { padLog("DI 1.0 is now " + aux); DI[0] = aux; change = true; }
		aux = plc.getDIF11();
		if(aux != DI[1]) { padLog("DI 1.1 is now " + aux); DI[1] = aux; change = true; }
		aux = plc.getDIF12();
		if(aux != DI[2]) { padLog("DI 1.2 is now " + aux); DI[2] = aux; change = true; }
		aux = plc.getDIF13();
		if(aux != DI[3]) { padLog("DI 1.3 is now " + aux); DI[3] = aux; change = true; }
		
		return change;
	}
	
	public void configPadKeys() { // BUTTONS --------------------------------------------------------
		IUserKeyListener padKeysListener1 = new IUserKeyListener() {
			@Override public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (event == UserKeyEvent.KeyDown) {
					switch (key.getSlot()) {
						case 0:  						// KEY - TOGGLE 0.4
							plc.setDO04(!plc.getDO04());
							padLog("Toggled Output 0.4 to " + plc.getDO04());
							break;
						case 1:  						// KEY - TOGGLE 0.5
							plc.setDO05(!plc.getDO05());
							padLog("Toggled Output 0.5 to " + plc.getDO05());
							break;
						case 2:  						// KEY - TRIGGER 0.6
							plc.trigDO06(10);
							padLog("Triggered Output 0.6 for 10ms");
							break;
						case 3:  						// KEY - TRIGGER 0.7
							plc.trigDO07(10);
							padLog("Triggered Output 0.7 for 10ms");
							break;
					}
				}
			}
		};
		pad.keyBarSetup(padKeysListener1, "DO", "<> 0.4", "<> 0.5", "T 0.6", "T 0.7");
	}
	
	*/
}