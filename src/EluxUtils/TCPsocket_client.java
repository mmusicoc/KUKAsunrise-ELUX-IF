package EluxUtils;

import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;

public class TCPsocket_client {
	private String IP;
	private int port;
	private Socket socket;

	public TCPsocket_client(String IP, int port) { 
		this.IP = IP; 
		this.port = port; 
	}

	public void send(String command) {
		try {
			socket = new Socket(IP, port);
			if (socket.isConnected()){
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				out.write(command.getBytes("US-ASCII"));
				out.flush();
			} else {
				System.err.println("Failed to open socket");
				socket.close();
			}
		} catch (IOException e) { System.out.println(e); }
	}

	public String read() {
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
		} catch (IOException e) { 
			System.err.println(e);
			//e.printStackTrace();
			return "ERR";}
	}
}