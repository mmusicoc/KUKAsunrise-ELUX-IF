package com.kuka.generated.ioAccess;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.ioModel.AbstractIOGroup;
import com.kuka.roboticsAPI.ioModel.IOTypes;

/**
 * Automatically generated class to abstract I/O access to I/O group <b>Plc_input</b>.<br>
 * <i>Please, do not modify!</i>
 * <p>
 * <b>I/O group description:</b><br>
 * Plc_input
 */
@Singleton
public class Plc_inputIOGroup extends AbstractIOGroup
{
	/**
	 * Constructor to create an instance of class 'Plc_input'.<br>
	 * <i>This constructor is automatically generated. Please, do not modify!</i>
	 *
	 * @param controller
	 *            the controller, which has access to the I/O group 'Plc_input'
	 */
	@Inject
	public Plc_inputIOGroup(Controller controller)
	{
		super(controller, "Plc_input");

		addInput("App_Auto", IOTypes.BOOLEAN, 1);
		addInput("App_Enable", IOTypes.BOOLEAN, 1);
		addInput("App_Reset", IOTypes.BOOLEAN, 1);
		addInput("App_Start", IOTypes.BOOLEAN, 1);
		addInput("Life_bit", IOTypes.BOOLEAN, 1);
		addInput("Mission_ExitAllow", IOTypes.BOOLEAN, 1);
		addInput("Mission_Index", IOTypes.UNSIGNED_INTEGER, 16);
		addInput("Mission_PosAllow", IOTypes.BOOLEAN, 1);
		addInput("Mission_Start", IOTypes.BOOLEAN, 1);
		addInput("Pinza_Error", IOTypes.BOOLEAN, 1);
		addInput("Pinza_Gripping", IOTypes.BOOLEAN, 1);
		addInput("Pinza_Holding", IOTypes.BOOLEAN, 1);
		addInput("Pinza_Idle", IOTypes.BOOLEAN, 1);
		addInput("Pinza_NoPart", IOTypes.BOOLEAN, 1);
		addInput("Pinza_PartLost", IOTypes.BOOLEAN, 1);
		addInput("Pinza_Positionig", IOTypes.BOOLEAN, 1);
		addInput("Pinza_Releasing", IOTypes.BOOLEAN, 1);
	}

	/**
	 * Gets the value of the <b>digital input '<i>App_Auto</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'App_Auto'
	 */
	public boolean getApp_Auto()
	{
		return getBooleanIOValue("App_Auto", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>App_Enable</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'App_Enable'
	 */
	public boolean getApp_Enable()
	{
		return getBooleanIOValue("App_Enable", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>App_Reset</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'App_Reset'
	 */
	public boolean getApp_Reset()
	{
		return getBooleanIOValue("App_Reset", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>App_Start</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'App_Start'
	 */
	public boolean getApp_Start()
	{
		return getBooleanIOValue("App_Start", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Life_bit</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Life_bit'
	 */
	public boolean getLife_bit()
	{
		return getBooleanIOValue("Life_bit", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Mission_ExitAllow</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Mission_ExitAllow'
	 */
	public boolean getMission_ExitAllow()
	{
		return getBooleanIOValue("Mission_ExitAllow", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Mission_Index</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [0; 65535]
	 *
	 * @return current value of the digital input 'Mission_Index'
	 */
	public java.lang.Integer getMission_Index()
	{
		return getNumberIOValue("Mission_Index", false).intValue();
	}

	/**
	 * Gets the value of the <b>digital input '<i>Mission_PosAllow</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Mission_PosAllow'
	 */
	public boolean getMission_PosAllow()
	{
		return getBooleanIOValue("Mission_PosAllow", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Mission_Start</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Mission_Start'
	 */
	public boolean getMission_Start()
	{
		return getBooleanIOValue("Mission_Start", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Pinza_Error</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Pinza_Error'
	 */
	public boolean getPinza_Error()
	{
		return getBooleanIOValue("Pinza_Error", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Pinza_Gripping</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Pinza_Gripping'
	 */
	public boolean getPinza_Gripping()
	{
		return getBooleanIOValue("Pinza_Gripping", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Pinza_Holding</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Pinza_Holding'
	 */
	public boolean getPinza_Holding()
	{
		return getBooleanIOValue("Pinza_Holding", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Pinza_Idle</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Pinza_Idle'
	 */
	public boolean getPinza_Idle()
	{
		return getBooleanIOValue("Pinza_Idle", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Pinza_NoPart</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Pinza_NoPart'
	 */
	public boolean getPinza_NoPart()
	{
		return getBooleanIOValue("Pinza_NoPart", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Pinza_PartLost</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Pinza_PartLost'
	 */
	public boolean getPinza_PartLost()
	{
		return getBooleanIOValue("Pinza_PartLost", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Pinza_Positionig</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Pinza_Positionig'
	 */
	public boolean getPinza_Positionig()
	{
		return getBooleanIOValue("Pinza_Positionig", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>Pinza_Releasing</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [false; true]
	 *
	 * @return current value of the digital input 'Pinza_Releasing'
	 */
	public boolean getPinza_Releasing()
	{
		return getBooleanIOValue("Pinza_Releasing", false);
	}

}
