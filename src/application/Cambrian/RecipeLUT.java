package application.Cambrian;

//import static EluxUtils.Utils.*;

public class RecipeLUT {
	
	public RecipeLUT() { 	// CONSTRUCTOR
	}
	
	// 92550331220
	
	public String getRecipe(int PNC) {
		//int PNCend = (int)((PNC - 925000000) / 10);
		//padLog(PNCend);
		String recipe = "";
		switch(PNC) {
			case 1:
			case 2:
			case 3:
				recipe = "F2";	break;
			case 4:
			case 925033122:
				recipe = "F4"; break;
				
				
			default:
				recipe = "ERR"; break;
		}
		// padLog("Recipe is " + recipe);
		return recipe;
	}
}
