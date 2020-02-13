package application.utils;

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

import com.kuka.roboticsAPI.applicationModel.*;
import com.kuka.roboticsAPI.controllerModel.Controller;		// All
import com.kuka.roboticsAPI.deviceModel.LBR;				// All

import com.kuka.roboticsAPI.geometricModel.Frame;			// For frame management (edit,add data)
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.CartDOF; 
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.Workpiece;

public class Utils{
	private Plc_inputIOGroup 		_plcin;
	private Plc_outputIOGroup 		_plcout;
	private MediaFlangeIOGroup 		_mfio;

	public Utils(Plc_inputIOGroup plcin, Plc_outputIOGroup plcout, MediaFlangeIOGroup mfio){
		_plcin = plcin;
		_plcout = plcout;
		_mfio = mfio;
	}
	
	public void waitMillis(int millis) {
		ThreadUtil.milliSleep(millis);
	}
	
	public void waitUserButton() {
		while (true) {
			if (_mfio.getUserButton()) break;
			ThreadUtil.milliSleep(50);
		}
		ThreadUtil.milliSleep(1500);
	}
	
	public void setRGB(boolean r, boolean g, boolean b) {
		_mfio.setLEDRed(r);
		_mfio.setLEDGreen(g);
		_mfio.setLEDBlue(b);
	}
	
	public void setRGB(String color) {
		if (color.equalsIgnoreCase("R")) {setRGB(true,false,false);}
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
		_plcout.setPinza_Chiudi(false);
		ThreadUtil.milliSleep(10);
		_plcout.setPinza_Apri(true);
		System.out.println("Opening gripper");
	}
	
	public void closeGripper() {
		_plcout.setPinza_Apri(false);
		ThreadUtil.milliSleep(10);
		_plcout.setPinza_Chiudi(true);
		System.out.println("Closing gripper");
	}
}