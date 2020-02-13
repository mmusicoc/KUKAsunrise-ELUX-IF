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
//import com.kuka.roboticsAPI.motionModel.PositionHold;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;

public class HandlerMov extends RoboticsAPIApplication {
	// Standard KUKA API objects
	@Override public void run() { while (true) { break; } }
	@Inject private LBR kiwa;
	@Inject private HandlerMFio mf;
	
	// Private properties
	private ICondition JTConds;
	
	@Inject			// CONSTRUCTOR
	public HandlerMov(HandlerMFio _mf) {
		this.mf = _mf;
	}
	
	/***************************************************************************
	* STANDARD METHODS BY mario.musico@electrolux.com <p>
	***************************************************************************/
	
// Standard moves
	public void setHome(String frameName){
		kiwa.setHomePosition(getApplicationData().getFrame(frameName));
	}
	
	public void PTP(String frameName, double relSpeed) {
		padLog("Move PTP to " + frameName);
		kiwa.move(ptp(getApplicationData().getFrame(frameName))
					.setJointVelocityRel(relSpeed));
	}
	
	public void LIN(String frameName, double relSpeed) {
		padLog("Move LIN to " + frameName);
		kiwa.move(lin(getApplicationData().getFrame(frameName))
					.setJointVelocityRel(relSpeed));
	}
	
	public void CIRC(String frameName1, String frameName2, double relSpeed) {
		padLog("Move CIRC to " + frameName1 + " then to " + frameName2);
		kiwa.move(circ(	getApplicationData().getFrame(frameName1),
						getApplicationData().getFrame(frameName2))
					.setJointVelocityRel(relSpeed));
	}
	
	public ICondition getJTConds() { return this.JTConds; }
	
	public void setJTConds(double maxTorque){
		JointTorqueCondition JTCond[] = new JointTorqueCondition[8];
		JTCond[1] = new JointTorqueCondition(JointEnum.J1, -maxTorque, maxTorque);	
		JTCond[2] = new JointTorqueCondition(JointEnum.J2, -maxTorque, maxTorque);
		JTCond[3] = new JointTorqueCondition(JointEnum.J3, -maxTorque, maxTorque);	
		JTCond[4] = new JointTorqueCondition(JointEnum.J4, -maxTorque, maxTorque);
		JTCond[5] = new JointTorqueCondition(JointEnum.J5, -maxTorque, maxTorque);	
		JTCond[6] = new JointTorqueCondition(JointEnum.J6, -maxTorque, maxTorque);
		JTCond[7] = new JointTorqueCondition(JointEnum.J7, -maxTorque, maxTorque);
		JTConds = JTCond[1].or(JTCond[2]).or(JTCond[3]).or(JTCond[4]).or(JTCond[5]).or(JTCond[6]).or(JTCond[7]);
		padLog("Max Axis Torque set to " + maxTorque + " Nm.");
	}
	
	public void PTPwithJTConds(Frame nextFrame, double relSpeed){		// overloading for taught points
		IMotionContainer torqueBreakMotion;
		IFiredConditionInfo JTBreak;
		CartesianImpedanceControlMode softMode = new CartesianImpedanceControlMode();
		softMode.parametrize(CartDOF.TRANSL).setStiffness(300).setDamping(0.9);
		softMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(0.9);
		do {
			mf.setRGB("G");
			torqueBreakMotion = kiwa.move(ptp(nextFrame).setJointVelocityRel(relSpeed).breakWhen(this.JTConds)); 
			JTBreak = torqueBreakMotion.getFiredBreakConditionInfo();
			if (JTBreak != null) {
				mf.setRGB("R");
				kiwa.move(linRel(0, 0, -30).setJointVelocityRel(1));
				padLog("Collision detected!"); 
				mf.waitUserButton();
				PTPwithJTConds(nextFrame, relSpeed);
			}
		} while (JTBreak != null);
	}
	
	public void PTPwithJTConds(String framePath, double relSpeed){
		ObjectFrame nextFrame = getApplicationData().getFrame(framePath);
		this.PTPwithJTConds(nextFrame.copyWithRedundancy(), relSpeed);
	}
	
	public void LINRELwithJTConds(double x, double y, double z, double reducedSpeed) {
		int holeFindError = 7;
		IMotionContainer torqueBreakMotion;
		IFiredConditionInfo JTBreak;
		Frame currentFrame, targetFrame;
		ICondition tempJTConds = this.JTConds;
		this.setJTConds(3);
		mf.setRGB("G");
		torqueBreakMotion = kiwa.move(linRel(x, y, z).setJointVelocityRel(reducedSpeed).breakWhen(JTConds)); 
		do {
			JTBreak = torqueBreakMotion.getFiredBreakConditionInfo();
			if (JTBreak != null) {
				currentFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
				targetFrame = kiwa.getCommandedCartesianPosition(kiwa.getFlange());
				if (Math.abs(currentFrame.getX() - targetFrame.getX()) < holeFindError && 
					Math.abs(currentFrame.getY() - targetFrame.getY()) < holeFindError && 
					Math.abs(currentFrame.getZ() - targetFrame.getZ()) < holeFindError) {
					padLog("Collision inside error margin for Pin/Hole search.");
					mf.blinkRGB("GB", 500);
					break;
				} else {
					padLog("Collision detected!"); 
					mf.setRGB("R");
					kiwa.move(linRel(0, 0, -30).setJointVelocityRel(reducedSpeed));
					mf.waitUserButton();
					mf.setRGB("G");
					torqueBreakMotion = kiwa.move(lin(targetFrame).setJointVelocityRel(reducedSpeed).breakWhen(JTConds));
				}
			}
		} while (JTBreak != null);
		this.JTConds = tempJTConds;
	}
}