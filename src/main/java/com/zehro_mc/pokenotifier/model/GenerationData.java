package com.zehro_mc.pokenotifier.model;

import java.util.HashSet;
import java.util.Set;

public class GenerationData {
    // Este campo corresponderá a "region": "kanto" en el JSON
    public String region = "Unknown";
    // Este campo corresponderá a "pokemon": [...] en el JSON
    public Set<String> pokemon = new HashSet<>();
}