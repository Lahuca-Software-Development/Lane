/**
 * Developed and created by Lahuca Software Development.
 * <br>
 * Lahuca Software Development
 * Netherlands
 * <a href="lahuca.com">lahuca.com</a>
 * <a href="mailto:info@lahuca.com">info@lahuca.com</a>
 * KvK (Chamber of Commerce): 76521621
 * <br>
 * This file is originally created for Lane on 14-3-2024 at 17:38 UTC+1.
 * <br>
 * Lahuca Software Development owns all rights regarding the code.
 * Modifying, copying, nor publishing without Lahuca Software Development's consent is not allowed.
 * © Copyright Lahuca Software Development - 2024
 */
package com.lahuca.lanecontrollervelocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.logging.Logger;

@Plugin(id = "lanecontrollervelocity", name = "Lane Controller Velocity", version = "1.0",
		url = "https://lahuca.com", description = "I did it!", authors = {"Lahuca Software Development (Laurenshup)", "_Neko1"})
public class VelocityController {

	private static VelocityController instance;

	private final ProxyServer server;
	private final Logger logger;

	@Inject
	public VelocityController(ProxyServer server, Logger logger) {
		instance = this;
		this.server = server;
		this.logger = logger;

		logger.info("Hello there! I made my first plugin with Velocity.");
	}

	@Subscribe
	public void onJoin(PostLoginEvent event) {

	}

	public ProxyServer getServer() {
		return server;
	}

	public Logger getLogger() {
		return logger;
	}

	public static VelocityController getInstance() {
		return instance;
	}
}
