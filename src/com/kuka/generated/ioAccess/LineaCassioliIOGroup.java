package com.kuka.generated.ioAccess;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.ioModel.AbstractIOGroup;
import com.kuka.roboticsAPI.ioModel.IOTypes;

/**
 * Automatically generated class to abstract I/O access to I/O group <b>LineaCassioli</b>.<br>
 * <i>Please, do not modify!</i>
 * <p>
 * <b>I/O group description:</b><br>
 * Linea profibus interfaccia Cassioli
 */
@Singleton
public class LineaCassioliIOGroup extends AbstractIOGroup
{
	/**
	 * Constructor to create an instance of class 'LineaCassioli'.<br>
	 * <i>This constructor is automatically generated. Please, do not modify!</i>
	 *
	 * @param controller
	 *            the controller, which has access to the I/O group 'LineaCassioli'
	 */
	@Inject
	public LineaCassioliIOGroup(Controller controller)
	{
		super(controller, "LineaCassioli");

		addInput("CodiceModelloIN", IOTypes.UNSIGNED_INTEGER, 8);
		addInput("FreeIn1", IOTypes.BOOLEAN, 1);
		addDigitalOutput("CodiceModelloOut", IOTypes.UNSIGNED_INTEGER, 8);
		addDigitalOutput("EsitoOK", IOTypes.BOOLEAN, 1);
		addInput("LBInPos", IOTypes.BOOLEAN, 1);
		addInput("RobotEscluso", IOTypes.BOOLEAN, 1);
		addInput("LineaAuto", IOTypes.BOOLEAN, 1);
		addDigitalOutput("FineCiclo", IOTypes.BOOLEAN, 1);
		addDigitalOutput("FuoriIngombro", IOTypes.BOOLEAN, 1);
		addDigitalOutput("RobotNOK", IOTypes.BOOLEAN, 1);
		addDigitalOutput("Reset", IOTypes.BOOLEAN, 1);
	}

	/**
	 * Gets the value of the <b>digital input '<i>CodiceModelloIN</i>'</b>.<br>
	 * <i>This method is automatically generated. Please, do not modify!</i>
	 * <p>
	 * <b>I/O direction and type:</b><br>
	 * digital input
	 * <p>
	 * <b>User description of the I/O:</b><br>
	 * ./.
	 * <p>
	 * <b>Range of the I/O value:</b><br>
	 * [0; 255]
	 *
	 * @return current value of the digital input 'CodiceModelloIN'
	 */
	public java.lang.Integer getCodiceModelloIN()
	{
		return getNumberIOValue("CodiceModelloIN", false).intValue();
	}

	/**
	 * Gets the value of the <b>digital input '<i>FreeIn1</i>'</b>.<br>
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
	 * @return current value of the digital input 'FreeIn1'
	 */
	public boolean getFreeIn1()
	{
		return getBooleanIOValue("FreeIn1", false);
	}

	/**
	 * Gets the value of the <b>digital output '<i>CodiceModelloOut</i>'</b>.<br>
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
	 * @return current value of the digital output 'CodiceModelloOut'
	 */
	public java.lang.Integer getCodiceModelloOut()
	{
		return getNumberIOValue("CodiceModelloOut", true).intValue();
	}

	/**
	 * Sets the value of the <b>digital output '<i>CodiceModelloOut</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'CodiceModelloOut'
	 */
	public void setCodiceModelloOut(java.lang.Integer value)
	{
		setDigitalOutput("CodiceModelloOut", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>EsitoOK</i>'</b>.<br>
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
	 * @return current value of the digital output 'EsitoOK'
	 */
	public boolean getEsitoOK()
	{
		return getBooleanIOValue("EsitoOK", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>EsitoOK</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'EsitoOK'
	 */
	public void setEsitoOK(java.lang.Boolean value)
	{
		setDigitalOutput("EsitoOK", value);
	}

	/**
	 * Gets the value of the <b>digital input '<i>LBInPos</i>'</b>.<br>
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
	 * @return current value of the digital input 'LBInPos'
	 */
	public boolean getLBInPos()
	{
		return getBooleanIOValue("LBInPos", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>RobotEscluso</i>'</b>.<br>
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
	 * @return current value of the digital input 'RobotEscluso'
	 */
	public boolean getRobotEscluso()
	{
		return getBooleanIOValue("RobotEscluso", false);
	}

	/**
	 * Gets the value of the <b>digital input '<i>LineaAuto</i>'</b>.<br>
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
	 * @return current value of the digital input 'LineaAuto'
	 */
	public boolean getLineaAuto()
	{
		return getBooleanIOValue("LineaAuto", false);
	}

	/**
	 * Gets the value of the <b>digital output '<i>FineCiclo</i>'</b>.<br>
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
	 * @return current value of the digital output 'FineCiclo'
	 */
	public boolean getFineCiclo()
	{
		return getBooleanIOValue("FineCiclo", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>FineCiclo</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'FineCiclo'
	 */
	public void setFineCiclo(java.lang.Boolean value)
	{
		setDigitalOutput("FineCiclo", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>FuoriIngombro</i>'</b>.<br>
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
	 * @return current value of the digital output 'FuoriIngombro'
	 */
	public boolean getFuoriIngombro()
	{
		return getBooleanIOValue("FuoriIngombro", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>FuoriIngombro</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'FuoriIngombro'
	 */
	public void setFuoriIngombro(java.lang.Boolean value)
	{
		setDigitalOutput("FuoriIngombro", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>RobotNOK</i>'</b>.<br>
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
	 * @return current value of the digital output 'RobotNOK'
	 */
	public boolean getRobotNOK()
	{
		return getBooleanIOValue("RobotNOK", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>RobotNOK</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'RobotNOK'
	 */
	public void setRobotNOK(java.lang.Boolean value)
	{
		setDigitalOutput("RobotNOK", value);
	}

	/**
	 * Gets the value of the <b>digital output '<i>Reset</i>'</b>.<br>
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
	 * @return current value of the digital output 'Reset'
	 */
	public boolean getReset()
	{
		return getBooleanIOValue("Reset", true);
	}

	/**
	 * Sets the value of the <b>digital output '<i>Reset</i>'</b>.<br>
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
	 *            the value, which has to be written to the digital output 'Reset'
	 */
	public void setReset(java.lang.Boolean value)
	{
		setDigitalOutput("Reset", value);
	}

}
