package application.Cambrian;

public class LUTcambrianModel {
	
	public LUTcambrianModel() { }
	
	public String getCambrianModel(char jointType) {
		String model = "";
		switch(jointType) {
			case 'J': model = "Elux_weldedpipes"; break;
			case 'C': model = "Elux_crimp6"; break;
			case 'B': model = "Elux_fridge_ref_bolt"; break;
			default:  model = "ERR"; break;
		}
		return model;
	}
}
