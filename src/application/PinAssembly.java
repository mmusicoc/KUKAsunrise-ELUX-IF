package application;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.lin;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.linRel;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.positionHold;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptp;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.applicationModel.tasks.UseRoboticsAPIContext;
import com.kuka.roboticsAPI.conditionModel.ForceCondition;
import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;
import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import com.kuka.roboticsAPI.geometricModel.math.Vector;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.PositionHold;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
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
	private boolean break_loop, isAttachedTool = false;  
	private enum States {go_home, approach_pin, pick_pin, approach_cabinet, find_hole, insert, retreat};
	private States state; 
	private double defRelVelocity = 0.30;	//0.25
	private int defMaxForce = 10;
	private Frame holePos1, holePos2, holePos3, holePos4, lastCheckpoint;
	private int width = 565; 	// in mm
	private int height = 465; 	// in mm

	private CartesianImpedanceControlMode cartImpMode = new CartesianImpedanceControlMode();


	@Override
	public void initialize() { 
		System.out.println("Initializing");
		setRGB(true, true, true);

		cartImpMode.parametrize(CartDOF.TRANSL).setStiffness(3000.0);
		cartImpMode.parametrize(CartDOF.ROT).setStiffness(200.0);
		cartImpMode.parametrize(CartDOF.ALL).setDamping(0.7);


		home = new JointPosition( 0, Math.toRadians(3),  0, Math.toRadians(-99),  0,  Math.toRadians(78),  0);
		kiwa.setHomePosition(home);
		resetPlcOutput();
		pin_number = 1;	
		
		state = States.go_home;		// DEFAULT!!			 
		break_loop = false; 


		setRGB(false, false, true);
		prompt_ans = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, "Is the tool attached?", "Yes", "No");
		setRGB(false, true, false);
		System.out.println("Prompt ans : "+prompt_ans);
		if (prompt_ans==0){
			isAttachedTool = true;
		}else{
			isAttachedTool = false;
		}

	
		//Setting the initial hole positions
		setInitHolePositions();		
