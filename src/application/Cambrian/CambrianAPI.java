package application.Cambrian;

import static EluxUtils.Utils.*;
import static EluxUtils.UMath.*;
import EluxAPI.*;
import EluxUtils.*;
/*
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;
*/
import javax.inject.Inject;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;

public class CambrianAPI {
	private xAPI__ELUX elux;
    private TCPsocket_client socket;
    
    private boolean logger;
    private boolean cambrian_success;
    private int cambrian_msg_type;
    private String cambrian_reply;
    
    private Frame target_frame;
    private int pick_type;
    
	// CONSTRUCTOR --------------------------------------------------------
	@Inject public CambrianAPI(xAPI__ELUX _elux) { this.elux = _elux; }
    
	// SETTER METHODS -------------------------------------------------------
	public boolean init(String IP, int port) { 
		socket = new TCPsocket_client(IP, port, 15000, '\n');
		if (socket.open()) {
			sendRequest("PING", "");
			if (cambrian_success) { padLog("Connection with Cambrian established"); return true; }
			else padErr("Unable to communicate with Cambrian, stopping application.");
		} else padErr("Unable to open TCP socket, stopping application");
		return false;
	}
	
	public boolean end() { 
		padLog("Closing socket...");
		return socket.close();
	}
	
	public void setLogger(boolean log) { logger = log; }
	
	public void startCalibration(){ sendRequest("START CALIBRATION", ""); }
	public void captureCalibration(){ sendRequest("CAPTURE CALIBRATION IMAGE", ""); }
	public void loadModel(String model_name){ 
		sendRequest("LOAD MODEL", model_name); 
		if(logger) padLog("Loading model " + model_name + "..."); }
    
	// GETTER METHODS -----------------------------------------------------   
	public Frame getTargetFrame() { return target_frame; }
	public int getPickType() { return pick_type; }
	public boolean getNewPrediction(String model_name) {
		return getPrediction("GET PREDICTION", model_name); }
	public boolean getNextPrediction(String model_name) {
		return getPrediction("GET NEXT PREDICTION", ""); }

	private boolean getPrediction(String prediction_type, String model_name) {
		double initTime = getTimeStamp();
		if(sendRequest(prediction_type, model_name)) {
			try {
				String[] results = cambrian_reply.split(",");
				if(Integer.parseInt(results[0]) == 1) {
					Double x, y, z, a, b, c;
					x    = Double.parseDouble(results[1]);
					y    = Double.parseDouble(results[2]);
					z    = Double.parseDouble(results[3]);
					a    = -Double.parseDouble(results[4]) + 90;
					b    = Double.parseDouble(results[5]);
					c    = Double.parseDouble(results[6]);
					pick_type = Integer.parseInt(results[7]);
    
					//target_frame = new Frame(x, y, z, a, b, c);
					Frame flange_pos = elux.getMove().getFlangePos();
					target_frame = new Frame();
					target_frame.setParent(flange_pos, false);
					target_frame.setTransformationFromParent(Transformation.ofDeg
                    		(x, y, z, a, b, c));
					if(logger) padLog("Cambrian ANS time = " + 
                    					(getTimeStamp() - initTime) + "ms");
                    return true;
                }                
            } catch (Exception e) {
            	e.printStackTrace();
            	padErr("Failed to parse prediction result");
            }
        } else padErr("Unable to send command");
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
		
		String ans;
		String request = command + "#" + data + "#" + robot_pose + 
				"#p[0,0,0,0,0,0]#p[0,0,0,0,0,0]#<<"; 
		//padLog(request);
		if (socket.send(request)) {
			if((ans = socket.read()) != "") {
				try {
					String msg = ans;
					String success = msg.substring(msg.indexOf("<<") + 2, msg.indexOf(">>"));
					cambrian_success = (success.compareTo("OK") == 0 ? true : false);
					msg = msg.substring(msg.indexOf(">>") + 2);
					cambrian_msg_type = Integer.parseInt(msg.substring(
							msg.indexOf("<<") + 2, msg.indexOf(">>")));
					msg = msg.substring(msg.indexOf(">>") + 2);        
					cambrian_reply = msg.substring(msg.indexOf("<<") + 2, msg.indexOf(">>"));
					if(ans == "") padLog(cambrian_msg_type); // Useless, to avoid warning
					return true;
				} catch (Exception e) { 
					e.printStackTrace();
	            	padErr("Failed to parse data"); 
				}
			} else padErr("Received no data from Cambrian");
		} else padErr("Unable to send request to Cambrian");
		return false;
	}
}