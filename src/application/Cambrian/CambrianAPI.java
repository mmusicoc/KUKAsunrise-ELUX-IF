package application.Cambrian;

import static EluxAPI.Utils.*;
import EluxAPI.*;

import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;
import javax.inject.Inject;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;

public class CambrianAPI {
	private xAPI__ELUX elux;
    private Client_Socket socket;
    
    private boolean cambrian_success;
    private int cambrian_msg_type;
    private String cambrian_reply;
    
    private Frame target_frame;
    private Frame approach_frame;
    private int pick_type;
    private int approach_dist;
    private int depth_offset;
    
	// CONSTRUCTOR --------------------------------------------------------
	@Inject public CambrianAPI(xAPI__ELUX _elux) { this.elux = _elux; }
    
	// INIT METHODS -------------------------------------------------------
	public boolean init(String IP, int port) { 
		this.socket = new Client_Socket(IP, port); 
		sendRequest("PING", "");
		if (cambrian_success) { padLog("Connection established"); return true; }
		else padLog("Unable to communicate with Cambrian, stopping application.");
		return false;
	}
	public void setApproachDist(int approach_dist) { this.approach_dist = approach_dist; }
	public void setDepthOffset(int depth_offset) { this.depth_offset = depth_offset; }
	public void startCalibration(){ sendRequest("START CALIBRATION", ""); }
	public void captureCalibration(){ sendRequest("CAPTURE CALIBRATION IMAGE", ""); }
	public void loadModel(String model_name){ sendRequest("LOAD MODEL", model_name); 
					padLog("Loading model " + model_name + "..."); }
    
	// GETTER METHODS -----------------------------------------------------   
	public Frame getTargetFrame() { return target_frame; }
	public Frame getApproachFrame() { return approach_frame; }
	public int getPickType() { return pick_type; }
	public boolean getNewPrediction(String model_name) {
		return getPrediction("GET PREDICTION", model_name); }
	public boolean getNextPrediction(String model_name) {
		return getPrediction("GET NEXT PREDICTION", ""); }

	private boolean getPrediction(String prediction_type, String model_name) {
		if(sendRequest(prediction_type, model_name)) {
			try {
				String[] results = cambrian_reply.split(",");
				if(Integer.parseInt(results[0]) == 1) {
					Double x, y, z, a, b, c;
					x    = Double.parseDouble(results[1]);
					y    = Double.parseDouble(results[2]);
					z    = Double.parseDouble(results[3]) + depth_offset;
					a    = -Double.parseDouble(results[4]) + 90;
					b    = Double.parseDouble(results[5]);
					c    = Double.parseDouble(results[6]);
					pick_type = Integer.parseInt(results[7]);
    
                    target_frame = new Frame(x, y, z, a, b, c);
                    approach_frame = new Frame(x, y, z, a, b, c);
                    Frame flange_pos = elux.getMove().getFlangePos();
                    target_frame.setParent(flange_pos, false);
                    target_frame.setTransformationFromParent(Transformation.ofDeg
                    		(x, y, z, a, b, c));
    				approach_frame.setParent(target_frame, false);
    				approach_frame.setTransformationFromParent(Transformation.ofDeg
    						(0, 0, -this.approach_dist, 0, 0, 0));
                    
                    //padLog(target_frame.toStringInWorld());
    				//padLog(target_frame.toStringTrafo());
                    return true;
                }                
            } catch (Exception e) {
                e.printStackTrace();
                padErr("Failed to parse prediction result");
            }
        }
        return false;
    }
	
	private boolean sendRequest(String command, String data) {
        Frame pose = elux.getMove().getFlangePos();
        String robot_pose = "p[";
        robot_pose += Double.toString(pose.getX()) + ",";
        robot_pose += Double.toString(pose.getY()) + ",";
        robot_pose += Double.toString(pose.getZ()) + ",";
        robot_pose += Double.toString(rad2deg(pose.getAlphaRad())) + ",";
        robot_pose += Double.toString(rad2deg(pose.getBetaRad())) + ",";
        robot_pose += Double.toString(rad2deg(pose.getGammaRad())) + "]";
        
        String request, ans, success, msg;
        request = command + "#" + data + "#" + robot_pose + 
        		"#p[0,0,0,0,0,0]#p[0,0,0,0,0,0]#<<"; 
        //padLog(request);
        socket.Send(request);
        ans = socket.Read();

        if(ans != "") {
            try {
            	//padLog("New msg:\n" + ans);
                msg = ans;
                success = msg.substring(msg.indexOf("<<") + 2, msg.indexOf(">>"));
                cambrian_success = (success.compareTo("OK") == 0 ? true : false);
                msg = msg.substring(msg.indexOf(">>") + 2);
                cambrian_msg_type = Integer.parseInt(msg.substring(
                		msg.indexOf("<<") + 2, msg.indexOf(">>")));
                msg = msg.substring(msg.indexOf(">>") + 2);        
                cambrian_reply = msg.substring(msg.indexOf("<<") + 2, msg.indexOf(">>"));
                
                //padLog("Cambrian success: " + cambrianSuccess);
                if(ans == "") padLog("Cambrian message type: " + cambrian_msg_type);
                //if(cambrianReply != "") padLog("Reply: " + cambrianReply);
                return true;
            } catch (Exception e) { padErr("Failed to parse data"); return false; }
        } else { padErr("Received no data"); return false; }
	}
}

//SOCKET HANDLER CLASS -----------------------------------------------------------------

class Client_Socket {
	private String TCP_IP;
	private int TCP_port;
	private Socket socket;

	public Client_Socket(String IP, int port) { this.TCP_IP = IP; this.TCP_port = port; }

	public void Send(String command) {
		try {
			socket = new Socket(TCP_IP, TCP_port);
			if (socket.isConnected()){
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				out.write(command.getBytes("US-ASCII"));
				out.flush();
			} else {
				padErr("Failed to open socket");
				socket.close();
			}
		} catch (IOException e) { System.out.println(e); }
	}

	public String Read() {
		try {
			InputStreamReader reader = new InputStreamReader(socket.getInputStream());
			int character;
			StringBuilder data = new StringBuilder();
			while ((character = reader.read()) != -1) {
				data.append((char) character);
			}
			//System.out.println(data);
			//System.out.println(data.toString());
			return data.toString();
		} catch (IOException e) { System.out.println(e); return "ERR";}
	}
}