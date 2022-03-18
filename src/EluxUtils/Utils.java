package EluxUtils;

import com.kuka.common.ThreadUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;

public class Utils {
	public static final String ROOT_PATH = System.getProperty("user.dir") + "\\";
	public static final String FILES_FOLDER = "_ELUXiiwa\\";
	public static final String LOGS_FOLDER = "Logs\\";
	public static final String OEE_FOLDER = "OEE\\";
	
	public static void waitMillis(int millis) { waitMillis(millis, false); }
	public static void waitMillis(int millis, boolean log) {
		if (log) logmsg("Wait for " + millis + " millis");
		ThreadUtil.milliSleep(millis);
	}

	public static void logmsg(String msg) { System.out.println(msg); }
	public static void logmsg(int msg) { System.out.println(msg); }
	public static void logmsg(boolean msg) { System.out.println(msg); }
	public static void logmsg(double msg) { System.out.println(msg); }
	
	public static void logErr(String msg) { System.err.println(msg); }
	public static void debug() { logmsg("Arrived here"); }
	
	public static double getCurrentTime() {
		return System.currentTimeMillis();
	}
	
/*	public static String getDateAndTime(char sepChar) {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd" + sepChar + "HH:mm:ss")
									.format(new Date());
		return timeStamp;
	}
	*/
	
	public static String getDate() {
		return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
	}
	
	public static String getTime(char sep) {
		return new SimpleDateFormat("HH" + sep + "mm" + sep + "ss").format(new Date());
	}
	
	public static boolean isFileEmpty(String filename) {
		File f = new File(FILES_FOLDER + filename);
		return (f.length() == 0);
	}
}