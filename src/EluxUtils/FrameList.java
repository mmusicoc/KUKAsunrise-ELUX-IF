package EluxUtils;

import static EluxUtils.Utils.*;

import java.util.ArrayList;

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
		if(log) logmsg("Added Frame #" + this.size() + " : " + this.getLast().toString());
	}
	public void add(Frame newFrame) { this.add(newFrame, false); }
	public void inject(int index, Frame newFrame) { frameList.add(index, newFrame); }
	public void set(int index, Frame newFrame) { frameList.set(index, newFrame); }
	public void remove(int index) { 
		if (frameList.isEmpty()) {
			logErr("Frames list is empty.");
		} else {
			//logmsg("Deleted Frame #" + (index+1) + " : " + this.get(index).toString());
			frameList.remove(index);
		}
		frameList.trimToSize();
	}
	public void removeLast() { this.remove(this.size() - 1); }
	public void free() { frameList.clear(); }
}