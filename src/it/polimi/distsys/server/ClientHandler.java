package it.polimi.distsys.server;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.xml.bind.DatatypeConverter;

public class ClientHandler implements Runnable, Closeable {
	private Integer id = null;
	private Socket socket = null;
	private BufferedReader in = null;
	private PrintWriter out = null;
	private Boolean closing = false;
	
	public ClientHandler(Integer id, Socket socket) throws IOException {
		this.id = id;
		this.socket = socket;
		this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.out = new PrintWriter(socket.getOutputStream(), true);
		
		connect();
	}

	private void connect() throws IOException {
		try {
			out.println(id);

			PublicKey rsaKey = KeyFactory.getInstance("RSA").generatePublic(
					new X509EncodedKeySpec(DatatypeConverter.parseBase64Binary(in.readLine())));
			
			Cipher cipher = Cipher.getInstance("RSA");
		    cipher.init(Cipher.ENCRYPT_MODE, rsaKey);
		    
		    out.println(DatatypeConverter.printBase64Binary(cipher.doFinal(Server.dek.getEncoded())));
		    
		    for (int i = 0; i < Server.keks.length; ++i) {
		    	Key key = Server.keks[i][(id >> i) & 1];
		    	out.println(DatatypeConverter.printBase64Binary(cipher.doFinal(key.getEncoded())));
		    }
		} catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
	}

	public void send(String message) {
		out.println(message);
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
			if (!closing) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void close() throws IOException {
		closing  = true; 
		socket.close();
	}
}