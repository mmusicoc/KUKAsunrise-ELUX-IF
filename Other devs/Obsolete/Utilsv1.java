package application.Utils;

import java.lang.*;
import java.util.concurrent.TimeUnit;		// For timers, delays, cycles
import java.util.logging.Logger;			// Message logger for SmarPad

import javax.inject.Inject;					// Java injection API
import javax.inject.Named;					// Same

import com.kuka.common.ThreadUtil;			// All
import com.kuka.task.ITaskLogger;			// Logger

// WorkVisual API for I/O signals
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;		// mfio class
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;

import com.kuka.roboticsAPI.controllerModel.Controller;		// All
import com.kuka.roboticsAPI.deviceModel.LBR;				// All

import com.kuka.roboticsAPI.geometricModel.Frame;			// For frame management (edit,add data)
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.CartDOF; 
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.Workpiece;

public class utils {
	
	public void waitMillis(int millis) {
		ThreadUtil.milliSleep(millis);
	}
	
	public void waitUserButton() {
		while (true) {									// Just for test
			if (mfio.getUserButton()) break;
			ThreadUtil.milliSleep(50);
		}
		ThreadUtil.milliSleep(1500);
	}
	
	public void setRGB(boolean r, boolean g, boolean b) {
		mfio.setLEDRed(r);
		mfio.setLEDGreen(g);
		mfio.setLEDBlue(b);
	}
	
	public void setRGB(String color) {
		if (color.equalsIgnoreCase("R") setRGB(1,0,0);
		else if (color.equalsIgnoreCase("G") setRGB(0,1,0);
		else if (color.equalsIgnoreCase("B") setRGB(0,0,1);
		else if (color.equalsIgnoreCase("RG") setRGB(1,1,0);
		else if (color.equalsIgnoreCase("RB") setRGB(1,0,1);
		else if (color.equalsIgnoreCase("GB") setRGB(0,1,1);
		else if (color.equalsIgnoreCase("RGB") setRGB(1,1,1);
		else if (color.equalsIgnoreCase("OFF") setRGB(0,0,0);
		else System.out.println("MediaFlange color not valid");
	}
	
	public void openGripper() {
		plcout.setPinza_Chiudi(false);
		ThreadUtil.milliSleep(10);
		plcout.setPinza_Apri(true);
		System.out.println("Opening gripper");
	}
	
	public void closeGripper() {
		plcout.setPinza_Apri(false);
		ThreadUtil.milliSleep(10);
		plcout.setPinza_Chiudi(true);
		System.out.println("Closing gripper");
	}
}