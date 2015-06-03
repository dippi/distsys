package it.polimi.distsys.client;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

public class ServerHandler implements Runnable, Closeable {
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	
	private int id;
	private Key dek;
	private Key[] keks = new Key[3];
	
	private Boolean closing = false;

	public ServerHandler(String host, int port) throws IOException {
		System.out.printf("Connecting to %s:%s%n", host, port);
		
		this.socket = new Socket(host, port);
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.out = new PrintWriter(socket.getOutputStream(), true);
		
		connect();
		
		new Thread(this).start();
	}
	
	private void connect() throws IOException {
		try {
			id = Integer.parseInt(in.readLine());
			
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(1024);
		    KeyPair keyPair = keyPairGenerator.genKeyPair();
		    
		    out.println(DatatypeConverter.printBase64Binary(keyPair.getPublic().getEncoded()));
		    
		    Cipher cipher = Cipher.getInstance("RSA");
		    cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
		    
		    dek = new SecretKeySpec(cipher.doFinal(DatatypeConverter.parseBase64Binary(in.readLine())), "AES");
		    for (int i = 0; i < keks.length; ++i) {
		    	keks[i] = new SecretKeySpec(cipher.doFinal(DatatypeConverter.parseBase64Binary(in.readLine())), "AES");
		    }
		} catch (NoSuchAlgorithmException | NumberFormatException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
	}

	public void send(String message) {
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, dek);
			
			out.println(DatatypeConverter.printBase64Binary(cipher.doFinal(message.getBytes())));
			
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
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
					
					Cipher cipher = Cipher.getInstance("AES");
					cipher.init(Cipher.DECRYPT_MODE, dek);
					String message = new String(cipher.doFinal(DatatypeConverter.parseBase64Binary(in.readLine())));
					
					Client.receive(id, message);
				} else if (line.equals("keys")) {
					synchronized (Client.class) {
						Integer diff = Integer.parseInt(in.readLine()) ^ id;
						
						Cipher cipher = Cipher.getInstance("AES");
						
						Boolean updated = false;
						for (int i = 0; i < keks.length; i++) {
							String ciphertext = in.readLine();
							if (!updated && ((diff >> i) & 1) == 1) {
								cipher.init(Cipher.DECRYPT_MODE, keks[i]);
								dek = new SecretKeySpec(cipher.doFinal(DatatypeConverter.parseBase64Binary(ciphertext)), "AES");
								updated = true;
							}
						}
						
						for (int i = 0; i < keks.length; i++) {
							String ciphertext = in.readLine();
							if (((diff >> i) & 1) == 0) {
								cipher.init(Cipher.DECRYPT_MODE, dek);
								byte[] intermediate = cipher.doFinal(DatatypeConverter.parseBase64Binary(ciphertext));
								cipher.init(Cipher.DECRYPT_MODE, keks[i]);
								keks[i] = new SecretKeySpec(cipher.doFinal(intermediate), "AES");
							}
						}
					}
				} else {
					System.err.println("Unknown communication sequence!");
				}
			}
			Client.quit();
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
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
