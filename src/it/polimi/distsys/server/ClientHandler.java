package it.polimi.distsys.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
	private Integer id = null;
	private Socket socket = null;
	private BufferedReader in = null;
	private PrintWriter out = null;
	
	public ClientHandler(Integer id, Socket socket) throws IOException {
		this.id = id;
		this.socket = socket;
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.out = new PrintWriter(socket.getOutputStream(), true);
	}

	@Override
	public void run() {
		try {
			String message;
			while ((message = in.readLine()) != null) {
			    Server.receive(id, message);
			}
			Server.free(id);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void send(Integer id, String message) {
		out.printf("[Client %s] Message: \"%s\"%n", id, message);
	}
}