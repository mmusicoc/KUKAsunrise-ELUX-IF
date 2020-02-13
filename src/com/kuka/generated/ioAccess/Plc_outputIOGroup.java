package com.kuka.generated.ioAccess;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.ioModel.AbstractIOGroup;
import com.kuka.roboticsAPI.ioModel.IOTypes;
import com.kuka.roboticsAPI.ioModel.OutputReservedException;

/**
 * Automatically generated class to abstract I/O access to I/O group <b>Plc_output</b>.<br>
 * <i>Please, do not modify!</i>
 * <p>
 * <b>I/O group description:</b><br>
 * ./.
 */
@Singleton
public class Plc_outputIOGroup extends AbstractIOGroup
{
	/**
	 * Constructor to create an instance of class 'Plc_output'.<br>
	 * <i>This constructor is automatically generated. Please, do not modify!</i>
	 *
	 * @param controller
	 *            the controller, which has access to the I/O group 'Plc_output'
	 */
	@Inject
	public Plc_outputIOGroup(Controller controller)
	{
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

	/**
	 * Gets the value of the <b>digital output '<i>AutExt_Active</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'AutExt_Active'
	* 
	 * @deprecated The output 'AutExt_Active' is currently used as station state output in the Sunrise project properties.
	 */
	@Deprecated
	public boolean getAutExt_Active()
	{
		return getBooleanIOValue("AutExt_Active", true);
	}

	/**
	 * Always throws an {@code OutputReservedException}, because the <b>digital output '<i>AutExt_Active</i>'</b> is currently used as station state output in the Sunrise project properties.
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'AutExt_Active'
	 * @throws OutputReservedException
	 *            Always thrown, because this output is currently used as station state output in the Sunrise project properties.
	* 
	 * @deprecated The output 'AutExt_Active' is currently used as station state output in the Sunrise project properties.
	 */
	@Deprecated
	public void setAutExt_Active(java.lang.Boolean value) throws OutputReservedException
	{
		throw new OutputReservedException("The output 'AutExt_Active' must not be set because it is currently used as station state output in the Sunrise project properties.");
	}

	/**
	 * Gets the value of the <b>digital output '<i>AutExt_AppReadyToStart</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'AutExt_AppReadyToStart'
	* 
	 * @deprecated The output 'AutExt_AppReadyToStart' is currently used as station state output in the Sunrise project properties.
	 */
	@Deprecated
	public boolean getAutExt_AppReadyToStart()
	{
		return getBooleanIOValue("AutExt_AppReadyToStart", true);
	}

	/**
	 * Always throws an {@code OutputReservedException}, because the <b>digital output '<i>AutExt_AppReadyToStart</i>'</b> is currently used as station state output in the Sunrise project properties.
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'AutExt_AppReadyToStart'
	 * @throws OutputReservedException
	 *            Always thrown, because this output is currently used as station state output in the Sunrise project properties.
	* 
	 * @deprecated The output 'AutExt_AppReadyToStart' is currently used as station state output in the Sunrise project properties.
	 */
	@Deprecated
	public void setAutExt_AppReadyToStart(java.lang.Boolean value) throws OutputReservedException
	{
		throw new OutputReservedException("The output 'AutExt_AppReadyToStart' must not be set because it is currently used as station state output in the Sunrise project properties.");
	}

	/**
	 * Gets the value of the <b>digital output '<i>DefaultApp_Error</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'DefaultApp_Error'
	* 
	 * @deprecated The output 'DefaultApp_Error' is currently used as station state output in the Sunrise project properties.
	 */
	@Deprecated
	public boolean getDefaultApp_Error()
	{
		return getBooleanIOValue("DefaultApp_Error", true);
	}

	/**
	 * Always throws an {@code OutputReservedException}, because the <b>digital output '<i>DefaultApp_Error</i>'</b> is currently used as station state output in the Sunrise project properties.
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'DefaultApp_Error'
	 * @throws OutputReservedException
	 *            Always thrown, because this output is currently used as station state output in the Sunrise project properties.
	* 
	 * @deprecated The output 'DefaultApp_Error' is currently used as station state output in the Sunrise project properties.
	 */
	@Deprecated
	public void setDefaultApp_Error(java.lang.Boolean value) throws OutputReservedException
	{
		throw new OutputReservedException("The output 'DefaultApp_Error' must not be set because it is currently used as station state output in the Sunrise project properties.");
	}

	/**
	 * Gets the value of the <b>digital output '<i>Station_Error</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'Station_Error'
	* 
	 * @deprecated The output 'Station_Error' is currently used as station state output in the Sunrise project properties.
	 */
	@Deprecated
	public boolean getStation_Error()
	{
		return getBooleanIOValue("Station_Error", true);
	}

	/**
	 * Always throws an {@code OutputReservedException}, because the <b>digital output '<i>Station_Error</i>'</b> is currently used as station state output in the Sunrise project properties.
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Station_Error'
	 * @throws OutputReservedException
	 *            Always thrown, because this output is currently used as station state output in the Sunrise project properties.
	* 
	 * @deprecated The output 'Station_Error' is currently used as station state output in the Sunrise project properties.
	 */
	@Deprecated
	public void setStation_Error(java.lang.Boolean value) throws OutputReservedException
	{
		throw new OutputReservedException("The output 'Station_Error' must not be set because it is currently used as station state output in the Sunrise project properties.");
	}

	/**
	 * Gets the value of the <b>digital output '<i>Life_BitFBK</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'Life_BitFBK'
	 */
	public boolean getLife_BitFBK()
	{
		return getBooleanIOValue("Life_BitFBK", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Life_BitFBK</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Life_BitFBK'
	 */
	public void setLife_BitFBK(java.lang.Boolean value)
	{
		setDigitalOutput("Life_BitFBK", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>App_ResetDone</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'App_ResetDone'
	 */
	public boolean getApp_ResetDone()
	{
		return getBooleanIOValue("App_ResetDone", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>App_ResetDone</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'App_ResetDone'
	 */
	public void setApp_ResetDone(java.lang.Boolean value)
	{
		setDigitalOutput("App_ResetDone", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Robot_InHome</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'Robot_InHome'
	 */
	public boolean getRobot_InHome()
	{
		return getBooleanIOValue("Robot_InHome", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Robot_InHome</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Robot_InHome'
	 */
	public void setRobot_InHome(java.lang.Boolean value)
	{
		setDigitalOutput("Robot_InHome", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Pinza_Apri</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'Pinza_Apri'
	 */
	public boolean getPinza_Apri()
	{
		return getBooleanIOValue("Pinza_Apri", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Pinza_Apri</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Pinza_Apri'
	 */
	public void setPinza_Apri(java.lang.Boolean value)
	{
		setDigitalOutput("Pinza_Apri", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Pinza_Chiudi</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'Pinza_Chiudi'
	 */
	public boolean getPinza_Chiudi()
	{
		return getBooleanIOValue("Pinza_Chiudi", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Pinza_Chiudi</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Pinza_Chiudi'
	 */
	public void setPinza_Chiudi(java.lang.Boolean value)
	{
		setDigitalOutput("Pinza_Chiudi", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Mission_IndexFBK</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [0; 65535]
	 *
	 * @return current value of the digital output 'Mission_IndexFBK'
	 */
	public java.lang.Integer getMission_IndexFBK()
	{
		return getNumberIOValue("Mission_IndexFBK", true).intValue();
	}

	/**
	 * Sets the value of the <b>digital output '<i>Mission_IndexFBK</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [0; 65535]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Mission_IndexFBK'
	 */
	public void setMission_IndexFBK(java.lang.Integer value)
	{
		setDigitalOutput("Mission_IndexFBK", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Mission_Run</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'Mission_Run'
	 */
	public boolean getMission_Run()
	{
		return getBooleanIOValue("Mission_Run", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Mission_Run</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Mission_Run'
	 */
	public void setMission_Run(java.lang.Boolean value)
	{
		setDigitalOutput("Mission_Run", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Mission_AtPos</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'Mission_AtPos'
	 */
	public boolean getMission_AtPos()
	{
		return getBooleanIOValue("Mission_AtPos", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Mission_AtPos</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Mission_AtPos'
	 */
	public void setMission_AtPos(java.lang.Boolean value)
	{
		setDigitalOutput("Mission_AtPos", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Mission_ExitDone</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital output 'Mission_ExitDone'
	 */
	public boolean getMission_ExitDone()
	{
		return getBooleanIOValue("Mission_ExitDone", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Mission_ExitDone</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Mission_ExitDone'
	 */
	public void setMission_ExitDone(java.lang.Boolean value)
	{
		setDigitalOutput("Mission_ExitDone", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Mission_Result</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [0; 255]
	 *
	 * @return current value of the digital output 'Mission_Result'
	 */
	public java.lang.Integer getMission_Result()
	{
		return getNumberIOValue("Mission_Result", true).intValue();
	}

	/**
	 * Sets the value of the <b>digital output '<i>Mission_Result</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital output
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [0; 255]
	 *
	 * @param value
	 *            the value, which has to be written to the digital output 'Mission_Result'
	 */
	public void setMission_Result(java.lang.Integer value)
	{
		setDigitalOutput("Mission_Result", value);
	}

}
