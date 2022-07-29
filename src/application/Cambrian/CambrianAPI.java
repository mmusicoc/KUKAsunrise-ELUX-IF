package application.Cambrian;

import static EluxUtils.Utils.*;
import static EluxUtils.UMath.*;
import EluxAPI.*;
import EluxLogger.Event;
import EluxLogger.ProLogger;
import EluxUtils.*;

import javax.inject.Inject;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;

public class CambrianAPI {
	private xAPI__ELUX elux;
	private ProLogger log;
    private TCPsocket_client socket;
    
    private boolean logger;
    private boolean cambrian_success;
    private int cambrian_msg_type;
    private String cambrian_data;
    
    FrameList predictionsList;
    private Frame target_frame;
    private int pick_type;
    
	// CONSTRUCTOR --------------------------------------------------------
	@Inject public CambrianAPI(xAPI__ELUX _elux) { this.elux = _elux; }
    
	// SETTER METHODS -------------------------------------------------------
	public boolean init(ProLogger logger) { 
		this.log = logger;
		socket = new TCPsocket_client("192.168.2.50", 4444, 15000, '\n');
		if (socket.open()) {
			sendRequest("PING", "");
			if (cambrian_success) { logmsg("Connection with Cambrian established"); return true; }
			else logErr("Unable to communicate with Cambrian, stopping application.");
		} else logErr("Unable to open TCP socket, stopping application");
		return false;
	}
	
	public boolean end() { 
		logmsg("Closing socket...");
		return socket.close();
	}
	
	public void setLogger(boolean log) { logger = log; }
	
	public void startCalibration(){ sendRequest("START CALIBRATION", ""); }
	public void captureCalibration(){ sendRequest("CAPTURE CALIBRATION IMAGE", ""); }
	public void loadModel(String model_name){ 
		sendRequest("LOAD MODEL", model_name); 
		if(logger) logmsg("Cambrian loading model " + model_name + "..."); }
	
	public int doScan(String modelName) {
		int totalPredictions = 0;
		//double initTime = getCurrentTime();
		predictionsList = new FrameList();
		if (getNewPrediction(modelName)) {
			predictionsList.add(this.target_frame);
			totalPredictions++; 
			while(getNextPrediction(modelName)) {
				Frame newPrediction = this.target_frame;
				totalPredictions++;
				boolean ghost = false;
				for(int i = 0; i < predictionsList.size(); i++) {
					if(predictionsList.get(i).distanceTo(newPrediction) < 8) {	// FILTER GHOST PREDICTIONS
						if(logger) logmsg("Prediction with higher confidence #" + i +
											" already found in same scan");
						ghost = true;
					}
				}
				if(!ghost) predictionsList.add(newPrediction);
			}
		}
		// Response time already logged by ProLogger
		log.msg(Event.Vision, /*"Cambrian response time = " + (getCurrentTime() - initTime) + " ms. */"Found " +
				predictionsList.size() + "/" + totalPredictions + " unique predictions", 0, true);
		return predictionsList.size();		
	}
    
	// GETTER METHODS -----------------------------------------------------  
	
	public boolean getInit() { return socket != null; }
	
	public int getPredictAmount() { return predictionsList.size(); }
	
	public FrameList getPredictFrames() { return predictionsList; }
	
	public int getPickType() { return pick_type; }
	
	private boolean getNewPrediction(String modelName) {
		return getPrediction("GET PREDICTION", modelName); }
	private boolean getNextPrediction(String modelName) {
		return getPrediction("GET NEXT PREDICTION", ""); }

	private boolean getPrediction(String prediction_type, String model_name) {
		if(sendRequest(prediction_type, model_name)) {
			try {
				String[] results = cambrian_data.split(",");
				if(Integer.parseInt(results[0]) == 1) {
					Double x, y, z, a, b, c;
					x    = Double.parseDouble(results[1]);
					y    = Double.parseDouble(results[2]);
					z    = Double.parseDouble(results[3]);
					a    = -Double.parseDouble(results[4]) + 90;
					b    = Double.parseDouble(results[5]);
					c    = Double.parseDouble(results[6]);
					pick_type = Integer.parseInt(results[7]);

					Frame flange_pos = elux.getMove().getFlangePos();
					target_frame = new Frame();
					target_frame.setParent(flange_pos, false);
					target_frame.setTransformationFromParent(Transformation.ofDeg
                    		(x, y, z, a, b, c));
                    return true;
                }  
				else {
					if(logger && (predictionsList.size() == 0)) 
						log.msg(Event.Vision, "Cambrian model not found: " + cambrian_data, 0, false);
				}
            } catch (Exception e) {
            	e.printStackTrace();
            	logErr("Failed to parse prediction result");
            }
        } else logErr("Unable to send command");
        return false;
    }
	
	private boolean sendRequest(String command, String data) {
		Frame pose = elux.getMove().getFlangePos();
		String robot_pose = "p[";
		robot_pose += Double.toString(pose.getX()) + ",";
		robot_pose += Double.toString(pose.getY()) + ",";
		robot_pose += Double.toString(pose.getZ()) + ",";
		robot_pose += Double.toString(r2d(pose.getAlphaRad())) + ",";
		robot_pose += Double.toString(r2d(pose.getBetaRad())) + ",";
		robot_pose += Double.toString(r2d(pose.getGammaRad())) + "]";
		
		String request = command + "#" + data + "#" + robot_pose + 
				"#p[0,0,0,0,0,0]#p[0,0,0,0,0,0]#<<"; 
		
		if (socket.send(request)) {
			String ans;
			if((ans = socket.read()) != "") {
				try {
					String msg = ans;
					String success = msg.substring(msg.indexOf("<<") + 2, msg.indexOf(">>"));
					cambrian_success = (success.compareTo("OK") == 0 ? true : false);
					msg = msg.substring(msg.indexOf(">>") + 2);
					cambrian_msg_type = Integer.parseInt(msg.substring(
							msg.indexOf("<<") + 2, msg.indexOf(">>")));
					msg = msg.substring(msg.indexOf(">>") + 2);        
					cambrian_data = msg.substring(msg.indexOf("<<") + 2, msg.indexOf(">>"));
					if(ans == "") logmsg(cambrian_msg_type); // Useless, to avoid warning
					return true;
				} catch (Exception e) { 
					e.printStackTrace();
	            	logErr("Failed to parse data"); 
				}
			} else logErr("Received no data from Cambrian");
		} else logErr("Unable to send request to Cambrian");
		return false;
	}
}