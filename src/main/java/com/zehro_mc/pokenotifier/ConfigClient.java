package com.zehro_mc.pokenotifier;

public class ConfigClient {

    public String[] _instructions = new String[]{
            "Poke Notifier Configuration - Client Settings",
            "This file defines client-side preferences and general mod settings.",
            "notification_distance: The maximum distance in blocks a player can be from a Pokémon to receive a notification."
    };

    // --- General Settings ---
    public int notification_distance = 200;
    public int glowing_duration_seconds = 120;

    // --- Client-side Alerts (from future steps) ---
    public boolean alert_sounds_enabled = true; // Step 5
    public boolean alert_toast_enabled = true; // Step 6
    public boolean alert_chat_enabled = true; // Nueva opción para el chat
    public boolean silent_mode_enabled = false;
    public boolean searching_enabled = true;
}