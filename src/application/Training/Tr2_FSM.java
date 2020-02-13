package application.Training;

import static utils.Utils.*;
import utils.*;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import javax.inject.Inject;

public class Tr2_FSM extends RoboticsAPIApplication {
	// Standard KUKA API objects
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	@Inject private MediaFlangeIOGroup	mfio; 
	
	// Custom modularizing handler objects
	@Inject private HandlerMFio	mf = new HandlerMFio(mfio);
	@Inject private HandlerPLCio plc = new HandlerPLCio(mf, plcin, plcout);
	@Inject private HandlerMov move = new HandlerMov(mf);
	
	// Private properties - application variables
	private enum States {state_A, state_B, state_C, state_D};
	private States state;
	
	@Override public void initialize() {
		padLog("Initializing..");
		move.setHome("/_HOME/_2_Teach_CENTRAL");
		state = States.state_A;
	}

	@Override public void run() {
		move.PTPhome();
		while (true) {				// Endless loop
			switch (state) {
				case state_A:		// Close gripper, LED to Red, wait for button
					padLog("Current state : state A");
					mf.setRGB("R");
					plc.closeGripper();
					padLog("Gripper closed");
					mf.waitUserButton();
					state = States.state_B;
					break;
					
				case state_B:		// LED to Green, wait for button
					padLog("Current state : state B");
					mf.setRGB("G");
					mf.waitUserButton();
					state = States.state_C; 
					break;
					
				case state_C: 		// LED to Blue, wait for button
					padLog("Current state : state C");
					mf.setRGB("B");
					mf.waitUserButton();
					state = States.state_D; 
					break;
					
				case state_D:		// All LEDs on, open gripper, wait for button and loop back to state_A
					padLog("Current state : state D");
					mf.setRGB("RGB");
					plc.openGripper();
					padLog("Gripper opened");
					mf.waitUserButton();
					state = States.state_A; 
					break;
					
				default:
					break;
			}
		}
	}
}