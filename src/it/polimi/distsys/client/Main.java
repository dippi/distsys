package it.polimi.distsys.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {
	private static Boolean closing = false;

	public static synchronized void quit() {
		closing = true;
		System.out.println("Connection closed, press ENTER to quit.");
	}

	public static void main(String[] args) {
		String address = "localhost";
		Integer port = 12345;
		try (Client server = new Client(address, port)) {
			System.out.println("Press ^D to exit.");
			
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			
			String userInput;
			while ((userInput = in.readLine()) != null && !closing) {
				server.send(userInput);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
