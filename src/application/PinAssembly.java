package application;

import javax.inject.Inject;

import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.conditionModel.ForceCondition;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import com.kuka.roboticsAPI.geometricModel.math.Vector;

import com.kuka.roboticsAPI.motionModel.IMotion;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.sensorModel.ForceSensorData;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;

/**
 * Implementation of a robot application.
 * <p>
 * The application provides a {@link RoboticsAPITask#initialize()} and a 
 * {@link RoboticsAPITask#run()} method, which will be called successively in 
 * the application lifecycle. The application will terminate automatically after 
 * the {@link RoboticsAPITask#run()} method has finished or after stopping the 
 * task. The {@link RoboticsAPITask#dispose()} method will be called, even if an 
 * exception is thrown during initialization or run. 
 * <p>
 * <b>It is imperative to call <code>super.dispose()</code> when overriding the 
 * {@link RoboticsAPITask#dispose()} method.</b> 
 * 
 * @see UseRoboticsAPIContext
 * @see #initialize()
 * @see #run()
 * @see #dispose()
 */


public class PinAssembly extends RoboticsAPIApplication {
	@Inject
	private LBR kiwa;
	@Inject private Plc_inputIOGroup 		plcin;
	@Inject private Plc_outputIOGroup 		plcout;
	@Inject private MediaFlangeIOGroup 		mfio;
	private JointPosition home;
	private int prompt_ans;
	private int pin_number; 
	private boolean break_loop; 
	private ForceCondition collisionCond;
	private enum States {go_home, approach_pin, pick_pin, approach_cabinet, find_hole, insert, retreat};
	private States state; 
	private double defRelVelocity = 0.25;
	private int defMaxForce = 30;

	@Override
	public void initialize() {
		System.out.println("Initializing");
		mfio.setLEDRed(false);
		mfio.setLEDBlue(true);
		mfio.setLEDGreen(false);
		
		home = new JointPosition( 0, Math.toRadians(3),  0, Math.toRadians(-99),  0,  Math.toRadians(78),  0);
		kiwa.setHomePosition(home);
		resetPlcOutput();
		state = States.go_home;
		pin_number = 1;
		break_loop = false;
		collisionCond = ForceCondition.createSpatialForceCondition(kiwa.getFlange(),30);
		
		
		
		getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, " Press OK to start \n", "OK");
		
		mfio.setLEDBlue(false);
		mfio.setLEDGreen(true);
		
