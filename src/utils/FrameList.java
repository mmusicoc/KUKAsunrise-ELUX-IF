package utils;

import java.util.ArrayList;			// Check documentation at https://docs.oracle.com/javase/8/docs/api/java/util/ArrayList.html#add-int-E-
import static utils.Utils.*;
import com.kuka.roboticsAPI.geometricModel.Frame;

public class FrameList {
	private ArrayList<Frame> frameList = new ArrayList<Frame>();
	
	public FrameList() { }			// Constructor method
	
	// Getter methods
	public int size() { return frameList.size(); }
	public Frame get(int index) { return frameList.get(index); }
	public Frame getFirst() { return frameList.get(0); }
	public Frame getLast() { return frameList.get(frameList.size() - 1); }
	
	// Setter methods
	public void add(Frame newFrame, boolean log) { 
		frameList.add(newFrame); 
		logPad("Added Frame #" + this.size() + " : " + this.getLast().toString());
	}
	public void add(Frame newFrame) { this.add(newFrame, false); }
	public void inject(int index, Frame newFrame) { frameList.add(index, newFrame); }
	public void set(int index, Frame newFrame) { frameList.set(index, newFrame); }
	public void remove(int index) { 
		if (frameList.isEmpty()) {
			logPad("Frames list is empty.");
		} else {
			logPad("Deleted Frame #" + index + " : " + this.get(index - 1).toString());
			frameList.remove(index - 1);
		}
		frameList.trimToSize();
	}
	public void removeLast() { this.remove(this.size()); }
	public void free() { frameList.clear(); }
}