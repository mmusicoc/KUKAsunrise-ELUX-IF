package com.kuka.generated.ioAccess;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.ioModel.AbstractIOGroup;
import com.kuka.roboticsAPI.ioModel.IOTypes;
import com.kuka.roboticsAPI.ioModel.OutputReservedException;

/**
 * Automatically generated class to abstract I/O access to I/O group <b>EthercatIO</b>.<br>
 * <i>Please, do not modify!</i>
 * <p>
 * <b>I/O group description:</b><br>
 * ./.
 */
@Singleton
public class EthercatIOIOGroup extends AbstractIOGroup
{
	/**
	 * Constructor to create an instance of class 'EthercatIO'.<br>
	 * <i>This constructor is automatically generated. Please, do not modify!</i>
	 *
	 * @param controller
	 *            the controller, which has access to the I/O group 'EthercatIO'
	 */
	@Inject
	public EthercatIOIOGroup(Controller controller)
	{
		super(controller, "EthercatIO");

		addInput("BtStartRobot", IOTypes.BOOLEAN, 1);
		addInput("NoVision", IOTypes.BOOLEAN, 1);
		addInput("BtnRestartRobot", IOTypes.BOOLEAN, 1);
		addInput("BtnScarto", IOTypes.BOOLEAN, 1);
		addInput("BtnRipristino", IOTypes.BOOLEAN, 1);
		addInput("ExtStart", IOTypes.BOOLEAN, 1);
		addInput("In_7", IOTypes.BOOLEAN, 1);
		addInput("In_8", IOTypes.BOOLEAN, 1);
		addInput("In_9", IOTypes.BOOLEAN, 1);
		addInput("In_10", IOTypes.BOOLEAN, 1);
		addInput("In_11", IOTypes.BOOLEAN, 1);
		addInput("In_12", IOTypes.BOOLEAN, 1);
		addInput("In_13", IOTypes.BOOLEAN, 1);
		addInput("In_14", IOTypes.BOOLEAN, 1);
		addInput("In_15", IOTypes.BOOLEAN, 1);
		addInput("In_16", IOTypes.BOOLEAN, 1);
		addDigitalOutput("LedStartRobot", IOTypes.BOOLEAN, 1);
		addMockedDigitalOutput("OutExtStart", IOTypes.BOOLEAN, 1);
		addDigitalOutput("LedRestartRobot", IOTypes.BOOLEAN, 1);
		addDigitalOutput("LedScarto", IOTypes.BOOLEAN, 1);
		addDigitalOutput("LedRipristino", IOTypes.BOOLEAN, 1);
		addDigitalOutput("IlluminatoreON", IOTypes.BOOLEAN, 1);
		addDigitalOutput("ApriPinza", IOTypes.BOOLEAN, 1);
		addDigitalOutput("ChiudiPinza", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Out_9", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Out_10", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Out_11", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Out_12", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Out_13", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Out_14", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Out_15", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Out_16", IOTypes.BOOLEAN, 1);
	}

