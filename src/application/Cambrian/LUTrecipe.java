package application.Cambrian;

//import static EluxUtils.Utils.*;

public class LUTrecipe {
	
	public LUTrecipe() { 	// CONSTRUCTOR
	}
	public String getRecipe(int PNC) {
		String recipe = "";
		switch(PNC) {
			case 925503307:
			case 925503308:
			case 925503313:
			case 925503316: recipe = "F1"; break;
			case 925503312:	recipe = "F4"; break;
			case 925975000: recipe = "F5"; break;
			case 925561304: recipe = "F6"; break;
			case 925501302:
			case 925501312: 
			case 925501321:
			case 925501322: recipe = "F7"; break;
			case 925503306: recipe = "F8"; break;
			
			default:
				recipe = "RCP NOT FOUND"; break;
		}
		return recipe;
	}
}
