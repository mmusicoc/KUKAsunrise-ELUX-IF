package application.Training;

import static utils.Utils.*;
import javax.inject.Inject;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;

public class Tr2_FSM extends RoboticsAPIApplication {
	@Inject private LBR 				kiwa;
	@Inject private Plc_outputIOGroup 	plcout;
	@Inject private MediaFlangeIOGroup	mfio;
	
	private enum States {state_A, state_B, state_C, state_D};
	private States state; 
	
	@Override
	public void initialize() {
		System.out.println("Initializing..");
		kiwa.setHomePosition(getApplicationData().getFrame("/Rest"));
		state = States.state_A;
	}

	@Override
	public void run() {
		kiwa.move(ptpHome());
		while (true) {				// Endless loop
			switch (state) {
				case state_A:		// Close gripper, LED to Red, wait for button
					padLog("Current state : state A");
					setRGB("R");
					closeGripper();
					waitUserButton();
					state = States.state_B;
					break;
					
				case state_B:		// LED to Green, wait for button
					padLog("Current state : state B");
					setRGB("G");
					waitUserButton();
					state = States.state_C; 
					break;
					
				case state_C: 		// LED to Blue, wait for button
					padLog("Current state : state C");
					setRGB("B");
					waitUserButton();
					state = States.state_D; 
					break;
					
				case state_D:		// All LEDs on, open gripper, wait for button and loop back to state_A
					padLog("Current state : state D");
					setRGB("RGB");
					openGripper();
					waitUserButton();
					state = States.state_A; 
					break;
					
				default:
					break;
			}
		}
	}
	
	/**********************************************
	* CUSTOM METHODS BY mario.musico@electrolux.com <p>
	***********************************************/
	
	public void waitUserButton() {
		padLog("Press green button to continue");
		while (true) {
			if (mfio.getUserButton()) break;
			waitMillis(50);
		}
		waitMillis(500, true);		// Wait for torque to stabilize
	}
	
	public void setRGB(boolean r, boolean g, boolean b) {
		mfio.setLEDRed(r);
		mfio.setLEDGreen(g);
		mfio.setLEDBlue(b);
	}

	public void setRGB(String color) {
		padLog("MediaFlange LED ring to " + color);
		if (color.equalsIgnoreCase("R")) setRGB(true,false,false);
		else if (color.equalsIgnoreCase("G")) setRGB(false,true,false);
		else if (color.equalsIgnoreCase("B")) setRGB(false,false,true);
		else if (color.equalsIgnoreCase("RG")) setRGB(true,true,false);
		else if (color.equalsIgnoreCase("RB")) setRGB(true,false,true);
		else if (color.equalsIgnoreCase("GB")) setRGB(false,true,false);
		else if (color.equalsIgnoreCase("RGB")) setRGB(true,true,true);
		else if (color.equalsIgnoreCase("OFF")) setRGB(false,false,false);
		else System.out.println("MediaFlange color not valid");
	}
	
	public void openGripper() {
		plcout.setPinza_Chiudi(false);
		waitMillis(10);
		plcout.setPinza_Apri(true);
		padLog("Opening gripper");
		waitMillis(2000, true);
	}
	
	public void closeGripper() {
		plcout.setPinza_Apri(false);
		waitMillis(10);
		plcout.setPinza_Chiudi(true);
		padLog("Closing gripper");
		waitMillis(2000, true);
	}
}