	/**
	 * Gets the value of the <b>digital input '<i>BtStartRobot</i>'</b>.<br>
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
	 * @return current value of the digital input 'BtStartRobot'
	 */
	public boolean getBtStartRobot()
	{
		return getBooleanIOValue("BtStartRobot", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>NoVision</i>'</b>.<br>
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
	 * @return current value of the digital input 'NoVision'
	 */
	public boolean getNoVision()
	{
		return getBooleanIOValue("NoVision", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>BtnRestartRobot</i>'</b>.<br>
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
	 * @return current value of the digital input 'BtnRestartRobot'
	 */
	public boolean getBtnRestartRobot()
	{
		return getBooleanIOValue("BtnRestartRobot", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>BtnScarto</i>'</b>.<br>
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
	 * @return current value of the digital input 'BtnScarto'
	 */
	public boolean getBtnScarto()
	{
		return getBooleanIOValue("BtnScarto", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>BtnRipristino</i>'</b>.<br>
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
	 * @return current value of the digital input 'BtnRipristino'
	 */
	public boolean getBtnRipristino()
	{
		return getBooleanIOValue("BtnRipristino", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>ExtStart</i>'</b>.<br>
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
	 * @return current value of the digital input 'ExtStart'
	 */
	public boolean getExtStart()
	{
		return getBooleanIOValue("ExtStart", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>In_7</i>'</b>.<br>
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
	 * @return current value of the digital input 'In_7'
	 */
	public boolean getIn_7()
	{
		return getBooleanIOValue("In_7", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>In_8</i>'</b>.<br>
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
	 * @return current value of the digital input 'In_8'
	 */
	public boolean getIn_8()
	{
		return getBooleanIOValue("In_8", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>In_9</i>'</b>.<br>
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
	 * @return current value of the digital input 'In_9'
	 */
	public boolean getIn_9()
	{
		return getBooleanIOValue("In_9", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>In_10</i>'</b>.<br>
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
	 * @return current value of the digital input 'In_10'
	 */
	public boolean getIn_10()
	{
		return getBooleanIOValue("In_10", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>In_11</i>'</b>.<br>
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
	 * @return current value of the digital input 'In_11'
	 */
	public boolean getIn_11()
	{
		return getBooleanIOValue("In_11", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>In_12</i>'</b>.<br>
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
	 * @return current value of the digital input 'In_12'
	 */
	public boolean getIn_12()
	{
		return getBooleanIOValue("In_12", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>In_13</i>'</b>.<br>
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
	 * @return current value of the digital input 'In_13'
	 */
	public boolean getIn_13()
	{
		return getBooleanIOValue("In_13", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>In_14</i>'</b>.<br>
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
	 * @return current value of the digital input 'In_14'
	 */
	public boolean getIn_14()
	{
		return getBooleanIOValue("In_14", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>In_15</i>'</b>.<br>
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
	 * @return current value of the digital input 'In_15'
	 */
	public boolean getIn_15()
	{
		return getBooleanIOValue("In_15", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>In_16</i>'</b>.<br>
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
	 * @return current value of the digital input 'In_16'
	 */
	public boolean getIn_16()
	{
		return getBooleanIOValue("In_16", false);
	}

	/**
	 * Gets the value of the <b>digital output '<i>LedStartRobot</i>'</b>.<br>
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
	 * @return current value of the digital output 'LedStartRobot'
	 */
	public boolean getLedStartRobot()
	{
		return getBooleanIOValue("LedStartRobot", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>LedStartRobot</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'LedStartRobot'
	 */
	public void setLedStartRobot(java.lang.Boolean value)
	{
		setDigitalOutput("LedStartRobot", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>OutExtStart</i>'</b>.<br>
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
	 * @return current value of the digital output 'OutExtStart'
	* 
	 * @deprecated The output 'OutExtStart' is currently used as station state output in the Sunrise project properties.
	 */
	@Deprecated
	public boolean getOutExtStart()
	{
		return getBooleanIOValue("OutExtStart", true);
	}

	/**
	 * Always throws an {@code OutputReservedException}, because the <b>digital output '<i>OutExtStart</i>'</b> is currently used as station state output in the Sunrise project properties.
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
	 *            the value, which has to be written to the digital output 'OutExtStart'
	 * @throws OutputReservedException
	 *            Always thrown, because this output is currently used as station state output in the Sunrise project properties.
	* 
	 * @deprecated The output 'OutExtStart' is currently used as station state output in the Sunrise project properties.
	 */
	@Deprecated
	public void setOutExtStart(java.lang.Boolean value) throws OutputReservedException
	{
		throw new OutputReservedException("The output 'OutExtStart' must not be set because it is currently used as station state output in the Sunrise project properties.");
	}

	/**
	 * Gets the value of the <b>digital output '<i>LedRestartRobot</i>'</b>.<br>
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
	 * @return current value of the digital output 'LedRestartRobot'
	 */
	public boolean getLedRestartRobot()
	{
		return getBooleanIOValue("LedRestartRobot", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>LedRestartRobot</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'LedRestartRobot'
	 */
	public void setLedRestartRobot(java.lang.Boolean value)
	{
		setDigitalOutput("LedRestartRobot", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>LedScarto</i>'</b>.<br>
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
	 * @return current value of the digital output 'LedScarto'
	 */
	public boolean getLedScarto()
	{
		return getBooleanIOValue("LedScarto", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>LedScarto</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'LedScarto'
	 */
	public void setLedScarto(java.lang.Boolean value)
	{
		setDigitalOutput("LedScarto", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>LedRipristino</i>'</b>.<br>
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
	 * @return current value of the digital output 'LedRipristino'
	 */
	public boolean getLedRipristino()
	{
		return getBooleanIOValue("LedRipristino", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>LedRipristino</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'LedRipristino'
	 */
	public void setLedRipristino(java.lang.Boolean value)
	{
		setDigitalOutput("LedRipristino", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>IlluminatoreON</i>'</b>.<br>
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
	 * @return current value of the digital output 'IlluminatoreON'
	 */
	public boolean getIlluminatoreON()
	{
		return getBooleanIOValue("IlluminatoreON", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>IlluminatoreON</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'IlluminatoreON'
	 */
	public void setIlluminatoreON(java.lang.Boolean value)
	{
		setDigitalOutput("IlluminatoreON", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>ApriPinza</i>'</b>.<br>
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
	 * @return current value of the digital output 'ApriPinza'
	 */
	public boolean getApriPinza()
	{
		return getBooleanIOValue("ApriPinza", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>ApriPinza</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'ApriPinza'
	 */
	public void setApriPinza(java.lang.Boolean value)
	{
		setDigitalOutput("ApriPinza", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>ChiudiPinza</i>'</b>.<br>
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
	 * @return current value of the digital output 'ChiudiPinza'
	 */
	public boolean getChiudiPinza()
	{
		return getBooleanIOValue("ChiudiPinza", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>ChiudiPinza</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'ChiudiPinza'
	 */
	public void setChiudiPinza(java.lang.Boolean value)
	{
		setDigitalOutput("ChiudiPinza", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Out_9</i>'</b>.<br>
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
	 * @return current value of the digital output 'Out_9'
	 */
	public boolean getOut_9()
	{
		return getBooleanIOValue("Out_9", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Out_9</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'Out_9'
	 */
	public void setOut_9(java.lang.Boolean value)
	{
		setDigitalOutput("Out_9", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Out_10</i>'</b>.<br>
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
	 * @return current value of the digital output 'Out_10'
	 */
	public boolean getOut_10()
	{
		return getBooleanIOValue("Out_10", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Out_10</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'Out_10'
	 */
	public void setOut_10(java.lang.Boolean value)
	{
		setDigitalOutput("Out_10", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Out_11</i>'</b>.<br>
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
	 * @return current value of the digital output 'Out_11'
	 */
	public boolean getOut_11()
	{
		return getBooleanIOValue("Out_11", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Out_11</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'Out_11'
	 */
	public void setOut_11(java.lang.Boolean value)
	{
		setDigitalOutput("Out_11", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Out_12</i>'</b>.<br>
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
	 * @return current value of the digital output 'Out_12'
	 */
	public boolean getOut_12()
	{
		return getBooleanIOValue("Out_12", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Out_12</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'Out_12'
	 */
	public void setOut_12(java.lang.Boolean value)
	{
		setDigitalOutput("Out_12", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Out_13</i>'</b>.<br>
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
	 * @return current value of the digital output 'Out_13'
	 */
	public boolean getOut_13()
	{
		return getBooleanIOValue("Out_13", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Out_13</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'Out_13'
	 */
	public void setOut_13(java.lang.Boolean value)
	{
		setDigitalOutput("Out_13", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Out_14</i>'</b>.<br>
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
	 * @return current value of the digital output 'Out_14'
	 */
	public boolean getOut_14()
	{
		return getBooleanIOValue("Out_14", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Out_14</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'Out_14'
	 */
	public void setOut_14(java.lang.Boolean value)
	{
		setDigitalOutput("Out_14", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Out_15</i>'</b>.<br>
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
	 * @return current value of the digital output 'Out_15'
	 */
	public boolean getOut_15()
	{
		return getBooleanIOValue("Out_15", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Out_15</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'Out_15'
	 */
	public void setOut_15(java.lang.Boolean value)
	{
		setDigitalOutput("Out_15", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Out_16</i>'</b>.<br>
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
	 * @return current value of the digital output 'Out_16'
	 */
	public boolean getOut_16()
	{
		return getBooleanIOValue("Out_16", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Out_16</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'Out_16'
	 */
	public void setOut_16(java.lang.Boolean value)
	{
		setDigitalOutput("Out_16", value);
	}

}
