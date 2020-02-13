package eluxLibs;

/*******************************************************************
* <b> STANDARD UTIL CLASS BY mario.musico@electrolux.com </b> <p>
* static void waitMillis(int millis)/(int millis, boolean log)<p>
* static void sleep() <p>
* static void padLog(String/int/boolean/double msg) <p>
* static void padErr(String msg) <p>
*/

import com.kuka.common.ThreadUtil;

public class Utils {
	public static void waitMillis(int millis) { waitMillis(millis, false); }
	public static void waitMillis(int millis, boolean log) {
		if (log) padLog("Wait for " + millis + " millis");
		ThreadUtil.milliSleep(millis);
	}
	public static void sleep() {
		padLog("Wait indefinitely - sleep");
		for(;;) waitMillis(1000);
	}
	public static void padLog(String msg) { System.out.println(msg); }
	public static void padLog(int msg) { System.out.println(msg); }
	public static void padLog(boolean msg) { System.out.println(msg); }
	public static void padLog(double msg) { System.out.println(msg); }
	public static void padErr(String msg) { System.err.println(msg); }
}