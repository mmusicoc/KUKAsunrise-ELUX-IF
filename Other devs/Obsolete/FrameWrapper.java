package utils;

import com.kuka.roboticsAPI.geometricModel.Frame;

public class FrameWrapper {
	private Frame[] elems;
	private int counter = 0;
	
	public FrameWrapper() {	elems = new Frame[1]; }				// Constructor method
	public int GetCounter() { return counter; }					// Get no. of frames stored in list
	public Frame GetFrame(int index) { return elems[index]; }	// Return frame given its index no.
	public Frame Last() { return elems[counter-1]; }			// Return last frame in list
	
	public void Add(Frame f) {
		if(elems.length <= counter) Resize();
		elems[counter] = f;
		counter++;
	}
	
	private void Resize() {
		Frame[] temp = new Frame[2*elems.length];
		for(int i = 0; i < counter; i++) temp[i] = elems[i];
		elems = temp;
	}
	
	public void Free() {
		elems = new Frame[1];
		counter = 0;
	}
	
	public void deleteLastFrame (){
		if(counter <= 0) {
			System.out.println("No Frames saved.");
			counter = 0;
		} else {
			System.out.println("Deleted Frame "+counter+" : " + this.Last().toString());
			counter--;
		}
	}
}
