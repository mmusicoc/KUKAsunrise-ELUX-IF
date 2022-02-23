package application.Cambrian;

//import static EluxUtils.Utils.*;

public class LUTrecipe {
	
	public LUTrecipe() { 	// CONSTRUCTOR
	}
	
	// 92550331220
	
	public String getRecipe(int PNC) {
		//int PNCend = (int)((PNC - 925000000) / 10);
		//padLog(PNCend);
		String recipe = "";
		switch(PNC) {
			case 1: recipe = "F1"; break;
			case 925503312:	recipe = "F4"; break;
			case 925975000: recipe = "F5"; break;
			case 925561304: recipe = "F6"; break;
			case 925501312: recipe = "F7"; break;
			case 925503306: recipe = "F8"; break;
			
			default:
				recipe = "ERR"; break;
		}
		// padLog("Recipe is " + recipe);
		return recipe;
	}
}
