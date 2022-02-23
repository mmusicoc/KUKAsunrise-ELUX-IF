package EluxUtils;

import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TCPsocket_client {
	private Socket socket;
	private String IP;
	private int port;
	private int timeoutAns;
	InputStreamReader reader;
    DataOutputStream out;
    char endchar;

	public TCPsocket_client(String IP, int port, int timeoutAns, char endchar) { 
		this.IP = IP;
		this.port = port;
		this.endchar = endchar;
		this.timeoutAns = timeoutAns;
		
		socket = new Socket();
	}
	
	public boolean open() {
		try {
			socket.connect(new InetSocketAddress(IP,port), 1000);
			socket.setSoTimeout(timeoutAns);
			
			out = new DataOutputStream(socket.getOutputStream());
			reader = new InputStreamReader(socket.getInputStream());
			return true;
		} catch (IOException e) {
			System.err.println(e);
			return false;
		}
	}
	
	public boolean close() {
		if(socket.isConnected()) try {
			socket.close();
			if(socket.isClosed()) return true;
			else return false;
		} catch (Exception e) {
			System.err.println(e);
			return false;
		}
		return false;
	}

	public boolean send(String command) {
		try {
			if (socket.isConnected()){
				out.write(command.getBytes("US-ASCII"));
				out.flush();
				return true;
			} else {
				System.err.println("Socket was disconnected");
				socket.close();
				return false;
			}
		} catch (IOException e) {
			System.err.println(e);
			return false;
		}
	}

	public String read() {
		try {
			int character;
			StringBuilder data = new StringBuilder();
			while((character = reader.read()) != endchar) {
				data.append((char) character);
			}
			// System.out.println("Response: " + data.toString());
			return data.toString();
		} catch (IOException e) { 
			System.err.println(e);
			// e.printStackTrace();
			return "ERR";
		}
	}
}