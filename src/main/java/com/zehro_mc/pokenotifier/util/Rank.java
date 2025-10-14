package com.zehro_mc.pokenotifier.util;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public enum Rank {
    TRAINEE("[Trainee]", Formatting.GRAY, Identifier.of(PokeNotifier.MOD_ID, "textures/gui/cup0.png")),
    GREAT("[Great]", Formatting.GOLD, Identifier.of(PokeNotifier.MOD_ID, "textures/gui/cup1.png")),
    EXPERT("[Expert]", Formatting.AQUA, Identifier.of(PokeNotifier.MOD_ID, "textures/gui/cup2.png")),
    VETERAN("[Veteran]", Formatting.YELLOW, Identifier.of(PokeNotifier.MOD_ID, "textures/gui/cup3.png")),
    MASTER("[Master]", Formatting.LIGHT_PURPLE, Identifier.of(PokeNotifier.MOD_ID, "textures/gui/cup4_master.png"));

    public final Text displayText;
    public final Identifier icon;

    Rank(String text, Formatting color, Identifier icon) {
        this.displayText = Text.literal(text).formatted(color);
        this.icon = icon;
    }
}