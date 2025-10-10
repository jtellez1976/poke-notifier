package com.zehro_mc.pokenotifier;

public class ConfigServer {

    public String[] _instructions = new String[]{
            "Poke Notifier Configuration - Server-Only Settings",
            "This file contains settings that only affect the server.",
            " ",
            "debug_mode_enabled: If true, the server will print detailed informational logs to the console.",
            "enable_test_mode: If true, the notifier will also trigger for Pokémon spawned with commands (e.g., /pokespawn)."
    };

    public boolean debug_mode_enabled = false;
    public boolean enable_test_mode = false;
}