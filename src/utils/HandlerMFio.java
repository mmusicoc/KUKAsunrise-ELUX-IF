package utils;

import static utils.Utils.*;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HandlerMFio {
	private MediaFlangeIOGroup MFio;
	private boolean[] prevRGB;
	
	@Inject
	public HandlerMFio(MediaFlangeIOGroup _MFio){ this.MFio = _MFio; } // CONSTRUCTOR
	
	/***************************************************************************
	* STANDARD METHODS BY mario.musico@electrolux.com <p>
	***************************************************************************/
	
	public boolean getUserButton() { return MFio.getUserButton(); }
	
	public void waitUserButton() {
		logPad("Press USER GREEN BUTTON to continue");
		while (true) {
			if (this.getUserButton()) break;
			waitMillis(50);
		}
		waitMillis(500, false);		// Wait for torque to stabilize
	}
	
	public boolean [] getRGB() {
		boolean[] rgb = new boolean [3];
		rgb[0] = MFio.getLEDRed();
		rgb[1] = MFio.getLEDGreen();
		rgb[2] = MFio.getLEDBlue();
		return rgb;
	}
	
	public void saveRGB() { prevRGB = getRGB(); }
	
	public void setRGB(boolean[] rgb) {
		MFio.setLEDRed(rgb[0]);
		MFio.setLEDGreen(rgb[1]);
		MFio.setLEDBlue(rgb[2]);
	}
	
	public void setRGB(boolean r, boolean g, boolean b) {
		MFio.setLEDRed(r);
		MFio.setLEDGreen(g);
		MFio.setLEDBlue(b);
	}

	public void setRGB(String color, boolean log) {
		if (log) logPad("MediaFlange LED ring to " + color);
		if (color.equalsIgnoreCase("R")) this.setRGB(true,false,false);
		else if (color.equalsIgnoreCase("G")) this.setRGB(false,true,false);
		else if (color.equalsIgnoreCase("B")) this.setRGB(false,false,true);
		else if (color.equalsIgnoreCase("RG")) this.setRGB(true,true,false);
		else if (color.equalsIgnoreCase("RB")) this.setRGB(true,false,true);
		else if (color.equalsIgnoreCase("GB")) this.setRGB(false,true,true);
		else if (color.equalsIgnoreCase("RGB")) this.setRGB(true,true,true);
		else if (color.equalsIgnoreCase("OFF")) this.setRGB(false,false,false);
		else logPad("MediaFlange color not valid");
	}
	
	public void setRGB(String color) { this.setRGB(color, false); }
	
	public void resetRGB() { this.setRGB(this.prevRGB); }
	
	public void blinkRGB(String color, int millis) { this.blinkRGB(color, millis, false); }
	
	public void blinkRGB(String color, int millis, boolean log) {
		this.saveRGB();
		if (log) logPad("MediaFlange LED blink " + color + " for " + millis + " millis");
		this.setRGB(color);
		waitMillis(millis);
		this.resetRGB();
	}
	
	public int checkBtnInput(){						// determine user button input
		int timeCount = 0;
		int pressCountShort = 0;
		int pressCountLong = 0;
		this.saveRGB();
		this.setRGB("GB");
		
		outerLoop:
		while(true) {
			while (true) {								// While pressed
				if (!this.getUserButton()) {			// Button unpressed
					this.resetRGB();
					if (timeCount < 9) pressCountShort += 1;
					else pressCountLong += 1;
					timeCount = 0;
					break;
				}
				waitMillis(50);
				timeCount += 1;
				if (timeCount > 9) this.setRGB("R");
			}
			while (true) {								// While unpressed
				if (timeCount > 9) break outerLoop;		// Timeout, finished interaction
				if (this.getUserButton()) {				// new action detected
					this.setRGB("GB");
					timeCount = 0;
					break;
				}
				waitMillis(50);
				timeCount += 1;
			}
		}
		this.resetRGB();
		// logPad("#Short = " + pressCountShort + ", #Long = " + pressCountLong);
		return (pressCountLong * 10 + pressCountShort);
	}
}