package com.wenyi.mafiaservice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import com.google.gson.Gson;

public class GameLogic implements Runnable {
	public static final int VILLAGER = 1,IDIOT = 10,THIEF = 11,WOLF = 2,SEER = 30,HUNTER = 31,WITCH = 32,GUARD = 33;
	public static final int NIGHT_START = 100,NIGHT_THIEF = 200,NIGHT_GUARD = 300,NIGHT_SEER = 400,NIGHT_WOLF = 500,NIGHT_WITCH = 600,
			DAY_START_CHIEF = 700,DAY_START_MEET = 800;
	private final int[] godRoles = {WITCH,HUNTER,GUARD,IDIOT,THIEF,SEER};
	private boolean hasMedicine = true,hasPoison = true,wolfKillSelf = false,gaming;
	//0：守人 1：验人 2：狼刀 3：是否救人 4：毒人
	private int[] select = {-1,-1,-1,-1,-1};
	private int gameProcedure,chiefIndex = -1,lastDeadIndex,day,max,aliveMens,aliveWolfs,voteMens,voteMax,sameVoteTimes = 0,exile;
	private RoomInfo roomInfo;
	private HashMap<Integer,Client> room;
	private HashMap<Integer,Integer> voteSituation = new HashMap<>();
	private Gson gson = new Gson();
	private ArrayList<Integer> allRole;
	private ArrayList<Integer> allAlive;
	private ArrayList<Integer> aliveWolfIndex;
	private Vector<Integer> candidate;
	private ArrayList<Integer> abandonList = new ArrayList<>();
	public void setWolfKillSelf(boolean wolfKillSelf,int seat) throws InterruptedException {
		this.wolfKillSelf = wolfKillSelf;
		this.exile = seat;
//		endSpeak(false, 0);
		sendToAll(Client.WOLF_IDIOCTONIA, String.valueOf(seat));
//		checkVote(false, 0);
	}
	public int getGameProcedure() {
		return gameProcedure;
	}
	public GameLogic(RoomInfo roomInfo,HashMap<Integer,Client> room) {
		// TODO Auto-generated constructor stub
		this.room = room;
		this.roomInfo = roomInfo;
		System.out.println("ID : "+roomInfo.getRoomID());
		max = roomInfo.getMens();
//		aliveMens = roomInfo.getCurrent();
		aliveMens = max;
		aliveWolfs = roomInfo.getWolfs();
		allRole = new ArrayList<>(max);
		allAlive = new ArrayList<>(max);
		aliveWolfIndex = new ArrayList<>(aliveWolfs+1);
		String[] gameRole = roomInfo.getRoleMsg().split("\n");
		boolean hasThief = false;
		for (int i = 0; i < 6; i++) {
			if("1".equals(gameRole[i])) {
				allRole.add(godRoles[i]);
				allAlive.add(1);
				if(i == 4) {
					hasThief = true;
				}
			}
		}
		for (int i = 0; i <= aliveWolfs; i++) {
			allRole.add(WOLF);
			allAlive.add(1);
		}
		for (int i = allRole.size(); i < max; i++) {
			allRole.add(VILLAGER);
			allAlive.add(1);
		}
		if(hasThief){
			allRole.add(VILLAGER);
			allRole.add(VILLAGER);
			allRole.trimToSize();
		}
		Collections.shuffle(allRole);
		//test code
//		allRole.add(HUNTER);
//		allAlive.add(1);
//		allRole.add(WOLF);
//		allAlive.add(1);
//		allRole.add(IDIOT);
//		allAlive.add(1);
//		for (int i = 3; i < max; i++) {
//			allRole.add(VILLAGER);
//			allAlive.add(1);
//		}
//		allRole.add(WITCH);
//		allRole.add(SEER);
		//test code end
		for (int i = 0; i < max; i++) {
			if(allRole.get(i) == WOLF) aliveWolfIndex.add(i);
		}
		aliveWolfs = aliveWolfIndex.size();
		gameProcedure = NIGHT_START;
		day = 0;
		gaming = true;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			int index;
			Client indexRole;
			while (gaming) {
				switch (gameProcedure) {
				case NIGHT_START:
					if(checkCurrent()){
						gaming = false;
						break;
					}
					Thread.sleep(1000);
					sendRole(gameProcedure);
					System.out.println("---1---");
					if (day == 0) {
						gameProcedure += 100;
					} else {
						//复原夜晚各种技能情况
						for (int i = 0; i < select.length; i++) {
							select[i] = -1;
						}
						gameProcedure += 200;
					}
					Thread.sleep(1000);
					break;
				case NIGHT_THIEF:
					if(checkCurrent()){
						gaming = false;
						break;
					}
					reSetVote(1);
					index = allRole.indexOf(THIEF);
					System.out.println("---2---");
					if (index == -1) {
						gameProcedure += 100;
						continue;
					} else {
						sendProcedure(gameProcedure);
						if (index < max && allAlive.get(index) == 1) {
							if (room.containsKey(index)) {
								indexRole = room.get(index);
								StringBuilder sb = new StringBuilder();
								int first = allRole.get(max), last = allRole.get(max + 1);
								sb.append(first);
								sb.append("\n");
								sb.append(last);
								indexRole.sendMsg(gson.toJson(new FirstMsg(Client.ROLE_NIGHT, sb.toString())));
								checkVote(true, 40);
								if (voteSituation.containsKey(index)) {
									allRole.remove(index);
									allRole.add(index, voteSituation.get(index));
								} else {
									allRole.remove(index);
									allRole.add(index, last == WOLF ? last : first);
								}
								if (allRole.get(index) == WOLF) {
									aliveWolfIndex.add(index);
									aliveWolfs++;
								}
								indexRole.sendMsg(gson.toJson(
										new FirstMsg(Client.GAME_SEND_ROLE, String.valueOf(allRole.get(index)))));
							} else {
								//send掉线信息
								allRole.remove(index);
								allRole.add(index, allRole.get(max) == WOLF || allRole.get(max + 1) == WOLF ? WOLF : allRole.get(max + 1));
							}
						}
					}
					gameProcedure += 100;
					Thread.sleep((new Random().nextInt(5)+5)*1000);
					break;
				case NIGHT_GUARD:
					if(checkCurrent()){
						gaming = false;
						break;
					}
					reSetVote(1);
					index = allRole.indexOf(GUARD);
					System.out.println("---3---");
					if (index == -1) {
						gameProcedure += 100;
						continue;
					}else {
						sendProcedure(gameProcedure);
						if(index < max && allAlive.get(index) == 1 && room.containsKey(index)){
							indexRole = room.get(index);
							indexRole.sendMsg(gson.toJson(new FirstMsg(Client.ROLE_NIGHT, "")));
							checkVote(true,40);
							select[0] = voteSituation.containsKey(index) ? voteSituation.get(index):-1;
							indexRole.sendMsg(gson.toJson(new FirstMsg(Client.CONFIRM_VOTE, String.valueOf(select[0]))));
						}
					}
					gameProcedure += 100;
					Thread.sleep((new Random().nextInt(5)+5)*1000);
					break;
				case NIGHT_SEER:
					if(checkCurrent()){
						gaming = false;
						break;
					}
					reSetVote(1);
					index = allRole.indexOf(SEER);
					System.out.println("---4---");
					if (index == -1) {
						gameProcedure += 100;
						continue;
					}else {
						System.out.println("----seer----");
						sendProcedure(gameProcedure);
						if(index < max && allAlive.get(index) == 1 && room.containsKey(index)){
							indexRole = room.get(index);
							indexRole.sendMsg(gson.toJson(new FirstMsg(Client.ROLE_NIGHT, "")));
							checkVote(true,40);
							int checkRole = voteSituation.containsKey(index) ? voteSituation.get(index) : -1;
							if(checkRole != -1) select[1] = allRole.get(checkRole);
							indexRole.sendMsg(gson.toJson(new FirstMsg(Client.CONFIRM_VOTE, select[1]+"\n"+checkRole)));
						}
					}
					gameProcedure += 100;
					Thread.sleep((new Random().nextInt(5)+5)*1000);
					break;
				case NIGHT_WOLF:
					if(checkCurrent()){
						gaming = false;
						break;
					}
					reSetVote(aliveWolfs);
					sendProcedure(gameProcedure);
					StringBuilder sBuilder = new StringBuilder();
					for (Integer wolf : aliveWolfIndex) {
						sBuilder.append(wolf);
						sBuilder.append("\n");
					}
					System.out.println("111222333"+aliveWolfs);
					for (int i = 0; i < aliveWolfs; i++) {
						System.out.println("111222333");
						if(room.containsKey(aliveWolfIndex.get(i)))
							room.get(aliveWolfIndex.get(i)).sendMsg(gson.toJson(new FirstMsg(Client.ROLE_NIGHT, sBuilder.toString())));
						else voteMax -- ;
					}
					if (voteMax > 0) {
						checkVote(true, 80);
						int[] arr = new int[max];
						for (int i = 0; i < aliveWolfs; i++) {
							int j = aliveWolfIndex.get(i);
							if (voteSituation.containsKey(j) && voteSituation.get(j) != -1) {
								arr[voteSituation.get(j)]++;
							}
						}
						int[] voteResult = countVoteTwo(arr);
						if (voteResult.length == 1) {
							select[2] = voteResult[0];
							System.out.println("---wolf---");
						}
						System.out.println("---wolf end---" + select[2]);
						for (int i = 0; i < aliveWolfs; i++) {
							if (room.containsKey(aliveWolfIndex.get(i)))
								room.get(aliveWolfIndex.get(i)).sendMsg(
										gson.toJson(new FirstMsg(Client.CONFIRM_VOTE, String.valueOf(select[2]))));
						}
						//检验胜利
						if(select[2] != -1 && select[2] != select[0] &&
								((!allRole.contains(WITCH) || allRole.indexOf(WITCH) >= max || allAlive.get(allRole.indexOf(WITCH)) != 1) || 
										(!hasMedicine || (select[2] == allRole.indexOf(WITCH) && roomInfo.getSaveSelfDays() <= day)))){
							allAlive.remove(select[2]);
							allAlive.add(select[2],0);
							if(aliveWolfIndex.contains(select[2]))
								aliveWolfs --;
							aliveMens --;
							int wres = checkIsOver();
							if(wres==0){
								allAlive.remove(select[2]);
								allAlive.add(select[2],1);
								if(aliveWolfIndex.contains(select[2]))
									aliveWolfs ++;
								aliveMens ++;
							}else {
								sendGameOver(wres);
								gaming = false;
								break;
							}
						}
					}else Thread.sleep((new Random().nextInt(20)+20)*1000);
					gameProcedure += 100;
					Thread.sleep((new Random().nextInt(5)+5)*1000);
					break;
				case NIGHT_WITCH:
					if(checkCurrent()){
						gaming = false;
						break;
					}
					index = allRole.indexOf(WITCH);
					System.out.println("-----witch-----"+index);
					if (index == -1) {
						gameProcedure += 100;
						continue;
					}else {
						sendProcedure(gameProcedure);
						if(index < max && allAlive.get(index) == 1 && room.containsKey(index)){
							indexRole = room.get(index);
//							indexRole.sendMsg(gson.toJson(new FirstMsg(Client.ROLE_NIGHT, "")));
							boolean useing = false;
							if (hasMedicine) {
								indexRole.sendMsg(gson.toJson(new FirstMsg(Client.TELL_WHO_DEAD, String.valueOf(select[2]))));
								//女巫是否能自救
								if (select[2] != index || roomInfo.getSaveSelfDays() > day) {
									reSetVote(1);
									checkVote(true, 40);
									select[3] = voteSituation.containsKey(index) ? voteSituation.get(index) : -1;
									// if(select[3] != -1) select[2] = -1;
									if (select[3] != -1) {
										useing = true;
										hasMedicine = false;
									}
								}else
									Thread.sleep((new Random().nextInt(3)+3)*1000);
							}else Thread.sleep((new Random().nextInt(3)+3)*1000);
							sendProcedure(gameProcedure+50);
							if(hasPoison && !useing){
								reSetVote(1);
								indexRole.sendMsg(gson.toJson(new FirstMsg(Client.ROLE_NIGHT, "")));
								checkVote(true,40);
								select[4] = voteSituation.containsKey(index) ? voteSituation.get(index) : -1;
								indexRole.sendMsg(
										gson.toJson(new FirstMsg(Client.CONFIRM_VOTE, select[3]+"\n"+select[4])));
								//检验胜利
								if(select[4] != -1){
									hasPoison = false;
									allAlive.remove(select[4]);
									allAlive.add(select[4],3);
									if(aliveWolfIndex.contains(select[4]))
										aliveWolfs --;
									aliveMens --;
									int wres = checkIsOver();
									if(wres==0){
										allAlive.remove(select[4]);
										allAlive.add(select[4],1);
										if(aliveWolfIndex.contains(select[4]))
											aliveWolfs ++;
										aliveMens ++;
									}else {
										sendGameOver(wres);
										gaming = false;
										break;
									}
								}
							}else Thread.sleep((new Random().nextInt(3)+3)*1000);
						}else {
							Thread.sleep((new Random().nextInt(5)+5)*1000);
							sendProcedure(gameProcedure+50);
						}
					}
					gameProcedure += 100;
					Thread.sleep((new Random().nextInt(3)+3)*1000);
					break;
				case DAY_START_CHIEF:
					if(checkCurrent()){
						gaming = false;
						break;
					}
					sendProcedure(gameProcedure);
					if(day++ == 0){
						reSetVote(max);
						sendToAll(Client.ELECTIONEERINGA, "");
						checkVote(true,30);
						candidate = new Vector<>();
						StringBuilder sb = new StringBuilder();
						for (int i = 0; i < max; i++) {
							if(voteSituation.containsKey(i) && voteSituation.get(i) != -1){
								candidate.add(i);
								sb.append(i);
								sb.append("\n");
							}
						}
						int eleMens = candidate.size();
//						voteMax -= eleMens;
//						voteMens = 0;
						String candidateMsg = sb.length() == 0? "non" : sb.toString();
						sendToAll(Client.CANDIDATE, candidateMsg);
						sb.setLength(0);
						if(eleMens == 0 || eleMens == max){
							gameProcedure += 100;
							continue;
						}else if (eleMens == 1) {
							chiefIndex = candidate.get(0);
							gameProcedure += 100;
							continue;
						}else {
//							voteMax = eleMens;
//							voteMens = 0;
							sendToAll(Client.CANDIDATE_SPEAK, "1");
							for (int i = 0; i < eleMens; i++) {
								Thread.sleep(5*1000);
								if(room.containsKey(candidate.get(i)) && !abandonList.contains(candidate.get(i))){
									sendToAll(Client.SPEAK, candidate.get(i)+"\n0");
								}else {
									Thread.sleep(5*1000);
									continue;
								}
								endSpeak(true, 130);
//								voteMens = 0;
							}
							Thread.sleep(5*1000);
							sendToAll(Client.CANDIDATE_SPEAK, "0");
							//判断剩余参选人数
							candidate.removeAll(abandonList);
							eleMens = candidate.size();
							if(eleMens == 0){
								sendToAll(Client.CANDIDATE, "non");
								gameProcedure += 100;
								continue;
							}else if(eleMens == 1){
								chiefIndex = candidate.get(0);
								sendToAll(Client.CANDIDATE, String.valueOf(chiefIndex));
								gameProcedure += 100;
								continue;
							}else {
								for (int i = 0; i < max; i++) {
									if(voteSituation.containsKey(i) && voteSituation.get(i) != -1){
										candidate.add(i);
										sb.append(i);
										sb.append("\n");
									}
								}
								sendToAll(Client.CANDIDATE, sb.toString());
								reSetVote(max - eleMens);
								sendToAll(Client.DAY_VOTE, "");
								checkVote(true, 30);
								//统计选票
								int[] allv = countVoteOne(chiefIndex);
								if(allv == null)
									//无人投票，无警长
									sendToAll(Client.VOTE_INVALID, "non");
								else {
									//发送票型详情
									sendVoteSituation(allv);
									//计算票型
									int[] vResult = countVoteTwo(allv);
									chiefIndex = checkVoteEnding(vResult);
									if(chiefIndex != -1){
										sendToAll(Client.CHIEF_ELECTED, String.valueOf(chiefIndex));
									}
								}
							}
						}
					}
					gameProcedure += 100;
//					Thread.sleep((new Random().nextInt(5)+5)*1000);
					break;
				case DAY_START_MEET:
					if(checkCurrent()){
						gaming = false;
						break;
					}
					sendProcedure(gameProcedure);
					exile = -1;
					//返回值为死亡玩家的座位号
					ArrayList<Integer> deadNum = checkAllNightSelect();
					int result = checkIsOver();
					if(result != 0){
						sendGameOver(result);
						gaming = false;
						break;
					}else {
						//判断死者是否发动技能，猎人被毒不能开枪
						if (!deadNum.isEmpty()) {
							for (Integer deadMen : deadNum) {
								int shoot = checkSkill(deadMen, 0);
								if(shoot != -1){
									int res = checkIsOver();
									if(res > 0){
										sendGameOver(res);
										gaming = false;
										break;
									}
								}
								if(day <= roomInfo.getLastWordDays())
									speakLastWord(deadMen);
								if(deadMen == chiefIndex)
									transferChief();
								if(shoot != -1){
									speakLastWord(shoot);
									if(shoot == chiefIndex)
										transferChief();
								}
							}
							if(!gaming) break;
						}
						//继续游戏,判断是否有遗言
//						if (!deadNum.isEmpty() && day <= roomInfo.getLastWordDays()) {
//							for (Integer lastWord : deadNum) {
//								speakLastWord(lastWord);
//							}
//						}
						//移交警徽
//						if(deadNum.contains(chiefIndex)){
//							reSetVote(1);
//							sendToAll(Client.TRANSFER_CHIEF,"");
//							if (room.containsKey(chiefIndex)) {
//								checkVote(true, 30);
//								if (voteSituation.containsKey(chiefIndex) && voteSituation.get(chiefIndex) != -1) {
//									int temp = voteSituation.get(chiefIndex);
//									chiefIndex = temp;
//								} else
//									chiefIndex = -1;
//							}else 
//								chiefIndex = -1;
//							String chiefMsg = String.valueOf(chiefIndex);
//							sendToAll(Client.TRANSFER_CHIEF, chiefMsg);
//						}
						//发言逻辑,狼人自爆则直接进入黑夜
						sendProcedure(gameProcedure+50);
						reSetVote(aliveMens);
						int speaker;
						if (chiefIndex == -1)
							speaker = lastDeadIndex;
						else
							speaker = chiefIndex;
						for (int i = 0; i < max; i++) {
							speaker ++ ;
							if(speaker >= max) speaker -= max;
							if(allAlive.get(speaker) == 1){
								if(room.containsKey(speaker)){
									Thread.sleep(3*1000);
									if(wolfKillSelf) 
										break;
									String speakerMsg = speaker+"\n0";
									sendToAll(Client.SPEAK, speakerMsg);
									endSpeak(true, 130);
								} else {
									voteMax -- ;
									Thread.sleep(1000);
								}
							}
						}
						Thread.sleep(5*1000);
						//统计投票，结束后进入黑夜
						sendProcedure(gameProcedure);
						if(!wolfKillSelf){
							sendToAll(Client.DAY_VOTE, "");
							checkVote(true, 30);
							int[] aVote = countVoteOne(chiefIndex);
							if(aVote == null)
								//无人投票，无警长
								sendToAll(Client.VOTE_INVALID, "non");
							else {
								//发送票型详情
								sendVoteSituation(aVote);
								//计算票型
								int[] vResult = countVoteTwo(aVote);
								exile = checkVoteEnding(vResult);
							}
							String exileMsg = exile+"\n"+String.valueOf(-2);
							sendToAll(Client.TELL_WHO_DEAD, exileMsg);
						}
						//遗言发表
						if(exile != -1){
							//dead method
							if(allRole.get(exile) != IDIOT){
								whoDeath(exile,2);
								int res1 = checkIsOver();
								if(res1 > 0){
									sendGameOver(res1);
									gaming = false;
									break;
								}
							}
							int shoot = checkSkill(exile, 1);
							int res2 = checkIsOver();
							if(res2 > 0){
								sendGameOver(res2);
								gaming = false;
								break;
							}
							if(shoot != -1 && shoot == chiefIndex)
								transferChief();
							if(day <= roomInfo.getLastWordDays() && allRole.get(exile) != IDIOT)
								speakLastWord(exile);
//							allAlive.remove(exile);
//							allAlive.add(exile, 0);
//							aliveMens--;
						}
						gameProcedure = NIGHT_START;
						voteSituation.clear();
						Thread.sleep((new Random().nextInt(5)+5)*1000);
					}
					break;
				default:
					gaming = false;
					break;
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Client.game.remove(this);
		LoginServer.peerServer.gameOver(roomInfo.getRoomID());
	}
	public void transferChief() throws InterruptedException{
		reSetVote(1);
		sendToAll(Client.TRANSFER_CHIEF,"");
		if (room.containsKey(chiefIndex)) {
			checkVote(true, 30);
			if (voteSituation.containsKey(chiefIndex) && voteSituation.get(chiefIndex) != -1) {
				int temp = voteSituation.get(chiefIndex);
				chiefIndex = temp;
			} else
				chiefIndex = -1;
		}else 
			chiefIndex = -1;
		String chiefMsg = String.valueOf(chiefIndex);
		sendToAll(Client.TRANSFER_CHIEF, chiefMsg);
	}
	public boolean checkCurrent(){
		if(room.size() == 0)
			return true;
		return false;
	}
	public void sendToAll(int type,String msg){
		for (Client target : room.values()) {
			target.sendMsg(gson.toJson(new FirstMsg(type, msg)));
		}
	}
	//判断游戏结束 0： 未结束 2：狼人胜利 1：平民胜利
	public int checkIsOver(){
		//没有狼则平民赢
		if(aliveWolfs == 0) return 1;
		//狼队绑票能平票、平民或神死完则狼赢,
		if(aliveWolfs * 2 > aliveMens || checkAliveGood()) return 2;
		//不绑票
//		if(checkAliveGood()) return 2;
		//
		return 0;
	}
	public void sendGameOver(int result){
		System.out.println("-----game over-----"+result);
		//游戏结束
		StringBuilder sb = new StringBuilder();
		sb.append(result);
		sb.append("\n");
		for (Integer i : allRole) {
			sb.append(i);
			sb.append("\n");
		}
		sendToAll(Client.GAME_OVER, sb.toString());
	}
	public boolean checkAliveGood(){
		boolean v = true,g = true;
		for (int i = 0;i < max;i++){
			if(allRole.get(i) == VILLAGER && allAlive.get(i) == 1) v = false;
			if(allRole.get(i) >= IDIOT && allAlive.get(i) == 1) g = false;
			if(!v && !g) return false;
		}
		if(v || g) 
			return true;
		else return false;
	}
	//重置投票情况
	public void reSetVote(int max){
		voteSituation.clear();
		voteMax = max;
		voteMens = 0;
	}
	//发表遗言
	public void speakLastWord(int seat) throws InterruptedException{
		String seatMsg = seat+"\n1";
		sendToAll(Client.SPEAK, seatMsg);
		if(room.containsKey(seat))
			endSpeak(true, 130);
		else endSpeak(true, 3);
	}
	//判断死者能否发动技能
	public int checkSkill(int seat, int stage) throws InterruptedException {
		String seatMsg = String.valueOf(seat);
		switch (allRole.get(seat)) {
		case HUNTER:
			if (select[4] != seat) {
				reSetVote(1);
				sendToAll(Client.HUNTER_SKILL, seatMsg);
				int dead = -1;
				if(room.containsKey(seat))
					checkVote(true, 20);
				else Thread.sleep(3000);
				if(voteSituation.containsKey(seat)){
					dead = voteSituation.get(seat);
					seatMsg = seat + "\n" + dead;
					if(dead != -1)
						whoDeath(dead,4);
				}else {
					seatMsg = seat + "\n" + -1;
				}
				sendToAll(Client.HUNTER_SKILL, seatMsg);
				return dead;
			}
			break;
		case IDIOT:
			if (stage == 1) {
//				reSetVote(1);
				sendToAll(Client.IDIOT_SKILL, seatMsg);
				allAlive.remove(seat);
				allAlive.add(seat, 1);
				aliveMens++;
			}
			break;
		}
		return -1;
	}
	//统计、发送夜晚死人消息
	//0：守人 1：验人 2：狼刀 3：是否救人 4：毒人
	public ArrayList<Integer> checkAllNightSelect(){
		ArrayList<Integer> dead = new ArrayList<>(2);
		if(select[2] != -1){
			if(select[0] == select[2] ^ select[3] == -1){
				//死
				dead.add(select[2]);
				whoDeath(select[2],0);
				lastDeadIndex = select[2];
			}
		}
		if(select[4] != -1 && select[4] != select[2]){
			dead.add(select[4]);
			whoDeath(select[4],3);
			if(select[4] > select[2]) lastDeadIndex = select[4];
		}
		if(dead.isEmpty())
			//平安夜
			sendToAll(Client.TELL_WHO_DEAD, "-1");
		else {
			StringBuilder sb = new StringBuilder();
			for (Integer deadSeat : dead) {
				sb.append(deadSeat);
				sb.append("\n");
			}
			//发送死亡讯息
			sendToAll(Client.TELL_WHO_DEAD, sb.toString());
		}
		return dead;
	}
	//deathWay 0：刀 1：活着 2：放逐 3：毒死 4：猎人打死
	public void whoDeath(int seat,int way){
		allAlive.remove(seat);
		allAlive.add(seat, way);
		aliveMens--;
		if(allRole.get(seat) == WOLF) {
			aliveWolfIndex.remove((Integer)seat);
			aliveWolfs = aliveWolfIndex.size();
		}
	}
	//游戏开始时发送身份信息给每个用户
	private void sendRole(int procedure){
		for (Client target : room.values()) {
			//告诉服务器的线程和客户端个人身份信息
			if (day == 0) {
				target.getMe().setRole(allRole.get(target.getSeatNumber()));
				target.sendMsg(gson.toJson(new FirstMsg(Client.GAME_SEND_ROLE, String.valueOf(allRole.get(target.getSeatNumber())))));
			}
			target.sendMsg(gson.toJson(new FirstMsg(Client.ALL_NIGHT, String.valueOf(procedure))));
		}
	}
	private void sendProcedure(int procedure){
		for (Client target : room.values()) {
			target.sendMsg(gson.toJson(new FirstMsg(Client.ALL_NIGHT, String.valueOf(procedure))));
		}
	}
	public void upDateVote(int seatNumber,int vote) throws InterruptedException{
		voteSituation.put(seatNumber, vote);
		if(++voteMens == voteMax) checkVote(false,0);
	}
	public void abandonElection(int seat){
//		candidate.remove((Integer)seat);
		abandonList.add(seat);
	}
	public synchronized void endSpeak(boolean tag,int second) throws InterruptedException{
		if(tag){
			this.wait(second*1000);
			sendToAll(Client.END_SPEAK, "");
		}else{
			this.notify();
		}
	}
	public synchronized void checkVote(boolean tag,int second) throws InterruptedException{
		if(tag){
			this.wait(second*1000);
		}else{
			this.notify();
		}
	}
	public void wolfTalk(String msg){
		for (int i = 0; i < aliveWolfs; i++) {
			if(room.containsKey(aliveWolfIndex.get(i)) && allAlive.get(aliveWolfIndex.get(i)) == 1)
				room.get(aliveWolfIndex.get(i)).sendMsg(gson.toJson(new FirstMsg(Client.READY_TALK, msg)));
		}
	}
	public void sendVoteSituation(int[] allv){
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < max; i++) {
			if(allv[i] != 0){
				sb.append(i);
				sb.append("\n");
				for (int j = 0; j < max; j++) {
					if(voteSituation.containsKey(j) && voteSituation.get(j) == i){
						sb.append(j);
						sb.append("\n");
					}
				}
				sendToAll(Client.VOTE_SITUATION, sb.toString());
				sb.setLength(0);
			}
		}
	}
	//统计投票结果，返回投票结果的int数组，下标为座位号，值为票数
	public int[] countVoteOne(int chief){
		boolean tag = false;
		int[] allv = new int[max];
		for (int i = 0; i < max; i++) {
			if(voteSituation.containsKey(i) && voteSituation.get(i) != -1){
				allv[voteSituation.get(i)]++;
				if(chief == i)
					allv[voteSituation.get(i)]++;
				tag = true;
			}
		}
		if(tag)
			return allv;
		else return null;
	}
	//arr数组  index 为座位号   数组为   上票数
	//return 的数组为最高票的人的座位号组
	public int[] countVoteTwo(int... arr){
		int max = Integer.MIN_VALUE;
		int a = 1;
		for (int i = 0; i < arr.length; i++) {
			if(arr[i]>max){
				max = arr[i];
				arr[0] = i;
				a = 1;
			}else if(arr[i] == max){
				arr[a++] = i;
			}
		}
		int[] reVote = new int[a];
		for (int i = 0; i < a; i++) {
			reVote[i] = arr[i];
		}
		return reVote;
	}
	//统计警长投票，平票则递归循环，最多递归一次
	public int checkVoteEnding(int... vResult) throws InterruptedException {
		StringBuilder sb = new StringBuilder();
		if (vResult.length == 1) {
//			for (Client target : room.values()) {
//				target.sendMsg(gson.toJson(new FirstMsg(Client.CHIEF_ELECTED, String.valueOf(vResult[0]))));
////				chiefIndex = vResult[0];
//				//投票结束
//			}
//			sendToAll(Client.CHIEF_ELECTED, String.valueOf(vResult[0]));
			return vResult[0];
		} else if(vResult.length > 1){
			sameVoteTimes++;
			for (int can : vResult) {
				sb.append(can);
				sb.append("\n");
			}
			// 发送平票玩家信息
			sendToAll(Client.CANDIDATE, sb.toString());
			for (int can : vResult) {
				Thread.sleep(5 * 1000);
				String canMsg = can+"\n0";
				sendToAll(Client.SPEAK, canMsg);
				checkVote(true, 130);
			}
			voteSituation.clear();
			voteMens = 0;
			sendToAll(Client.DAY_VOTE, "");
			checkVote(true, 30);
			if(sameVoteTimes == 2){
				//投票失效
				sendToAll(Client.VOTE_INVALID, "same");
				sameVoteTimes = 0;
				return -1;
			}else {
				//统计选票
				int[] allv = countVoteOne(chiefIndex);
				//发送票型详情
				sendVoteSituation(allv);
				//计算票型
				int[] vRecursion = countVoteTwo(allv);
				return checkVoteEnding(vRecursion);
			}
		}
		return -1;
	}
}