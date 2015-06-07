package it.polimi.distsys.server;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.xml.bind.DatatypeConverter;

public class EncryptionManager {
	public enum Algorithm { AES, RSA };
	
	private Key dek;
	private Key[][] keks = new Key[3][2];
	
	private KeyGenerator keyGenerator;
	
	public EncryptionManager() throws Exception {
		try {
			keyGenerator = KeyGenerator.getInstance(Algorithm.AES.toString());
			keyGenerator.init(128);
			
		    dek = keyGenerator.generateKey();
		    
		    for (int i = 0; i < keks.length; i++) {
				for (int j = 0; j < keks[i].length; j++) {
					keks[i][j] = keyGenerator.generateKey();
				}
			}
		} catch (NoSuchAlgorithmException e) {
			throw new Exception("Error initializing EncryptionManager", e);
		}
	}
	
	private byte[] encrypt(Algorithm algorithm, Key key, byte[] plaintext) throws Exception {
		try {
			Cipher cipher = Cipher.getInstance(algorithm.toString());
			cipher.init(Cipher.ENCRYPT_MODE, key);
			return cipher.doFinal(plaintext);
		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			throw new Exception("Error encrypting plaintext", e);
		}
	}
	
	public synchronized byte[][] initialKeysDistribution(Integer id, byte[] rawKey) throws Exception {
		try {
			byte[][] result = new byte[keks.length + 1][];
			
			Key key = KeyFactory.getInstance(Algorithm.RSA.toString()).generatePublic(new X509EncodedKeySpec(rawKey));
			
			result[0] = encrypt(Algorithm.RSA, key, dek.getEncoded());
			
			for (int i = 0; i < keks.length; ++i) {
				int j = (id >> i) & 1;
		    	result[i + 1] = encrypt(Algorithm.RSA, key, keks[i][j].getEncoded());
			}
	
			return result;
		} catch (Exception e) {
			throw new Exception("Error in initial key distribution", e);
		}
	}
	
	public synchronized byte[][] regenerateKeys(Integer id) throws Exception {
		try {
			byte[][] result = new byte[keks.length * 2][];
			
			System.out.println("======== Changing DEK ========");
			System.out.printf("from: %s%n", DatatypeConverter.printBase64Binary(dek.getEncoded()));
			
			dek = keyGenerator.generateKey();
			
			System.out.printf("to:   %s%n", DatatypeConverter.printBase64Binary(dek.getEncoded()));
			
			
			for (int i = 0; i < keks.length; ++i) {
				int j = ~(id >> i) & 1;
				result[i] = encrypt(Algorithm.AES, keks[i][j], dek.getEncoded());
			}
			
			for (int i = 0; i < keks.length; ++i) {
				int j = (id >> i) & 1;
				int k = i + keks.length;
				
				System.out.printf("===== Changing DEK[%d][%d] =====%n", i, j);
				System.out.printf("from: %s%n", DatatypeConverter.printBase64Binary(keks[i][j].getEncoded()));
				
				Key oldKek = keks[i][j];
				keks[i][j] = keyGenerator.generateKey();
				
				System.out.printf("to:   %s%n", DatatypeConverter.printBase64Binary(keks[i][j].getEncoded()));
				
				result[k] = encrypt(Algorithm.AES, dek, encrypt(Algorithm.AES, oldKek, keks[i][j].getEncoded()));
			}
			
			return result;
		} catch (Exception e) {
			throw new Exception("Error regenerating keys", e);
		}
	}
}