//		System.out.println("Hole 1: " + holePos1.getX() + ", " + holePos1.getY() + ", " + holePos1.getZ() + ", " + holePos1.getAlphaRad() + ", " + holePos1.getBetaRad() + ", " + holePos1.getGammaRad());
//		System.out.println("Hole 2: " + holePos2.getX() + ", " + holePos2.getY() + ", " + holePos2.getZ() + ", " + holePos2.getAlphaRad() + ", " + holePos2.getBetaRad() + ", " + holePos2.getGammaRad());
//		System.out.println("Hole 3: " + holePos3.getX() + ", " + holePos3.getY() + ", " + holePos3.getZ() + ", " + holePos3.getAlphaRad() + ", " + holePos3.getBetaRad() + ", " + holePos3.getGammaRad());
//		System.out.println("Hole 4: " + holePos4.getX() + ", " + holePos4.getY() + ", " + holePos4.getZ() + ", " + holePos4.getAlphaRad() + ", " + holePos4.getBetaRad() + ", " + holePos4.getGammaRad());
	}

	@Override
	public void run() {
		while (true) {
			switch (state) {
			case go_home:
				System.out.println("Current state : home");

				//kiwa.move(ptpHome().setJointVelocityRel(0.25).breakWhen(collisionCond));	// move to set home  //no collision detection yet
				movePtpWithCollisionDetection("/PinAssem", defRelVelocity, defMaxForce, state.name() + pin_number); 

				//attach the tool if not already done
				if (isAttachedTool == false) {
					isAttachedTool = true;
					openGripper(state.name());
					ThreadUtil.milliSleep(1000);
					
					setRGB(false, false, true);
					System.out.println("Insert the tool.");
					kiwa.move(positionHold(cartImpMode, -1, TimeUnit.SECONDS).breakWhen(ForceCondition.createSpatialForceCondition(kiwa.getFlange(),20)));		// position hold for infinite secs
					closeGripper(state.name());
					ThreadUtil.milliSleep(1000);
					setRGB(false, true, false);
				}

				state = States.approach_pin;					// next state
				break;

			case approach_pin: 
				if (pin_number == 1) {
					//getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, " Are the Pins loaded? \n", "Yes");
				}				

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
					setRGB(true, false, false);
					getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, " Pin Number error! : " + pin_number, "Restart"); 
					state = States.go_home;
					setRGB(false, true, false);
					break;
				}

				state = States.pick_pin;

				break;

			case pick_pin:
				System.out.println("Current state : Picking pin " + pin_number);
				// 	Slowly go down 	
				moveLinRelWithCollisionDetection(0,0,-50, 0.05, 3, state.name()); // move 50 mm in z-direction, // just 2 N		

				// Force sense if Pin loaded
				if (checkInsertion() == false){
					setRGB(false, false, true);
					System.out.println("In HandGuiding mode: disabled");
					kiwa.move(linRel(0, 0, -50).setJointVelocityRel(0.1));		// go back after failed insertion check 
					ThreadUtil.milliSleep(500);
					// stiffless handguiding
					while(!mfio.getUserButton()){
						ThreadUtil.milliSleep(20);
					}
					System.out.println("Exiting HandGuiding mode:");
					setRGB(false, true, false);
					state = States.approach_pin;
					break;
				}
				// Come up
				//moveLinRelWithCollisionDetection(0, 0, 50, 0.05, defMaxForce, "Coming back after picking pin");
				kiwa.move(linRel(0,0,-50).setJointVelocityRel(0.05));		// to avoid Force detection
				
				//waitIndefinately();			// WAIT INDEFINATELY


				// next state
				state = States.approach_cabinet;
				break;

			case approach_cabinet:
				System.out.println("Current state : Approach cabinet");		
				// move to the start position on the side of the conveyer		//First orient the tool correctly to prevent A4/A6 singularity
				//movePtpWithCollisionDetection("/startCabinet", defRelVelocity, defMaxForce, state.name());		
				movePtpWithCollisionDetection("/startCabinet2", defRelVelocity, defMaxForce, state.name());	
				// move to the side of the conveyer
				movePtpWithCollisionDetection("/pinInsertion", defRelVelocity, defMaxForce, "Starting pin insertion");	
				
				//stifflessHandguiding('0','y','z','0','0','0');		// FOR TEST!!!!
				
				// next state
				state = States.find_hole;
				break;

			case find_hole:
	
				System.out.println("Current state : Finding hole " + pin_number);
				// Check pin
				// Hover over the pin
				if (pin_number == 1) {
					movePtpWithCollisionDetection(holePos1, defRelVelocity, defMaxForce, state.name() + pin_number);
				} else if (pin_number == 2){
					movePtpWithCollisionDetection(holePos2, defRelVelocity, defMaxForce, state.name() + pin_number);
				} else if (pin_number == 3){
					movePtpWithCollisionDetection(holePos3, defRelVelocity, defMaxForce, state.name() + pin_number);
				} else if (pin_number == 4){
					movePtpWithCollisionDetection(holePos4, defRelVelocity, defMaxForce, state.name() + pin_number);
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
				moveLinRelWithCollisionDetection(0, -50, 0, 0.05, 3, state.name() + pin_number);  // just 3 N		

				// Force sense if inserted
				if (checkInsertion() == false){
					setRGB(false, false, true);
					kiwa.move(linRel(0, 0, -50).setJointVelocityRel(0.1));		// go back after failed insertion check 
					ThreadUtil.milliSleep(500);
					System.out.println("In HandGuiding mode: disabled");
					// stiffless handguiding
					while(!mfio.getUserButton()){
						ThreadUtil.milliSleep(20);
					}
					System.out.println("Exiting HandGuiding mode:");
					ThreadUtil.milliSleep(1000);
					//updateHolePositions();
					setRGB(false, true, false);
				}else{
					
					ThreadUtil.milliSleep(1000);
				}



				// twist the pin with force detection
				twistPin();



				// Come back
				//moveLinRelWithCollisionDetection(0, 50, 0, 0.05, defMaxForce, "Coming back after pin insertion");
				kiwa.move(linRel(0,0,-50).setJointVelocityRel(0.05));		// to avoid Force detection

				//iterate pin number
				if (pin_number <4) {
					pin_number ++;
				} else {
					pin_number = 1;
				}

				// next state
				state = States.retreat;
				break;

			case retreat:
				System.out.println("Current state : Retreat");
				// retreat to the start position on the side of the conveyer
				movePtpWithCollisionDetection("/pinInsertion", defRelVelocity, defMaxForce, state.name());   	
				movePtpWithCollisionDetection("/startCabinet2", defRelVelocity, defMaxForce, state.name());	

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

	// stiffless handguiding
	private void stifflessHandguiding(char x, char y, char z, char a, char b, char c){	// X is along the length of the gripper, Z is point of the tool
		
		CartesianImpedanceControlMode ctrMode = new CartesianImpedanceControlMode(); 
		setRGB(false, false, true);
		ctrMode.parametrize(CartDOF.TRANSL).setStiffness(5000).setDamping(1);
		ctrMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1);
		
		if (x == 'x' || x == 'X') {
			ctrMode.parametrize(CartDOF.X).setStiffness(0);
			System.out.println("Stiffness set to zero: X");
		} 
		if (y == 'y' || y == 'Y') {
			ctrMode.parametrize(CartDOF.Y).setStiffness(0);
			System.out.println("Stiffness set to zero: Y");
		} 
		if (z == 'z' || z == 'Z') {
			ctrMode.parametrize(CartDOF.Z).setStiffness(0);
			System.out.println("Stiffness set to zero: Z");
		} 
		if (a == 'a' || a == 'A') {
			ctrMode.parametrize(CartDOF.A).setStiffness(0);
			System.out.println("Stiffness set to zero: A");
		} 
		if (b == 'b' || b == 'B') {
			ctrMode.parametrize(CartDOF.B).setStiffness(0);
			System.out.println("Stiffness set to zero: B");
		} 
		if (c == 'c' || c == 'C') {
			ctrMode.parametrize(CartDOF.C).setStiffness(0);
			System.out.println("Stiffness set to zero: C");
		} 
		
		int ans = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, "The above coordinates are with respect to the flange. Proceed stiffless HandGuiding?", "YES", "NO"); 
		
		if (ans == 0) {
			System.out.println("Starting Stiffless handguiding");
			
			ctrMode.setMaxCartesianVelocity(200, 200, 200, 2, 2, 2);			// set max speed in positionHold with zero stiffness
			PositionHold posHold = new PositionHold(ctrMode, -1, null); 
			IMotionContainer positionHoldContainer = kiwa.moveAsync(posHold); 
			while (true) {
				ThreadUtil.milliSleep(50);
				if ( mfio.getUserButton()) {
					positionHoldContainer.cancel();
					System.out.println("Current position saved for hole "+ pin_number);
					System.out.println("Exiting Stiffless handguiding");
					ThreadUtil.milliSleep(500);
					break;
				}
			}
			
		}
		setRGB(false, true, false);
		
	}

	private void movePtpWithCollisionDetection (String strFrame, double relVelocity, int maxForce, String currentStatus){ // overload: saved Frames
		IMotionContainer motionCmd;
		ForceCondition breakCondition;
		IFiredConditionInfo info;
		double breakForce = kiwa.getExternalForceTorque(kiwa.getFlange()).getForce().length() + maxForce;

		breakCondition = ForceCondition.createSpatialForceCondition(kiwa.getFlange(),breakForce); 
		System.out.println("Current Force: "+ kiwa.getExternalForceTorque(kiwa.getFlange()).getForce().length() + " Breakforce: " + breakForce);

		//motionCmd = kiwa.move(ptp(getApplicationData().getFrame(strFrame)).setJointVelocityRel(relVelocity).breakWhen(breakCondition).setMode(cartImpMode));
		motionCmd = kiwa.move(ptp(getApplicationData().getFrame(strFrame)).setJointVelocityRel(relVelocity).breakWhen(breakCondition));

		info = motionCmd.getFiredBreakConditionInfo();
		while (info != null){
			System.out.println("Collision detected in " + currentStatus);  
			setRGB(true, false, false);
			printCurrentForce(); 
			while(!mfio.getUserButton()){
				ThreadUtil.milliSleep(20);
				setRGB(false, true, false);
			}
			//motionCmd = kiwa.move(ptp(getApplicationData().getFrame(strFrame)).setJointVelocityRel(relVelocity).breakWhen(breakCondition).setMode(cartImpMode));
			motionCmd = kiwa.move(ptp(getApplicationData().getFrame(strFrame)).setJointVelocityRel(relVelocity).breakWhen(breakCondition));

			info = motionCmd.getFiredBreakConditionInfo();
		} 
	}

	private void movePtpWithCollisionDetection (Frame targetFrame, double relVelocity, int maxForce, String currentStatus){	//overload: derived Frames
		IMotionContainer motionCmd;
		ForceCondition breakCondition;
		IFiredConditionInfo info;
		double breakForce = kiwa.getExternalForceTorque(kiwa.getFlange()).getForce().length() + maxForce;

		breakCondition = ForceCondition.createSpatialForceCondition(kiwa.getFlange(),breakForce); 
		System.out.println("Current Force: "+ kiwa.getExternalForceTorque(kiwa.getFlange()).getForce().length() + " Breakforce: " + breakForce);


		//motionCmd = kiwa.move(ptp(targetFrame).setJointVelocityRel(relVelocity).breakWhen(breakCondition).setMode(cartImpMode));
		motionCmd = kiwa.move(ptp(targetFrame).setJointVelocityRel(relVelocity).breakWhen(breakCondition));

		info = motionCmd.getFiredBreakConditionInfo();
		while (info != null){
			System.out.println("Collision detected in " + currentStatus);  
			setRGB(true, false, false);
			printCurrentForce(); 
			while(!mfio.getUserButton()){
				ThreadUtil.milliSleep(20);
			}
			setRGB(false, true, false);
			ThreadUtil.milliSleep(500);			// This is necessary, OW. the next breakConditions are again triggered
			//motionCmd = kiwa.move(ptp(targetFrame).setJointVelocityRel(relVelocity).breakWhen(breakCondition).setMode(cartImpMode));
			motionCmd = kiwa.move(ptp(targetFrame).setJointVelocityRel(relVelocity).breakWhen(breakCondition));

			info = motionCmd.getFiredBreakConditionInfo();
		} 
	}

	private void moveLinRelWithCollisionDetection (double x, double y, double z, double relVelocity, double maxForce, String currentStatus){
		IMotionContainer motionCmd;
		Frame targetFrame, currentFrame;
		JointTorqueCondition breakCondition1, breakCondition2;
		IFiredConditionInfo info;
		double currentTorqueJ1, currentTorqueJ2;

		targetFrame = makeTargetFrame(x, y, z); 
	
		ThreadUtil.milliSleep(500);
		currentTorqueJ1 = kiwa.getExternalTorque().getSingleTorqueValue(JointEnum.J1);
		currentTorqueJ2 = kiwa.getExternalTorque().getSingleTorqueValue(JointEnum.J2);
		breakCondition1 = new JointTorqueCondition(JointEnum.J1, currentTorqueJ1 - 2, currentTorqueJ1 + 2);		// 2N on axis 1
		breakCondition2 = new JointTorqueCondition(JointEnum.J2, currentTorqueJ2 - 2, currentTorqueJ2 + 2);  	// 2N on axis 2
		System.out.println("Breaktorque J1 is "+ (currentTorqueJ1-1.5) +" : "+ (currentTorqueJ1+1.5));
		System.out.println("Breaktorque J2 is "+ (currentTorqueJ2-1.5) +" : "+ (currentTorqueJ2+1.5));
		
		//motionCmd = kiwa.move(lin(targetFrame).setJointVelocityRel(relVelocity).breakWhen(breakCondition).setMode(cartImpMode));
		motionCmd = kiwa.move(lin(targetFrame).setJointVelocityRel(relVelocity).breakWhen(breakCondition1).breakWhen(breakCondition2));
		System.out.println("Is this skipped?");
		info = motionCmd.getFiredBreakConditionInfo(); 
		while (info != null){
			System.out.println("Collision detected in " + currentStatus);
			setRGB(true, false, false);

			currentFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
			if (Math.abs(currentFrame.getX() - targetFrame.getX()) < 7 && Math.abs(currentFrame.getY() - targetFrame.getY()) < 7 && Math.abs(currentFrame.getZ() - targetFrame.getZ()) < 7) {
				System.out.println("Pin/Hole " + pin_number + " found by collision.");	
				setRGB(false, false, true);
				ThreadUtil.milliSleep(1000);
				//printCurrentFrameDist(targetFrame); 
				break;
			} else {
				kiwa.move(linRel(0, 0, -30).setJointVelocityRel(relVelocity));		// go back after collision 
				ThreadUtil.milliSleep(500);
				//printCurrentFrameDist(targetFrame); 
				if (state == States.insert) {
					stifflessHandguiding('0','0','z','0','0','0');		//only in inserting state, move only in z direction of the flange 
					ThreadUtil.milliSleep(500);			// This is necessary, OW. the next breakConditions are again triggered
					breakCondition1 = new JointTorqueCondition(JointEnum.J1, currentTorqueJ1 - 2, currentTorqueJ1 + 2);		// 2N on axis 1
					breakCondition2 = new JointTorqueCondition(JointEnum.J2, currentTorqueJ2 - 2, currentTorqueJ2 + 2);  	// 2N on axis 2
					
				}else {
					while(!mfio.getUserButton()){
						ThreadUtil.milliSleep(20);
					}
				}
				
				setRGB(false, true, false);
				ThreadUtil.milliSleep(500);			// This is necessary, OW. the next breakConditions are again triggered
				//motionCmd = kiwa.move(lin(targetFrame).setJointVelocityRel(relVelocity).breakWhen(breakCondition).setMode(cartImpMode));
				motionCmd = kiwa.move(lin(targetFrame).setJointVelocityRel(relVelocity).breakWhen(breakCondition1).breakWhen(breakCondition2));
				info = motionCmd.getFiredBreakConditionInfo();
			}

		}
		System.out.println("Pin/Hole " + pin_number + " found.");
		setRGB(false, true, false);
		//printCurrentFrameDist(targetFrame); 
	}

	private boolean checkInsertion(){							// MAKE THIS TORQUE CONDITION,   J6  1Nm difference
		System.out.println("Checking Insertion");
		setRGB(false, true, true);
		IMotionContainer motionCmd;
		JointTorqueCondition breakCondition;
		IFiredConditionInfo info;
		boolean checkPositive = false;
 
		ThreadUtil.milliSleep(500);
		double currentTorque = kiwa.getExternalTorque().getSingleTorqueValue(JointEnum.J6);
		breakCondition = new JointTorqueCondition(JointEnum.J6, currentTorque - 1, currentTorque + 1); 

		System.out.println("Breaktorque is "+ (currentTorque-1.0) +" : "+ (currentTorque+1.0));
		
		lastCheckpoint = kiwa.getCurrentCartesianPosition(kiwa.getFlange());	// Save the location of center of the hole
		
		motionCmd = kiwa.move(linRel(-5, -5, 0).setJointVelocityRel(0.01).breakWhen(breakCondition));
		info = motionCmd.getFiredBreakConditionInfo(); 

		if (info != null) {
			checkPositive = true;
			System.out.println("Check 1 successful");
			setRGB(false, true, false);
			ThreadUtil.milliSleep(500); 
			kiwa.move(ptp(lastCheckpoint).setJointVelocityRel(0.01)); //Go back to the center of the hole
			ThreadUtil.milliSleep(500); 
			setRGB(false, true, true);
		}  
		if (checkPositive == false) {
			System.out.println("Check 1 unsuccessful");
			setRGB(true, false, false);
			ThreadUtil.milliSleep(2000);
			kiwa.move(ptp(lastCheckpoint).setJointVelocityRel(0.01)); //Go back to the center of the hole
			ThreadUtil.milliSleep(500); 
			setRGB(false, true, false);
			return false;
		} else {
			currentTorque = kiwa.getExternalTorque().getSingleTorqueValue(JointEnum.J6);
			breakCondition = new JointTorqueCondition(JointEnum.J6, currentTorque - 1, currentTorque + 1); 
			motionCmd = kiwa.move(linRel(5, 5, 0).setJointVelocityRel(0.01).breakWhen(breakCondition));

			info = motionCmd.getFiredBreakConditionInfo(); 
			if (info != null) {
				System.out.println("Check 2 successful");
				setRGB(false, true, false);
				ThreadUtil.milliSleep(500); 
				kiwa.move(ptp(lastCheckpoint).setJointVelocityRel(0.02)); //Go back to the center of the hole
				ThreadUtil.milliSleep(500); 
				System.out.println("Insertion check successful");
				
				return true;
			}else{
				System.out.println("Check 2 unsuccessful");
				setRGB(true, false, false);
				ThreadUtil.milliSleep(2000);
				kiwa.move(ptp(lastCheckpoint).setJointVelocityRel(0.01)); //Go back to the center of the hole
				ThreadUtil.milliSleep(500); 
				setRGB(false, true, false);
				return false;
			}
		}

	}

	private void twistPin(){
		System.out.println("Twisting the pin");
		setRGB(false, true, true);
		IMotionContainer motionCmd;
		JointTorqueCondition breakCondition;
		IFiredConditionInfo info;

		ThreadUtil.milliSleep(500); 

		breakCondition = new JointTorqueCondition(JointEnum.J7, -0.7, 0.7); 

		motionCmd = kiwa.move(linRel(Transformation.ofDeg(0, 0, 0, 60, 0, 0)).setJointVelocityRel(0.15).breakWhen(breakCondition));	// x,y,z,a,b,c
		info = motionCmd.getFiredBreakConditionInfo(); 
		if (info != null) {
			System.out.println("Cannot twist anymore");
			System.out.println("Current torque: " + kiwa.getExternalTorque().getSingleTorqueValue(JointEnum.J7));
			setRGB(true, true, true);
			ThreadUtil.milliSleep(500);
		}
		setRGB(false, true, false);
	}


	private Frame makeTargetFrame (double x, double y, double z) {
		Frame targetFrame, currentFrame;
		currentFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
		targetFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange()).setX(currentFrame.getX()+x).setY(currentFrame.getY()+y).setZ(currentFrame.getZ()+z);

		//System.out.println("Current coordinates: " + currentFrame.getX()+", "+currentFrame.getY()+", "+currentFrame.getZ());
		//System.out.println("Target coordinates: " + targetFrame.getX()+", "+targetFrame.getY()+", "+targetFrame.getZ()+", " + targetFrame.distanceTo(currentFrame));

		return targetFrame;
	} 


	private Frame objectFrameToFrame(ObjectFrame objFrame){
		Frame transformedFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());

		transformedFrame.setParentAndTransformation(objFrame.getParent(), objFrame.getTransformationFromParent());
		transformedFrame.setRedundancyInformation(kiwa, objFrame.getRedundancyInformationForDevice(kiwa));
		transformedFrame.setTransformationProvider(objFrame.getTransformationProvider());
		//		transformedFrame.setAlphaRad(objFrame.getAlphaRad());		//A
		//		transformedFrame.setBetaRad(objFrame.getBetaRad());			//B
		//		transformedFrame.setGammaRad(objFrame.getGammaRad());		//C
		//		transformedFrame.setX(objFrame.getX());						//X
		//		transformedFrame.setY(objFrame.getY());						//Y
		//		transformedFrame.setZ(objFrame.getZ());						//Z 

		//		System.out.println("transformedFrame: " + transformedFrame.getX() + ", " + transformedFrame.getY() + ", " + transformedFrame.getZ() + ", " + transformedFrame.getAlphaRad() + ", " + transformedFrame.getBetaRad() + ", " + transformedFrame.getGammaRad());
		//		System.out.println("objFrame: " + objFrame.getX() + ", " + objFrame.getY() + ", " + objFrame.getZ() + ", " + objFrame.getAlphaRad() + ", " + objFrame.getBetaRad() + ", " + objFrame.getGammaRad());


		return transformedFrame;
	}

	private void setInitHolePositions(){
		System.out.println("Setting initial hole positions");
		double x_offset, y_offset, z_offset;
		x_offset = 0.0;
		y_offset = 0.0;
		z_offset = height;

		// Get hole 1 from the saved frames
		holePos1 = objectFrameToFrame(getApplicationData().getFrame("/holePos1"));

		// transform to hole 2
		//		x_offset = Math.cos(holePos1.getAlphaRad()) * width; 
		//		y_offset = Math.sin(holePos1.getAlphaRad()) * width;
		//		holePos2 = holePos1.copy();
		//		holePos2.setX(holePos1.getX() + x_offset).setY(holePos1.getY()+y_offset) ;
		holePos2 = objectFrameToFrame(getApplicationData().getFrame("/holePos2"));


		// transform to hole 3
		//		z_offset = (-1.0) * height;
		//		holePos3 = holePos2.copy();
		//		holePos3.setZ(holePos2.getZ() + z_offset);
		holePos3 = objectFrameToFrame(getApplicationData().getFrame("/holePos3"));

		// transform to hole 4
		//		holePos4 = holePos1.copy();
		//		holePos4.setZ(holePos1.getZ() + z_offset);
		holePos4 = objectFrameToFrame(getApplicationData().getFrame("/holePos4"));



	}



	private void updateHolePositions(){
		System.out.println("Updating hole positions");
		double x_offset, y_offset, z_offset;
		x_offset = 0.0;
		y_offset = 0.0;
		z_offset = height;
		switch (pin_number) {				// check by pin_number which hole is being used as reference
		case 1:
			// get hole 1
			holePos1 = kiwa.getCurrentCartesianPosition(kiwa.getFlange());

			// transform to hole 2
			x_offset = Math.cos(holePos1.getAlphaRad()) * width; 
			y_offset = Math.sin(holePos1.getAlphaRad()) * width;
			holePos2 = holePos1.copy();
			holePos2.setX(holePos1.getX() + x_offset).setY(holePos1.getY()+y_offset) ;

			// transform to hole 3
			z_offset = (-1.0) * height;
			holePos3 = holePos2.copy();
			holePos3.setZ(holePos2.getZ() + z_offset);

			// transform to hole 4
			holePos4 = holePos1.copy();
			holePos4.setZ(holePos1.getZ() + z_offset);
			break;

		case 2: 
			//get hole 2
			holePos2 = kiwa.getCurrentCartesianPosition(kiwa.getFlange());

			//transform to hole 1 
			x_offset = (-1.0)* Math.cos(holePos2.getAlphaRad()) * width;
			y_offset = (-1.0)* Math.sin(holePos2.getAlphaRad()) * width;
			holePos1 = holePos2.copy();
			holePos1.setX(holePos2.getX() + x_offset).setY(holePos2.getY()+y_offset) ;

			// transform to hole 3 
			z_offset = -(double) height;
			holePos3 = holePos2.copy();
			holePos3.setZ(holePos2.getZ() + z_offset);

			// transform to hole 4
			holePos4 = holePos1.copy();
			holePos4.setZ(holePos1.getZ() + z_offset);
			break;

		case 3:
			// get hole 3
			holePos3 = kiwa.getCurrentCartesianPosition(kiwa.getFlange());

			//transform to hole 2
			z_offset = height;
			holePos2 = holePos3.copy();
			holePos2.setZ(holePos3.getZ() + z_offset);

			//transform to hole 1
			x_offset = (-1.0)* Math.cos(holePos2.getAlphaRad()) * width;
			y_offset = (-1.0)* Math.sin(holePos2.getAlphaRad()) * width;
			holePos1 = holePos2.copy();
			holePos1.setX(holePos2.getX() + x_offset).setY(holePos2.getY()+y_offset) ;


			// transform to hole 4
			z_offset = (-1.0) * height;
			holePos4 = holePos1.copy();
			holePos4.setZ(holePos1.getZ() + z_offset);

			break;

		case 4:
			//get hole 4
			holePos4 = kiwa.getCurrentCartesianPosition(kiwa.getFlange());

			//transform to hole 1
			z_offset = height;
			holePos1 = holePos4.copy();
			holePos1.setZ(holePos4.getZ() + z_offset);

			// transform to hole 2
			x_offset = Math.cos(holePos1.getAlphaRad()) * width; 
			y_offset = Math.sin(holePos1.getAlphaRad()) * width;
			holePos2 = holePos1.copy();
			holePos2.setX(holePos1.getX() + x_offset).setY(holePos1.getY()+y_offset) ;

			// transform to hole 3
			z_offset = (-1.0) * height;
			holePos3 = holePos2.copy();
			holePos3.setZ(holePos2.getZ() + z_offset);


			break;
		default:
			System.out.println("Error: pin number out of range");
			waitIndefinately();
			break;
		}
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
		double forceLength = currentForce.length();
		Vector inaccurancy = kiwa.getExternalForceTorque(kiwa.getFlange()).getForceInaccuracy();
		System.out.println("Current Force: "+currentForce+" length: "+ forceLength);
		System.out.println("Force inaccurancy: "+ inaccurancy+" length: "+ inaccurancy.length());
	}
	private void closeGripper(String state){
		plcout.setPinza_Apri(false);
		System.out.println("Closing gripper in state:" + state);
		ThreadUtil.milliSleep(50);
		mAtPos();
		plcout.setPinza_Chiudi(true);
		ThreadUtil.milliSleep(1000);
		while(plcin.getPinza_Idle()==false){
			ThreadUtil.milliSleep(20);
			if(plcin.getPinza_Holding() == true){
				break;
			}
		}

	}
	private void openGripper(String state) {
		plcout.setPinza_Chiudi(false);
		System.out.println("Opening gripper in state: " + state);
		ThreadUtil.milliSleep(50);
		mAtPos();

		plcout.setPinza_Apri(true);
		ThreadUtil.milliSleep(1000);
		while(plcin.getPinza_Idle()==false){
			ThreadUtil.milliSleep(20);
		}
	}
	private void waitIndefinately(){
		System.out.println("Waiting indefinately");
		while(true){
			int b = 1;
			if (b ==2) {
				break;
			}
		}
	}
	
	private void setRGB(boolean r, boolean g, boolean b){
		mfio.setLEDRed(r);
		mfio.setLEDGreen(g);
		mfio.setLEDBlue(b);
	}

}