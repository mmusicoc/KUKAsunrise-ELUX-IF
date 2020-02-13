package eluxLibs;

/*******************************************************************
* <b> STANDARD UTIL CLASS BY mario.musico@electrolux.com </b> <p>
* static void waitMillis(int millis)/(int millis, boolean log)<p>
* static void sleep() <p>
* static void padLog(String/int/boolean/double) <p>
*/

import com.kuka.common.ThreadUtil;

public class Utils {
	
	/**
	 * Introduce a timed delay in the program running <p>
	 * Use <b>true</b> as 2nd parameter for logging
	 * @param int millis
	 * @param boolean log (optional)
	 */
	public static void waitMillis(int millis) { waitMillis(millis, false); }
	
	/**
	 * Introduce a timed delay in the program running <p>
	 * Use <b>true</b> as 2nd parameter for logging
	 * @param int millis
	 * @param boolean log (optional)
	 */
	public static void waitMillis(int millis, boolean log) {
		if (log) padLog("Wait for " + millis + " millis");
		ThreadUtil.milliSleep(millis);
	}
	
	/**
	 * Suspend indefinetely the application from running, after notifiying at smartPad logger
	 * - USE AT END OF PROGRAM IF MUST RUN ONLY LIMITED TIMES OR FOR DEBUGGING PURPOSES -
	 */
	public static void sleep() {
		padLog("Wait indefinitely - sleep");
		for(;;) waitMillis(50);
	}
	
	public static void padLog(String msg) { System.out.println(msg); }
	public static void padLog(int msg) { System.out.println(msg); }
	public static void padLog(boolean msg) { System.out.println(msg); }
	public static void padLog(double msg) { System.out.println(msg); }
}