package utils;

/*******************************************************************
* <b> STANDARD HANDLER CLASS BY mario.musico@electrolux.com </b> <p>
* void saveRGB() <p>
* void resetRGB(String color, [boolean log]) - DEFAULT log = false <p>
* void blinkRGB(String color, int millis, [boolean log]) - DEFAULT log = false <p>
* boolean getUserButton() <p>
* void waitUserButton() <p>
* int ckeckButtonInput() <p>
*/

import static utils.Utils.padLog;
import static utils.Utils.waitMillis;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;

@Singleton
public class HandlerMFio {
	// Standard KUKA API objects
	private MediaFlangeIOGroup MFio;
	
	// Private properties
	private boolean[] prevRGB;
	
	// CONSTRUCTOR
	@Inject	public HandlerMFio(MediaFlangeIOGroup _MFio) { 
		this.MFio = _MFio;
	}
	
	// Save & restore RGB status ******************************************************************
	
	public void saveRGB() { prevRGB = getRGB(); }
	
	public void resetRGB() { this.setRGB(this.prevRGB); }
	
	private boolean [] getRGB() {
		boolean[] rgb = new boolean [3];
		rgb[0] = MFio.getLEDRed();
		rgb[1] = MFio.getLEDGreen();
		rgb[2] = MFio.getLEDBlue();
		return rgb;
	}
	
	private void setRGB(boolean[] rgb) {
		MFio.setLEDRed(rgb[0]);
		MFio.setLEDGreen(rgb[1]);
		MFio.setLEDBlue(rgb[2]);
	}
	
	// Force new RGB status ***********************************************************************
	
	private void setRGB(boolean r, boolean g, boolean b) {
		MFio.setLEDRed(r);
		MFio.setLEDGreen(g);
		MFio.setLEDBlue(b);
	}

	public void setRGB(String color, boolean log) {
		if (log) padLog("MediaFlange LED ring to " + color);
		if (color.equalsIgnoreCase("R")) this.setRGB(true,false,false);
		else if (color.equalsIgnoreCase("G")) this.setRGB(false,true,false);
		else if (color.equalsIgnoreCase("B")) this.setRGB(false,false,true);
		else if (color.equalsIgnoreCase("RG")) this.setRGB(true,true,false);
		else if (color.equalsIgnoreCase("RB")) this.setRGB(true,false,true);
		else if (color.equalsIgnoreCase("GB")) this.setRGB(false,true,true);
		else if (color.equalsIgnoreCase("RGB")) this.setRGB(true,true,true);
		else if (color.equalsIgnoreCase("OFF")) this.setRGB(false,false,false);
		else padLog("MediaFlange color not valid");
	}
	
	public void setRGB(String color) { this.setRGB(color, false); }
	
	public void blinkRGB(String color, int millis) { this.blinkRGB(color, millis, false); }
	
	public void blinkRGB(String color, int millis, boolean log) {
		this.saveRGB();
		if (log) padLog("MediaFlange LED blink " + color + " for " + millis + " millis");
		this.setRGB(color);
		waitMillis(millis);
		this.resetRGB();
	}
	
	// Button handlers ****************************************************************************
	
	public boolean getUserButton() { return MFio.getUserButton(); }
	
	public void waitUserButton() {
		this.saveRGB();
		if(prevRGB[0] == true && prevRGB[1] == false && prevRGB[2] == false) ;
		else this.setRGB("B");
		padLog("Press USER GREEN BUTTON to continue");
		while (true) {
			if (this.getUserButton()) break;
			waitMillis(50);
		}
		this.blinkRGB("GB", 500);		// Wait for torque to stabilize and notify input
		this.resetRGB();
	}
	
	public int checkButtonInput(){						// determine user button input
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
		// padLog("#Short = " + pressCountShort + ", #Long = " + pressCountLong);
		return (pressCountLong * 10 + pressCountShort);
	}
}