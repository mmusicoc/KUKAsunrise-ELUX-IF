package application.Cambrian;

public class LUTcambrianModel {
	
	public LUTcambrianModel() { }
	
	public String getCambrianModel(char jointType) {
		String model = "";
		switch(jointType) {
			case 'J': model = "Elux_weldedpipes_finalrig2"; break;	// Std Joint
			case 'U': model = "Elux_Crimp1"; break;					// Ultrasonic weld
			case 'W': model = "susegana_pipes"; break;				// Wide FOV
			case 'C': model = "susegana_capillaries"; break;		// Capillaries
			case 'B': model = "Elux_fridge_ref_bolt"; break;		// Bolt
			default:  model = "ERR"; break;
		}
		return model;
	}
}
