package com.wenyi.mafiaservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

public class Client implements Runnable {
	public static final int LOGIN = 0,CREATE_ROOM = 1,OUT_ROOM = 2,SKIP = 3,INTO_ROOM = 4,ROOM_LIST = 5,ROOM_CHANGE = 6,CURRENT_CHANGE = 7,
			GET_ROOM = 8,START_GAME = 9,LOGOUT = 10,READY_GAME = 11,SEND_SEAT = 12,SEND_SELF_SEAT = 13,REC_SEAT = 14,START_GAME_ERROR = 15,
			READY_TALK = 16,GAME_SEND_ROLE = 17,ROLE_NIGHT = 18,ALL_NIGHT = 19,GAME_VOTE = 20,CONFIRM_VOTE = 21,TELL_WHO_DEAD = 22,
			ELECTIONEERINGA = 23,CANDIDATE = 24,SPEAK = 25,DAY_VOTE = 26,CHIEF_ELECTED = 27,VOTE_SITUATION = 28,VOTE_INVALID = 29,END_SPEAK = 30,
			ABANDON_ELECTION = 31,GAMING_TALK = 32,WOLF_IDIOCTONIA = 33,TRANSFER_CHIEF = 34,GAME_OVER = 35,INTO_ROOM_ERROR = 36,PASSWORD_ERROR = 37,
			CHANGE_ROOM = 38,ROOM_PASSWORD = 39,FALL_LINE = 40,HUNTER_SKILL = 41,IDIOT_SKILL = 42,CANDIDATE_SPEAK = 43,LOGIN_ERROR = 44,
			CHANGE_GAME_TYPE = 45,SEND_EMOJI = 46;
	public static int ROOM_INDEX = 10000;
	private int thisRoomID = 0;
	private String nickName,address;
	private Socket socket;
	private static final Vector<Client> list = new Vector<>();
	private static final ConcurrentHashMap<Integer,RoomInfo> roomlist = new ConcurrentHashMap<>(52);
	private static final ConcurrentHashMap<Integer,RoomInfo> roomlistAudio = new ConcurrentHashMap<>(52);
	private static final ConcurrentHashMap<Integer,HashMap<Integer,Client>> room  = new ConcurrentHashMap<>(52);
	private static final ConcurrentHashMap<Integer,String> password  = new ConcurrentHashMap<>(52);
	public static final ConcurrentHashMap<Integer,GameLogic> game  = new ConcurrentHashMap<>(52);
	private int gameType,seatNumber = -1;
	private PrintWriter os;
	private BufferedReader is;
	private FirstMsg firstMsg;
	private RoomInfo roomInfo;
	private Gson gson = new Gson();
	private static Gson g = new Gson();
	private Player me;
	private boolean islogin = false;
	public Player getMe() {
		return me;
	}

	public void setMe(Player me) {
		this.me = me;
	}

	public int getThisRoomID() {
		return thisRoomID;
	}

	public void setThisRoomID(int thisRoomID) {
		this.thisRoomID = thisRoomID;
	}

	public int getSeatNumber() {
		return seatNumber;
	}

	public void setSeatNumber(int seatNumber) {
		this.seatNumber = seatNumber;
	}

	public int getGameType() {
		return gameType;
	}

	public void setGameType(int gameType) {
		this.gameType = gameType;
	}

