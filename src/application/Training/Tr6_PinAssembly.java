package application.Training;

import static EluxAPI.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyEvent;

public class Tr6_PinAssembly extends RoboticsAPIApplication {
	// #Define parameters
	private static final boolean log1 = false;	// Log level 1: main events
	
	// Standard KUKA API objects
	@Inject private LBR 				kiwa;
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	@Inject private MediaFlangeIOGroup 	mfio;
	@Inject	@Named("Gripper") 		private Tool 		gripper;
	
	// Custom modularizing handler objects
	@Inject private API_MF	mf = new API_MF(mfio);
	@Inject private API_Pad pad = new API_Pad(mf);
	@Inject private API_PLC plc = new API_PLC(mf, plcin, plcout);
	@Inject private API_Movements move = new API_Movements(mf);
	@Inject private API_CobotMacros cobot = new API_CobotMacros(mf, plc, move);
	
	// Private properties - application variables
	private FrameList frameList = new FrameList();
	private enum States {home, teach, loop};
	private States state;
	private double relSpeed = 0.25;
	private static final double approachOffset = 40;
	private static final double approachSpeed = 0.1;
	private static final double probeSpeed = 0.1;
	
	private void progInfo() {
		pad.info("Description of this program operation:\n" + 
					"\tRun mode:\n" +
						"\t\tLoop back and forward along recorded frame list\n" +
						"\t\tPress TEACH Key to return to teach mode\n" +
						"\t\tDefault relSpeed = 0.25\n" +
						"\t\tDefault maxTorque = 10.0 Nm");
	}
	
	@Override public void initialize() {
		progInfo();
		gripper.attachTo(kiwa.getFlange());
		configPadKeysGENERAL();
		state = States.home;
		move.setHome("/_PinAssembly/PrePick");
		move.setGlobalSpeed(0.25);
		move.setJTconds(10.0);
	}

	@Override public void run() {
		while (true) {
			switch (state) {
				case home:
					move.PTPhomeCobot();
					cobot.checkGripper();
					state = States.loop;
					break;
				case teach:
					frameList.free();
					//teachRoutine(); 
					state = States.loop;
					break;
				case loop:
					loopRoutine();
					break;
			}
		}
	}
	
	private void loopRoutine(){
		if (log1) padLog("Loop routine.");
		pickPinZ("/_PinAssembly/PrePick/Pick1");
		move.PTPsafe("/_PinAssembly/PrePlace", relSpeed);
		placePinY("/_PinAssembly/PrePlace/Place1");
		move.PTPsafe("/_PinAssembly/PrePlace", relSpeed);
		pickPinZ("/_PinAssembly/PrePick/Pick2");
		move.PTPsafe("/_PinAssembly/PrePlace", relSpeed);
		placePinY("/_PinAssembly/PrePlace/Place2");
		move.PTPsafe("/_PinAssembly/PrePlace", relSpeed);
		pickPinZ("/_PinAssembly/PrePick/Pick3");
		move.PTPsafe("/_PinAssembly/PrePlace/PrePlace2", relSpeed);
		placePinY("/_PinAssembly/PrePlace/Place3");
		move.PTPsafe("/_PinAssembly/PrePlace/PrePlace2", relSpeed);
		pickPinZ("/_PinAssembly/PrePick/Pick4");
		move.PTPsafe("/_PinAssembly/PrePlace/PrePlace2", relSpeed);
		placePinY("/_PinAssembly/PrePlace/Place4");
		move.PTPsafe("/_PinAssembly/PrePlace/PrePlace2", relSpeed);
	}
	
	private void pickPinZ(Frame targetFrame) {
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setZ(preFrame.getZ() - approachOffset);
		move.PTPsafe(preFrame, relSpeed);
		if(log1) padLog("Picking process");
		move.LINsafe(targetFrame, approachSpeed);
		cobot.checkPinPick(5, probeSpeed);
		move.LINsafe(preFrame, approachSpeed);
	}
	
	private void pickPinZ(String targetFramePath) {
		if (log1) padLog("Pick pin macro at " + targetFramePath);
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		this.pickPinZ(targetFrame.copyWithRedundancy());
	}
	
	private void placePinY(Frame targetFrame) {
		boolean inserted;
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setZ(preFrame.getZ() - approachOffset);
		do  {
			move.PTPsafe(preFrame, relSpeed);
			if (log1) padLog("Placing process");
			move.LINsafe(targetFrame, approachSpeed);
			cobot.checkPinPlace(5, probeSpeed);
			inserted = move.twistJ7safe(45, 30, 0.15, 0.7);
			move.LINREL(0, 0, -30, true, approachSpeed, false);
		}
		while (!inserted);		
	}
	
	private void placePinY(String targetFramePath) {
		if (log1) padLog("Place pin macro at " + targetFramePath);
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		this.placePinY(targetFrame.copyWithRedundancy());
	}
	
	private void configPadKeysGENERAL() { 					// TEACH buttons						
		IUserKeyListener padKeysListener = new IUserKeyListener() {
			@Override public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (event == UserKeyEvent.KeyDown) {
					switch (key.getSlot()) {
						case 0:  						// KEY - TEACH MODE
							if (state == States.loop) {
								state = States.home;
							} else padLog("Already going to teach mode.");
							break;
						case 1: 						// KEY - DELETE PREVIOUS
							if (state == States.teach) {
								if (frameList.getLast().hasAdditionalParameter("PICK")) plc.openGripper();	
								else if (frameList.getLast().hasAdditionalParameter("PLACE")) plc.closeGripper();	
								frameList.removeLast();
							} else padLog("Key not available in this mode.");
							break;
						case 2:  						// KEY - SET SPEED
							move.setGlobalSpeed(pad.askSpeed());
							break;
						case 3:							// KEY - SET TORQUE
							double maxTorque = pad.askTorque();
							move.setJTconds(maxTorque);
							break;
					}
				}
			}
		};
		pad.keyBarSetup(padKeysListener, "GENERAL", "Teach", "Delete Previous", "Speed", "Torque");
	}
}