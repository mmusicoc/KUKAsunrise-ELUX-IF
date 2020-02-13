package utils;

import com.kuka.common.ThreadUtil;

/**********************************************
* CUSTOM METHODS BY mario.musico@electrolux.com <p>
* static void waitMillis(int millis)/(int millis, boolean log)<p>
* static void sleep()
*/

public class Utils {
	public static void waitMillis(int millis) { waitMillis(millis, false); }
	public static void waitMillis(int millis, boolean log) {
		if (log) logPad("Wait for " + millis + " millis");
		ThreadUtil.milliSleep(millis);
	}
	
	public static void sleep() {
		logPad("Wait indefinitely - sleep");
		for(;;){}
	}
	
	public static void logPad(String msg) { System.out.println(msg); }
	public static void logPad(int msg) { System.out.println(msg); }
	public static void logPad(boolean msg) { System.out.println(msg); }
	public static void logPad(double msg) { System.out.println(msg); }
}