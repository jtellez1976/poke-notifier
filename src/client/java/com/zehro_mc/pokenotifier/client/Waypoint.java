package com.zehro_mc.pokenotifier.client;

import net.minecraft.util.math.BlockPos;

public class Waypoint {
    private final String id;
    private final String name;
    private final BlockPos pos;
    private final int color;

    public Waypoint(String id, String name, BlockPos pos, int color) {
        this.id = id;
        this.name = name;
        this.pos = pos;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getColor() {
        return color;
    }
}