package EluxRecipe;

import java.util.ArrayList;
import static EluxAPI.Utils.*;

public class LastHistory {
	private int historySize;
	private boolean historyFull;
	public ArrayList<String> items = new ArrayList<String>();
	
	public LastHistory(int _historySize) { this.historySize = _historySize; }	// CONSTRUCTOR
	
	public int getHistorySize() { return this.historySize; }
	public boolean isHistoryFull() { return this.historyFull; }
	
	public void setHistory(ArrayList<String> _items) {
		this.items = _items;
		historyFull = (this.items.size() == this.historySize);
	}
	
	public void addItem(String item) {
		if(items.contains(item)) {
			padLog("Item already in history");
		} else if(historyFull) {
			items.remove(0);
			items.add(item);
		} else {
			items.add(item);
			historyFull = (this.items.size() == this.historySize);
		}
	}
	
	public String[] getHistoryString() {
		String[] history = new String[items.size() + 3];
		history[0] = new String("CANC");
		history[1] = new String("NEW");
		history[2] = new String("TYPE");
		for(int i = 0; i < items.size(); i++) {
			history[i + 3] = new String(items.get(i));
		}
		return history;
	}
}