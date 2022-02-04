package EluxUtils;

//import static EluxUtils.Utils.*;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TCPsocket_client {
	//private String IP;
	//private int port;
	private Socket socket;
	InputStreamReader reader;
    DataOutputStream out;
    char endchar;

	public TCPsocket_client(String IP, int port, char endchar) { 
		//this.IP = IP; 
		//this.port = port; 
		this.endchar = endchar;
		
		try{
			socket = new Socket();
			socket.connect(new InetSocketAddress(IP,port), 1000);
			socket.setSoTimeout(5*1000);
			
			out = new DataOutputStream(socket.getOutputStream());
			reader = new InputStreamReader(socket.getInputStream());
		}
		catch (IOException e){
			System.err.println(e);
			
		}
	}

	public boolean send(String command) {
		try {
			if (socket.isConnected()){
				// System.out.println(command);
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
			while ((character = reader.read()) != endchar) {
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