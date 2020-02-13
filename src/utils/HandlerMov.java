package utils;

import static utils.Utils.*;
import javax.inject.Inject;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.*;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.PositionHold;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;

public class HandlerMov extends RoboticsAPIApplication {
	@Inject private LBR kiwa;
	@Inject private HandlerMFio mf;
	@Inject private HandlerPad pad = new HandlerPad();
	
	@Inject
	public HandlerMov(HandlerMFio _mf) { 
		mf = _mf;
	}
	
	@Override
	public void run() {
		while (true) { break; }
	}
	
	/***************************************************************************
	* STANDARD METHODS BY mario.musico@electrolux.com <p>
	***************************************************************************/
	
// Standard moves
	public void setHome(String frameName){
		kiwa.setHomePosition(getApplicationData().getFrame(frameName));
	}
	
	public void PTP(String frameName, double relSpeed) {
		pad.log("Move PTP to " + frameName);
		kiwa.move(ptp(getApplicationData().getFrame(frameName))
					.setJointVelocityRel(relSpeed));
	}
	
	public void LIN(String frameName, double relSpeed) {
		pad.log("Move LIN to " + frameName);
		kiwa.move(lin(getApplicationData().getFrame(frameName))
					.setJointVelocityRel(relSpeed));
	}
	
	public void CIRC(String frameName1, String frameName2, double relSpeed) {
		logPad("Move CIRC to " + frameName1 + " then to " + frameName2);
		kiwa.move(circ(	getApplicationData().getFrame(frameName1),
						getApplicationData().getFrame(frameName2))
					.setJointVelocityRel(relSpeed));
	}
	
	public void PTPwithJTConds (Frame nextFrame, double relSpeed, ICondition JTConds){		// overloading for taught points
		IMotionContainer torqueBreakMotion, posHoldMotion;
		CartesianImpedanceControlMode softMode = new CartesianImpedanceControlMode();
		PositionHold posHold = new PositionHold(softMode, -1, null);  
		IFiredConditionInfo JTBreak;
		do {
			torqueBreakMotion = kiwa.move(ptp(nextFrame).setJointVelocityRel(relSpeed).breakWhen(JTConds)); 
			JTBreak = torqueBreakMotion.getFiredBreakConditionInfo();
			if (JTBreak != null) {
				logPad("Collision detected!"); 
				mf.saveRGB();
				mf.setRGB("R");
				mf.waitUserButton();
				posHoldMotion = kiwa.moveAsync(posHold);	// Enable unpinching
				mf.waitUserButton();
				posHoldMotion.cancel();
				PTPwithJTConds(nextFrame, relSpeed, JTConds);
				mf.resetRGB();
			}
		} while (JTBreak != null);
	}
	
	public void PTPwithJTConds (String framePath, double relSpeed, ICondition JTConds){
		ObjectFrame nextFrame = getApplicationData().getFrame(framePath);
		PTPwithJTConds(nextFrame.copyWithRedundancy(), relSpeed, JTConds);
	}
	
	public void setupJTconds (ICondition JTConds, double maxTorque){
		JointTorqueCondition JTCond[] = new JointTorqueCondition[8];
		JTCond[1] = new JointTorqueCondition(JointEnum.J1, -maxTorque, maxTorque);	
		JTCond[2] = new JointTorqueCondition(JointEnum.J2, -maxTorque, maxTorque);
		JTCond[3] = new JointTorqueCondition(JointEnum.J3, -maxTorque, maxTorque);	
		JTCond[4] = new JointTorqueCondition(JointEnum.J4, -maxTorque, maxTorque);
		JTCond[5] = new JointTorqueCondition(JointEnum.J5, -maxTorque, maxTorque);	
		JTCond[6] = new JointTorqueCondition(JointEnum.J6, -maxTorque, maxTorque);
		JTCond[7] = new JointTorqueCondition(JointEnum.J7, -maxTorque, maxTorque);
		JTConds = JTCond[1].or(JTCond[2]).or(JTCond[3]).or(JTCond[4]).or(JTCond[5]).or(JTCond[6]).or(JTCond[7]);
		pad.log("Max Axis Torque set to " + maxTorque + " Nm.");
	}
}