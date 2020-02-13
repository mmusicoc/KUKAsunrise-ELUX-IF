package application;

import javax.inject.Inject; 
import javax.inject.Named;

import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import static com.kuka.roboticsAPI.motionModel.HRCMotions.*;

import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;
import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.CartDOF; 
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.Workpiece;
import com.kuka.roboticsAPI.motionModel.*;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.motionModel.controlModeModel.HandGuidingControlMode;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyBar;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyAlignment;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyEvent;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyLED;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyLEDSize;
import com.kuka.task.ITaskLogger;
import com.sun.java.swing.plaf.nimbus.ButtonPainter;
import com.sun.org.apache.bcel.internal.generic.Select;

public class FrameWrapper {
	private Frame[] elems;
	private int counter = 0;
	
	public FrameWrapper() {	elems = new Frame[1]; }				// Constructor method
	public int GetCounter() { return counter; }					// Get no. of frames stored in list
	public Frame GetFrame(int index) { return elems[index]; }	// Return frame given its index no.
	public Frame Last() { return elems[counter-1]; }			// Return last frame in list
	
	public void Add(Frame f) {
		if(elems.length <= counter) Resize();
		elems[counter] = f;
		counter++;
	}
	
	private void Resize() {
		Frame[] temp = new Frame[2*elems.length];
		for(int i = 0; i < counter; i++) temp[i] = elems[i];
		elems = temp;
	}
	
	public void Free() {
		elems = new Frame[1];
		counter = 0;
	}
	
	public void deleteLastFrame (){
		if(counter <= 0) {
			System.out.println("No Frames saved.");
			counter = 0;
		} else {
			System.out.println("Deleted Frame "+counter+" : " + this.Last().toString());
			counter--;
		}
	}
}
