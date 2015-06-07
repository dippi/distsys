package it.polimi.distsys.server;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.xml.bind.DatatypeConverter;

public class ClientHandler implements Runnable, Closeable {
	private Integer id = null;
	
	private Socket socket = null;
	private BufferedReader in = null;
	private PrintWriter out = null;
	
	private Server server;
	
	private Boolean started = false;
	private Boolean closing = false;
	
	public ClientHandler(Integer id, Socket socket, Server server) throws Exception {
		try {
			this.id = id;
			
			this.socket = socket;
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.out = new PrintWriter(socket.getOutputStream(), true);
			
			this.server = server;
		} catch (Exception e) {
			throw new Exception("Unable to initialize connection.", e);
		}
	}
	
	public synchronized byte[] readBinaryLine() throws Exception {
		if (started) {
			throw new Exception("Read loop started, cannot read manually!");
		}
		
		return DatatypeConverter.parseBase64Binary(in.readLine());
	}
	
	public synchronized void start() {
		started = true;
		new Thread(this).start();
	}

	public synchronized void send(String ...lines) {
		for (String line : lines) {
			out.println(line);
		}
	}
	
	public synchronized void send(byte[] ...binaryLines) {
		String[] lines = new String[binaryLines.length];
		for (int i = 0; i < binaryLines.length; i++) {
			lines[i] = DatatypeConverter.printBase64Binary(binaryLines[i]);
		}
		send(lines);
	}

	@Override
	public void run() {
		try {
			String message;
			while ((message = in.readLine()) != null) {
			    server.forward(id, message);
			}
			server.free(id);
		} catch (IOException e) {
			if (!closing) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public synchronized void close() throws IOException {
		closing  = true; 
		socket.close();
	}
}