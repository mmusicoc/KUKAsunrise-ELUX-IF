package EluxOEE;

import java.io.Serializable;

public class OEEdata implements Serializable {
	private static final long serialVersionUID = 1L;

	OEEitem cycle;
	OEEitem[] items;
	
	public OEEdata() { } 	// CONSTRUCTOR ---------------------------------
}