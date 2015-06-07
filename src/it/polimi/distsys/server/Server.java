package it.polimi.distsys.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class Server implements Runnable, Closeable {
	
	private ClientHandler[] sockets = new ClientHandler[8];
	private Queue<Integer> freelist = new LinkedList<>(Arrays.asList(0,1,2,3,4,5,6,7));
	
	private ServerSocket serverSocket;
	private EncryptionManager encryptionManager;
	
	private Boolean closing = false;
	
	public Server(Integer port) throws Exception {
		try {
			encryptionManager = new EncryptionManager();
			
			serverSocket = new ServerSocket(port);
			
			System.out.printf("Listening on port:%s%n", port);
			
			new Thread(this).start();
			
		} catch (IOException e) {
			throw new Exception("Failed lifting the server.", e);
		}
	}
	
	private synchronized void accept(Socket socket) {
		try {
			Integer id = freelist.poll();
			if(id == null) {
				System.err.println("Connection refused, max clients reached.");
			} else {
				System.out.printf("[Client %s] Connected!%n", id);
				
				send("keys", id.toString());
				send(encryptionManager.regenerateKeys(id));
				
				sockets[id] = new ClientHandler(id, socket, this);
				
				sockets[id].send(id.toString());
				sockets[id].send(encryptionManager.initialKeysDistribution(id, sockets[id].readBinaryLine()));
				
				sockets[id].start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void free(Integer id) {
		try {
			sockets[id] = null;
			freelist.add(id);
			
			System.out.printf("[Client %s] Disconnected!%n", id);
		
			send("keys", id.toString());
			send(encryptionManager.regenerateKeys(id));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void forward(Integer id, String message) {
		System.out.printf("[Client %s] Sent a Message!%n", id);
		send("message", id.toString(), message);
	}
	
	private synchronized void send(String ...lines) {
		for(ClientHandler handler : sockets) {
			if (handler != null) {
				handler.send(lines);
			}
		}
	}
	
	private synchronized void send(byte[] ...lines) {
		for(ClientHandler handler : sockets) {
			if (handler != null) {
				handler.send(lines);
			}
		}
	}
	
	@Override
	public void run() {
		try {
			while (true) {
				accept(serverSocket.accept());
			}
		} catch (IOException e) {
			if (!closing) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public synchronized void close() throws IOException {
		closing  = true;
		
		for (ClientHandler handler : sockets) {
			if (handler != null) {
				handler.close();
			}
		}
		
		serverSocket.close();
	}
}
