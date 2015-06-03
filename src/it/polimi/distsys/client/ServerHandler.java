package it.polimi.distsys.client;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerHandler implements Runnable, Closeable {
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	private Boolean closing = false;

	public ServerHandler(String host, int port) throws IOException {
		System.out.printf("Connecting to %s:%s%n", host, port);
		this.socket = new Socket(host, port);
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.out = new PrintWriter(socket.getOutputStream(), true);
		
		new Thread(this).start();
	}
	
	public void send(String message) {
		out.println(message);
	}

	@Override
	public void run() {
		try {
			String message;
			while ((message = in.readLine()) != null) {
			    Client.receive(message);
			}
			Client.quit();
		} catch (IOException e) {
			if (!closing) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void close() throws IOException {
		closing = true; 
		socket.close();
	}
}
