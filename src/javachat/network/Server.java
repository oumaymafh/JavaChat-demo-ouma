package javachat.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import javachat.JavaChat;
import javachat.network.socket.SocketController;
import javachat.network.socket.SocketHandler;
import javachat.network.util.IPUtil;
import javachat.network.util.UPnP;

/**
 * Server class, waits for connection on given port
 * 
 * @author DrLabman
 */
public class Server implements Runnable, SocketHandler {
	private int port;
	private boolean connected;
	private boolean disconnect;
	private ArrayList<SocketController> clients;
	private ServerSocket srvr;
	
	public Server(int port){
		this.port = port;
		disconnect = false;
		connected = false;
		
		clients = new ArrayList<SocketController>();
		
		IPUtil.printExternalIP();
		IPUtil.printInternalIP();
		UPnP.RegisterPort(port);

		Thread t = new Thread(this);
		t.start();
	}

	@Override
	public void run() {
		try {
			srvr = new ServerSocket(port);
			connected = true;
			while (!disconnect){
				Socket skt = srvr.accept();
				clients.add(new SocketController(this, skt));
			}
		} catch (SocketException ex) {
			if (!ex.getMessage().equals("socket closed"))
				JavaChat.println("Socket Exception: " + ex.getMessage());
		} catch (IOException ex) {
			JavaChat.println("IO Exception: " + ex.getMessage());
		}
	}
	
	public boolean isConnected() {
		return connected;
	}

	@Override
	public void receiveMsg(SocketController socketControl, String msg){
		if (msg.length() != 0){
			if (msg.endsWith("\n")){
				msg = msg.substring(0,msg.length()-1);
			}
			
			if (msg.startsWith("CMD")){
				String cmd = msg.substring(4);
				if (cmd.equals("QUIT")){
					socketControl.disconnect();
					JavaChat.println("Client disconnected.");
				} else if (cmd.startsWith("HELO")){
					String name = cmd.substring(5);
					sendMsg(null, name + " connected...");
				} else if (cmd.startsWith("NAME")){
					String names[] = cmd.substring(5).split(" ");
					sendMsg(null, names[0] + " changed name to " + names[1]);
				} else {
					JavaChat.println("Unknown command from connection: " + msg);
				}
			} else if (msg.startsWith("MSG")) {
				sendMsg(socketControl, msg.substring(4));
			}
		}
	}
	
	@Override
	public void disconnected(SocketController client){
		clients.remove(client);
	}
	
	public void sendMsg(SocketController sender, String msg) {
		for (SocketController client: clients){
			if (client != sender)
				client.sendMsg(msg);
		}
	}
	
	public void disconnect() {
		disconnect = true;
		for (SocketController client: clients){
			client.disconnect();
		}
		
		try {
			srvr.close();
		} catch (IOException ex) {
			JavaChat.println("Exception when closing socket: " + ex.getMessage());
		}
		JavaChat.println("No longer listening for connections.");
		
		UPnP.UnregisterPort();
	}
}
