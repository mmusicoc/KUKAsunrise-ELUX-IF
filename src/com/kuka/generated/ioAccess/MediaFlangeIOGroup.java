package com.kuka.generated.ioAccess;

import javax.inject.Inject;
import javax.inject.Singleton;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.ioModel.AbstractIOGroup;
import com.kuka.roboticsAPI.ioModel.IOTypes;

@Singleton
public class MediaFlangeIOGroup extends AbstractIOGroup {
	@Inject public MediaFlangeIOGroup(Controller controller) {
		super(controller, "MediaFlange");
		addInput("InputX3Pin3", IOTypes.BOOLEAN, 1);
		addInput("InputX3Pin4", IOTypes.BOOLEAN, 1);
		addInput("InputX3Pin10", IOTypes.BOOLEAN, 1);
		addInput("InputX3Pin13", IOTypes.BOOLEAN, 1);
		addInput("InputX3Pin16", IOTypes.BOOLEAN, 1);
		addInput("UserButton", IOTypes.BOOLEAN, 1);
		addDigitalOutput("LEDBlue", IOTypes.BOOLEAN, 1);
		addDigitalOutput("SwitchOffX3Voltage", IOTypes.BOOLEAN, 1);
		addDigitalOutput("OutputX3Pin1", IOTypes.BOOLEAN, 1);
		addDigitalOutput("OutputX3Pin2", IOTypes.BOOLEAN, 1);
		addDigitalOutput("OutputX3Pin11", IOTypes.BOOLEAN, 1);
		addDigitalOutput("OutputX3Pin12", IOTypes.BOOLEAN, 1);
		addDigitalOutput("LEDGreen", IOTypes.BOOLEAN, 1);
		addDigitalOutput("LEDRed", IOTypes.BOOLEAN, 1);
	}

	public boolean getInputX3Pin3() { return getBooleanIOValue("InputX3Pin3", false); }
	public boolean getInputX3Pin4() { return getBooleanIOValue("InputX3Pin4", false); }
	public boolean getInputX3Pin10() { return getBooleanIOValue("InputX3Pin10", false); }
	public boolean getInputX3Pin13() { return getBooleanIOValue("InputX3Pin13", false); }
	public boolean getInputX3Pin16() { return getBooleanIOValue("InputX3Pin16", false); }
	public boolean getUserButton() { return getBooleanIOValue("UserButton", false); }
	public boolean getLEDBlue() { return getBooleanIOValue("LEDBlue", true); }
	public void setLEDBlue(java.lang.Boolean value) { setDigitalOutput("LEDBlue", value); }
	public boolean getSwitchOffX3Voltage() { return getBooleanIOValue("SwitchOffX3Voltage", true); }
	public void setSwitchOffX3Voltage(java.lang.Boolean value) { setDigitalOutput("SwitchOffX3Voltage", value); }
	public boolean getOutputX3Pin1() { return getBooleanIOValue("OutputX3Pin1", true); }
	public void setOutputX3Pin1(java.lang.Boolean value) { setDigitalOutput("OutputX3Pin1", value); }
	public boolean getOutputX3Pin2() { return getBooleanIOValue("OutputX3Pin2", true); }
	public void setOutputX3Pin2(java.lang.Boolean value) { setDigitalOutput("OutputX3Pin2", value); }
	public boolean getOutputX3Pin11() { return getBooleanIOValue("OutputX3Pin11", true); }
	public void setOutputX3Pin11(java.lang.Boolean value) { setDigitalOutput("OutputX3Pin11", value); }
	public boolean getOutputX3Pin12() { return getBooleanIOValue("OutputX3Pin12", true); }
	public void setOutputX3Pin12(java.lang.Boolean value) { setDigitalOutput("OutputX3Pin12", value); }
	public boolean getLEDGreen() { return getBooleanIOValue("LEDGreen", true); }
	public void setLEDGreen(java.lang.Boolean value) { setDigitalOutput("LEDGreen", value); }
	public boolean getLEDRed() { return getBooleanIOValue("LEDRed", true); }
	public void setLEDRed(java.lang.Boolean value) { setDigitalOutput("LEDRed", value); }
}
