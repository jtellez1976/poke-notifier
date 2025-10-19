package com.zehro_mc.pokenotifier.client.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.zehro_mc.pokenotifier.client.FallbackConfigScreen;
import com.zehro_mc.pokenotifier.client.PokeNotifierCustomScreen; // CAMBIAR IMPORT

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            try {
                return new PokeNotifierCustomScreen(parent); // CAMBIAR AQUÍ
            } catch (Exception e) {
                return new FallbackConfigScreen(parent, e.getMessage());
            }
        };
    }
}