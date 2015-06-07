package it.polimi.distsys.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {
	public static void main(String[] args) {
		try (
			Server broadcast = new Server(12345);
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in))
		) {
			System.out.println("Press ^D to exit.");
			while (in.readLine() != null) {}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
