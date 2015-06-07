package it.polimi.distsys.client;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.xml.bind.DatatypeConverter;

public class Client implements Runnable, Closeable {
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	
	private int id;
	
	private EncryptionManager encryptionManager;
	
	private Boolean closing = false;

	public Client(String host, int port) throws Exception {
		System.out.printf("Connecting to %s:%s%n", host, port);
		
		this.socket = new Socket(host, port);
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.out = new PrintWriter(socket.getOutputStream(), true);
		
		this.encryptionManager = new EncryptionManager();
		
		id = Integer.parseInt(in.readLine());
		out.println(DatatypeConverter.printBase64Binary(encryptionManager.getPublicKey()));
		
		byte[][] keys = new byte[4][];
		for (int i = 0; i < keys.length; i++) {
			keys[i] = DatatypeConverter.parseBase64Binary(in.readLine());
		}
		
		encryptionManager.initialKeysDistribution(keys);
		
		new Thread(this).start();
	}
	
	public void send(String message) {
		try {
			out.println(DatatypeConverter.printBase64Binary(encryptionManager.encryptMessage(message)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			String line;
			while ((line = in.readLine()) != null) {
				if (line.equals("message")) {
					Integer id = Integer.parseInt(in.readLine());
					String message = encryptionManager.decryptMessage(DatatypeConverter.parseBase64Binary(in.readLine()));
					
					System.out.printf("[Client %s] Message: \"%s\"%n", id, message);
					
				} else if (line.equals("keys")) {
					Integer selector = Integer.parseInt(in.readLine()) ^ id;
					
					byte[][] keys = new byte[6][];
					for (int i = 0; i < keys.length; i++) {
						keys[i] = DatatypeConverter.parseBase64Binary(in.readLine());
					}
					
					encryptionManager.refreshKeys(selector, keys);
				} else {
					System.err.println("Unknown communication sequence!");
				}
			}
			Main.quit();
		} catch (Exception e) {
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
