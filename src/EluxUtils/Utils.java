package EluxUtils;

import static EluxUtils.UMath.*;

import com.kuka.common.ThreadUtil;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;

public class Utils {
	public static final String FILE_ROOTPATH = "_ELUXiiwa\\";
	
	/*private static TCPsocket_client socket;
	public static void initLoggerSocket(String IP, int port) {
		socket = new TCPsocket_client(IP, port);
	}*/
	
	public static void waitMillis(int millis) { waitMillis(millis, false); }
	public static void waitMillis(int millis, boolean log) {
		if (log) padLog("Wait for " + millis + " millis");
		ThreadUtil.milliSleep(millis);
	}
	public static void halt() {
		padLog("Wait indefinitely - sleep");
		for(;;) waitMillis(1000);
	}
	//public static void padClear() { System.out.print("\033[H\033[2J"); } // NOT WORKING
	public static void padLog(String msg) { System.out.println(msg); /*socket.send(msg);*/ }
	public static void padLog(int msg) { System.out.println(msg); /*socket.send(i2s(msg));*/ }
	public static void padLog(boolean msg) { System.out.println(msg); /*socket.send(b2s(msg));*/ }
	public static void padLog(double msg) { System.out.println(msg); /*socket.send(d2s(msg));*/ }
	public static void padErr(String msg) { System.err.println(msg); /*socket.send(msg); */}
	public static void debug() { padLog("Arrived here"); }
	public static String af2s(Frame pose) { return pose.toStringInWorld(); }
	public static String rf2s(Frame pose, boolean csv, boolean dist) {
		if(csv) {
			String sFrame = new String();
			sFrame = sFrame + d2s(pose.getX()) + ",";
			sFrame = sFrame + d2s(pose.getY()) + ",";
			sFrame = sFrame + d2s(pose.getZ()) + ",";
			if(dist) sFrame = sFrame + d2s(pose.distanceTo(pose.getParent())) + ",";
			sFrame = sFrame + d2s(r2d(pose.getAlphaRad())) + ",";
			sFrame = sFrame + d2s(r2d(pose.getBetaRad())) + ",";
			sFrame = sFrame + d2s(r2d(pose.getGammaRad()));
			return sFrame;
		}
		else return pose.toStringTrafo();
	}
	
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
	
	public static double getTimeStamp() {
		return System.currentTimeMillis();
	}
	
	public static String getDateAndTime() {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss")
									.format(new Date());
		return timeStamp;
	}
	
	public static boolean isFileEmpty(String filename) {
		File f = new File(FILE_ROOTPATH + filename);
		return (f.length() == 0);
	}
}