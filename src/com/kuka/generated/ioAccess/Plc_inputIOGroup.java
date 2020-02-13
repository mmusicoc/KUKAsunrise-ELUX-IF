package com.kuka.generated.ioAccess;

import javax.inject.Inject;
import javax.inject.Singleton;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.ioModel.AbstractIOGroup;
import com.kuka.roboticsAPI.ioModel.IOTypes;

@Singleton
public class Plc_inputIOGroup extends AbstractIOGroup {
	@Inject public Plc_inputIOGroup(Controller controller) {
		super(controller, "Plc_input");
		addInput("App_Enable", IOTypes.BOOLEAN, 1);
		addInput("App_Start", IOTypes.BOOLEAN, 1);
		addInput("Life_bit", IOTypes.BOOLEAN, 1);
		addInput("App_Reset", IOTypes.BOOLEAN, 1);
		addInput("App_Auto", IOTypes.BOOLEAN, 1);
		addInput("Mission_Index", IOTypes.UNSIGNED_INTEGER, 16);
		addInput("Mission_Start", IOTypes.BOOLEAN, 1);
		addInput("Mission_PosAllow", IOTypes.BOOLEAN, 1);
		addInput("Mission_ExitAllow", IOTypes.BOOLEAN, 1);
		addInput("Pinza_Idle", IOTypes.BOOLEAN, 1);
		addInput("Pinza_Gripping", IOTypes.BOOLEAN, 1);
		addInput("Pinza_NoPart", IOTypes.BOOLEAN, 1);
		addInput("Pinza_PartLost", IOTypes.BOOLEAN, 1);
		addInput("Pinza_Holding", IOTypes.BOOLEAN, 1);
		addInput("Pinza_Releasing", IOTypes.BOOLEAN, 1);
		addInput("Pinza_Positionig", IOTypes.BOOLEAN, 1);
		addInput("Pinza_Error", IOTypes.BOOLEAN, 1);
	}
	
	public boolean getApp_Enable() { return getBooleanIOValue("App_Enable", false); }
	public boolean getApp_Start() { return getBooleanIOValue("App_Start", false); }
	public boolean getLife_bit() { return getBooleanIOValue("Life_bit", false); }
	public boolean getApp_Reset() { return getBooleanIOValue("App_Reset", false); }
	public boolean getApp_Auto() { return getBooleanIOValue("App_Auto", false); }
	public java.lang.Integer getMission_Index() { return getNumberIOValue("Mission_Index", false).intValue(); }
	public boolean getMission_Start() { return getBooleanIOValue("Mission_Start", false); }
	public boolean getMission_PosAllow() { return getBooleanIOValue("Mission_PosAllow", false); }
	public boolean getMission_ExitAllow() { return getBooleanIOValue("Mission_ExitAllow", false); }
	public boolean getPinza_Idle() { return getBooleanIOValue("Pinza_Idle", false); }
	public boolean getPinza_Gripping() { return getBooleanIOValue("Pinza_Gripping", false); }
	public boolean getPinza_NoPart() { return getBooleanIOValue("Pinza_NoPart", false); }
	public boolean getPinza_PartLost() { return getBooleanIOValue("Pinza_PartLost", false); }
	public boolean getPinza_Holding() { return getBooleanIOValue("Pinza_Holding", false); }
	public boolean getPinza_Releasing() { return getBooleanIOValue("Pinza_Releasing", false); }
	public boolean getPinza_Positionig() { return getBooleanIOValue("Pinza_Positionig", false); }
	public boolean getPinza_Error() { return getBooleanIOValue("Pinza_Error", false); }
}
