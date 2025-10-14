/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.api;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;

import java.util.stream.Stream;

/**
 * A utility class to expose parts of the Poke Notifier API or interact with Cobblemon.
 */
public class PokeNotifierApi {

    public static Stream<String> getAllPokemonNames() {
        return PokemonSpecies.INSTANCE.getSpecies().stream().map(species -> species.getResourceIdentifier().getPath());
    }
}