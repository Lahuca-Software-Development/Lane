/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 14-3-2024 at 12:44 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.laneinstance;

public abstract class LaneGame extends GameInfo {

	public LaneGame(long gameId, String name, GameState gameState) {
		super(gameId, name, gameState);
	}

	public abstract void onStartup();
	public abstract void onShutdown();
	public abstract void onJoin(InstancePlayer instancePlayer);
	public abstract void onQuit(InstancePlayer instancePlayer);

	@Override
	public String toString() {
		return super.toString();
	}

}
