package it.polimi.distsys.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Client {
	private static Boolean close = false;

	public static void receive(String message) {
		System.out.println(message);
	}

	public static void quit() {
		close = true;
		System.out.println("Connection closed, press ENTER to quit.");
	}

	public static void main(String[] args) {
		String address = "localhost"; //InetAddress.getLocalHost().getHostAddress();
		Integer port = 12345;
		try (ServerHandler server = new ServerHandler(address, port)) {
			System.out.println("Press ^D to exit.");
			
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			
			String userInput;
			while ((userInput = in.readLine()) != null && !close) {
			    server.send(userInput);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}