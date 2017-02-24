package com.wenyi.mafiaservice;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
/*
 * 待定
 * client  发送      房号+"\n"+本地地址
 * server  保存      map  <房号,IP><房号,PORT>  <IP,本地IP>
 *
 *服务器发送房间所有地址以及本地地址，客户端自行分析，如果存在同局域网的IP，则发送到其本地地址
 *客户端可通过回传的本地地址确认自己
 *
 */
public class PeerServer implements Runnable {
	HashMap<Integer, ArrayList<SocketAddress>> gameMap = new HashMap<>(52);
	HashMap<Integer, String> gameMsg = new HashMap<>();
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			DatagramSocket server = new DatagramSocket(6789);
			byte[] buf = new byte[1024];
			DatagramPacket packet = new DatagramPacket(buf, 1024);
//			SocketAddress address = new Ine
			while (true) {
				server.receive(packet);
				if(packet.getLength() == 0)
					continue;
				//test peer
				if(packet.getLength() == 1){
					LoginServer.eService.execute(new PeerTest((InetSocketAddress) packet.getSocketAddress(), server));
					continue;
				}
				//客户端发送过来的数据格式   roomID + localIP //+ port
//				System.out.println(new String(packet.getData(),"utf-8").trim());
				String[] clientMsg = new String(packet.getData(),"utf-8").trim().split("\n",2);
				int roomID = Integer.parseInt(clientMsg[0]);
				//服务器发送给客户端的数据格式   InetIP + port + localIP
				String ipMsg = "|"+packet.getAddress()+"\n"+packet.getPort()+"\n"+ clientMsg[1];
				String echo = ipMsg+"\n0";
				DatagramPacket echoPacket = new DatagramPacket(echo.getBytes(), echo.getBytes().length, packet.getSocketAddress());
				server.send(echoPacket);
				server.send(echoPacket);
				server.send(echoPacket);
//				byte[] self = ipMsg.getBytes();
//				DatagramPacket p = new DatagramPacket(self, self.length, packet.getSocketAddress());
//				server.send(p);
				//暂定
				if(gameMap.containsKey(roomID) && !gameMap.get(roomID).contains(packet.getSocketAddress())){
					gameMap.get(roomID).add(packet.getSocketAddress());
					//自定义分割符
					String msg = gameMsg.get(roomID)+ipMsg;
					gameMsg.put(roomID, msg);
				}else if (!gameMap.containsKey(roomID)) {
					ArrayList<SocketAddress> list = new ArrayList<>();
					list.add(packet.getSocketAddress());
					gameMap.put(roomID, list);
					gameMsg.put(roomID, ipMsg);
				}
				//send to all
				System.out.println(gameMsg.get(roomID));
				byte[] msg = gameMsg.get(roomID).getBytes();
				for (SocketAddress ip : gameMap.get(roomID)) {
					DatagramPacket p = new DatagramPacket(msg, msg.length, ip);
					server.send(p);
					server.send(p);
					server.send(p);
				}
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void gameOver(int roomID){
		if(gameMap.containsKey(roomID)){
			gameMap.remove(roomID);
			gameMsg.remove(roomID);
		}
	}
	//int与byte数组之间的转换，暂时无用
//	public static int bytesToInt(byte[] src, int offset) {  
//	    int value;    
//	    value = (int) ((src[offset] & 0xFF)   
//	            | ((src[offset+1] & 0xFF)<<8)   
//	            | ((src[offset+2] & 0xFF)<<16)   
//	            | ((src[offset+3] & 0xFF)<<24));  
//	    return value;  
//	}  
	public static byte[] intToBytes( int value )   
	{   
	    byte[] src = new byte[4];  
	    src[3] =  (byte) ((value>>24) & 0xFF);  
	    src[2] =  (byte) ((value>>16) & 0xFF);  
	    src[1] =  (byte) ((value>>8) & 0xFF);    
	    src[0] =  (byte) (value & 0xFF);                  
	    return src;   
	}
	class PeerTest implements Runnable {
		private InetSocketAddress address;
		private DatagramSocket server;
		public PeerTest(InetSocketAddress address,DatagramSocket server) {
			// TODO Auto-generated constructor stub
			this.address = address;
			this.server = server;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				DatagramSocket peer = new DatagramSocket();
				DatagramPacket p = new DatagramPacket(intToBytes(peer.getLocalPort()), 4 ,address);
				server.send(p);
				server.send(p);
				server.send(p);
				//不接收
				DatagramPacket packet = new DatagramPacket(new byte[0], 0, address);
				for (int i = 0; i < 10; i++) {
					peer.send(packet);
					Thread.sleep(500);
				}
				peer.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
