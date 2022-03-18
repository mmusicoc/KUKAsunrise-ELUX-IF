package EluxLogger;

import static EluxUtils.Utils.*;

public class ProLogger {
	private String filename;
	private CSVLogger csv;
	private boolean padLogger;
	private double prevTime, currentTime;
	
	public ProLogger() { // CONSTRUCTOR ------------------------
	}
	
	public boolean getPadLogger() { return this.padLogger; }
	
	public String getFileName() { return this.filename; }
	
	public void setPadLogger(boolean padLogger) { 
		if(this.padLogger != padLogger) {
			this.padLogger = padLogger;
			logmsg(padLogger ? "Logger on" : "Logger off");
		}
	}
	
	public void newLog(String logTitle) {
		filename = LOGS_FOLDER + getDate() + '_' + getTime('-') + '_' + logTitle + ".csv";
		csv = new CSVLogger(filename, true, ';');
		csv.setHeader("Date;Time;EventType;CT;Msg\n");
		prevTime = getCurrentTime();
	}
	
	public String eventType(Event reasonCode) {
		switch(reasonCode) {
			case Move: return "Movement";
			case IO: return "I/O";
			case HMI: return "User input";
			case ESMI: return "ESMI-IIoT";
			case Proc: return "Process";
			case Prod: return "Product";
			case Rcp: return "Recipe";
			case Vision: return "Vision";
			case Quality: return "Quality";
			case Fail: return "Fail";
			case Alarm: return "Alarm";
			case Other: return "Other";
			
			default: return "ERR";
		}
	}
	
	/**
	 * @param eventType Event
	 * @param msg String
	 * @param forcePadLog int 1: always, 0: if globally enabled, -1: disabled, only CSV
	 * @param CSVeventLog boolean
	 */
	
	public void msg(Event eventType, String msg, int forcePadLog, boolean CSVeventLog) {
		if(csv == null) { logErr("CSV logger File not initialized"); return; }
		if((padLogger && forcePadLog == 0) || forcePadLog == 1) logmsg(msg);
		if(CSVeventLog) {
			currentTime = getCurrentTime();
			double CT = (currentTime - prevTime) / 1000.0;
			prevTime = currentTime;
			csv.open(false);
			csv.log(getDate(), false);
			csv.log(getTime(':'), true);
			csv.log(eventType(eventType), true);
			csv.log(CT, true);
			csv.log(msg, true);
			csv.eol();
			csv.close(false);
		}
	}
}