		ThreadUtil.milliSleep(50);
		mAtPos();
		plcout.setPinza_Apri(true);
		ThreadUtil.milliSleep(3000);
		plcout.setPinza_Apri(false);

	}

	@Override
	public void run() {
		while (true) {
			switch (state) {
			case go_home:
				System.out.println("Current state : home");
				
				//kiwa.move(ptpHome().setJointVelocityRel(0.25).breakWhen(collisionCond));	// move to set home  //no collision detection yet
				kiwa.move(ptp(getApplicationData().getFrame("/PinAssem")).setJointVelocityRel(defRelVelocity));
				
				state = States.approach_pin;					// next state
				break;

			case approach_pin: 
				System.out.println("Current state : Approach pin");

				// check pin number 
				// hover over the pin
				if (pin_number == 1) {
					movePtpWithCollisionDetection("/PinAssem/pinPos1", defRelVelocity, defMaxForce, state.name() + pin_number);
				} else if (pin_number == 2){
					movePtpWithCollisionDetection("/PinAssem/pinPos2", defRelVelocity, defMaxForce, state.name() + pin_number);
				} else if (pin_number == 3){
					movePtpWithCollisionDetection("/PinAssem/pinPos3", defRelVelocity, defMaxForce, state.name() + pin_number);
				} else if (pin_number == 4){
					movePtpWithCollisionDetection("/PinAssem/pinPos4", defRelVelocity, defMaxForce, state.name() + pin_number);
				} else {
					getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, " Pin Number error! : " + pin_number, "Restart"); 
					state = States.go_home;
					break;
				}
				

				state = States.pick_pin;
				
				break;

			case pick_pin:
				System.out.println("Current state : Picking pin " + pin_number);
				// 	Slowly go down 	
				moveLinRelWithCollisionDetection(0,0,-150, 0.05, 20, state.name()); // move 50 mm in z-direction
				
				//  Activate gripper
				closeGripper(state.name());
				// Come up
				moveLinRelWithCollisionDetection(0, 0, 150, 0.05, defMaxForce, "Coming back after picking pin");

				// next state
				state = States.approach_cabinet;
				break;

			case approach_cabinet:
				System.out.println("Current state : Approach cabinet");
				// move to the start position on the side of the conveyer
				movePtpWithCollisionDetection("/PinAssem/startCabinet", defRelVelocity, defMaxForce, state.name());			

				// next state
				state = States.find_hole;
				break;

			case find_hole:
				System.out.println("Current state : Finding hole " + pin_number);
				// Check pin
				// Hover over the pin
				if (pin_number == 1) {
					movePtpWithCollisionDetection("/PinAssem/startCabinet/posHole1", defRelVelocity, defMaxForce, state.name() + pin_number);
				} else if (pin_number == 2){
					movePtpWithCollisionDetection("/PinAssem/startCabinet/posHole2", defRelVelocity, defMaxForce, state.name() + pin_number);
				} else if (pin_number == 3){
					movePtpWithCollisionDetection("/PinAssem/startCabinet/posHole3", defRelVelocity, defMaxForce, state.name() + pin_number);
				} else if (pin_number == 4){
					movePtpWithCollisionDetection("/PinAssem/startCabinet/posHole4", defRelVelocity, defMaxForce, state.name() + pin_number);
				} else {
					getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, " Pin Number error! : " + pin_number, "Restart"); 
					state = States.go_home;
					break;
				}

				// next state
				state = States.insert;
				break;

			case insert: 
				System.out.println("Current state : Inserting pin" + pin_number);
				//  Slowly go toward the hole 
				moveLinRelWithCollisionDetection(0, 0, 100, 0.05, defMaxForce, state.name() + pin_number);
				// twist the pin
				kiwa.move(linRel(Transformation.ofDeg(0, 0, 0, 30, 0, 0)).setJointVelocityRel(0.05));	// no dollision detection yet
				
				//  DeActivate gripper
				openGripper(state.name());
				// Come back
				moveLinRelWithCollisionDetection(0, 0, -100, 0.05, defMaxForce, "Coming back after pin insertion");

				//iterate pin number
				if (pin_number <4) {
					pin_number ++;
				} else {
					pin_number = 0;
				}

				// next state
				state = States.retreat;
				break;

			case retreat:
				System.out.println("Current state : Retreat");
				// retreat to the start position on the side of the conveyer
				movePtpWithCollisionDetection("/PinAssem/startCabinet", defRelVelocity, defMaxForce, state.name());

				// next state
				state = States.go_home;
				break;


			default:
				break;
			}
			if (break_loop == true) {				// conditionally break the while loop
				break_loop = false;
				break;
			}
		}

	}


	// ------------------------------------------- METHODS -----------------------------------------------------------------
	
	private void mAtPos(){
		plcout.setMission_AtPos(true);
		mfio.setOutputX3Pin1(false);
	}
	
	private void resetPlcOutput() {
		plcout.setMission_AtPos(false);
		plcout.setMission_IndexFBK(0);
		plcout.setMission_ExitDone(false);
		plcout.setMission_Result(0);
		plcout.setMission_Run(false);
		plcout.setApp_ResetDone(false);
	}
	

	private void movePtpWithCollisionDetection (String strFrame, double relVelocity, int maxForce, String currentStatus){
		IMotionContainer motionCmd;
		ForceCondition breakCondition;
		IFiredConditionInfo info;
		breakCondition = ForceCondition.createSpatialForceCondition(kiwa.getFlange(),maxForce);

		motionCmd = kiwa.move(ptp(getApplicationData().getFrame(strFrame)).setJointVelocityRel(relVelocity).breakWhen(breakCondition));
		
		info = motionCmd.getFiredBreakConditionInfo();
		while (info != null){
			System.out.println("Collision detected in " + currentStatus);
			printCurrentForce();
			mfio.setLEDRed(true);
			while(!mfio.getUserButton()){
				ThreadUtil.milliSleep(20);
			}
			motionCmd = kiwa.move(ptp(getApplicationData().getFrame(strFrame)).setJointVelocityRel(relVelocity).breakWhen(breakCondition));
			info = motionCmd.getFiredBreakConditionInfo();
		}
		mfio.setLEDRed(false);
	}
	private void moveLinRelWithCollisionDetection (double x, double y, double z, double relVelocity, int maxForce, String currentStatus){
		IMotionContainer motionCmd;
		Frame targetFrame, currentFrame;
		ForceCondition breakCondition;
		IFiredConditionInfo info;
		
		targetFrame = makeTargetFrame(x, y, z);

		breakCondition = ForceCondition.createSpatialForceCondition(kiwa.getFlange(),maxForce); 
		motionCmd = kiwa.move(lin(targetFrame).setJointVelocityRel(relVelocity).breakWhen(breakCondition));
		
		info = motionCmd.getFiredBreakConditionInfo();
		while (info != null){
			System.out.println("Collision detected in " + currentStatus);
			printCurrentForce();
			
			currentFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
			if (Math.abs(currentFrame.getX() - targetFrame.getX()) < 20 && Math.abs(currentFrame.getY() - targetFrame.getY()) < 20 && Math.abs(currentFrame.getZ() - targetFrame.getZ()) < 20) {
				System.out.println("Pin/Hole " + pin_number + " found.");	
				printCurrentFrameDist(targetFrame);
				mfio.setLEDBlue(true);
				break;
			} else {
				printCurrentFrameDist(targetFrame);
				mfio.setLEDRed(true);
				while(!mfio.getUserButton()){
					ThreadUtil.milliSleep(20);
				}																					
				motionCmd = kiwa.move(lin(targetFrame).setJointVelocityRel(relVelocity).breakWhen(breakCondition));
				info = motionCmd.getFiredBreakConditionInfo();
			}
			
		}
		System.out.println("Pin/Hole " + pin_number + " found.");
		printCurrentFrameDist(targetFrame);
		mfio.setLEDRed(false);
	}
	
	private Frame makeTargetFrame (double x, double y, double z) {
		Frame targetFrame, currentFrame;
		currentFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
		targetFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange()).setX(currentFrame.getX()+x).setY(currentFrame.getY()+y).setZ(currentFrame.getZ()+z);
		
		System.out.println("Current coordinates: " + currentFrame.getX()+", "+currentFrame.getY()+", "+currentFrame.getZ());
		System.out.println("Target coordinates: " + targetFrame.getX()+", "+targetFrame.getY()+", "+targetFrame.getZ()+", " + targetFrame.distanceTo(currentFrame));

		return targetFrame;
	}
	
	private void printCurrentFrame () {
		Frame currentFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
		System.out.println("Current coordinates: " + currentFrame.getX()+", "+currentFrame.getY()+", "+currentFrame.getZ());	
	}
	private void printCurrentFrameDist (Frame targetFrame) {
		Frame currentFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
		System.out.println("Current coordinates: " + currentFrame.getX()+", "+currentFrame.getY()+", "+currentFrame.getZ()+", " + currentFrame.distanceTo(targetFrame));	
	}
	private void printCurrentForce(){
		Vector currentForce = kiwa.getExternalForceTorque(kiwa.getFlange()).getForce();
		System.out.println(currentForce);
	}
	private void closeGripper(String state){
		System.out.println("Closing gripper in state:" + state);
		ThreadUtil.milliSleep(50);
		mAtPos();
		plcout.setPinza_Chiudi(true);
		ThreadUtil.milliSleep(1000);
		while(plcin.getPinza_Idle()==false){
			ThreadUtil.milliSleep(20);
			if (plcin.getPinza_NoPart())		// just a hack because no part in the gripper yet
			{
				ThreadUtil.milliSleep(1000);
				break;
			}
		}
		plcout.setPinza_Chiudi(false);
	}
	private void openGripper(String state) {
		System.out.println("Opening gripper in state: " + state);
		ThreadUtil.milliSleep(50);
		mAtPos();
		
		plcout.setPinza_Apri(true);
		ThreadUtil.milliSleep(1000);
		while(plcin.getPinza_Idle()==false){
			ThreadUtil.milliSleep(20);
		}
		plcout.setPinza_Apri(false);
	}

}