	public Client(Socket socket) {
		// TODO Auto-generated constructor stub
		super();
		this.socket = socket;
		this.address = socket.getInetAddress().getHostAddress();
		this.gameType = 0;
		this.me = new Player(this);
		this.islogin = false;
		try {
			this.os = new PrintWriter(this.socket.getOutputStream());
			this.is = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			socket.setSoTimeout(20*1000);
			String readMsg;
			while (!islogin) {
				readMsg = is.readLine();
				firstMsg = gson.fromJson(readMsg, FirstMsg.class);
				if(firstMsg == null)
					break;
				if(firstMsg.getType() == LOGIN){
					//往后登录后设置gametype
					this.nickName = firstMsg.getMsg();
					sendMsg(readMsg);
					Client.list.addElement(this);
					System.out.println(this.nickName + "登录！");
					islogin = true;
				}else if(firstMsg.getType() == LOGOUT){
					sendMsg(readMsg);
					break;
				}else if(firstMsg.getType() != SKIP){
					firstMsg.setType(LOGIN_ERROR);
					sendMsg(gson.toJson(firstMsg));
				}
			}
			while (islogin) {
				readMsg = is.readLine();
//				System.out.println(readMsg);
				firstMsg = gson.fromJson(readMsg, FirstMsg.class);
				if(firstMsg == null)
					break;
				switch (firstMsg.getType()) {
//					case LOGIN:
//						this.nickName = firstMsg.getMsg();
//						sendMsg(readMsg);
//						System.out.println(this.nickName + "登录！");
//						break;
					case CREATE_ROOM:
						String[] msg = firstMsg.getMsg().split("\n");
						roomInfo = gson.fromJson(msg[0], RoomInfo.class);
						createRoom(this, roomInfo);
						String echo = gson.toJson(roomInfo);
						firstMsg.setMsg(echo);
						sendMsg(gson.toJson(firstMsg));
						firstMsg.setType(SEND_SELF_SEAT);
						firstMsg.setMsg(String.valueOf(0));
						sendMsg(gson.toJson(firstMsg));
						if(roomInfo.getHasPassword() == 0){
							password.put(roomInfo.getRoomID(), msg[1]);
							firstMsg.setType(ROOM_PASSWORD);
							firstMsg.setMsg(msg[1]);
							sendMsg(gson.toJson(firstMsg));
						}
						break;
					case OUT_ROOM:
						System.out.println("out");
						// 两个map要同步
						// roomlist.remove(address);
//						roomInfo = gson.fromJson(firstMsg.getMsg(), RoomInfo.class);
						outRoom(this, Integer.parseInt(firstMsg.getMsg()));
						break;
					case SKIP:
						break;
					case GET_ROOM:
						sendRoom(this);
						break;
					case INTO_ROOM:
						String[] idAndPassword = firstMsg.getMsg().split("\n");
						int INTemp = Integer.parseInt(idAndPassword[0]);
						if(!password.containsKey(INTemp) || password.get(INTemp).equals(idAndPassword[1])){
							me.setReady(0);
							if (intoRoom(this, INTemp)) {
								sendSeat(this, INTemp);
								this.thisRoomID = INTemp;
							}else {
								sendMsg(gson.toJson(new FirstMsg(INTO_ROOM_ERROR, "")));
								//进入房间错误  人满或者其他异常
							}
						}else {
							sendMsg(gson.toJson(new FirstMsg(PASSWORD_ERROR, "")));
							//密码错误
						}
						
						break;
					case START_GAME:
						//start game 与 ready game 一起
						break;
					case LOGOUT:
						sendMsg(readMsg);
						System.out.println(this.nickName + "登出！");
						islogin = false;
						break;
					case READY_GAME:
						if (me.isReady() != 2) {
							me.setReady(Integer.parseInt(firstMsg.getMsg()));
							sendMsg(readMsg);
							changeReady(this, thisRoomID);
						} else {
							if (!startGame(this, thisRoomID)) {
								sendMsg(gson.toJson(new FirstMsg(START_GAME_ERROR, "")));
							}
						}
						break;
					case REC_SEAT:
						sendSeat(this, Integer.parseInt(firstMsg.getMsg()));
						break;
					case READY_TALK:
						if (game.containsKey(thisRoomID) && game.get(thisRoomID).getGameProcedure() == GameLogic.NIGHT_WOLF) {
								game.get(thisRoomID).wolfTalk(firstMsg.getMsg());
						}else {
							for (Client client : room.get(thisRoomID).values()) {
								if (client != this)
									client.sendMsg(readMsg);
							}
						}
						break;
					case GAME_VOTE:
						if(game.containsKey(thisRoomID))
							vote(seatNumber, thisRoomID, Integer.parseInt(firstMsg.getMsg()));
						break;
					case END_SPEAK:
//						firstMsg.setMsg(String.valueOf(seatNumber));
//						readMsg = gson.toJson(firstMsg);
//						for (Client client : room.get(thisRoomID).values()) {
//							client.sendMsg(readMsg);
//						}
						game.get(thisRoomID).endSpeak(false, 0);
						break;
					case ABANDON_ELECTION:
						firstMsg.setMsg(String.valueOf(seatNumber));
						readMsg = gson.toJson(firstMsg);
						for (Client client : room.get(thisRoomID).values()) {
							client.sendMsg(readMsg);
						}
						game.get(thisRoomID).abandonElection(seatNumber);
						break;
					case GAMING_TALK:
						//并入READY_TALK
						for (Client client : room.get(thisRoomID).values()) {
							if(client != this)client.sendMsg(readMsg);
						}
						break;
					case CHANGE_ROOM:
						String[] changeMsg = firstMsg.getMsg().split("\n");
						RoomInfo target = gson.fromJson(changeMsg[0], RoomInfo.class);
						changeRoomMsg(thisRoomID, target, target.getHasPassword() == 0 ? changeMsg[1]:"");
						break;
					case WOLF_IDIOCTONIA:
						//狼人自爆
						game.get(thisRoomID).setWolfKillSelf(true, seatNumber);
						break;
					case GAME_OVER:
//						game.remove(thisRoomID);
//						LoginServer.peerServer.gameOver(thisRoomID);
						break;
					case CHANGE_GAME_TYPE:
						this.setGameType(Integer.parseInt(firstMsg.getMsg()));
						sendRoom(this);
						break;
					case SEND_EMOJI:
						for (Client client : room.get(thisRoomID).values()) {
							if (client != this)
								client.sendMsg(readMsg);
						}
						break;
					default:
						break;
				}
			}
			this.os.close();
			this.is.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(thisRoomID != 0){
			outRoom(this, thisRoomID);
		}
		Client.list.remove(this);
		try {
			this.socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public synchronized void sendMsg(String msg) {
		this.os.println(msg);
		this.os.flush();
	}
//	public static synchronized void roomTalk(Client client,int roomID,String msg){
//		HashMap<Integer,Client> targetRoom = room.get(roomID);
//		String fmsg = g.toJson(new FirstMsg(READY_TALK, msg));
//		for (Client target : targetRoom.values()) {
//			if(target != client)target.sendMsg(fmsg);
//		}
//	}
	public static void changeReady(Client client,int roomID) {
		final HashMap<Integer,Client> targetRoom = room.get(roomID);
		synchronized (targetRoom) {
			String fmsg = g.toJson(new FirstMsg(SEND_SEAT, client.getSeatNumber()+"\n"+client.getMe().isReady()));
			for (Client target : targetRoom.values()) {
				target.sendMsg(fmsg);
			}
		}
	}
	public static void vote(int seatNumber,int roomID,int vote) throws InterruptedException{
		game.get(roomID).upDateVote(seatNumber, vote);
	}
	public static void changeRoomMsg(int roomID,RoomInfo info,String psw){
		//更改RoomInfo  通知其他人      重置所有准备状态
		final HashMap<Integer,Client> targetRoom = room.get(roomID);
		synchronized (targetRoom) {
			if (roomlist.contains(roomID)) {
				roomlist.remove(roomID);
				roomlist.put(roomID, info);
			} else if (roomlistAudio.contains(roomID)) {
				roomlistAudio.remove(roomID);
				roomlistAudio.put(roomID, info);
			} else
				return;
			if (info.getHasPassword() == 0) {
				password.put(roomID, psw);
			} else {
				password.remove(roomID);
			}
			FirstMsg firstMsg = new FirstMsg(CHANGE_ROOM, g.toJson(info));
			String msg = g.toJson(firstMsg);
			StringBuilder sb = new StringBuilder();
			for (Client client : room.get(roomID).values()) {
				if (client.getMe().isReady() == 1) {
					client.getMe().setReady(0);
					sb.append(client.getSeatNumber());
					sb.append("\n");
					sb.append(0);
					sb.append("\n");
				}
				client.sendMsg(msg);
			}
			String fmsg = g.toJson(new FirstMsg(SEND_SEAT, sb.toString()));
			for (Client client : room.get(roomID).values()) {
				client.sendMsg(fmsg);
			}
			for (Client client : list) {
				client.sendMsg(msg);
			}
			sb.setLength(0);
		}
	}
	public static boolean startGame(Client client,int roomID){
		final HashMap<Integer,Client> targetRoom = room.get(roomID);
		synchronized (targetRoom) {
			RoomInfo thisRoom;
			if(client.getGameType() == 0)
				thisRoom = roomlist.get(roomID);
			else if (client.getGameType() == 1) 
				thisRoom = roomlistAudio.get(roomID);
			else return false;
			int mens = thisRoom.getMens(),current = thisRoom.getCurrent();
			if(current < mens) return false;
			for (Client target : targetRoom.values()) {
				if(target.getMe().isReady()==0) return false;
			}
			thisRoom.setGaming(1);
			GameLogic gameLogic = new GameLogic(thisRoom,targetRoom);
			game.put(roomID, gameLogic);
			for (Client target : targetRoom.values()) {
				target.sendMsg(g.toJson(new FirstMsg(START_GAME, String.valueOf(roomID))));
			}
			for (Client other : list) {
				other.sendMsg(g.toJson(new FirstMsg(START_GAME, String.valueOf(roomID))));
			}
			LoginServer.eService.execute(gameLogic);
			return true;
		}
	}
	public static boolean intoRoom(Client client,int roomID){
		final HashMap<Integer,Client> targetRoom = room.get(roomID);
		if(targetRoom == null) return false;
		synchronized (targetRoom) {
			int current = targetRoom.size();
			if(current == 0) return false;
			RoomInfo thisRoom;
			if(client.getGameType() == 0)
				thisRoom = roomlist.get(roomID);
			else if (client.getGameType() == 1) 
				thisRoom = roomlistAudio.get(roomID);
			else return false;
			int max = thisRoom.getMens();
			if (current < max) {
				for (int i = 0; i < max; i++) {
					if (!targetRoom.containsKey(i)) {
						targetRoom.put(i, client);
						client.setSeatNumber(i);
						FirstMsg seat = new FirstMsg(SEND_SELF_SEAT, String.valueOf(i));
						client.sendMsg(g.toJson(seat));
						list.remove(client);
						thisRoom.setCurrent(current + 1);
						String msg = g.toJson(new FirstMsg(CURRENT_CHANGE, roomID + "\n" + (current + 1) + "\n" + max));
						String fmsg = g.toJson(
								new FirstMsg(SEND_SEAT, client.getSeatNumber() + "\n" + client.getMe().isReady()));
						client.sendMsg(g.toJson(new FirstMsg(INTO_ROOM, g.toJson(thisRoom))));
						for (Client target : targetRoom.values()) {
							if (target != client) {
								target.sendMsg(msg);
								target.sendMsg(fmsg);
							}
						}
						for (Client other : list) {
							if (other.getGameType() == 0) {
								other.sendMsg(msg);
							}
						}
						return true;
					}
				}
			}
		}
		return false;
	}
	public synchronized static void createRoom(Client client,RoomInfo roomInfo){
		//两个map要同步
		HashMap<Integer,Client> newRoom = new HashMap<>();
//		int mens = roomInfo.getMens();
//		for (int i = 0; i < mens; i++) {
//			if(!newRoom.containsKey(i)){
//				newRoom.put(i, client);
//			}
//		}
		newRoom.put(0, client);
		client.setSeatNumber(0);
		client.getMe().setReady(2);
		list.remove(client);
		if(ROOM_INDEX == 100000) ROOM_INDEX = 10000;
		while(room.containsKey(ROOM_INDEX)){
			ROOM_INDEX++;
		}
		roomInfo.setRoomID(ROOM_INDEX);
		room.put(ROOM_INDEX, newRoom);
		if(client.getGameType() == 0)
			roomlist.put(ROOM_INDEX, roomInfo);
		else if(client.getGameType() == 1)
			roomlistAudio.put(ROOM_INDEX, roomInfo);
		client.setThisRoomID(ROOM_INDEX);
		ROOM_INDEX++;
		String msg = g.toJson(new FirstMsg(ROOM_CHANGE, g.toJson(roomInfo)));
		for (Client otherClient : list) {
			if(otherClient.getGameType()==0){
				otherClient.sendMsg(msg);
			}
		}
//		FirstMsg firstMsg = new FirstMsg(SEND_SEAT, 0+"\n"+ 2);
//		String fmsg = g.toJson(firstMsg);
//		client.sendMsg(fmsg);
//		sendRoom();
		//发送创建成功提示
//		client.sendMsg(msg);
	}
	public static void outRoom(Client client,int roomID){
		final HashMap<Integer,Client> targetRoom = room.get(roomID);
		synchronized (targetRoom) {
			RoomInfo targetInfo;
			if (client.getGameType() == 0)
				targetInfo = roomlist.get(roomID);
			else if (client.getGameType() == 1)
				targetInfo = roomlistAudio.get(roomID);
			else
				return;
			int seat = client.getSeatNumber();
			targetRoom.remove(seat);
			client.setSeatNumber(-1);
			int current = targetRoom.size(), max = targetInfo.getMens();
			// int mens = current-1;
			list.addElement(client);
			client.setThisRoomID(0);
			String msg = g.toJson(new FirstMsg(CURRENT_CHANGE, roomID + "\n" + current + "\n" + max));
			if (current == 0) {
				roomlist.remove(roomID);
				roomlistAudio.remove(roomID);
				room.remove(roomID);
				password.remove(roomID);
			} else {
				targetInfo.setCurrent(current);
				if (client.getMe().isReady() == 2) {
					for (int i = 0; i < max; i++) {
						if (targetRoom.containsKey(i)) {
							targetRoom.get(i).getMe().setReady(2);
							String fmsg = g.toJson(new FirstMsg(SEND_SEAT, seat + "\n" + -1 + "\n" + i + "\n" + 2));
							if (password.containsKey(roomID))
								targetRoom.get(i).sendMsg(g.toJson(new FirstMsg(ROOM_PASSWORD, password.get(roomID))));
							for (Client target : targetRoom.values()) {
								target.sendMsg(msg);
								target.sendMsg(fmsg);
							}
							break;
						}
					}
				} else {
					String fmsg = g.toJson(new FirstMsg(SEND_SEAT, seat + "\n" + -1));
					for (Client target : targetRoom.values()) {
						target.sendMsg(msg);
						target.sendMsg(fmsg);
					}
				}
			}
			for (Client otherClient : list) {
				if (otherClient.getGameType() == 0) {
					otherClient.sendMsg(msg);
				}
			}
		}
	}
	public static void sendSeat(Client client,int roomID) {
		final HashMap<Integer,Client> targetRoom = room.get(roomID);
		synchronized (targetRoom) {
			StringBuilder sb = new StringBuilder();
			for (Client target : targetRoom.values()) {
				sb.append(target.getSeatNumber());
				sb.append("\n");
				sb.append(target.getMe().isReady());
				sb.append("\n");
			}
			String msg = g.toJson(new FirstMsg(SEND_SEAT, sb.toString()));
			client.sendMsg(msg);
			sb.setLength(0);
		}
	}
	public static void sendRoom(Client client) {
		StringBuilder sb1 = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		Collection<RoomInfo> allRoom;
		if (client.getGameType() == 0)
			allRoom = roomlist.values();
		else if (client.getGameType() == 1)
			allRoom = roomlistAudio.values();
		else allRoom = new ArrayList<>();
		//其他type类型开发中 
		for (RoomInfo roomInfo : allRoom) {
			if (roomInfo.getGaming() == 0) {
				sb1.append(g.toJson(roomInfo));
				sb1.append("\n");
			} else {
				sb2.append(g.toJson(roomInfo));
				sb2.append("\n");
			}
		}
		sb1.append(sb2);
		String msg = g.toJson(new FirstMsg(ROOM_LIST, sb1.toString()));
//		for (Client client : list) {
//			if(client.getGameType() == 0){
//				client.sendMsg(msg);
//			}
//		}
		client.sendMsg(msg);
	}
//	static class SkipRunnable implements Runnable {
//		@Override
//		public void run() {
//			// TODO Auto-generated method stub
//			while(true){
//				try {
//					Thread.sleep(15000);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
////				for(Client allClient : list){
////					allClient.sendMsg(g.toJson(new FirstMsg(SKIP, "")));
////				}
//				for(HashMap<Integer, Client> map:room.values()){
//					for(Client client:map.values()){
//						client.sendMsg(g.toJson(new FirstMsg(SKIP, "")));
//					}
//				}
//			}
//		}
//
//	}
}
