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
 * ����
 * client  ����      ����+"\n"+���ص�ַ
 * server  ����      map  <����,IP><����,PORT>  <IP,����IP>
 *
 *���������ͷ������е�ַ�Լ����ص�ַ���ͻ������з������������ͬ��������IP�����͵��䱾�ص�ַ
 *�ͻ��˿�ͨ���ش��ı��ص�ַȷ���Լ�
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
				//�ͻ��˷��͹��������ݸ�ʽ   roomID + localIP //+ port
//				System.out.println(new String(packet.getData(),"utf-8").trim());
				String[] clientMsg = new String(packet.getData(),"utf-8").trim().split("\n",2);
				int roomID = Integer.parseInt(clientMsg[0]);
				//���������͸��ͻ��˵����ݸ�ʽ   InetIP + port + localIP
				String ipMsg = "|"+packet.getAddress()+"\n"+packet.getPort()+"\n"+ clientMsg[1];
				String echo = ipMsg+"\n0";
				DatagramPacket echoPacket = new DatagramPacket(echo.getBytes(), echo.getBytes().length, packet.getSocketAddress());
				server.send(echoPacket);
				server.send(echoPacket);
				server.send(echoPacket);
//				byte[] self = ipMsg.getBytes();
//				DatagramPacket p = new DatagramPacket(self, self.length, packet.getSocketAddress());
//				server.send(p);
				//�ݶ�
				if(gameMap.containsKey(roomID) && !gameMap.get(roomID).contains(packet.getSocketAddress())){
					gameMap.get(roomID).add(packet.getSocketAddress());
					//�Զ���ָ��
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
	//int��byte����֮���ת������ʱ����
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
				//������
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
