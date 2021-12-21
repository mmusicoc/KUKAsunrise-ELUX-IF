package application.Training;

import static EluxUtils.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;

public class Tr1_FSM extends RoboticsAPIApplication {
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_MF	mf = elux.getMF();
	@Inject private xAPI_PLC plc = elux.getPLC();
	@Inject private xAPI_Move move = elux.getMove();
	
	// Private properties - application variables
	private enum States {state_A, state_B, state_C, state_D};
	private States state;
	
	@Override public void initialize() {
		padLog("Initializing..");
		move.setHome("/_HOME/_2_Teach_CENTRAL");
		state = States.state_A;
		move.setGlobalSpeed(0.25);
		move.setJTconds(10.0);
	}

	@Override public void run() {
		move.PTP("/_HOME/_2_Teach_CENTRAL",1, false);
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