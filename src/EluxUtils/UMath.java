package EluxUtils;

import java.util.Locale;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;

public class UMath {
	public static double pi() { return 3.14159265359; }
	
	public static String d2s(double value) { return 
			String.format(Locale.US, "%.2f",value); }
	public static String i2s(int value) { return (Integer.toString(value)); }
	public static String b2s(boolean value) { return  value ? "true" : "false"; }
	
	public static double s2d(String value) { return Double.parseDouble(value); }
	public static double s2i(String value) { return Integer.parseInt(value); }
	
	public static double r2d(double rad) { return (rad * 180 / pi()); }
	public static double d2r(double deg) { return (deg * pi() / 180); }
	
	public static String af2s(Frame pose) { return pose.toStringInWorld(); }
	public static String rf2s(Frame pose, boolean csv, boolean dist) {
		if(csv) {
			String sFrame = new String();
			sFrame = sFrame + d2s(pose.getX()) + ";";
			sFrame = sFrame + d2s(pose.getY()) + ";";
			sFrame = sFrame + d2s(pose.getZ()) + ";";
			if(dist) sFrame = sFrame + d2s(pose.distanceTo(pose.getParent())) + ";";
			sFrame = sFrame + d2s(r2d(pose.getAlphaRad())) + ";";
			sFrame = sFrame + d2s(r2d(pose.getBetaRad())) + ";";
			sFrame = sFrame + d2s(r2d(pose.getGammaRad()));
			return sFrame;
		}
		else return pose.toStringTrafo();
	}
	
	public static String f2p(Frame pose) { return pose.getPath(); }
	public static String f2p(ObjectFrame pose) { return pose.getPath(); }
	
	public static int abs(int num) { return (num < 0) ? -num : num; }
	public static double abs(double num) { return (num < 0) ? -num : num; }
	
	public static double dist(double value1, double value2) {
		return abs(value1 - value2); }
	
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
	
	public static double mapDouble(double input, double imin, double imax, 
								   double omin, double omax, boolean signed) {
		double inRangeSize = (imax - imin);
		double outRangeSize = (omax - omin) * (signed ? 2 : 1);
		double value = (input - imin) / inRangeSize * outRangeSize;
		if(signed) {
			value -= (outRangeSize / 2.0);
			return value + (omin * (value > 0 ? 1 : -1));
		} else return value + omin;
		}
	
	public static double random(double min, double max, boolean signed) {
		return mapDouble(Math.random(), 0.0, 1.0, min, max, signed); }
	
	public static Frame offsetFrame(Frame parent, double x, double y, double z,
												  double a, double b, double c) {
		Frame offseted = new Frame();
		offseted.setParent(parent, false);
		offseted.setTransformationFromParent(Transformation.ofDeg(x, y, z, a, b, c));
		return offseted;
	}
	
	public static Frame randomizeFrame(Frame target, double rangeMin, double rangeMax) {
		target.transform(Transformation.ofDeg(random(rangeMin, rangeMax, true),
											  random(rangeMin, rangeMax, true),
											  random(rangeMin, rangeMax, true),
											  0, 0, 0));
		return target;
	}
}