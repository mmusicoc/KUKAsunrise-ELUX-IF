package EluxUtils;

import java.util.Locale;

public class UMath {
	public static double pi() { return 3.14159265359; }
	
	public static String d2s(double value) { return 
			String.format(Locale.US, "%,.2f",value); }
	public static String i2s(int value) { return (Integer.toString(value)); }
	
	public static double r2d(double rad) { return (rad * 180 / pi()); }
	public static double d2r(double deg) { return (deg * pi() / 180); }
	
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
}