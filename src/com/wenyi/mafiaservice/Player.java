package com.wenyi.mafiaservice;

public class Player {
	private int role,ready = 0;
	private boolean alive;
	private Client me;
	public Player(Client me) {
		super();
		this.me = me;
	}
	public int getRole() {
		return role;
	}
	public void setRole(int role) {
		this.role = role;
	}
	public boolean isAlive() {
		return alive;
	}
	public void setAlive(boolean alive) {
		this.alive = alive;
	}
	public int isReady() {
		return ready;
	}
	public void setReady(int ready) {
		this.ready = ready;
	}
}
