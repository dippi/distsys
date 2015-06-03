package it.polimi.distsys.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.xml.bind.DatatypeConverter;

public class Server {
	private static ClientHandler[] sockets = new ClientHandler[8];
	private static Queue<Integer> freelist = new LinkedList<>(Arrays.asList(0,1,2,3,4,5,6,7));
	
	private static KeyGenerator keyGenerator;
	
	static Key dek;
	static Key[][] keks = new Key[3][2];
	
	public static synchronized void receive(Integer id, String message) {
		System.out.printf("[Client %s] Sent a Message!%n", id);
		for(ClientHandler handler : sockets) {
			if (handler != null) {
				handler.send("message");
				handler.send(id.toString());
				handler.send(message);
			}
		}
	}
	
	public static synchronized void free(Integer id) {
		sockets[id] = null;
		freelist.add(id);
		regenerateKeys(id);
		System.out.printf("[Client %s] Disconnected!%n", id);
	}
	
	private static synchronized void regenerateKeys(Integer id) {
		try {
			dek = keyGenerator.generateKey();
			
			Cipher cipher = Cipher.getInstance("AES");
			
			String[] encryptedDek = new String[keks.length];
			for (int i = 0; i < keks.length; ++i) {
				int j = ~(id >> i) & 1;
				
				cipher.init(Cipher.ENCRYPT_MODE, keks[i][j]);
				encryptedDek[i] = DatatypeConverter.printBase64Binary(cipher.doFinal(dek.getEncoded()));
			}
			
			String[] encryptedKeks = new String[keks.length];
			for (int i = 0; i < keks.length; ++i) {
				int j = (id >> i) & 1;
				
				cipher.init(Cipher.ENCRYPT_MODE, keks[i][j]);
				keks[i][j] = keyGenerator.generateKey();
				byte[] intermediate = cipher.doFinal(keks[i][j].getEncoded());
				
				cipher.init(Cipher.ENCRYPT_MODE, dek);
				encryptedKeks[i] = DatatypeConverter.printBase64Binary(cipher.doFinal(intermediate));
			}
			
			for(ClientHandler handler : sockets) {
				if (handler != null) {
					handler.send("keys");
					handler.send(id.toString());
					
					for (String key : encryptedDek) {
						handler.send(key);
					}
					
					for (String key : encryptedKeks) {
						handler.send(key);
					}
				}
			}
		} catch (InvalidKeyException | IllegalBlockSizeException | NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(128);
			
		    dek = keyGenerator.generateKey();
		    
		    for (int i = 0; i < keks.length; i++) {
				for (int j = 0; j < keks[i].length; j++) {
					keks[i][j] = keyGenerator.generateKey();
				}
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
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
							
							regenerateKeys(id);
							
							ClientHandler handler = new ClientHandler(id, socket);
							
							sockets[id] = handler;
							pool.execute(handler);
						}
					}
				} catch (IOException e) {
					System.out.println("I/O error: " + e);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
