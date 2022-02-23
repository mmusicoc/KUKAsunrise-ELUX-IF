package EluxUtils;

import com.kuka.common.ThreadUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;

public class Utils {
	public static final String FILE_ROOTPATH = "_ELUXiiwa\\";
	
	public static void waitMillis(int millis) { waitMillis(millis, false); }
	public static void waitMillis(int millis, boolean log) {
		if (log) padLog("Wait for " + millis + " millis");
		ThreadUtil.milliSleep(millis);
	}

	public static void padLog(String msg) { System.out.println(msg); }
	public static void padLog(int msg) { System.out.println(msg); }
	public static void padLog(boolean msg) { System.out.println(msg); }
	public static void padLog(double msg) { System.out.println(msg); }
	
	public static void padErr(String msg) { System.err.println(msg); }
	public static void debug() { padLog("Arrived here"); }
	
	public static double getTimeStamp() {
		return System.currentTimeMillis();
	}
	
	public static String getDateAndTime() {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd;HH:mm:ss")
									.format(new Date());
		return timeStamp;
	}
	
	public static boolean isFileEmpty(String filename) {
		File f = new File(FILE_ROOTPATH + filename);
		return (f.length() == 0);
	}
}