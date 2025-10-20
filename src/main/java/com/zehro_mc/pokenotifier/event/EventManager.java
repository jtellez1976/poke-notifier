/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.event;

import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Central event management system for Poke Notifier.
 * Coordinates all event systems including bounty, swarm, and scheduled tasks.
 */
public class EventManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventManager.class);
    private static final List<Runnable> PENDING_TASKS = new ArrayList<>();
    
    private static BountySystem bountySystem;
    private static SwarmSystem swarmSystem;
    
    /**
     * Initializes all event systems.
     * @param server The minecraft server instance
     */
    public static void initialize(MinecraftServer server) {
        bountySystem = new BountySystem();
        swarmSystem = new SwarmSystem();
        
        LOGGER.info("Event systems initialized successfully");
    }
    
    /**
     * Ticks all event systems. Called from server tick event.
     * @param server The minecraft server instance
     */
    public static void tick(MinecraftServer server) {
        // Process pending tasks
        if (!PENDING_TASKS.isEmpty()) {
            for (Runnable task : new ArrayList<>(PENDING_TASKS)) {
                task.run();
            }
            PENDING_TASKS.clear();
        }
        
        // Tick event systems
        if (bountySystem != null) {
            bountySystem.tick(server);
        }
        
        if (swarmSystem != null) {
            swarmSystem.tick(server);
        }
    }
    
    /**
     * Schedules a task to be executed on the next server tick.
     * @param task The task to schedule
     */
    public static void scheduleTask(Runnable task) {
        PENDING_TASKS.add(task);
    }
    
    /**
     * Gets the bounty system instance.
     * @return The bounty system
     */
    public static BountySystem getBountySystem() {
        return bountySystem;
    }
    
    /**
     * Gets the swarm system instance.
     * @return The swarm system
     */
    public static SwarmSystem getSwarmSystem() {
        return swarmSystem;
    }
    
    /**
     * Shuts down all event systems.
     */
    public static void shutdown() {
        PENDING_TASKS.clear();
        bountySystem = null;
        swarmSystem = null;
        LOGGER.info("Event systems shut down");
    }
}