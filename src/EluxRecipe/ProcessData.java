package EluxRecipe;

import EluxAPI.SimpleFrame;
import java.util.ArrayList;

public class ProcessData {
	public SimpleFrame worldOffset;
	public ArrayList<SimpleFrame> tools = new ArrayList<SimpleFrame>();
	
	public ProcessData() { }	// CONSTRUCTOR
	
	public void fetchData(String filename) {
		
	}
	
	public void saveData() {
		
	}
	
	public SimpleFrame getWorldOffset() {
		SimpleFrame offset = new SimpleFrame();
		return offset;
	}
	
	public SimpleFrame getTool(int toolIndex) {
		SimpleFrame offset = new SimpleFrame();
		return offset;
	}
}