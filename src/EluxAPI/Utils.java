package EluxAPI;

import com.kuka.common.ThreadUtil;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;

public class Utils {
	public static void waitMillis(int millis) { waitMillis(millis, false); }
	public static void waitMillis(int millis, boolean log) {
		if (log) padLog("Wait for " + millis + " millis");
		ThreadUtil.milliSleep(millis);
	}
	public static void halt() {
		padLog("Wait indefinitely - sleep");
		for(;;) waitMillis(1000);
	}
	//public static void padClear() { System.out.print("\033[H\033[2J"); }
	public static void padLog(String msg) { System.out.println(msg); }
	public static void padLog(int msg) { System.out.println(msg); }
	public static void padLog(boolean msg) { System.out.println(msg); }
	public static void padLog(double msg) { System.out.println(msg); }
	public static void padErr(String msg) { System.err.println(msg); }
	public static void debug() { padLog("Arrived here"); }
	public static void printAbsFrame(Frame pose) { padLog(pose.toStringInWorld()); }
	public static void printRelFrame(Frame pose) { padLog(pose.toStringTrafo()); }
	
	public static Frame offsetFrame(Frame parent, double x, double y, double z,
												  double a, double b, double c) {
		Frame offseted = new Frame();
		offseted.setParent(parent, false);
		offseted.setTransformationFromParent(Transformation.ofDeg(x, y, z, a, b, c));
		return offseted;
	}
	
	public static double mapDouble(double input, double imin, double imax, 
							double omin, double omax, boolean signed) {
		double inRangeSize = (imax - imin);
		double outRangeSize = (omax - omin) * (signed ? 2 : 1);
		double value = (input - imin) / inRangeSize * outRangeSize;
		if(signed) {
			value -= (outRangeSize / 2.0);
			value += (omin * (value > 0 ? 1 : -1));
		} else {
			value += omin;
		}
		return value;
	}
	
	public static double random(double min, double max, boolean signed) {
		return mapDouble(Math.random(), 0.0, 1.0, min, max, signed);
	}
	
	public static Frame randomizeFrame(Frame target, double rangeMin, double rangeMax) {
		target.transform(Transformation.ofDeg(
				random(rangeMin, rangeMax, true),
				random(rangeMin, rangeMax, true),
				random(rangeMin, rangeMax, true),
				0, 0, 0));
		return target;
	}
	
	public static double pi() { return 3.14159265359; }
	public static double rad2deg(double rad) { return (rad * 180 / pi()); }
	public static double deg2rad(double deg) { return (deg * pi() / 180); }
	public static int abs(int num) { 
		if (num < 0) return -num;
		else return num;
	}
	
	public static double abs(double num) { 
		if (num < 0) return -num;
		else return num;
	}
	
	public static double dist(double value1, double value2) {
		return abs(value1 - value2);
	}
	
	public static double deg(double deg) {
		while (deg > 180) deg -= 360;
		while (deg <= -180) deg += 360;
		return deg;
	}
	
	public static double round(double value, int decimals) {
		double aux = Math.pow(10, decimals);
		return Math.round(value * aux) / aux;
	}
	
	public static double roundAngle(double value, int decimals, double threshold) {
		double aux = deg(round(value, decimals));
		if(dist(aux, -180) < threshold) return 180;
		else if(dist(aux, -90) < threshold) return -90;
		else if(dist(aux, 0) < threshold) return 0;
		else if(dist(aux, 90) < threshold) return 90;
		else if(dist(aux, 180) < threshold) return 180;
		return aux;
	}
	
	public static double getTimeStamp() {
		return System.currentTimeMillis();
	}
	
	public static String getDateAndTime() {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss")
									.format(new Date());
		return timeStamp;
	}
	
	public static boolean isFileEmpty(String filename) {
		File f = new File(filename);
		return (f.length() == 0);
	}
}