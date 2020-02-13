package com.kuka.generated.ioAccess;

import javax.inject.Inject;
import javax.inject.Singleton;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.ioModel.AbstractIOGroup;
import com.kuka.roboticsAPI.ioModel.IOTypes;
import com.kuka.roboticsAPI.ioModel.OutputReservedException;

@Singleton 
public class Plc_outputIOGroup extends AbstractIOGroup {
	@Inject public Plc_outputIOGroup(Controller controller) {
		super(controller, "Plc_output");
		addMockedDigitalOutput("AutExt_Active", IOTypes.BOOLEAN, 1);
		addMockedDigitalOutput("AutExt_AppReadyToStart", IOTypes.BOOLEAN, 1);
		addMockedDigitalOutput("DefaultApp_Error", IOTypes.BOOLEAN, 1);
		addMockedDigitalOutput("Station_Error", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Life_BitFBK", IOTypes.BOOLEAN, 1);
		addDigitalOutput("App_ResetDone", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Robot_InHome", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Pinza_Apri", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Pinza_Chiudi", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Mission_IndexFBK", IOTypes.UNSIGNED_INTEGER, 16);
		addDigitalOutput("Mission_Run", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Mission_AtPos", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Mission_ExitDone", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Mission_Result", IOTypes.UNSIGNED_INTEGER, 8);
	}
	@Deprecated
	public boolean getAutExt_Active() { return getBooleanIOValue("AutExt_Active", true); }
	@Deprecated
	public void setAutExt_Active(java.lang.Boolean value) throws OutputReservedException {
		throw new OutputReservedException("The output 'AutExt_Active' must not be set because it is currently used as station state output in the Sunrise project properties."); }
	@Deprecated
	public boolean getAutExt_AppReadyToStart() { return getBooleanIOValue("AutExt_AppReadyToStart", true); }
	@Deprecated
	public void setAutExt_AppReadyToStart(java.lang.Boolean value) throws OutputReservedException {
		throw new OutputReservedException("The output 'AutExt_AppReadyToStart' must not be set because it is currently used as station state output in the Sunrise project properties."); }
	@Deprecated
	public boolean getDefaultApp_Error() { return getBooleanIOValue("DefaultApp_Error", true); }
	@Deprecated
	public void setDefaultApp_Error(java.lang.Boolean value) throws OutputReservedException {
		throw new OutputReservedException("The output 'DefaultApp_Error' must not be set because it is currently used as station state output in the Sunrise project properties."); }
	@Deprecated
	public boolean getStation_Error() { return getBooleanIOValue("Station_Error", true); }
	@Deprecated
	public void setStation_Error(java.lang.Boolean value) throws OutputReservedException {
		throw new OutputReservedException("The output 'Station_Error' must not be set because it is currently used as station state output in the Sunrise project properties."); }
	public boolean getLife_BitFBK() {
		return getBooleanIOValue("Life_BitFBK", true); }
	public void setLife_BitFBK(java.lang.Boolean value) { setDigitalOutput("Life_BitFBK", value); }
	public boolean getApp_ResetDone() { return getBooleanIOValue("App_ResetDone", true); }
	public void setApp_ResetDone(java.lang.Boolean value) { setDigitalOutput("App_ResetDone", value); }
	public boolean getRobot_InHome() { return getBooleanIOValue("Robot_InHome", true); }
	public void setRobot_InHome(java.lang.Boolean value) { setDigitalOutput("Robot_InHome", value); }
	public boolean getPinza_Apri() { return getBooleanIOValue("Pinza_Apri", true); }
	public void setPinza_Apri(java.lang.Boolean value) { setDigitalOutput("Pinza_Apri", value); }
	public boolean getPinza_Chiudi() { return getBooleanIOValue("Pinza_Chiudi", true); }
	public void setPinza_Chiudi(java.lang.Boolean value) { setDigitalOutput("Pinza_Chiudi", value); }
	public java.lang.Integer getMission_IndexFBK() { return getNumberIOValue("Mission_IndexFBK", true).intValue(); }
	public void setMission_IndexFBK(java.lang.Integer value) { setDigitalOutput("Mission_IndexFBK", value); }
	public boolean getMission_Run() { return getBooleanIOValue("Mission_Run", true); }
	public void setMission_Run(java.lang.Boolean value) { setDigitalOutput("Mission_Run", value); }
	public boolean getMission_AtPos() { return getBooleanIOValue("Mission_AtPos", true); }
	public void setMission_AtPos(java.lang.Boolean value) { setDigitalOutput("Mission_AtPos", value); }
	public boolean getMission_ExitDone() { return getBooleanIOValue("Mission_ExitDone", true); }
	public void setMission_ExitDone(java.lang.Boolean value) { setDigitalOutput("Mission_ExitDone", value); }
	public java.lang.Integer getMission_Result() { return getNumberIOValue("Mission_Result", true).intValue(); }
	public void setMission_Result(java.lang.Integer value) { setDigitalOutput("Mission_Result", value); }
}
