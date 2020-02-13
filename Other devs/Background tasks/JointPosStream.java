package backgroundTask;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;

public class JointPosStream extends RoboticsAPICyclicBackgroundTask {
	@Inject private Controller 	kukaController;
	@Inject private LBR 		kiwa;
	private int 				port = 0;
	private ServerSocket 		serverSocket = null; 
	private Socket 				clientSocket = null; 
	private String 				inputLine; 
	private PrintWriter 		out = null;
	private BufferedReader 		in = null;

	@Override
	public void initialize() {
		initializeCyclic(0, 1000, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
		port = 30000;
		/*

		try { 											// make server socket
			serverSocket = new ServerSocket(port);  
		} catch (IOException e) { 
			// System.err.println("Could not listen to port: " + port); 
			// System.exit(1); 
		}
		
		try { 											// make client socket
			clientSocket = serverSocket.accept(); 
		} catch (IOException e) { 
			// System.err.println("Accept failed."); 
			// System.exit(1); 
		} 
		*/
	}

	@Override
	public void runCyclic() {
	/*
		try {											// your task execution starts here 
			startStreaming(port);
		} catch (Exception e) {
			// TODO: handle exception
			//System.out.println(e.getMessage()); 
		} 
	}	

	public void startStreaming(int port) throws IOException 
	{ 
		ServerSocket serverSocket = null; 

		try { 
			serverSocket = new ServerSocket(port); 
			System.out.println("Listening to port: " + port);
		} catch (IOException e) { 
			System.err.println("Could not listen to port: " + port); 
			System.exit(1); 
		} 

		Socket clientSocket = null; 
		System.out.println ("Waiting for connection.....");

		try { 
			clientSocket = serverSocket.accept(); 
		} catch (IOException e) { 
			System.err.println("Accept failed."); 
			System.exit(1); 
		} 

		System.out.println("Connection successful");
		System.out.println("ServerSocker : InetAddress = " + serverSocket.getInetAddress() + ", Localport = " + serverSocket.getLocalPort());
		System.out.println("Client Socker : InetAddress = " + clientSocket.getInetAddress() + ", Localport = " + clientSocket.getLocalPort());
		System.out.println("Waiting for input.....");

		PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true); 
		BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); 

		String inputLine; 

		while ((inputLine = in.readLine()) != null)	{ 
			System.out.println ("Server: " + inputLine); 
			out.println(inputLine); 
			if (inputLine.equals("Bye.")) break; 
		} 

		out.close(); 
		in.close(); 
		clientSocket.close(); 
		serverSocket.close(); 
	*/
	} 
}