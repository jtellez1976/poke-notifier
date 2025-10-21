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
            boolean result = XaeroWaypointHelper.addWaypoint(name, x, y, z);
            
            // If successful, always register for tracking (all mod-created waypoints)
            if (result) {
                WaypointTracker.registerWaypointByLocation(x, y, z, name);
            }
            
            return result;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean removeWaypoint(String name) {
        if (!xaeroLoaded) return false;
        
        try {
            return XaeroWaypointHelper.removeWaypoint(name);
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
                refreshWaypoints();
                
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        
        public static boolean removeWaypoint(String name) {
            try {
                Class<?> builtInHudModulesClass = Class.forName("xaero.hud.minimap.BuiltInHudModules");
                Object minimapModule = builtInHudModulesClass.getField("MINIMAP").get(null);
                Object minimapSession = minimapModule.getClass().getMethod("getCurrentSession").invoke(minimapModule);
                
                if (minimapSession == null) return false;
                
                Object worldManager = minimapSession.getClass().getMethod("getWorldManager").invoke(minimapSession);
                Object currentWorld = worldManager.getClass().getMethod("getCurrentWorld").invoke(worldManager);
                
                if (currentWorld == null) return false;
                
                Object currentWaypointSet = currentWorld.getClass().getMethod("getCurrentWaypointSet").invoke(currentWorld);
                if (currentWaypointSet == null) return false;
                
                // Try multiple methods to get waypoints list
                Object waypointsList = null;
                try {
                    waypointsList = currentWaypointSet.getClass().getMethod("getList").invoke(currentWaypointSet);
                } catch (Exception e1) {
                    try {
                        waypointsList = currentWaypointSet.getClass().getMethod("getWaypoints").invoke(currentWaypointSet);
                    } catch (Exception e2) {
                        try {
                            waypointsList = currentWaypointSet.getClass().getMethod("iterator").invoke(currentWaypointSet);
                        } catch (Exception e3) {
                            return false;
                        }
                    }
                }
                
                if (waypointsList instanceof java.util.List<?> list) {
                    java.util.Iterator<?> iterator = list.iterator();
                    while (iterator.hasNext()) {
                        Object waypoint = iterator.next();
                        try {
                            String waypointName = (String) waypoint.getClass().getMethod("getName").invoke(waypoint);
                            if (name.equals(waypointName)) {
                                // Try different removal methods
                                try {
                                    iterator.remove();
                                } catch (Exception e1) {
                                    try {
                                        currentWaypointSet.getClass().getMethod("remove", waypoint.getClass()).invoke(currentWaypointSet, waypoint);
                                    } catch (Exception e2) {
                                        list.remove(waypoint);
                                    }
                                }
                                refreshWaypoints();
                                return true;
                            }
                        } catch (Exception e) {
                            // Skip this waypoint if we can't get its name
                            continue;
                        }
                    }
                } else if (waypointsList instanceof java.util.Iterator<?> iterator) {
                    while (iterator.hasNext()) {
                        Object waypoint = iterator.next();
                        try {
                            String waypointName = (String) waypoint.getClass().getMethod("getName").invoke(waypoint);
                            if (name.equals(waypointName)) {
                                iterator.remove();
                                refreshWaypoints();
                                return true;
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }
                }
                
                return false;
            } catch (Exception e) {
                return false;
            }
        }
        
        private static void refreshWaypoints() {
            try {
                Class<?> supportModsClass = Class.forName("xaero.map.mods.SupportMods");
                Object xaeroMinimap = supportModsClass.getField("xaeroMinimap").get(null);
                if (xaeroMinimap != null) {
                    xaeroMinimap.getClass().getMethod("requestWaypointsRefresh").invoke(xaeroMinimap);
                }
            } catch (Exception ignored) {
                // Refresh method might not exist in all versions
            }
        }
    }
}