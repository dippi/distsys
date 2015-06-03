package it.polimi.distsys.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.bind.DatatypeConverter;

public class Server {
	private static ClientHandler[] sockets = new ClientHandler[8];
	private static Queue<Integer> freelist = new LinkedList<>(Arrays.asList(0,1,2,3,4,5,6,7));
	
	public static synchronized void receive(Integer id, String message) {
		System.out.printf("[Client %s] Message: \"%s\"%n", id, message);
		for(ClientHandler handler : sockets) {
			if (handler != null) {
				handler.send(id, message);
			}
		}
	}
	
	public static synchronized void free(Integer id) {
		sockets[id] = null;
		freelist.add(id);
		System.out.printf("[Client %s] Disconnected!%n", id);
	}
	
	public static void main(String[] args) {
		// String => Cipher => Base64 => Socket
		
		Integer port = 12345; // Integer.parseInt(args[1]);
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			String address = "localhost"; //InetAddress.getLocalHost().getHostAddress();
			port = serverSocket.getLocalPort();
			System.out.printf("Listening on %s:%s%n", address, port);
			
			ExecutorService pool = Executors.newCachedThreadPool();
			
			while (true) {
				try {
					Socket socket = serverSocket.accept();
					synchronized (Server.class) {
						Integer id = freelist.poll();
						if(id == null) {
							System.err.println("Connection refused, max clients reached.");
							// TODO: warn the client
						} else {
							System.out.printf("[Client %s] Connected!%n", id);
							ClientHandler handler = new ClientHandler(id, socket);
							
							sockets[id] = handler;
							pool.execute(handler);
						}
					}
					//String encoded = DatatypeConverter.printBase64Binary(null);
				} catch (IOException e) {
					System.out.println("I/O error: " + e);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
