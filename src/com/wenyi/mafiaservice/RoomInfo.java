package com.wenyi.mafiaservice;

public class RoomInfo {
	String roomName,roomMaster,roleMsg;
	int mens,useAudio,roomID,current,gods,wolfs,saveSelfDays,lastWordDays,hasPassword,gaming;
	public RoomInfo() {
		// TODO Auto-generated constructor stub
	}
	public RoomInfo(String name,String master,String roleMsg,int mens,int useAudio,int roomID,int current,int gods,int wolfs,
			int saveSelfDays,int lastWordDays,int hasPassword,int gaming) {
		// TODO Auto-generated constructor stub
		super();
		this.roomName = name;
		this.roomMaster = master;
		this.roleMsg = roleMsg;
		this.mens = mens;
		this.useAudio = useAudio;
		this.roomID = roomID;
		this.current = current;
		this.gods = gods;
		this.wolfs = wolfs;
		this.saveSelfDays = saveSelfDays;
		this.lastWordDays = lastWordDays;
		this.hasPassword = hasPassword;
		this.gaming = gaming;
	}
	public String getRoomName() {
		return roomName;
	}
	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}
	public int getMens() {
		return mens;
	}
	public void setMens(int mens) {
		this.mens = mens;
	}
	public int getUseAudio() {
		return useAudio;
	}
	public void setUseAudio(int useAudio) {
		this.useAudio = useAudio;
	}
	public String getRoomMaster() {
		return roomMaster;
	}
	public void setRoomMaster(String roomMaster) {
		this.roomMaster = roomMaster;
	}
	public int getRoomID() {
		return roomID;
	}
	public void setRoomID(int roomID) {
		this.roomID = roomID;
	}
	public int getCurrent() {
		return current;
	}
	public void setCurrent(int current) {
		this.current = current;
	}
	public String getRoleMsg() {
		return roleMsg;
	}
	public void setRoleMsg(String roleMsg) {
		this.roleMsg = roleMsg;
	}
	public int getGods() {
		return gods;
	}
	public void setGods(int gods) {
		this.gods = gods;
	}
	public int getWolfs() {
		return wolfs;
	}
	public void setWolfs(int wolfs) {
		this.wolfs = wolfs;
	}
	public int getSaveSelfDays() {
		return saveSelfDays;
	}
	public void setSaveSelfDays(int saveSelfDays) {
		this.saveSelfDays = saveSelfDays;
	}
	public int getLastWordDays() {
		return lastWordDays;
	}
	public void setLastWordDays(int lastWordDays) {
		this.lastWordDays = lastWordDays;
	}
	public int getHasPassword() {
		return hasPassword;
	}
	public void setHasPassword(int hasPassword) {
		this.hasPassword = hasPassword;
	}
	public int getGaming() {
		return gaming;
	}
	public void setGaming(int gaming) {
		this.gaming = gaming;
	}
}
