package com.wenyi.mafiaservice;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginServer {
	public static final int PORT = 6636;
	public static PeerServer peerServer = new PeerServer();
	public static ExecutorService eService;
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		eService = Executors.newCachedThreadPool();
		Client cThread;
		eService.execute(peerServer);
//		eService.execute(new Client.SkipRunnable());
		try {
			@SuppressWarnings("resource")
			ServerSocket server = new ServerSocket(PORT);
			while (true) {
				System.out.println("--------");
				Socket client = server.accept();
				cThread = new Client(client);
				eService.execute(cThread);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
