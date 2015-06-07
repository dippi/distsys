package it.polimi.distsys.client;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

public class EncryptionManager {
	public enum Algorithm { AES, RSA };
	
	private KeyPair rsa;
	
	private Key dek;
	private Key[] keks = new Key[3];
	
	public EncryptionManager() {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(1024);
		    rsa = keyPairGenerator.genKeyPair();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized byte[] getPublicKey() {
		return rsa.getPublic().getEncoded();
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
	
	private byte[] decrypt(Algorithm algorithm, Key key, byte[] ciphertext) throws Exception {
		try {
			Cipher cipher = Cipher.getInstance(algorithm.toString());
			cipher.init(Cipher.DECRYPT_MODE, key);
			return cipher.doFinal(ciphertext);
		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			throw new Exception("Error encrypting plaintext", e);
		}
	}
	
	public synchronized void initialKeysDistribution(byte[][] keys) throws Exception {
		try {
		    dek = new SecretKeySpec(decrypt(Algorithm.RSA, rsa.getPrivate(), keys[0]), "AES");
		    for (int i = 0; i < keks.length; ++i) {
		    	keks[i] = new SecretKeySpec(decrypt(Algorithm.RSA, rsa.getPrivate(), keys[i+1]), "AES");
		    }
		} catch (Exception e) {
			throw new Exception("Error during initial key distribution", e);
		}
	}
	
	public synchronized void refreshKeys(Integer selector, byte[][] keys) throws Exception {
		try {
			for (int i = 0; i < keks.length; i++) {
				if (((selector >> i) & 1) == 1) {
					System.out.println("======== Changing DEK ========");
					System.out.printf("from: %s%n", DatatypeConverter.printBase64Binary(dek.getEncoded()));
					
					dek = new SecretKeySpec(decrypt(Algorithm.AES, keks[i], keys[i]), "AES");
					
					System.out.printf("to:   %s%n", DatatypeConverter.printBase64Binary(dek.getEncoded()));
					
					break;
				}
			}
			
			for (int i = 0; i < keks.length; i++) {
				if (((selector >> i) & 1) == 0) {
					int j = i + keks.length;
					
					System.out.printf("====== Changing DEK[%d] =======%n", i);
					System.out.printf("from: %s%n", DatatypeConverter.printBase64Binary(keks[i].getEncoded()));
					
					keks[i] = new SecretKeySpec(decrypt(Algorithm.AES, keks[i], decrypt(Algorithm.AES, dek, keys[j])), "AES");
					
					System.out.printf("to:   %s%n", DatatypeConverter.printBase64Binary(keks[i].getEncoded()));
				}
			}
		} catch (Exception e) {
			throw new Exception("Error during keys refresh", e);
		}
	}
	
	public synchronized byte[] encryptMessage(String message) throws Exception {
		try {
			return encrypt(Algorithm.AES, dek, message.getBytes());
		} catch (Exception e) {
			throw new Exception("Error during message encryption", e);
		}
	}
	
	public synchronized String decryptMessage(byte[] message) throws Exception {
		try {
			return new String(decrypt(Algorithm.AES, dek, message));
		} catch (Exception e) {
			throw new Exception("Error during message decryption", e);
		}
	}
}
