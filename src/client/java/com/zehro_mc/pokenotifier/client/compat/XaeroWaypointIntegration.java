/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client.compat;

import net.minecraft.client.MinecraftClient;

public class XaeroWaypointIntegration {
    private static boolean xaeroLoaded = false;
    
    static {
        try {
            Class.forName("xaero.hud.minimap.BuiltInHudModules");
            xaeroLoaded = true;
        } catch (ClassNotFoundException e) {
            xaeroLoaded = false;
        }
    }
    
    public static boolean isXaeroLoaded() {
        return xaeroLoaded;
    }
    
    public static boolean addWaypoint(String name, int x, int y, int z) {
        if (!xaeroLoaded) return false;
        
        try {
            return XaeroWaypointHelper.addWaypoint(name, x, y, z);
        } catch (Exception e) {
            return false;
        }
    }
    
    private static class XaeroWaypointHelper {
        public static boolean addWaypoint(String name, int x, int y, int z) {
            try {
                // Use reflection to avoid compile-time dependencies
                Class<?> builtInHudModulesClass = Class.forName("xaero.hud.minimap.BuiltInHudModules");
                Object minimapModule = builtInHudModulesClass.getField("MINIMAP").get(null);
                Object minimapSession = minimapModule.getClass().getMethod("getCurrentSession").invoke(minimapModule);
                
                if (minimapSession == null) return false;
                
                Object worldManager = minimapSession.getClass().getMethod("getWorldManager").invoke(minimapSession);
                Object currentWorld = worldManager.getClass().getMethod("getCurrentWorld").invoke(worldManager);
                
                if (currentWorld == null) return false;
                
                Object currentWaypointSet = currentWorld.getClass().getMethod("getCurrentWaypointSet").invoke(currentWorld);
                if (currentWaypointSet == null) return false;
                
                // Create waypoint using reflection
                Class<?> waypointColorClass = Class.forName("xaero.hud.minimap.waypoint.WaypointColor");
                Object greenColor = waypointColorClass.getField("GREEN").get(null);
                
                Class<?> waypointPurposeClass = Class.forName("xaero.hud.minimap.waypoint.WaypointPurpose");
                Object normalPurpose = waypointPurposeClass.getField("NORMAL").get(null);
                
                Class<?> waypointClass = Class.forName("xaero.common.minimap.waypoints.Waypoint");
                Object waypoint = waypointClass.getConstructor(
                    int.class, int.class, int.class, String.class, String.class,
                    waypointColorClass, waypointPurposeClass
                ).newInstance(
                    x, y, z, name, name.substring(0, Math.min(2, name.length())),
                    greenColor, normalPurpose
                );
                
                currentWaypointSet.getClass().getMethod("add", waypointClass).invoke(currentWaypointSet, waypoint);
                
                // Refresh waypoints
                try {
                    Class<?> supportModsClass = Class.forName("xaero.map.mods.SupportMods");
                    Object xaeroMinimap = supportModsClass.getField("xaeroMinimap").get(null);
                    xaeroMinimap.getClass().getMethod("requestWaypointsRefresh").invoke(xaeroMinimap);
                } catch (Exception ignored) {
                    // Refresh method might not exist in all versions
                }
                
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}